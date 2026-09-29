import { common } from '@kit.AbilityKit'
import { formBindingData, formProvider } from '@kit.FormKit'
import { MiyfApi } from '../api/MiyfApi'
import { Order, itemSummary } from '../api/Models'
import { FormCardStore, OrderCardSnapshot } from '../session/FormCardStore'
import { ApiClient } from '../api/ApiClient'

/**
 * 主进程刷新桌面卡片：仅推送备餐中 / 可取餐的一张预约。
 */
export class FormCardUpdater {
  static async refresh(context: common.UIAbilityContext): Promise<void> {
    await FormCardStore.load(context)
    let snap: OrderCardSnapshot = FormCardStore.empty()
    if (ApiClient.getToken().length > 0) {
      try {
        const binding = await MiyfApi.fetchMyBinding()
        const kitchenName = binding !== null ? (binding.kitchenName || '胡闹厨房') : '胡闹厨房'
        const orders = await MiyfApi.fetchOrders()
        const active = FormCardUpdater.pickActive(orders)
        if (active !== null) {
          snap = FormCardStore.fromOrder(
            kitchenName,
            active.status,
            itemSummary(active.items),
            active.id
          )
        }
      } catch (_err) {
        // 保持上次快照或空态
        snap = FormCardStore.snapshotOrEmpty()
        if (snap.hasActive !== '1') {
          snap = FormCardStore.empty()
        }
      }
    }
    await FormCardStore.saveSnapshot(context, snap)
    const ids = await FormCardStore.listFormIds(context)
    const data = formBindingData.createFormBindingData(snap)
    for (let i = 0; i < ids.length; i++) {
      try {
        await formProvider.updateForm(ids[i], data)
      } catch (_err) {
      }
    }
  }

  private static pickActive(orders: Order[]): Order | null {
    const preparing = orders.find((o: Order) => o.status === 'PREPARING')
    if (preparing !== undefined) {
      return preparing
    }
    const ready = orders.find((o: Order) => o.status === 'READY')
    if (ready !== undefined) {
      return ready
    }
    return null
  }
}
