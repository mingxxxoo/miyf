import Taro from '@tarojs/taro'
import { clearToken, getToken, get, post, put, del } from '@/api/request'
import type { PageResult } from '@/types'
import { asId } from '@/utils/id'
import { toAbsoluteResourceUrl, toResourceUrl } from '@/utils/resourceUrl'

const BASE_URL = process.env.TARO_APP_API_BASE || 'https://www.miyf.cn'

export interface KitchenVo {
  id: string
  name: string
  intro?: string
  coverImage?: string
  status?: string
}

export interface BindingVo {
  id: string
  kitchenId: string
  kitchenName?: string
  dinerNickname?: string
  status: string
  rejectReason?: string
}

export interface InviteVo {
  code: string
  token: string
  joinPath: string
}

export interface ChefDish {
  id: string
  name: string
  subtitle?: string
  description?: string
  status: string
  auditStatus?: string
  rejectReason?: string
  stock?: number
  stockType?: string
  unit?: string
  categoryId?: string
  categoryName?: string
  coverImage?: string
  coverUrl?: string
  images?: string[]
  recommend?: boolean
}

export interface ChefDishSavePayload {
  name: string
  stockType: string
  stock?: number
  unit?: string
  categoryId?: string
  subtitle?: string
  description?: string
  coverImage?: string
  images?: string[]
  recommend?: boolean
}

function mapChefDish(raw: ChefDish): ChefDish {
  const cover = raw.coverImage || raw.coverUrl || raw.images?.[0]
  return {
    ...raw,
    id: asId(raw.id),
    categoryId: raw.categoryId != null ? asId(raw.categoryId) : undefined,
    coverImage: toResourceUrl(cover) || undefined,
    coverUrl: toAbsoluteResourceUrl(cover) || undefined,
    images: (raw.images || [])
      .map((u) => toAbsoluteResourceUrl(u) || '')
      .filter(Boolean),
    recommend: Boolean(raw.recommend)
  }
}

export async function chooseRole(role: 'CHEF' | 'DINER') {
  return post('/api/user/me/role', { role })
}

export async function switchRole(role: 'CHEF' | 'DINER') {
  return put('/api/user/me/role', { role })
}

/** 首次开通用 choose；已开通该身份则 switch 工作台 */
export async function activateOrSwitchRole(
  role: 'CHEF' | 'DINER',
  profile?: { chef?: boolean; diner?: boolean } | null
) {
  const already =
    (role === 'CHEF' && profile?.chef) || (role === 'DINER' && profile?.diner)
  if (already) {
    return switchRole(role)
  }
  return chooseRole(role)
}

export async function fetchMyKitchen(): Promise<KitchenVo | null> {
  return get<KitchenVo | null>('/api/my-kitchen', undefined, { showError: false }).catch(() => null)
}

export async function saveMyKitchen(payload: { name: string; intro?: string; status?: string }) {
  return put<KitchenVo>('/api/my-kitchen', payload)
}

export async function fetchInvite(): Promise<InviteVo> {
  return get<InviteVo>('/api/my-kitchen/invite')
}

export async function rotateInvite(): Promise<InviteVo> {
  return post<InviteVo>('/api/my-kitchen/invite/rotate')
}

export async function fetchMyBinding(): Promise<BindingVo | null> {
  return get<BindingVo | null>('/api/bindings/mine', undefined, { showError: false }).catch(() => null)
}

export async function applyBinding(payload: { code?: string; token?: string }) {
  return post<BindingVo>('/api/bindings/apply', payload)
}

export async function fetchChefBindings(status?: string): Promise<PageResult<BindingVo>> {
  return get<PageResult<BindingVo>>('/api/bindings', { status, page: 1, rows: 50 })
}

export async function approveBinding(id: string) {
  return post(`/api/bindings/${asId(id)}/approve`)
}

export async function rejectBinding(id: string, reason?: string) {
  return post(`/api/bindings/${asId(id)}/reject`, { reason })
}

export async function unbindBinding(id: string) {
  return post(`/api/bindings/${asId(id)}/unbind`)
}

export async function fetchChefDishes(): Promise<PageResult<ChefDish>> {
  const page = await get<PageResult<ChefDish>>('/api/chef/dishes', { page: 1, rows: 50 })
  return {
    ...page,
    records: (page.records || []).map(mapChefDish)
  }
}

export async function createChefDish(payload: ChefDishSavePayload) {
  return post<ChefDish>('/api/chef/dishes', payload).then(mapChefDish)
}

export async function updateChefDish(id: string, payload: ChefDishSavePayload) {
  return put<ChefDish>(`/api/chef/dishes/${asId(id)}`, payload).then(mapChefDish)
}

export async function submitChefDish(id: string) {
  return post(`/api/chef/dishes/${asId(id)}/submit-audit`)
}

export async function withdrawChefDish(id: string) {
  return post(`/api/chef/dishes/${asId(id)}/withdraw-audit`)
}

export async function publishChefDish(id: string) {
  return post(`/api/chef/dishes/${asId(id)}/publish`)
}

export async function unpublishChefDish(id: string) {
  return post(`/api/chef/dishes/${asId(id)}/unpublish`)
}

