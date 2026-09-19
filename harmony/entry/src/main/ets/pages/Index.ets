import { router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { Theme } from '../common/AppConfig'
import { MiyfApi } from '../api/MiyfApi'
import { SessionStore } from '../session/SessionStore'

@Entry
@Component
struct Index {
  @State tip: string = '启动中…'

  aboutToAppear(): void {
    void this.boot()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private async boot(): Promise<void> {
    try {
      const token = await SessionStore.load(this.context())
      if (token.length === 0) {
        router.replaceUrl({ url: 'pages/Login' })
        return
      }
      const me = await MiyfApi.fetchMe()
      if (me.activeRole === undefined || me.activeRole.length === 0) {
        router.replaceUrl({ url: 'pages/RoleSelect' })
        return
      }
      if (me.activeRole !== 'DINER') {
        this.tip = '厨师工作台下一期开放，请先切换食客'
        router.replaceUrl({ url: 'pages/RoleSelect' })
        return
      }
      const binding = await MiyfApi.fetchMyBinding()
      if (binding === null || binding.status !== 'BOUND') {
        router.replaceUrl({ url: 'pages/Join' })
        return
      }
      router.replaceUrl({ url: 'pages/Main' })
    } catch (_err) {
      await SessionStore.clear(this.context())
      router.replaceUrl({ url: 'pages/Login' })
    }
  }

  build() {
    Column() {
      Text('miyf')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
      Text(this.tip)
        .fontSize(13)
        .fontColor(Theme.muted)
        .margin({ top: 12 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(Theme.cream)
  }
}
