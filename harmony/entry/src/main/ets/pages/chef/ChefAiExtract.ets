import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { ChefDishAiDraft } from '../../api/Models'

@Entry
@Component
struct ChefAiExtract {
  @State text: string = ''
  @State url: string = ''
  @State busy: boolean = false
  @State saving: boolean = false
  @State drafts: ChefDishAiDraft[] = []
  @State tip: string = ''
  @State selected: number[] = []

  private toggle(index: number): void {
    if (this.selected.indexOf(index) >= 0) {
      this.selected = this.selected.filter((i: number) => i !== index)
    } else {
      this.selected = this.selected.concat([index])
    }
  }

  private async extract(): Promise<void> {
    const t = this.text.trim()
    const u = this.url.trim()
    if (t.length === 0 && u.length === 0) {
      promptAction.showToast({ message: '请粘贴菜品文字或链接' })
      return
    }
    this.busy = true
    this.tip = ''
    this.drafts = []
    this.selected = []
    try {
      const result = await MiyfApi.extractChefDishes(t, u)
      if (result.degraded && result.dishes.length === 0) {
        this.tip = result.rejectReason || 'AI 暂不可用，请手动新建'
        promptAction.showToast({ message: this.tip })
        return
      }
      if (result.dishes.length === 0) {
        this.tip = result.rejectReason || '未识别到菜品'
        promptAction.showToast({ message: this.tip })
        return
      }
      this.drafts = result.dishes
      this.selected = result.dishes.map((_d: ChefDishAiDraft, i: number) => i)
      this.tip = result.dishes.length > 1
        ? '已识别 ' + result.dishes.length + ' 道菜，勾选后提交审核'
        : '已填入草稿，确认后提交审核'
    } finally {
      this.busy = false
    }
  }

  private async submitSelected(): Promise<void> {
    if (this.selected.length === 0) {
      promptAction.showToast({ message: '请至少勾选一道菜' })
      return
    }
    this.saving = true
    let ok = 0
    let fail = 0
    try {
      for (let i = 0; i < this.selected.length; i++) {
        const idx = this.selected[i]
        const draft = this.drafts[idx]
        if (draft === undefined || draft.name.length === 0) {
          continue
        }
        try {
          let stock = 10
          if (draft.stockType === 'UNLIMITED') {
            stock = 0
          } else if (draft.stock !== undefined && draft.stock > 0) {
            stock = draft.stock
          }
          const dish = await MiyfApi.createChefDish(
            draft.name,
            stock,
            draft.subtitle || '',
            draft.description || ''
          )
          try {
            await MiyfApi.submitChefDish(dish.id)
          } catch (_err) {
          }
          ok += 1
        } catch (_err) {
          fail += 1
        }
      }
      if (ok > 0) {
        promptAction.showToast({
          message: '已提交 ' + ok + ' 道菜审核' + (fail > 0 ? '，' + fail + ' 道失败' : '')
        })
        router.back()
      } else {
        promptAction.showToast({ message: '提交失败，请稍后重试' })
      }
    } finally {
      this.saving = false
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .onClick(() => {
            router.back()
          })
        Text('用文字生成')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ').width(22)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      Scroll() {
        Column({ space: 12 }) {
          Column() {
            Text('✦ AI 建菜')
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.white)
            Text('粘贴菜单文字或网页链接，生成可编辑草稿后提交审核，不会直接上架。')
              .fontSize(11)
              .fontColor('rgba(255,255,255,0.8)')
              .margin({ top: 8 })
          }
          .width('100%')
          .padding(18)
          .borderRadius(22)
          .linearGradient({ angle: 135, colors: [['#2B2622', 0], ['#4E3B28', 1]] })
          .alignItems(HorizontalAlign.Start)

          TextArea({ placeholder: '粘贴菜谱、菜单文字…', text: this.text })
            .height(110)
            .padding(12)
            .borderRadius(18)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .onChange((v: string) => {
              this.text = v
            })

          TextInput({ placeholder: '或粘贴网页链接（可选）', text: this.url })
            .height(44)
            .padding({ left: 12, right: 12 })
            .borderRadius(14)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .onChange((v: string) => {
              this.url = v
            })

          Row({ space: 8 }) {
            Text('红烧肉做法')
              .fontSize(11)
              .fontColor(Theme.orange)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(99)
              .backgroundColor(Theme.orangeSoft)
              .border({ width: 1, color: '#EFCB9C', style: BorderStyle.Dashed })
              .onClick(() => {
                this.text = '红烧肉：五花肉、冰糖、生抽。焯水后炒糖色焖煮。'
              })
            Text('家常三菜')
              .fontSize(11)
              .fontColor(Theme.orange)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .borderRadius(99)
              .backgroundColor(Theme.orangeSoft)
              .border({ width: 1, color: '#EFCB9C', style: BorderStyle.Dashed })
              .onClick(() => {
                this.text = '番茄炒蛋、清炒时蔬、紫菜蛋花汤。'
              })
          }
          .width('100%')

          Button(this.busy ? '识别中…' : '生成草稿')
            .width('100%')
            .height(48)
            .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
            .fontColor(Theme.white)
            .borderRadius(16)
            .enabled(!this.busy && !this.saving)
            .onClick(() => {
              void this.extract()
            })

          if (this.tip.length > 0) {
            Text(this.tip)
              .fontSize(12)
              .fontColor(Theme.muted)
              .width('100%')
          }

          if (this.drafts.length > 0) {
            Text('识别结果（可勾选）')
              .fontSize(14)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.ink)
              .width('100%')
              .margin({ top: 4 })

            ForEach(this.drafts, (draft: ChefDishAiDraft, index: number) => {
              Row({ space: 12 }) {
                Text(this.selected.indexOf(index) >= 0 ? '☑' : '☐')
                  .fontSize(18)
                  .fontColor(Theme.orange)
                  .onClick(() => {
                    this.toggle(index)
                  })
                Column() {
                  Text(draft.name)
                    .fontSize(15)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(Theme.ink)
                  Text((draft.subtitle || draft.categoryName || '未分类')
                    + ' · '
                    + (draft.stockType === 'UNLIMITED' ? '不限量' : ('库存 ' + (draft.stock ?? 10))))
                    .fontSize(11)
                    .fontColor(Theme.muted)
                    .margin({ top: 4 })
                  if (draft.description) {
                    Text(draft.description)
                      .fontSize(11)
                      .fontColor(Theme.faint)
                      .margin({ top: 4 })
                  }
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)
              }
              .width('100%')
              .padding(14)
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({
                width: 1,
                color: this.selected.indexOf(index) >= 0 ? Theme.orange2 : Theme.line
              })
              .onClick(() => {
                this.toggle(index)
              })
            }, (draft: ChefDishAiDraft, index: number) => draft.name + '_' + index)

            Button(this.saving ? '提交中…' : '提交审核')
              .width('100%')
              .height(48)
              .margin({ top: 4, bottom: 24 })
              .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
              .fontColor(Theme.white)
              .borderRadius(16)
              .enabled(!this.saving && !this.busy)
              .onClick(() => {
                void this.submitSelected()
              })
          }
        }
        .padding({ left: 16, right: 16, bottom: 16 })
      }
      .layoutWeight(1)
      .width('100%')
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }
}
