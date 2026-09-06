import { create } from 'zustand'
import * as orderApi from '@/api/order'
import type { Order, OrderItem } from '@/types'

interface OrderDraft {
  items: OrderItem[]
  scheduledAt: string
  note: string
  guestCount: number
}

interface OrderState {
  orders: Order[]
  currentOrder: Order | null
  draft: OrderDraft
  loading: boolean
  fetchOrders: (status?: string) => Promise<void>
  fetchOrderDetail: (id: string) => Promise<void>
  setDraftItem: (item: OrderItem) => void
  updateDraft: (patch: Partial<OrderDraft>) => void
  clearDraft: () => void
  submitOrder: () => Promise<Order | null>
  cancelOrder: (id: string) => Promise<Order | null>
}

const emptyDraft = (): OrderDraft => ({
  items: [],
  scheduledAt: '',
  note: '',
  guestCount: 2
})

function buildRemark(draft: OrderDraft): string {
  const parts: string[] = []
  if (draft.note?.trim()) parts.push(draft.note.trim())
  if (draft.scheduledAt) parts.push(`期望日期 ${draft.scheduledAt}`)
  if (draft.guestCount) parts.push(`${draft.guestCount} 位用餐`)
  return parts.join('；') || ''
}

export const useOrderStore = create<OrderState>((set, getState) => ({
  orders: [],
  currentOrder: null,
  draft: emptyDraft(),
  loading: false,

  fetchOrders: async (status) => {
    set({ loading: true })
    try {
      const page = await orderApi.fetchMyOrders({
        status: status || undefined,
        page: 1,
        rows: 50
      })
      set({ orders: page.records || [], loading: false })
    } catch {
      set({ loading: false })
    }
  },

  fetchOrderDetail: async (id) => {
    set({ loading: true })
    try {
      const order = await orderApi.fetchOrderDetail(id)
      set({ currentOrder: order, loading: false })
    } catch {
      set({ currentOrder: null, loading: false })
    }
  },

  setDraftItem: (item) => {
    const { draft } = getState()
    const existing = draft.items.find((i) => i.dishId === item.dishId)
    const items = existing
      ? draft.items.map((i) => (i.dishId === item.dishId ? { ...i, ...item } : i))
      : [...draft.items, item]
    set({ draft: { ...draft, items } })
  },

  updateDraft: (patch) => {
    set({ draft: { ...getState().draft, ...patch } })
  },

  clearDraft: () => {
    set({ draft: emptyDraft() })
  },

  submitOrder: async () => {
    const { draft } = getState()
    if (!draft.items.length) {
      return null
    }
    set({ loading: true })
    try {
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
