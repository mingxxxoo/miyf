import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import {
  Binding,
  Dish,
  OrderDraftAiItem,
  OrderDraftAiUnmatched,
  stockLabel
} from '../../api/Models'
import { DraftStore } from '../../session/DraftStore'
import { AppNav } from '../../session/AppNav'
import { AiInsightStore } from '../../session/AiInsightStore'

@Component
export struct HomeTab {
  @State kitchenName: string = '厨房'
  @State dishes: Dish[] = []
  @State loading: boolean = true
  @State error: string = ''
  @State draftQty: number = 0
  @State aiOpen: boolean = false
  @State aiText: string = ''
  @State aiBusy: boolean = false
  @State matched: OrderDraftAiItem[] = []
  @State unmatched: OrderDraftAiUnmatched[] = []
  @State aiTip: string = ''

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      const binding: Binding | null = await MiyfApi.fetchMyBinding()
      if (binding === null || binding.status !== 'BOUND') {
        router.replaceUrl({ url: 'pages/Join' })
        return
      }
      this.kitchenName = binding.kitchenName || '厨房'
      this.dishes = await MiyfApi.fetchRecommendDishes(6)
      this.draftQty = DraftStore.totalQty()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private lowStockTip(): string {
    const low = this.dishes.find((d: Dish) => d.stockType !== 'UNLIMITED' && (d.stock ?? 99) > 0 && (d.stock ?? 0) <= 3)
    if (low === undefined) {
      return ''
    }
    return low.name + '今日还剩 ' + (low.stock ?? 0) + ' 份，想吃抓紧预约'
  }

  private openAi(): void {
    this.aiOpen = true
    this.matched = []
    this.unmatched = []
    this.aiTip = ''
  }

  private closeAi(): void {
    this.aiOpen = false
    this.aiBusy = false
  }

  private async runAiDraft(): Promise<void> {
    const text = this.aiText.trim()
    if (text.length === 0) {
      promptAction.showToast({ message: '请先说一句想吃的' })
      return
    }
    this.aiBusy = true
    this.aiTip = ''
    try {
      const result = await MiyfApi.draftOrderFromText(text)
      this.matched = result.items
      this.unmatched = result.unmatched
      if (result.degraded && result.items.length === 0) {
        this.aiTip = 'AI 暂不可用，请手动选菜'
        promptAction.showToast({ message: this.aiTip })
        return
      }
      if (result.items.length === 0) {
        this.aiTip = result.unmatched.length > 0
          ? (result.unmatched[0].reason || '没匹配到在售菜')
          : (result.ambiguityNote || '没匹配到在售菜品，请手动选菜')
        promptAction.showToast({ message: this.aiTip.substring(0, 40) })
        return
      }
      if (result.ambiguityNote !== undefined && result.ambiguityNote.length > 0) {
        this.aiTip = result.ambiguityNote
      }
    } finally {
      this.aiBusy = false
    }
  }

  private async applyAiDraft(): Promise<void> {
    if (this.matched.length === 0) {
      promptAction.showToast({ message: '请先生成匹配结果' })
      return
    }
    const text = this.aiText.trim()
    DraftStore.clear()
    this.matched.forEach((item: OrderDraftAiItem) => {
      DraftStore.setQty(item.dishId, item.dishName, item.quantity)
    })
    this.draftQty = DraftStore.totalQty()
    AiInsightStore.clear()
    const draftJson = JSON.stringify({ items: this.matched, unmatched: this.unmatched })
    const insight = await MiyfApi.analyzeOrderInsight(text, draftJson)
    if (insight !== null) {
      AiInsightStore.set(insight, text)
    }
    const unmatchedHint = this.unmatched.length > 0 ? '，' + this.unmatched.length + ' 项未匹配' : ''
    promptAction.showToast({ message: '已生成草稿 ' + this.matched.length + ' 样' + unmatchedHint })
    this.closeAi()
    this.aiText = ''
    this.matched = []
    this.unmatched = []
    AppNav.goTab(2)
  }

