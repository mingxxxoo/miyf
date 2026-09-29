import { promptAction } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { AppNav } from '../session/AppNav'

@Entry
@Component
struct RoleSelect {
  @State busy: boolean = false
  @State picked: string = 'DINER'

  private async confirm(): Promise<void> {
    this.busy = true
    try {
      const me = await MiyfApi.fetchMe()
      const role = this.picked
      if ((role === 'CHEF' && me.chef) || (role === 'DINER' && me.diner)) {
        await MiyfApi.switchRole(role)
      } else {
        await MiyfApi.chooseRole(role)
      }
      const next = await MiyfApi.fetchMe()
      await AppNav.afterAuth(next)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '选择身份失败'
      promptAction.showToast({ message: message })
    } finally {
      this.busy = false
    }
  }

  build() {
    Column() {
      Text('选择身份')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .margin({ top: 56, bottom: 8 })
      Text('同一账号以后可切换。先选一个开始。')
        .fontSize(13)
        .fontColor(Theme.muted)
        .width('100%')

      Row({ space: 14 }) {
        Column({ space: 6 }) {
          Text('🍳').fontSize(26)
        }
        .width(56)
        .height(56)
        .borderRadius(18)
        .backgroundColor(Theme.orangeSoft)
        .justifyContent(FlexAlign.Center)
        Column() {
          Text('我是食客')
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.ink)
          Text('用邀请码加入厨房，浏览在售菜，预约今天吃什么。')
            .fontSize(12)
            .fontColor(Theme.muted)
            .margin({ top: 6 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .padding(18)
      .margin({ top: 24 })
      .borderRadius(22)
      .backgroundColor(Theme.white)
      .border({ width: this.picked === 'DINER' ? 1.5 : 1, color: this.picked === 'DINER' ? Theme.orange2 : Theme.line })
      .onClick(() => {
        this.picked = 'DINER'
      })

      Row({ space: 14 }) {
        Column({ space: 6 }) {
          Text('👨‍🍳').fontSize(26)
        }
        .width(56)
        .height(56)
        .borderRadius(18)
        .backgroundColor(Theme.orangeSofter)
        .justifyContent(FlexAlign.Center)
        Column() {
          Text('我是厨师')
            .fontSize(17)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.ink)
          Text('经营一个厨房：菜品、预约处理、邀请食客。')
            .fontSize(12)
            .fontColor(Theme.muted)
            .margin({ top: 6 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .padding(18)
      .margin({ top: 12 })
      .borderRadius(22)
      .backgroundColor(Theme.white)
      .border({ width: this.picked === 'CHEF' ? 1.5 : 1, color: this.picked === 'CHEF' ? Theme.orange2 : Theme.line })
      .onClick(() => {
        this.picked = 'CHEF'
      })

      Blank()

      Button(this.busy ? '保存中…' : '确认身份')
        .width('100%')
        .height(48)
        .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
        .fontColor(Theme.white)
        .borderRadius(16)
        .enabled(!this.busy)
        .onClick(async () => {
          await this.confirm()
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
