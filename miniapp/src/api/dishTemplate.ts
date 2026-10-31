import Taro from '@tarojs/taro'
import {clearToken, del, get, getToken, post, put} from '@/api/request'
import type {DishTemplate, DishTemplateIngredient, DishTemplateStatus, DishTemplateStep, PageResult} from '@/types'
import {pageRecords} from '@/types'
import {asId, asOptionalId} from '@/utils/id'
import {toResourceUrl} from '@/utils/resourceUrl'

const BASE_URL = process.env.TARO_APP_API_BASE || 'https://www.miyf.cn'

const TEMPLATE_URL = '/api/dishes/templates'

interface DishTemplateRaw {
  id: string | number
  kitchenId?: string | number
  orgUnitId?: string | number
  name?: string
  subtitle?: string
  coverImage?: string
  description?: string
  servings?: number
  difficulty?: string
  tags?: string[]
  status?: string
  sourceType?: string
  sourceUrl?: string
  estimatedTotalSeconds?: number
  creatorId?: string | number
  creatorName?: string
  createTime?: string
  lastModifyTime?: string
  editable?: boolean
  publishable?: boolean
  archivable?: boolean
  usageCount?: number
  made?: boolean
  ingredients?: DishTemplateIngredient[]
  steps?: DishTemplateStep[]
}

export interface DishTemplatePageParams {
  status?: DishTemplateStatus
  mine?: boolean
  recent?: boolean
  made?: boolean
  keyword?: string
  page?: number
  rows?: number
}

export interface DishTemplateSavePayload {
  name: string
  coverImage?: string
  description?: string
  servings?: number
  difficulty?: string
  tags?: string[]
  ingredients?: DishTemplateIngredient[]
  steps?: DishTemplateStep[]
  sourceType?: string
  sourceUrl?: string
  /** 保存状态：draft / pre_created */
  targetStatus?: 'draft' | 'pre_created'
}

function normalizeStatus(status?: string): DishTemplateStatus {
  switch ((status || '').toLowerCase()) {
    case 'pre_created':
      return 'pre_created'
    case 'published':
      return 'published'
    case 'archived':
      return 'archived'
    default:
      return 'draft'
  }
}

function mapTemplate(raw: DishTemplateRaw): DishTemplate {
  return {
    id: asId(raw.id),
    kitchenId: asOptionalId(raw.kitchenId),
    orgUnitId: asOptionalId(raw.orgUnitId),
    name: raw.name || '',
    subtitle: raw.subtitle,
    coverUrl: toResourceUrl(raw.coverImage) || '',
    description: raw.description,
    servings: raw.servings,
    difficulty: raw.difficulty,
    tags: raw.tags || [],
    status: normalizeStatus(raw.status),
    sourceType: raw.sourceType,
    sourceUrl: raw.sourceUrl,
    estimatedTotalSeconds: raw.estimatedTotalSeconds,
    creatorId: asOptionalId(raw.creatorId),
    creatorName: raw.creatorName,
    createTime: raw.createTime,
    lastModifyTime: raw.lastModifyTime,
    editable: raw.editable !== false,
    publishable: Boolean(raw.publishable),
    archivable: Boolean(raw.archivable),
    usageCount: raw.usageCount ?? 0,
    made: Boolean(raw.made ?? (raw.usageCount ?? 0) > 0),
    ingredients: (raw.ingredients || []).map((item) => ({
      ...item,
      required: item.required !== false
    })),
    steps: (raw.steps || []).map((step, index) => ({
      ...step,
      step: step.step ?? step.order ?? index + 1,
      order: step.order ?? step.step ?? index + 1,
      description: step.description || '',
      timerType: step.timerType || 'none',
      required: step.required !== false,
      canUndo: step.canUndo !== false
    }))
  }
}

export async function fetchDishTemplates(
  params?: DishTemplatePageParams
): Promise<PageResult<DishTemplate>> {
  const page = await get<PageResult<DishTemplateRaw>>(TEMPLATE_URL, {
    page: params?.page ?? 1,
    rows: params?.rows ?? 20,
    status: params?.status,
    mine: params?.mine,
    recent: params?.recent,
    made: params?.made,
    keyword: params?.keyword
  })
  return {
    records: pageRecords(page).map(mapTemplate),
    total: page.total ?? 0,
    page: page.page ?? params?.page ?? 1,
    pageSize: page.pageSize ?? params?.rows ?? 20
  }
}

export async function fetchDishTemplateDetail(id: string): Promise<DishTemplate> {
  const raw = await get<DishTemplateRaw>(`${TEMPLATE_URL}/${asId(id)}`)
  return mapTemplate(raw)
}

export async function createDishTemplate(payload: DishTemplateSavePayload): Promise<string> {
  const id = await post<string | number>(TEMPLATE_URL, payload as unknown as Record<string, unknown>)
  return asId(id)
}

export async function updateDishTemplate(id: string, payload: DishTemplateSavePayload): Promise<void> {
  await put(`${TEMPLATE_URL}/${asId(id)}`, payload as unknown as Record<string, unknown>)
}

export async function deleteDishTemplate(id: string): Promise<void> {
  await del(`${TEMPLATE_URL}/${asId(id)}`)
}

export async function publishDishTemplate(id: string): Promise<void> {
  await post(`${TEMPLATE_URL}/${asId(id)}/publish`)
}

export async function archiveDishTemplate(id: string): Promise<void> {
  await post(`${TEMPLATE_URL}/${asId(id)}/archive`)
}

/** 上传菜品模板图片，返回可写入 DTO 的 /r/{id} 路径 */
export async function uploadDishTemplateImage(filePath: string): Promise<string> {
  const token = getToken()
  const res = await Taro.uploadFile({
    url: `${BASE_URL}${TEMPLATE_URL}/images/upload`,
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