  build() {
    Stack() {
      Column() {
        Row() {
          Text('首页')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.ink)
          Blank()
          Row({ space: 5 }) {
            Column()
              .width(7)
              .height(7)
              .borderRadius(99)
              .backgroundColor(Theme.orange)
            Text('胡闹厨房')
              .fontSize(11)
              .fontWeight(FontWeight.Medium)
              .fontColor(Theme.ink)
          }
          .padding({ left: 12, right: 12, top: 5, bottom: 5 })
          .borderRadius(99)
          .backgroundColor(Theme.white)
          .border({ width: 1, color: Theme.line })
          .onClick(() => {
            router.pushUrl({ url: 'pages/health/HealthHome' })
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 4 })

        if (this.loading) {
          Text('加载中…').fontColor(Theme.muted).margin({ top: 40 })
        } else if (this.error.length > 0) {
          Column({ space: 12 }) {
            Text(this.error).fontColor(Theme.red)
            Button('重试').onClick(() => {
              void this.reload()
            })
          }
          .margin({ top: 40 })
        } else {
          Scroll() {
            Column() {
              Text(this.kitchenName + ' · 已加入')
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(Theme.orange)
                .padding({ left: 14, right: 14, top: 6, bottom: 6 })
                .borderRadius(99)
                .backgroundColor(Theme.orangeSoft)
                .border({ width: 1, color: '#F3D9B8' })
                .alignSelf(ItemAlign.Start)
                .margin({ left: 16, top: 8 })
              Text('今天想吃点什么？')
                .fontSize(12)
                .fontColor(Theme.faint)
                .width('100%')
                .padding({ left: 16, top: 10 })

              Row({ space: 8 }) {
                Text('✦')
                  .fontSize(12)
                  .fontColor(Theme.white)
                  .width(26)
                  .height(26)
                  .textAlign(TextAlign.Center)
                  .borderRadius(9)
                  .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                Text('说一句话点菜')
                  .fontSize(12)
                  .fontColor(Theme.muted)
                  .layoutWeight(1)
                Text('去试试')
                  .fontSize(12)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(Theme.orange)
              }
              .width('100%')
              .padding(12)
              .margin({ left: 16, right: 16, top: 14 })
              .borderRadius(14)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: '#F3D9B8' })
              .onClick(() => {
                this.openAi()
              })

              Row() {
                Text('今日推荐')
                  .fontSize(15)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.ink)
                Blank()
                Text('全部 ›')
                  .fontSize(11)
                  .fontColor(Theme.faint)
                  .onClick(() => {
                    AppNav.goTab(1)
                  })
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 18, bottom: 8 })

              if (this.dishes.length === 0) {
                Text('厨房还在备菜').fontColor(Theme.faint).margin({ top: 20 })
              } else {
                ForEach(this.dishes, (dish: Dish) => {
                  Row({ space: 12 }) {
                    Column() {
                      Text(dish.name)
                        .fontSize(15)
                        .fontWeight(FontWeight.Medium)
                        .fontColor(Theme.ink)
                      Text(dish.subtitle || stockLabel(dish.stockType, dish.stock, dish.unit))
                        .fontSize(12)
                        .fontColor(Theme.muted)
                        .margin({ top: 4 })
                      Text(stockLabel(dish.stockType, dish.stock, dish.unit))
                        .fontSize(11)
                        .fontColor(Theme.faint)
                        .margin({ top: 6 })
                    }
                    .alignItems(HorizontalAlign.Start)
                    .layoutWeight(1)
                    Button('+')
                      .width(34)
                      .height(34)
                      .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                      .fontColor(Theme.white)
                      .borderRadius(17)
                      .onClick(() => {
                        DraftStore.addOne(dish.id, dish.name)
                        this.draftQty = DraftStore.totalQty()
                        promptAction.showToast({ message: '已加入预约' })
                      })
                  }
                  .width('100%')
                  .padding(14)
                  .margin({ left: 16, right: 16, top: 12 })
                  .borderRadius(20)
                  .backgroundColor(Theme.white)
                  .border({ width: 1, color: Theme.line })
                  .onClick(() => {
                    router.pushUrl({ url: 'pages/DishDetail', params: { id: dish.id } })
                  })
                }, (dish: Dish) => dish.id)
              }

              if (this.lowStockTip().length > 0) {
                Text('⚠ ' + this.lowStockTip())
                  .fontSize(12)
                  .fontColor('#8A6A1F')
                  .width('100%')
                  .padding(12)
                  .margin({ left: 16, right: 16, top: 14, bottom: 8 })
                  .borderRadius(14)
                  .backgroundColor(Theme.amberSoft)
                  .border({ width: 1, color: '#F0DDB4' })
              }
            }
          }
          .layoutWeight(1)
          .width('100%')
          .scrollBar(BarState.Off)
        }

        if (this.draftQty > 0) {
          Row({ space: 12 }) {
            Text('已选 ' + this.draftQty + ' 样')
              .fontColor(Theme.cream)
              .fontSize(13)
              .fontWeight(FontWeight.Medium)
              .layoutWeight(1)
            Text('去预约')
              .fontSize(12)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.white)
              .padding({ left: 14, right: 14, top: 8, bottom: 8 })
              .borderRadius(12)
              .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
          }
          .width('100%')
          .padding(14)
          .margin({ left: 16, right: 16, bottom: 10 })
          .borderRadius(18)
          .backgroundColor(Theme.ink)
          .onClick(() => {
            AppNav.goTab(2)
          })
        }
      }
      .width('100%')
      .height('100%')
      .backgroundColor(Theme.cream)

      if (this.aiOpen) {
        Column() {
          Column()
            .width('100%')
            .layoutWeight(1)
            .backgroundColor('rgba(43,38,34,0.35)')
            .onClick(() => {
              this.closeAi()
            })
          Column() {
            Column()
              .width(36)
              .height(4)
              .borderRadius(99)
              .backgroundColor('#D8CFC2')
              .margin({ bottom: 14 })
            Text('说一句话点菜')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.ink)
              .width('100%')
            Text('只匹配当前厨房在售菜，对不上的会放进「没找到」。')
              .fontSize(12)
              .fontColor(Theme.muted)
              .margin({ top: 6 })
              .width('100%')
            TextArea({ placeholder: '例如：中午想吃红烧肉和一条鱼', text: this.aiText })
              .height(72)
              .margin({ top: 14 })
              .padding(12)
              .borderRadius(14)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
              .onChange((v: string) => {
                this.aiText = v
              })
            Button(this.aiBusy ? '识别中…' : '识别在售菜')
              .width('100%')
              .height(40)
              .margin({ top: 12 })
              .backgroundColor(Theme.orangeSoft)
              .fontColor(Theme.orange)
              .borderRadius(12)
              .enabled(!this.aiBusy)
              .onClick(() => {
                void this.runAiDraft()
              })
            if (this.matched.length > 0) {
              ForEach(this.matched, (item: OrderDraftAiItem) => {
                Row() {
                  Text(item.dishName)
                    .fontSize(13)
                    .fontWeight(FontWeight.Medium)
                    .layoutWeight(1)
                  Text('已匹配 · ×' + item.quantity)
                    .fontSize(11)
                    .fontColor(Theme.mint)
                }
                .width('100%')
                .padding(11)
                .margin({ top: 8 })
                .borderRadius(14)
                .backgroundColor(Theme.white)
                .border({ width: 1, color: Theme.line })
              }, (item: OrderDraftAiItem) => item.dishId)
            }
            if (this.unmatched.length > 0) {
              Text('没找到：' + this.unmatched.map((u: OrderDraftAiUnmatched) => u.rawText).join('、'))
                .fontSize(11)
                .fontColor(Theme.amber)
                .width('100%')
                .padding(10)
                .margin({ top: 8 })
                .borderRadius(10)
                .backgroundColor(Theme.amberSoft)
            }
            if (this.aiTip.length > 0) {
              Text(this.aiTip).fontSize(11).fontColor(Theme.muted).margin({ top: 8 }).width('100%')
            }
            Button(this.matched.length > 0 ? '加入草稿' : '关闭')
              .width('100%')
              .height(48)
              .margin({ top: 14, bottom: 8 })
              .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
              .fontColor(Theme.white)
              .borderRadius(16)
              .onClick(() => {
                if (this.matched.length > 0) {
                  void this.applyAiDraft()
                } else {
                  this.closeAi()
                }
              })
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 10, bottom: 16 })
          .borderRadius({ topLeft: 24, topRight: 24 })
          .backgroundColor(Theme.cream)
          .alignItems(HorizontalAlign.Center)
        }
        .width('100%')
        .height('100%')
      }
    }
    .width('100%')
    .height('100%')
    .onVisibleAreaChange([0.0, 1.0], (visible: boolean) => {
      if (visible) {
        this.draftQty = DraftStore.totalQty()
      }
    })
  }
}
