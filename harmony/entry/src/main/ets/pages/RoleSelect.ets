import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'

@Entry
@Component
struct RoleSelect {
  @State busy: boolean = false

  private async pick(role: string): Promise<void> {
    this.busy = true
    try {
      const me = await MiyfApi.fetchMe()
      if ((role === 'CHEF' && me.chef) || (role === 'DINER' && me.diner)) {
        await MiyfApi.switchRole(role)
      } else {
        await MiyfApi.chooseRole(role)
      }
      if (role === 'CHEF') {
        promptAction.showToast({ message: '厨师工作台下一期开放' })
        return
      }
      const binding = await MiyfApi.fetchMyBinding()
      if (binding === null || binding.status !== 'BOUND') {
        router.replaceUrl({ url: 'pages/Join' })
      } else {
        router.replaceUrl({ url: 'pages/Main' })
      }
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '选择身份失败'
      promptAction.showToast({ message: message })
    } finally {
      this.busy = false
    }
  }

  build() {
    Column({ space: 16 }) {
      Text('选择身份')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .margin({ top: 72, bottom: 8 })
      Text('同一账号可以开通厨师与食客，本期内食客可完整使用。')
        .fontSize(13)
        .fontColor(Theme.muted)

      Column({ space: 8 }) {
        Text('我是食客')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.ink)
        Text('用邀请码加入厨房，浏览菜品并提交预约')
          .fontSize(12)
          .fontColor(Theme.muted)
      }
      .width('100%')
      .padding(20)
      .borderRadius(20)
      .backgroundColor(Theme.white)
      .border({ width: 1, color: Theme.line })
      .onClick(async () => {
        if (!this.busy) {
          await this.pick('DINER')
        }
      })

      Column({ space: 8 }) {
        Text('我是厨师')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.ink)
        Text('经营厨房与处理预约（下一期开放工作台）')
          .fontSize(12)
          .fontColor(Theme.muted)
      }
      .width('100%')
      .padding(20)
      .borderRadius(20)
      .backgroundColor(Theme.white)
      .border({ width: 1, color: Theme.line })
      .onClick(async () => {
        if (!this.busy) {
          await this.pick('CHEF')
        }
      })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 20, right: 20 })
    .backgroundColor(Theme.cream)
    .alignItems(HorizontalAlign.Start)
  }
}
