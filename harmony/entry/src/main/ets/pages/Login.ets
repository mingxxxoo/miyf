import { promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { AppConfig, Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { SessionStore } from '../session/SessionStore'
import { AppNav } from '../session/AppNav'
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
    await AppNav.afterAuth(me)
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
      Column() {
        Text('m')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(Theme.white)
      }
      .width(72)
      .height(72)
      .borderRadius(22)
      .linearGradient({
        angle: 135,
        colors: [[Theme.orange2, 0], [Theme.orange, 1]]
      })
      .justifyContent(FlexAlign.Center)
      .margin({ top: 88 })

      Text('miyf')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .margin({ top: 20 })
      Text('家庭厨房预约 · 健康数据\n今天吃什么，不外卖')
        .fontSize(13)
        .fontColor(Theme.muted)
        .textAlign(TextAlign.Center)
        .margin({ top: 10 })
      Row({ space: 8 }) {
        Text('胡闹厨房')
          .fontSize(11)
          .fontColor(Theme.muted)
          .padding({ left: 12, right: 12, top: 5, bottom: 5 })
          .borderRadius(99)
          .backgroundColor(Theme.orangeSofter)
          .border({ width: 1, color: Theme.line })
        Text('胡闹健康')
          .fontSize(11)
          .fontColor(Theme.muted)
          .padding({ left: 12, right: 12, top: 5, bottom: 5 })
          .borderRadius(99)
          .backgroundColor(Theme.orangeSofter)
          .border({ width: 1, color: Theme.line })
      }
      .margin({ top: 16 })

      Blank()

      Button(this.busy ? '登录中…' : '华为账号登录')
        .width('100%')
        .height(48)
        .backgroundColor('#1A1A1A')
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
        Button('开发登录 · 食客')
          .width('100%')
          .height(48)
          .margin({ top: 10 })
          .backgroundColor(Theme.white)
          .fontColor(Theme.ink)
          .border({ width: 1, color: Theme.line })
          .borderRadius(16)
          .enabled(!this.busy)
          .onClick(async () => {
            await this.loginWithCode('mock:diner')
          })
        Button('开发登录 · 厨师')
          .width('100%')
          .height(48)
          .margin({ top: 10 })
          .backgroundColor(Theme.orangeSoft)
          .fontColor(Theme.orange)
          .borderRadius(16)
          .enabled(!this.busy)
          .onClick(async () => {
            await this.loginWithCode('mock:cook')
          })
      }

      if (this.tip.length > 0) {
        Text(this.tip)
          .fontSize(12)
          .fontColor(Theme.red)
          .margin({ top: 12 })
      }
      Text('登录即表示同意用户协议与隐私政策\n手机号验证码登录后续开放')
        .fontSize(11)
        .fontColor(Theme.faint)
        .textAlign(TextAlign.Center)
        .margin({ top: 14, bottom: 12 })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 24, right: 24 })
    .backgroundColor(Theme.cream)
  }
}
