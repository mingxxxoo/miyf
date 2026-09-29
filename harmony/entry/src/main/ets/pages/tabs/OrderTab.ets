import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import {
  DraftLine,
  Order,
  OrderInsightAiResult,
  OrderInsightDish,
  OrderItem,
  insightIsWarn,
  insightVerdictText,
  orderStatusText
} from '../../api/Models'
import { DraftStore } from '../../session/DraftStore'
import { AiInsightStore } from '../../session/AiInsightStore'
import { FormCardUpdater } from '../../platform/FormCardUpdater'
import { common } from '@kit.AbilityKit'

@Component
export struct OrderTab {
  @State draft: DraftLine[] = []
  @State orders: Order[] = []
  @State remark: string = ''
  @State meal: string = '午餐'
  @State loading: boolean = true
  @State submitting: boolean = false
  @State error: string = ''
  @State insight: OrderInsightAiResult | null = null

  aboutToAppear(): void {
    void this.reload()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private refreshInsight(): void {
    this.insight = AiInsightStore.get()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    this.draft = DraftStore.list()
    this.refreshInsight()
    try {
      this.orders = await MiyfApi.fetchOrders()
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private async submit(): Promise<void> {
    const lines = DraftStore.list()
    if (lines.length === 0) {
      promptAction.showToast({ message: '请先加入菜品' })
      return
    }
    this.submitting = true
    try {
      const note = this.meal + (this.remark.trim().length > 0 ? ' · ' + this.remark.trim() : '')
      await MiyfApi.createOrder(lines, note)
      DraftStore.clear()
      AiInsightStore.clear()
      this.draft = []
      this.insight = null
      this.remark = ''
      promptAction.showToast({ message: '预约已提交，等待厨师确认' })
      this.orders = await MiyfApi.fetchOrders()
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '提交失败' })
    } finally {
      this.submitting = false
    }
  }

  private confirmCancel(order: Order): void {
    if (order.status !== 'PENDING') {
      return
    }
    AlertDialog.show({
      title: '取消预约',
      message: '确认取消这张待确认的预约？',
      primaryButton: {
        value: '取消预约',
        fontColor: Theme.red,
        action: () => {
          void this.cancel(order)
        }
      },
      secondaryButton: {
        value: '再想想',
        action: () => {
        }
      }
    })
  }

  private async cancel(order: Order): Promise<void> {
    try {
      await MiyfApi.cancelOrder(order.id)
      promptAction.showToast({ message: '已取消' })
      this.orders = await MiyfApi.fetchOrders()
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '取消失败' })
    }
  }

  @Builder
  mealChip(label: string) {
    Text(label)
      .fontSize(11)
      .fontWeight(this.meal === label ? FontWeight.Bold : FontWeight.Regular)
      .fontColor(this.meal === label ? Theme.orange : Theme.muted)
      .layoutWeight(1)
      .textAlign(TextAlign.Center)
      .padding({ top: 9, bottom: 9 })
      .borderRadius(12)
      .backgroundColor(this.meal === label ? Theme.orangeSoft : Theme.orangeSofter)
      .border({ width: 1, color: this.meal === label ? Theme.orange2 : Theme.line })
      .onClick(() => {
        this.meal = label
      })
  }

  build() {
    Column() {
      Text('预约')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 8 })

      Scroll() {
        Column({ space: 12 }) {
          Row({ space: 8 }) {
            this.mealChip('午餐')
            this.mealChip('晚餐')
            this.mealChip('夜宵')
          }
          .width('100%')

          Column() {
            Row() {
              Text('预约草稿')
                .fontSize(14)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
                .layoutWeight(1)
              Text(this.meal + ' · ' + this.draft.length + ' 样')
                .fontSize(11)
                .fontColor(Theme.orange)
                .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                .borderRadius(99)
                .backgroundColor(Theme.orangeSoft)
            }
            .width('100%')
            .padding({ left: 14, right: 14, top: 14, bottom: 8 })

            if (this.draft.length === 0) {
              Text('今天还没定吃什么，去选菜')
                .fontSize(12)
                .fontColor(Theme.faint)
                .padding({ left: 14, right: 14, bottom: 16 })
            } else {
              ForEach(this.draft, (line: DraftLine) => {
                Row() {
                  Text(line.dishName)
                    .fontColor(Theme.ink)
                    .fontSize(13)
                    .layoutWeight(1)
                  Text('−')
                    .width(28)
                    .height(28)
                    .textAlign(TextAlign.Center)
                    .fontColor(Theme.orange)
                    .backgroundColor(Theme.white)
                    .borderRadius(9)
                    .onClick(() => {
                      DraftStore.setQty(line.dishId, line.dishName, line.quantity - 1)
                      this.draft = DraftStore.list()
                      if (this.draft.length === 0) {
                        AiInsightStore.clear()
                        this.insight = null
                      }
                    })
                  Text('×' + line.quantity)
                    .fontSize(13)
                    .fontColor(Theme.ink)
                    .width(36)
                    .textAlign(TextAlign.Center)
                  Text('+')
                    .width(28)
                    .height(28)
                    .textAlign(TextAlign.Center)
                    .fontColor(Theme.orange)
                    .backgroundColor(Theme.white)
                    .borderRadius(9)
                    .onClick(() => {
                      DraftStore.addOne(line.dishId, line.dishName)
                      this.draft = DraftStore.list()
                    })
                }
                .width('100%')
                .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                .border({ width: { top: 1 }, color: '#F7F2E9' })
              }, (line: DraftLine) => line.dishId)
              TextInput({ placeholder: '口味备注，如少辣（可选）', text: this.remark })
                .height(40)
                .margin({ left: 14, right: 14, top: 8, bottom: 8 })
                .backgroundColor(Theme.orangeSofter)
                .borderRadius(12)
                .onChange((value: string) => {
                  this.remark = value
                })
            }
          }
          .width('100%')
          .borderRadius(20)
          .backgroundColor(Theme.white)
          .border({ width: 1, color: Theme.line })
          .alignItems(HorizontalAlign.Start)

          if (this.insight !== null && this.draft.length > 0) {
            Column() {
              Row() {
                Text('◈')
                  .fontColor(insightIsWarn(this.insight.overallVerdict) ? Theme.amber : Theme.mint)
                Text('饮食参考 · ' + insightVerdictText(this.insight.overallVerdict || ''))
                  .fontSize(13)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.ink)
                  .layoutWeight(1)
                  .margin({ left: 6 })
                Text(insightVerdictText(this.insight.overallVerdict || ''))
                  .fontSize(10)
                  .fontColor(insightIsWarn(this.insight.overallVerdict) ? Theme.amber : Theme.mint)
                  .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                  .borderRadius(99)
                  .backgroundColor(insightIsWarn(this.insight.overallVerdict) ? Theme.amberSoft : Theme.mintSoft)
              }
              .width('100%')
              if (this.insight.summary) {
                Text(this.insight.summary)
                  .fontSize(12)
                  .fontColor(Theme.ink)
                  .margin({ top: 8 })
                  .width('100%')
              }
              if (this.insight.dishInsights.length > 0) {
                Flex({ wrap: FlexWrap.Wrap }) {
                  ForEach(this.insight.dishInsights, (d: OrderInsightDish) => {
                    Text(d.dishName + ' · ' + insightVerdictText(d.verdict))
                      .fontSize(11)
                      .fontColor(Theme.ink)
                      .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                      .margin({ right: 6, top: 8 })
                      .borderRadius(99)
                      .backgroundColor(Theme.white)
                      .border({
                        width: 1,
                        color: insightIsWarn(d.verdict) ? '#F0DDB4' : '#CBEBDD'
                      })
                  }, (d: OrderInsightDish) => d.dishId + d.verdict)
                }
                .width('100%')
              }
              Text(this.insight.disclaimer || '以上为一般性饮食参考，不构成医疗建议')
                .fontSize(10)
                .fontColor(Theme.faint)
                .margin({ top: 10 })
                .width('100%')
            }
            .width('100%')
            .padding(14)
            .borderRadius(20)
            .linearGradient({
              angle: 135,
              colors: insightIsWarn(this.insight.overallVerdict)
                ? [['#FFFDF5', 0], ['#FBF3E0', 1]]
                : [['#F4FBF8', 0], ['#E7F7F0', 1]]
            })
            .border({
              width: 1,
              color: insightIsWarn(this.insight.overallVerdict) ? '#F0DDB4' : '#CBEBDD'
            })
            .alignItems(HorizontalAlign.Start)
          }

          Text('历史预约')
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.ink)
            .width('100%')

          if (this.loading) {
            Text('加载中…').fontColor(Theme.muted)
          } else if (this.error.length > 0) {
            Column({ space: 8 }) {
              Text(this.error).fontColor(Theme.red)
              Button('重试').onClick(() => {
                void this.reload()
              })
            }
          } else if (this.orders.length === 0) {
            Text('暂无预约').fontColor(Theme.faint)
          } else {
            ForEach(this.orders, (order: Order) => {
              Column({ space: 8 }) {
                Row() {
                  Text(order.orderNo)
                    .fontSize(13)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(Theme.ink)
                    .layoutWeight(1)
                  Text(orderStatusText(order.status))
                    .fontSize(11)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(order.status === 'CANCELLED' ? Theme.faint :
                      (order.status === 'PENDING' ? Theme.amber : Theme.mint))
                    .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                    .borderRadius(99)
                    .backgroundColor(order.status === 'PENDING' ? Theme.amberSoft : Theme.mintSoft)
                }
                ForEach(order.items || [], (item: OrderItem) => {
                  Text(item.dishName + ' ×' + item.quantity)
                    .fontSize(12)
                    .fontColor(Theme.muted)
                }, (item: OrderItem) => item.dishId + '_' + item.quantity)
                if (order.remark) {
                  Text(order.remark).fontSize(11).fontColor(Theme.faint)
                }
                if (order.status === 'PENDING') {
                  Button('取消预约')
                    .height(32)
                    .fontSize(12)
                    .backgroundColor(Theme.white)
                    .fontColor(Theme.red)
                    .border({ width: 1, color: Theme.line })
                    .borderRadius(10)
                    .onClick(() => {
                      this.confirmCancel(order)
                    })
                }
              }
              .width('100%')
              .padding(14)
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
              .alignItems(HorizontalAlign.Start)
              .onClick(() => {
                router.pushUrl({ url: 'pages/OrderDetail', params: { id: order.id } })
              })
            }, (order: Order) => order.id)
          }
        }
        .padding({ left: 16, right: 16, bottom: 24 })
      }
      .layoutWeight(1)
      .width('100%')
      .scrollBar(BarState.Off)

      if (this.draft.length > 0) {
        Button(this.submitting ? '提交中…' : '提交预约')
          .width('100%')
          .height(48)
          .margin({ left: 16, right: 16, bottom: 10 })
          .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
          .fontColor(Theme.white)
          .borderRadius(16)
          .enabled(!this.submitting)
          .onClick(async () => {
            await this.submit()
          })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
    .onVisibleAreaChange([0.0, 1.0], (visible: boolean) => {
      if (visible) {
        this.draft = DraftStore.list()
        this.refreshInsight()
      }
    })
  }
}
