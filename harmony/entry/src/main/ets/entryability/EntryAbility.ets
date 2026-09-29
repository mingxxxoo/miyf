import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit'
import { router, window } from '@kit.ArkUI'
import { SessionStore } from '../session/SessionStore'
import { AppNav } from '../session/AppNav'
import { FormCardUpdater } from '../platform/FormCardUpdater'

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, _launchParam: AbilityConstant.LaunchParam): void {
    this.captureWant(want)
  }

  onNewWant(want: Want, _launchParam: AbilityConstant.LaunchParam): void {
    this.captureWant(want)
    this.openPendingOrderIfReady()
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', (err) => {
      if (err !== undefined && err.code !== 0) {
        console.error('Failed to load Index: ' + JSON.stringify(err))
      }
    })
    void SessionStore.load(this.context).then(() => {
      void FormCardUpdater.refresh(this.context)
    })
  }

  private captureWant(want: Want): void {
    if (want.parameters === undefined) {
      return
    }
    const target = want.parameters['targetPage']
    const orderId = want.parameters['orderId']
    if (target !== undefined && String(target) === 'orderDetail' && orderId !== undefined) {
      const id = String(orderId)
      if (id.length > 0) {
        AppNav.pendingOrderId = id
      }
    }
  }

  private openPendingOrderIfReady(): void {
    const id = AppNav.pendingOrderId
    if (id.length === 0) {
      return
    }
    // 已在前台时直接进详情；冷启动由 AppNav.afterAuth 消费
    AppNav.pendingOrderId = ''
    router.pushUrl({ url: 'pages/OrderDetail', params: { id: id } }).catch(() => {
      AppNav.pendingOrderId = id
    })
  }
}
