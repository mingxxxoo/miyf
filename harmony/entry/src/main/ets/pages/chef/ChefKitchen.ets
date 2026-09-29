import { promptAction, router } from '@kit.ArkUI'
import { pasteboard } from '@kit.BasicServicesKit'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'

@Entry
@Component
struct ChefKitchen {
  @State name: string = ''
  @State intro: string = ''
  @State code: string = ''
  @State saving: boolean = false

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    try {
      const k = await MiyfApi.fetchMyKitchen()
      if (k !== null) {
        this.name = k.name
        this.intro = k.intro || ''
      }
      const invite = await MiyfApi.fetchInvite()
      this.code = invite !== null ? invite.code : ''
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '加载失败' })
    }
  }

  private async save(): Promise<void> {
    if (this.name.trim().length === 0) {
      promptAction.showToast({ message: '请填写厨房名' })
      return
    }
    this.saving = true
    try {
      await MiyfApi.saveMyKitchen(this.name.trim(), this.intro.trim())
      promptAction.showToast({ message: '已保存' })
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '保存失败' })
    } finally {
      this.saving = false
    }
  }

  private async copyCode(): Promise<void> {
    if (this.code.length === 0) {
      return
    }
    try {
      const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, this.code)
      await pasteboard.getSystemPasteboard().setData(data)
      promptAction.showToast({ message: '邀请码已复制' })
    } catch (_err) {
      promptAction.showToast({ message: this.code })
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).onClick(() => {
          router.back()
        })
        Text('厨房资料')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ').width(22)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      Scroll() {
        Column({ space: 14 }) {
          Column()
            .width('100%')
            .height(120)
            .linearGradient({ angle: 120, colors: [['#FFD9AE', 0], [Theme.orange2, 1]] })

          Column({ space: 12 }) {
            Text('厨房名称').fontSize(12).fontColor(Theme.muted)
            TextInput({ placeholder: '例如：老王的灶', text: this.name })
              .height(44)
              .backgroundColor(Theme.orangeSofter)
              .borderRadius(12)
              .onChange((v: string) => {
                this.name = v
              })
            Text('简介').fontSize(12).fontColor(Theme.muted)
            TextArea({ placeholder: '风格、擅长菜系、取餐说明', text: this.intro })
              .height(84)
              .backgroundColor(Theme.orangeSofter)
              .borderRadius(12)
              .onChange((v: string) => {
                this.intro = v
              })
          }
          .width('100%')
          .padding(16)
          .margin({ left: 16, right: 16, top: -28 })
          .borderRadius(20)
          .backgroundColor(Theme.white)
          .border({ width: 1, color: Theme.line })
          .alignItems(HorizontalAlign.Start)

          if (this.code.length > 0) {
            Row() {
              Column() {
                Text('邀请码').fontSize(12).fontColor(Theme.muted)
                Text(this.code)
                  .fontSize(20)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.orange)
                  .letterSpacing(3)
                  .margin({ top: 4 })
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)
              Button('复制')
                .height(32)
                .fontSize(12)
                .backgroundColor(Theme.orangeSoft)
                .fontColor(Theme.orange)
                .borderRadius(10)
                .onClick(() => {
                  void this.copyCode()
                })
            }
            .width('100%')
            .padding(14)
            .margin({ left: 16, right: 16 })
            .borderRadius(16)
            .linearGradient({ angle: 135, colors: [['#FFF6EA', 0], ['#FFEFD8', 1]] })
            .border({ width: 1, color: '#EFCB9C', style: BorderStyle.Dashed })
          }

          Button(this.saving ? '保存中…' : '保存资料')
            .width('100%')
            .height(48)
            .margin({ left: 16, right: 16, top: 8, bottom: 24 })
            .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
            .fontColor(Theme.white)
            .borderRadius(16)
            .enabled(!this.saving)
            .onClick(() => {
              void this.save()
            })
        }
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
