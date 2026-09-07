export interface Category {
  id: string
  name: string
  icon?: string
  sortOrder?: number
  status?: string
}

export interface RecipeStep {
  step: number
  title?: string
  description?: string
  content?: string
  image?: string
  imageUrl?: string
  durationMinutes?: number
}

export interface Recipe {
  id: string
  dishId: string
  description?: string
  difficulty?: string
  prepareMinutes?: number
  cookMinutes?: number
  servings?: number
  steps: RecipeStep[]
  tips?: string
  ingredients?: { name: string; amount?: string }[]
  seasonings?: { name: string; amount?: string }[]
  nutrition?: Record<string, unknown>
}

export interface Dish {
  id: string
  name: string
  subtitle?: string
  description: string
  coverUrl: string
  categoryId: string
  categoryName?: string
  status?: string
  recommend?: boolean
  stock?: number
  stockType?: string
  unit?: string
  tags?: string[]
  difficulty?: string
  prepMinutes?: number
  rating?: number
  ratingCount?: number
  images?: string[]
  recipe?: Recipe
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'COMPLETED'
  | 'CANCELLED'

export interface OrderItem {
  id?: string
  dishId: string
  dishName: string
  coverUrl?: string
  quantity: number
  unit?: string
  note?: string
  remark?: string
}

export interface Order {
  id: string
  orderNo: string
  userId?: string
  status: OrderStatus
  items: OrderItem[]
  createTime: string
  lastModifyTime?: string
  /** 展示用：整单备注 */
  note?: string
  remark?: string
  displayTip?: string
  /** 草稿辅助字段，后端无对应列 */
  scheduledTime?: string
  guestCount?: number
}

export interface Comment {
  id: string
  orderId: string
  dishId?: string
  rating: number
  content?: string
  images?: string[]
  status?: string
  createTime: string
  userNickname?: string
  userAvatar?: string
}

export interface User {
  id: string
  username?: string
  nickname: string
  avatarUrl?: string
  phone?: string
  wechatId?: string
  bio?: string
}

export interface LoginResult {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  principalType?: string
}

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** @deprecated use ApiResult */
export type ApiResponse<T = unknown> = ApiResult<T>

export interface PageResult<T> {
  records?: T[]
  list?: T[]
  total: number
  page: number
  pageSize: number
}

export function pageRecords<T>(page?: PageResult<T> | T[] | null): T[] {
  if (!page) return []
  if (Array.isArray(page)) return page
  return page.records || page.list || []
}
