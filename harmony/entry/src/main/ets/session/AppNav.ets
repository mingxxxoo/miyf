import { router } from '@kit.ArkUI'
import { MiyfApi } from '../api/MiyfApi'
import { UserProfile } from '../api/Models'

/** 食客底栏切页：首页 0 / 菜品 1 / 预约 2 / 我的 3 */
export class AppNav {
  static tabIndex: number = 0
  /** 服务卡片深链：冷启动登录后打开预约详情 */
  static pendingOrderId: string = ''
  private static listeners: Array<() => void> = []

  static watch(fn: () => void): void {
    AppNav.listeners.push(fn)
  }

  static goTab(index: number): void {
    AppNav.tabIndex = index
    AppNav.listeners.forEach((fn: () => void) => {
      fn()
    })
  }

  static consumePendingOrderId(): string {
    const id = AppNav.pendingOrderId
    AppNav.pendingOrderId = ''
    return id
  }

  static async afterAuth(me: UserProfile): Promise<void> {
    if (me.activeRole === undefined || me.activeRole.length === 0) {
      router.replaceUrl({ url: 'pages/RoleSelect' })
      return
    }
    if (me.activeRole === 'CHEF') {
      router.replaceUrl({ url: 'pages/chef/ChefWorkbench' })
      return
    }
    const binding = await MiyfApi.fetchMyBinding()
    if (binding === null || binding.status !== 'BOUND') {
      router.replaceUrl({ url: 'pages/Join' })
      return
    }
    AppNav.tabIndex = 0
    const orderId = AppNav.consumePendingOrderId()
    await router.replaceUrl({ url: 'pages/Main' })
    if (orderId.length > 0) {
      router.pushUrl({ url: 'pages/OrderDetail', params: { id: orderId } })
    }
  }
}
