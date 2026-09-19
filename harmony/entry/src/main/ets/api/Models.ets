export interface LoginData {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  username?: string
  avatarUrl?: string
}

export interface UserProfile {
  id: string
  nickname: string
  username?: string
  phone?: string
  avatarUrl?: string
  activeRole?: string
  chef?: boolean
  diner?: boolean
}

export interface Binding {
  id: string
  kitchenName?: string
  status: string
  rejectReason?: string
  ownerSelf?: boolean
}

export interface Category {
  id: string
  name: string
}

export interface Dish {
  id: string
  name: string
  subtitle?: string
  description?: string
  coverImage?: string
  categoryId?: string
  categoryName?: string
  stock?: number
  stockType?: string
  unit?: string
}

export interface RecipeStep {
  step?: number
  content?: string
  description?: string
}

export interface Recipe {
  description?: string
  steps?: RecipeStep[]
}

export interface OrderItem {
  dishId: string
  dishName: string
  quantity: number
  remark?: string
}

export interface Order {
  id: string
  orderNo: string
  status: string
  remark?: string
  createTime?: string
  items?: OrderItem[]
}

export interface DraftLine {
  dishId: string
  dishName: string
  quantity: number
}

export function asText(value: Object | string | number | undefined | null): string {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

export function orderStatusText(status: string): string {
  if (status === 'PENDING') {
    return '待确认'
  }
  if (status === 'CONFIRMED') {
    return '已确认'
  }
  if (status === 'PREPARING') {
    return '备餐中'
  }
  if (status === 'READY') {
    return '可取餐'
  }
  if (status === 'COMPLETED') {
    return '已完成'
  }
  if (status === 'CANCELLED') {
    return '已取消'
  }
  return status
}

export function bindingStatusText(status: string): string {
  if (status === 'BOUND') {
    return '已绑定'
  }
  if (status === 'PENDING') {
    return '待确认'
  }
  if (status === 'REJECTED') {
    return '未通过'
  }
  return '未绑定'
}

export function absoluteUrl(base: string, raw: string | undefined): string {
  if (raw === undefined || raw.length === 0) {
    return ''
  }
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    return raw
  }
  if (raw.startsWith('/')) {
    return base + raw
  }
  return base + '/' + raw
}