export async function deleteChefDish(id: string) {
  return del(`/api/chef/dishes/${asId(id)}`)
}

/** 上传厨师菜品图片，返回可写入 DTO 的 /r/{id} 路径 */
export async function uploadChefDishImage(filePath: string): Promise<string> {
  const token = getToken()
  const res = await Taro.uploadFile({
    url: `${BASE_URL}/api/chef/dishes/images/upload`,
    filePath,
    name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (res.statusCode === 401) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  let body: { code?: number; message?: string; data?: { url?: string } } | null = null
  try {
    body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data as typeof body)
  } catch {
    throw new Error('图片上传失败')
  }
  if (body && body.code === 40100) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  const url = body?.data?.url
  if (!body || body.code !== 0 || !url) {
    throw new Error(body?.message || '图片上传失败')
  }
  return toResourceUrl(url) || url
}

export async function fetchChefOrders(status?: string): Promise<PageResult<ChefOrder>> {
  const page = await get<PageResult<ChefOrderRaw>>('/api/chef/orders', {
    page: 1,
    rows: 50,
    status: status || undefined
  })
  return {
    ...page,
    records: (page.records || []).map(mapChefOrder)
  }
}

export async function updateChefOrderStatus(id: string, status: string) {
  return put(`/api/chef/orders/${asId(id)}/status`, { status })
}

export interface ChefCategory {
  id: string
  name: string
  icon?: string
  sortOrder?: number
  status?: string
}

export async function fetchChefCategories(): Promise<ChefCategory[]> {
  const list = await get<ChefCategory[]>('/api/chef/categories')
  return (list || []).map((c) => ({
    ...c,
    id: asId(c.id)
  }))
}

export async function createChefCategory(payload: {
  name: string
  sortOrder?: number
  status?: string
}) {
  return post<ChefCategory>('/api/chef/categories', payload).then((c) => ({
    ...c,
    id: asId(c.id)
  }))
}

export async function updateChefCategory(
  id: string,
  payload: { name: string; sortOrder?: number; status?: string }
) {
  return put<ChefCategory>(`/api/chef/categories/${asId(id)}`, payload).then((c) => ({
    ...c,
    id: asId(c.id)
  }))
}

export async function deleteChefCategory(id: string) {
  return del(`/api/chef/categories/${asId(id)}`)
}

export interface ChefRecipe {
  id?: string
  dishId: string
  dishName?: string
  description?: string
  difficulty?: string
  prepareMinutes?: number
  cookMinutes?: number
  servings?: number
  tips?: string
  ingredients?: { name: string; amount?: string }[]
  seasonings?: { name: string; amount?: string }[]
  steps?: { step: number; title?: string; description?: string }[]
}

export async function fetchChefRecipe(dishId: string): Promise<ChefRecipe | null> {
  const raw = await get<ChefRecipe | null>(`/api/chef/dishes/${asId(dishId)}/recipe`, undefined, {
    showError: false
  }).catch(() => null)
  if (!raw) return null
  return {
    ...raw,
    id: raw.id != null ? asId(raw.id) : undefined,
    dishId: asId(raw.dishId)
  }
}

export async function saveChefRecipe(
  dishId: string,
  payload: {
    description?: string
    difficulty?: string
    prepareMinutes?: number
    cookMinutes?: number
    servings?: number
    tips?: string
    ingredients?: { name: string; amount: string }[]
    seasonings?: { name: string; amount: string }[]
    steps?: { step: number; title?: string; description: string }[]
  }
) {
  return put<ChefRecipe>(`/api/chef/dishes/${asId(dishId)}/recipe`, {
    dishId: asId(dishId),
    ...payload
  })
}

export async function deleteChefRecipe(dishId: string) {
  return del(`/api/chef/dishes/${asId(dishId)}/recipe`)
}

export interface ChefOrderItem {
  id?: string
  dishId: string
  dishName: string
  quantity: number
  unit?: string
  remark?: string
  coverUrl?: string
}

export interface ChefOrder {
  id: string
  orderNo: string
  userId?: string
  userNickname?: string
  status: string
  remark?: string
  createTime?: string
  items: ChefOrderItem[]
}

interface ChefOrderRaw {
  id: string
  orderNo: string
  userId?: string
  userNickname?: string
  status: string
  remark?: string
  createTime?: string
  items?: {
    id?: string
    dishId: string
    dishName: string
    quantity: number
    unit?: string
    remark?: string
    coverImage?: string
    coverUrl?: string
  }[]
}

function mapChefOrder(raw: ChefOrderRaw): ChefOrder {
  return {
    id: asId(raw.id),
    orderNo: raw.orderNo,
    userId: raw.userId != null ? asId(raw.userId) : undefined,
    userNickname: raw.userNickname,
    status: raw.status,
    remark: raw.remark,
    createTime: raw.createTime,
    items: (raw.items || []).map((it) => ({
      id: it.id != null ? asId(it.id) : undefined,
      dishId: asId(it.dishId),
      dishName: it.dishName,
      quantity: it.quantity,
      unit: it.unit,
      remark: it.remark,
      coverUrl: toAbsoluteResourceUrl(it.coverImage || it.coverUrl) || undefined
    }))
  }
}
