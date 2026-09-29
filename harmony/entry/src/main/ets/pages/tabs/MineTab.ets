import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { Theme } from '../../common/AppConfig'
import { LucideIcon } from '../../common/LucideIcon'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { Binding, UserProfile, bindingStatusText, nickInitial } from '../../api/Models'
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

  private async goChef(): Promise<void> {
    try {
      const me = await MiyfApi.fetchMe()
      if (me.chef) {
        await MiyfApi.switchRole('CHEF')
      } else {
        await MiyfApi.chooseRole('CHEF')
      }
      router.replaceUrl({ url: 'pages/chef/ChefWorkbench' })
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '切换失败' })
    }
  }

  private logout(): void {
    AlertDialog.show({
      title: '退出登录',
      message: '确定退出？本地预约草稿也会清空。',
      primaryButton: {
        value: '退出',
        fontColor: Theme.red,
        action: () => {
          void this.doLogout()
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private async doLogout(): Promise<void> {
    DraftStore.clear()
    await SessionStore.clear(this.context())
    router.replaceUrl({ url: 'pages/Login' })
  }

  build() {
    Column({ space: 14 }) {
      Text('我的')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .margin({ top: 16 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted)
      } else if (this.user !== null) {
        Row({ space: 12 }) {
          Text(nickInitial(this.user.nickname))
            .fontSize(24)
            .fontColor(Theme.white)
            .width(54)
            .height(54)
            .textAlign(TextAlign.Center)
            .borderRadius(27)
            .backgroundColor('rgba(255,255,255,0.25)')
          Column() {
            Row({ space: 8 }) {
              Text(this.user.nickname)
                .fontSize(16)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.white)
              Text(this.user.activeRole === 'CHEF' ? '厨师' : '食客')
                .fontSize(10)
                .fontColor(Theme.white)
                .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                .borderRadius(99)
                .backgroundColor('rgba(255,255,255,0.25)')
            }
            Text('华为账号已绑定')
              .fontSize(11)
              .fontColor('rgba(255,255,255,0.85)')
              .margin({ top: 4 })
          }
          .alignItems(HorizontalAlign.Start)
        }
        .width('100%')
        .padding(18)
        .borderRadius(22)
        .linearGradient({ angle: 135, colors: [['#FF9A3E', 0], [Theme.orange, 1]] })

        Row({ space: 12 }) {
          Text('🏠')
            .fontSize(20)
            .width(44)
            .height(44)
            .textAlign(TextAlign.Center)
            .borderRadius(14)
            .backgroundColor(Theme.orangeSoft)
          Column() {
            Text(this.binding !== null ? (this.binding.kitchenName || '厨房') : '尚未绑定厨房')
              .fontSize(13)
              .fontWeight(FontWeight.Medium)
              .fontColor(Theme.ink)
            Text(this.binding !== null
              ? bindingStatusText(this.binding.status) + ' · 邀请码加入'
              : '去加入厨房才能预约')
              .fontSize(11)
              .fontColor(Theme.faint)
              .margin({ top: 2 })
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          Text('›').fontColor(Theme.faint)
        }
        .width('100%')
        .padding(14)
        .borderRadius(20)
        .backgroundColor(Theme.white)
        .border({ width: 1, color: Theme.line })
        .onClick(() => {
          if (this.binding === null || this.binding.status !== 'BOUND') {
            router.pushUrl({ url: 'pages/Join' })
          }
        })

        Column() {
          this.menuRow('arrow-left-right', Theme.orangeSoft, '进入厨房工作台', () => {
            void this.goChef()
          })
          this.menuRow('heart', Theme.mintSoft, '胡闹健康', () => {
            router.pushUrl({ url: 'pages/health/HealthHome' })
          })
          this.menuRow('log-out', '#F4F0E9', '退出登录', () => {
            this.logout()
          })
        }
        .width('100%')
        .padding({ left: 14, right: 14 })
        .borderRadius(20)
        .backgroundColor(Theme.white)
        .border({ width: 1, color: Theme.line })
      }
    }
    .width('100%')
    .height('100%')
    .padding({ left: 16, right: 16 })
    .backgroundColor(Theme.cream)
    .alignItems(HorizontalAlign.Start)
  }

  @Builder
  menuRow(icon: string, bg: string, name: string, tap: () => void) {
    Row({ space: 12 }) {
      Stack({ alignContent: Alignment.Center }) {
        LucideIcon(icon, 16, '#5A524A')
      }
        .width(32)
        .height(32)
        .borderRadius(10)
        .backgroundColor(bg)
      Text(name)
        .fontSize(13)
        .fontColor(Theme.ink)
        .layoutWeight(1)
      Text('›').fontColor(Theme.faint)
    }
    .width('100%')
    .padding({ top: 13, bottom: 13 })
    .onClick(tap)
  }
}
