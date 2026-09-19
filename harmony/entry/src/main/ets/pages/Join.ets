import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { Binding, bindingStatusText } from '../api/Models'

@Entry
@Component
struct Join {
  @State code: string = ''
  @State busy: boolean = false
  @State binding: Binding | null = null
  @State tip: string = ''

  aboutToAppear(): void {
    void this.refresh()
  }

  private async refresh(): Promise<void> {
    try {
      this.binding = await MiyfApi.fetchMyBinding()
      if (this.binding !== null && this.binding.status === 'BOUND') {
        router.replaceUrl({ url: 'pages/Main' })
      }
    } catch (_err) {
      this.binding = null
    }
  }

  private async apply(): Promise<void> {
    const invite = this.code.trim()
    if (invite.length === 0) {
      promptAction.showToast({ message: '请输入邀请码' })
      return
    }
    this.busy = true
    this.tip = ''
    try {
      this.binding = await MiyfApi.applyBinding(invite)
      if (this.binding !== null && this.binding.status === 'BOUND') {
        router.replaceUrl({ url: 'pages/Main' })
        return
      }
      this.tip = this.binding !== null
        ? bindingStatusText(this.binding.status) + (this.binding.rejectReason ? '：' + this.binding.rejectReason : '')
        : '已提交申请'
      promptAction.showToast({ message: this.tip })
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '申请失败'
      this.tip = message
      promptAction.showToast({ message: message })
    } finally {
      this.busy = false
    }
  }

  build() {
    Column({ space: 16 }) {
      Text('加入厨房')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .margin({ top: 72 })
      Text('向厨师索取 6 位邀请码。一客一厨，通过后才能点菜。')
        .fontSize(13)
        .fontColor(Theme.muted)

      TextInput({ placeholder: '邀请码', text: this.code })
        .height(48)
        .backgroundColor(Theme.white)
        .borderRadius(12)
        .border({ width: 1, color: Theme.line })
        .padding({ left: 12, right: 12 })
        .onChange((value: string) => {
          this.code = value
        })

      Button(this.busy ? '提交中…' : '申请加入')
        .width('100%')
        .height(48)
        .backgroundColor(Theme.orange)
        .fontColor(Theme.white)
        .borderRadius(16)
        .enabled(!this.busy)
        .onClick(async () => {
          await this.apply()
        })

      if (this.binding !== null) {
        Text('当前状态：' + bindingStatusText(this.binding.status)
          + (this.binding.kitchenName ? ' · ' + this.binding.kitchenName : ''))
          .fontSize(13)
          .fontColor(Theme.muted)
      }
      if (this.tip.length > 0) {
        Text(this.tip)
          .fontSize(12)
          .fontColor(Theme.red)
      }
    }
    .width('100%')
    .height('100%')
    .padding({ left: 20, right: 20 })
    .backgroundColor(Theme.cream)
    .alignItems(HorizontalAlign.Start)
  }
}
