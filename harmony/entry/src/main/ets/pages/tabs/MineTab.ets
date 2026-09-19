import { promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { Binding, UserProfile, bindingStatusText } from '../../api/Models'
import { SessionStore } from '../../session/SessionStore'
import { DraftStore } from '../../session/DraftStore'

@Component
export struct MineTab {
  @State user: UserProfile | null = null
  @State binding: Binding | null = null
  @State loading: boolean = true

  aboutToAppear(): void {
    void this.reload()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private async reload(): Promise<void> {
    this.loading = true
    try {
      this.user = await MiyfApi.fetchMe()
      this.binding = await MiyfApi.fetchMyBinding()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '加载失败' })
    } finally {
      this.loading = false
    }
  }

  private async logout(): Promise<void> {
    DraftStore.clear()
    await SessionStore.clear(this.context())
    router.replaceUrl({ url: 'pages/Login' })
  }

  build() {
    Column({ space: 14 }) {
      Text('我的')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .margin({ top: 18 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted)
      } else if (this.user !== null) {
        Column({ space: 8 }) {
          Text(this.user.nickname)
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.ink)
          Text(this.user.activeRole === 'CHEF' ? '当前：厨师' : '当前：食客')
            .fontSize(13)
            .fontColor(Theme.muted)
          Text(this.binding !== null
            ? ('厨房：' + (this.binding.kitchenName || '-') + ' · ' + bindingStatusText(this.binding.status))
            : '尚未绑定厨房')
            .fontSize(12)
            .fontColor(Theme.faint)
        }
        .width('100%')
        .padding(16)
        .borderRadius(20)
        .backgroundColor(Theme.white)
        .border({ width: 1, color: Theme.line })
        .alignItems(HorizontalAlign.Start)
      }

      Button('退出登录')
        .width('100%')
        .height(44)
        .backgroundColor(Theme.white)
        .fontColor(Theme.red)
        .borderRadius(14)
        .border({ width: 1, color: Theme.line })
        .onClick(async () => {
          await this.logout()
        })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 18, right: 18 })
    .backgroundColor(Theme.cream)
    .alignItems(HorizontalAlign.Start)
  }
}
