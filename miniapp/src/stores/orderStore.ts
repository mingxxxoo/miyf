import Taro from '@tarojs/taro'
import { create } from 'zustand'
import { fetchDishDetail } from '@/api/dish'
import * as orderApi from '@/api/order'
import type { Order, OrderItem } from '@/types'

const DRAFT_KEY = 'miyf_order_draft'

interface OrderDraft {
  items: OrderItem[]
  scheduledTime: string
  /** 期望用餐时间 HH:mm（选填，写入备注） */
  scheduledTimeOfDay: string
  note: string
  guestCount: number
}

interface OrderState {
  orders: Order[]
  currentOrder: Order | null
  draft: OrderDraft
  loading: boolean
  loadError: boolean
  detailError: boolean
  fetchOrders: (status?: string) => Promise<void>
  fetchOrderDetail: (id: string) => Promise<void>
  setDraftItem: (item: OrderItem) => void
  updateDraftItemQty: (dishId: string, quantity: number) => void
  removeDraftItem: (dishId: string) => void
  updateDraft: (patch: Partial<OrderDraft>) => void
  clearDraft: () => void
  submitOrder: () => Promise<Order | null>
  cancelOrder: (id: string) => Promise<Order | null>
}

const emptyDraft = (): OrderDraft => ({
  items: [],
  scheduledTime: '',
  scheduledTimeOfDay: '',
  note: '',
  guestCount: 2
})

function clampQty(quantity: number): number {
  const n = Math.round(Number(quantity))
  if (!Number.isFinite(n)) return 1
  return Math.min(20, Math.max(1, n))
}

function persistDraft(draft: OrderDraft) {
  try {
    Taro.setStorageSync(DRAFT_KEY, draft)
  } catch {
    // ignore storage failures
  }
}

function readStoredDraft(): OrderDraft {
  try {
    const raw = Taro.getStorageSync(DRAFT_KEY) as Partial<OrderDraft> | ''
    if (!raw || typeof raw !== 'object') return emptyDraft()
    const items = Array.isArray(raw.items)
      ? raw.items
          .filter((i) => i && i.dishId)
          .map((i) => ({
            ...i,
            quantity: clampQty(i.quantity)
          }))
      : []
    return {
      items,
      scheduledTime: typeof raw.scheduledTime === 'string' ? raw.scheduledTime : '',
      scheduledTimeOfDay:
        typeof raw.scheduledTimeOfDay === 'string' ? raw.scheduledTimeOfDay : '',
      note: typeof raw.note === 'string' ? raw.note : '',
      guestCount: typeof raw.guestCount === 'number' && raw.guestCount > 0 ? raw.guestCount : 2
    }
  } catch {
    return emptyDraft()
  }
}

function buildRemark(draft: OrderDraft): string {
  const parts: string[] = []
  if (draft.note?.trim()) {
    parts.push(`给厨房的备注：${draft.note.trim()}`)
  }
  if (draft.scheduledTime && draft.scheduledTimeOfDay) {
    parts.push(`期望用餐 ${draft.scheduledTime} ${draft.scheduledTimeOfDay}`)
  } else if (draft.scheduledTime) {
    parts.push(`期望用餐 ${draft.scheduledTime}`)
  } else if (draft.scheduledTimeOfDay) {
    parts.push(`期望用餐时间 ${draft.scheduledTimeOfDay}`)
  }
  if (draft.guestCount) parts.push(`${draft.guestCount} 位用餐`)
  return parts.join('；') || ''
}

function setDraft(set: (partial: Partial<OrderState>) => void, draft: OrderDraft) {
  persistDraft(draft)
  set({ draft })
}

export const useOrderStore = create<OrderState>((set, getState) => ({
  orders: [],
  currentOrder: null,
  draft: readStoredDraft(),
  loading: false,
  loadError: false,
  detailError: false,

  fetchOrders: async (status) => {
    set({ loading: true })
    try {
      const page = await orderApi.fetchMyOrders({
        status: status || undefined,
        page: 1,
        rows: 100
      })
      set({ orders: page.records || [], loading: false, loadError: false })
    } catch {
      set({ loading: false, loadError: true })
    }
  },

  fetchOrderDetail: async (id) => {
    const prev = getState().currentOrder
    const softKeep = prev && prev.id === id
    set({ loading: true, detailError: false })
    try {
      const order = await orderApi.fetchOrderDetail(id)
      set({ currentOrder: order, loading: false, detailError: false })
    } catch {
      if (softKeep) {
        set({ loading: false, detailError: true })
      } else {
        set({ currentOrder: null, loading: false, detailError: true })
      }
    }
  },

  setDraftItem: (item) => {
    const { draft } = getState()
    const nextItem = { ...item, quantity: clampQty(item.quantity) }
    const existing = draft.items.find((i) => i.dishId === nextItem.dishId)
    const items = existing
      ? draft.items.map((i) => (i.dishId === nextItem.dishId ? { ...i, ...nextItem } : i))
      : [...draft.items, nextItem]
    setDraft(set, { ...draft, items })
  },

  updateDraftItemQty: (dishId, quantity) => {
    const { draft } = getState()
    if (quantity < 1) {
      const items = draft.items.filter((i) => i.dishId !== dishId)
      setDraft(set, { ...draft, items })
      return
    }
    const qty = clampQty(quantity)
    const items = draft.items.map((i) => (i.dishId === dishId ? { ...i, quantity: qty } : i))
    setDraft(set, { ...draft, items })
  },

  removeDraftItem: (dishId) => {
    const { draft } = getState()
    const items = draft.items.filter((i) => i.dishId !== dishId)
    setDraft(set, { ...draft, items })
  },

  updateDraft: (patch) => {
    setDraft(set, { ...getState().draft, ...patch })
  },

  clearDraft: () => {
    setDraft(set, emptyDraft())
  },

  submitOrder: async () => {
    const { draft } = getState()
    if (!draft.items.length) {
      return null
    }
    set({ loading: true })
    try {
      for (const item of draft.items) {
        try {
          const dish = await fetchDishDetail(item.dishId)
          if (dish.stockType === 'LIMITED') {
            const stock = dish.stock == null ? 0 : dish.stock
            if (stock < item.quantity) {
              Taro.showToast({
                title: `「${item.dishName}」库存不足（剩 ${stock}）`,
                icon: 'none'
              })
              set({ loading: false })
              return null
            }
          }
        } catch {
          Taro.showToast({ title: `无法校验「${item.dishName}」库存`, icon: 'none' })
          set({ loading: false })
          return null
        }
      }

      const order = await orderApi.createOrder({
        remark: buildRemark(draft),
        items: draft.items.map((i) => ({
          dishId: i.dishId,
          quantity: i.quantity,
          remark: i.note || i.remark
        }))
      })
      set({ loading: false })
      getState().clearDraft()
      await getState().fetchOrders()
      return order
    } catch {
      set({ loading: false })
      return null
    }
  },

  cancelOrder: async (id) => {
    set({ loading: true })
    try {
      const order = await orderApi.cancelOrder(id)
      set({
        loading: false,
        currentOrder: order,
        orders: getState().orders.map((o) => (o.id === order.id ? order : o))
      })
      return order
    } catch {
      set({ loading: false })
      return null
    }
  }
}))
