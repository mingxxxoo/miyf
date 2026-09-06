import { get, post } from '@/api/request'
import type { Order, OrderItem, PageResult } from '@/types'
import { pageRecords } from '@/types'
import { asId, asOptionalId } from '@/utils/id'

interface OrderItemRaw {
  id?: string
  dishId: string
  dishName: string
  quantity: number
  unit?: string
  remark?: string
}

interface OrderRaw {
  id: string
  orderNo: string
  userId?: string
  status: string
  remark?: string
  createdAt: string
  updatedAt?: string
  items?: OrderItemRaw[]
  displayTip?: string
}

function mapItem(raw: OrderItemRaw): OrderItem {
  return {
    id: asOptionalId(raw.id),
    dishId: asId(raw.dishId),
    dishName: raw.dishName,
    quantity: raw.quantity,
    unit: raw.unit,
    note: raw.remark,
    remark: raw.remark
  }
}

function mapOrder(raw: OrderRaw): Order {
  return {
    id: asId(raw.id),
    orderNo: raw.orderNo,
    userId: asOptionalId(raw.userId),
    status: String(raw.status).toUpperCase() as Order['status'],
    items: (raw.items || []).map(mapItem),
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
    note: raw.remark,
    remark: raw.remark,
    displayTip: raw.displayTip
  }
}

export async function fetchMyOrders(params?: {
  status?: string
  page?: number
  rows?: number
}): Promise<PageResult<Order>> {
  const page = await get<PageResult<OrderRaw>>('/api/orders', params as Record<string, unknown>)
  return {
    records: pageRecords(page).map(mapOrder),
    total: page.total ?? 0,
    page: page.page ?? 1,
    pageSize: page.pageSize ?? params?.rows ?? 20
  }
}

export async function fetchOrderDetail(id: string): Promise<Order> {
  const raw = await get<OrderRaw>(`/api/orders/${asId(id)}`)
  return mapOrder(raw)
}

export async function createOrder(payload: {
  remark?: string
  items: { dishId: string; quantity: number; remark?: string }[]
}): Promise<Order> {
  const body = {
    ...payload,
    items: payload.items.map((i) => ({ ...i, dishId: asId(i.dishId) }))
  }
  const raw = await post<OrderRaw>('/api/orders', body, { showLoading: true })
  return mapOrder(raw)
}

export async function cancelOrder(id: string): Promise<Order> {
  const raw = await post<OrderRaw>(`/api/orders/${asId(id)}/cancel`, undefined, { showLoading: true })
  return mapOrder(raw)
}
