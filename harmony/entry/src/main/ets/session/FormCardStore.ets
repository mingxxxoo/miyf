import { common } from '@kit.AbilityKit'
import { preferences } from '@kit.ArkData'

const PREF = 'miyf_form_card'
const KEY_IDS = 'formIds'
const KEY_SNAP = 'snapshot'

export interface OrderCardSnapshot {
  kitchenName: string
  statusText: string
  statusHint: string
  dishSummary: string
  orderId: string
  hasActive: string
  emptyTip: string
}

const EMPTY: OrderCardSnapshot = {
  kitchenName: '胡闹厨房',
  statusText: '',
  statusHint: '',
  dishSummary: '',
  orderId: '',
  hasActive: '0',
  emptyTip: '暂无进行中的预约'
}

/** 服务卡片 formId 与展示快照（主进程写、Form 进程读） */
export class FormCardStore {
  private static memory: OrderCardSnapshot = EMPTY

  static snapshotOrEmpty(): OrderCardSnapshot {
    return FormCardStore.memory
  }

  static async load(context: common.Context): Promise<OrderCardSnapshot> {
    try {
      const pref = await preferences.getPreferences(context, PREF)
      const raw = await pref.get(KEY_SNAP, '') as string
      if (raw.length === 0) {
        FormCardStore.memory = EMPTY
        return EMPTY
      }
      const parsed = JSON.parse(raw) as OrderCardSnapshot
      FormCardStore.memory = parsed
      return parsed
    } catch (_err) {
      FormCardStore.memory = EMPTY
      return EMPTY
    }
  }

  static async saveSnapshot(context: common.Context, snap: OrderCardSnapshot): Promise<void> {
    FormCardStore.memory = snap
    const pref = await preferences.getPreferences(context, PREF)
    await pref.put(KEY_SNAP, JSON.stringify(snap))
    await pref.flush()
  }

  static async listFormIds(context: common.Context): Promise<string[]> {
    try {
      const pref = await preferences.getPreferences(context, PREF)
      const raw = await pref.get(KEY_IDS, '[]') as string
      return JSON.parse(raw) as string[]
    } catch (_err) {
      return []
    }
  }

  static async addFormId(context: common.Context, formId: string): Promise<void> {
    if (formId.length === 0) {
      return
    }
    const ids = await FormCardStore.listFormIds(context)
    if (ids.indexOf(formId) >= 0) {
      return
    }
    ids.push(formId)
    const pref = await preferences.getPreferences(context, PREF)
    await pref.put(KEY_IDS, JSON.stringify(ids))
    await pref.flush()
  }

  static async removeFormId(context: common.Context, formId: string): Promise<void> {
    const ids = (await FormCardStore.listFormIds(context)).filter((id: string) => id !== formId)
    const pref = await preferences.getPreferences(context, PREF)
    await pref.put(KEY_IDS, JSON.stringify(ids))
    await pref.flush()
  }

  static empty(): OrderCardSnapshot {
    return {
      kitchenName: '胡闹厨房',
      statusText: '',
      statusHint: '',
      dishSummary: '',
      orderId: '',
      hasActive: '0',
      emptyTip: '暂无进行中的预约'
    }
  }

  static fromOrder(kitchenName: string, status: string, dishSummary: string, orderId: string): OrderCardSnapshot {
    let statusText = ''
    let statusHint = ''
    if (status === 'PREPARING') {
      statusText = '备餐中'
      statusHint = '做好会提醒你来取'
    } else if (status === 'READY') {
      statusText = '可取餐'
      statusHint = '可以来取啦'
    } else {
      return FormCardStore.empty()
    }
    return {
      kitchenName: kitchenName.length > 0 ? kitchenName : '胡闹厨房',
      statusText: statusText,
      statusHint: statusHint,
      dishSummary: dishSummary,
      orderId: orderId,
      hasActive: '1',
      emptyTip: ''
    }
  }
}
