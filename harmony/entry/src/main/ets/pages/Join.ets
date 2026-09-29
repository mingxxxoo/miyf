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
    const invite = this.code.trim().toUpperCase()
    if (invite.length !== 6) {
      promptAction.showToast({ message: '请输入 6 位邀请码' })
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

  private cell(index: number): string {
    if (this.code.length > index) {
      return this.code.substring(index, index + 1)
    }
    return ''
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .fontColor(Theme.ink)
          .onClick(() => {
            router.back()
          })
        Text('加入厨房')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.ink)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ')
          .width(22)
      }
      .width('100%')
      .padding({ top: 12, bottom: 8 })

      Column() {
        Text('输入 6 位邀请码')
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor(Theme.ink)
        Text('向厨师要邀请码。提交后需厨师确认，通过后即可预约。')
          .fontSize(12)
          .fontColor(Theme.muted)
          .margin({ top: 8 })
      }
      .width('100%')
      .padding(18)
      .borderRadius(22)
      .linearGradient({ angle: 135, colors: [['#FFF6EA', 0], ['#FFE8C8', 1]] })
      .border({ width: 1, color: '#F3D9B8' })
      .alignItems(HorizontalAlign.Start)
      .margin({ top: 8 })

      Row({ space: 8 }) {
        ForEach([0, 1, 2, 3, 4, 5], (i: number) => {
          Text(this.cell(i))
            .width(0)
            .layoutWeight(1)
            .height(52)
            .textAlign(TextAlign.Center)
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.orange)
            .borderRadius(14)
            .backgroundColor(this.cell(i).length > 0 ? Theme.orangeSofter : Theme.white)
            .border({ width: 1.5, color: this.cell(i).length > 0 ? Theme.orange2 : Theme.line })
        }, (i: number) => String(i))
      }
      .width('100%')
      .margin({ top: 22 })

      TextInput({ placeholder: '点这里输入 6 位邀请码', text: this.code })
        .height(44)
        .margin({ top: 16 })
        .backgroundColor(Theme.white)
        .borderRadius(12)
        .border({ width: 1, color: Theme.line })
        .maxLength(6)
        .onChange((value: string) => {
          this.code = value.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
        })

      if (this.binding !== null) {
        Text('● ' + bindingStatusText(this.binding.status)
          + (this.binding.kitchenName ? ' · ' + this.binding.kitchenName : ''))
          .fontSize(12)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.binding.status === 'REJECTED' ? Theme.red : Theme.amber)
          .padding({ left: 12, right: 12, top: 7, bottom: 7 })
          .borderRadius(99)
          .backgroundColor(this.binding.status === 'REJECTED' ? Theme.redSoft : Theme.amberSoft)
          .margin({ top: 16 })
      }

      Text('状态：未绑定 → 待确认 → 已绑定 / 未通过。未通过可重新申请。')
        .fontSize(12)
        .fontColor(Theme.faint)
        .margin({ top: 14 })

      if (this.tip.length > 0 && this.binding === null) {
        Text(this.tip).fontSize(12).fontColor(Theme.red).margin({ top: 8 })
      }

      Blank()

      Button(this.busy ? '提交中…' : '提交申请')
        .width('100%')
        .height(48)
        .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
        .fontColor(Theme.white)
        .borderRadius(16)
        .enabled(!this.busy)
        .onClick(async () => {
          await this.apply()
        })
        .margin({ bottom: 16 })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 16, right: 16 })
    .backgroundColor(Theme.cream)
    .alignItems(HorizontalAlign.Start)
  }
}
