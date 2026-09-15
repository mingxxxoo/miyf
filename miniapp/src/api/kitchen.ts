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

export async function fetchChefOrders(): Promise<PageResult<{ id: string; orderNo: string; status: string }>> {
  return get('/api/chef/orders', { page: 1, rows: 50 })
}

export async function updateChefOrderStatus(id: string, status: string) {
  return put(`/api/chef/orders/${asId(id)}/status`, { status })
}
