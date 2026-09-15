import { get, post, put } from '@/api/request'
import type { PageResult } from '@/types'
import { asId } from '@/utils/id'

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
  status: string
  auditStatus?: string
  rejectReason?: string
  stock?: number
  stockType?: string
  unit?: string
  categoryId?: string
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
  return get<PageResult<ChefDish>>('/api/chef/dishes', { page: 1, rows: 50 })
}

export async function createChefDish(payload: {
  name: string
  stockType: string
  stock?: number
  unit?: string
  categoryId?: string
}) {
  return post<ChefDish>('/api/chef/dishes', payload)
}

export async function updateChefDish(
  id: string,
  payload: {
    name: string
    stockType: string
    stock?: number
    unit?: string
    categoryId?: string
  }
) {
  return put<ChefDish>(`/api/chef/dishes/${asId(id)}`, payload)
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

export async function fetchChefOrders(): Promise<PageResult<{ id: string; orderNo: string; status: string }>> {
  return get('/api/chef/orders', { page: 1, rows: 50 })
}

export async function updateChefOrderStatus(id: string, status: string) {
  return put(`/api/chef/orders/${asId(id)}/status`, { status })
}
