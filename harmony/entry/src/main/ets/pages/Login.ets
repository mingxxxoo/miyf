import { promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { AppConfig, Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { SessionStore } from '../session/SessionStore'
import { requestHuaweiAuthorizationCode } from '../platform/HuaweiLogin'

@Entry
@Component
struct Login {
  @State busy: boolean = false
  @State tip: string = ''

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private async afterLogin(token: string, expireSeconds: number): Promise<void> {
    await SessionStore.save(this.context(), token, expireSeconds)
    const me = await MiyfApi.fetchMe()
    if (me.activeRole === undefined || me.activeRole.length === 0) {
      router.replaceUrl({ url: 'pages/RoleSelect' })
      return
    }
    if (me.activeRole === 'CHEF') {
      this.tip = '厨师工作台下一期开放，请先以食客身份使用'
      return
    }
    const binding = await MiyfApi.fetchMyBinding()
    if (binding === null || binding.status !== 'BOUND') {
      router.replaceUrl({ url: 'pages/Join' })
      return
    }
    router.replaceUrl({ url: 'pages/Main' })
  }

  private async loginWithCode(code: string): Promise<void> {
    this.busy = true
    this.tip = ''
    try {
      const login = await MiyfApi.huaweiLogin(code)
      await this.afterLogin(login.token, login.expireSeconds ?? 0)
    } catch (err) {
      const message = err instanceof ApiError ? err.message : '登录失败'
      this.tip = message
      promptAction.showToast({ message: message })
    } finally {
      this.busy = false
    }
  }

  build() {
    Column() {
      Text('miyf')
        .fontSize(36)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .margin({ top: 96 })
      Text('胡闹厨房 · 预约与健康')
        .fontSize(14)
        .fontColor(Theme.muted)
        .margin({ top: 8 })
      Text('家庭厨房预约，没有价格与支付')
        .fontSize(13)
        .fontColor(Theme.faint)
        .margin({ top: 24, bottom: 40 })

      Button(this.busy ? '登录中…' : '华为账号登录')
        .width('100%')
        .height(48)
        .backgroundColor(Theme.orange)
        .fontColor(Theme.white)
        .borderRadius(16)
        .enabled(!this.busy)
        .onClick(async () => {
          try {
            const code = await requestHuaweiAuthorizationCode(this.context())
            await this.loginWithCode(code)
          } catch (_err) {
            this.tip = '华为账号授权失败，可先用开发登录联调'
            promptAction.showToast({ message: this.tip })
          }
        })

      if (AppConfig.DEV_LOGIN) {
        Button('开发登录（Mock）')
          .width('100%')
          .height(48)
          .margin({ top: 12 })
          .backgroundColor(Theme.orangeSoft)
          .fontColor(Theme.orange)
          .borderRadius(16)
          .enabled(!this.busy)
          .onClick(async () => {
            await this.loginWithCode('mock:diner')
          })
      }

      if (this.tip.length > 0) {
        Text(this.tip)
          .fontSize(12)
          .fontColor(Theme.red)
          .margin({ top: 16 })
      }
    }
    .width('100%')
    .height('100%')
    .padding({ left: 24, right: 24 })
    .backgroundColor(Theme.cream)
  }
}
