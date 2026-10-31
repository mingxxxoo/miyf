import Taro from '@tarojs/taro'
import {clearToken, getToken, post} from '@/api/request'
import type {DishTemplateIngredient, DishTemplateStep} from '@/types'
import {toResourceUrl} from '@/utils/resourceUrl'

const BASE_URL = process.env.TARO_APP_API_BASE || 'https://www.miyf.cn'

const AI_URL = '/api/chef/ai/dish-templates'

/** AI 建菜来源：文字 / 图片 / 网页链接 */
export type DishAiSourceType = 'AI_TEXT' | 'AI_IMAGE' | 'AI_URL'

/** AI 自报的待确认字段 */
export interface DishAiUncertainField {
  path: string
  label: string
  confidence: string
}

interface DishTemplateAiDishRaw {
  name?: string
  description?: string
  servings?: number
  difficulty?: string
  tags?: string[]
  ingredients?: DishTemplateIngredient[]
  steps?: DishTemplateStep[]
  estimatedTotalSeconds?: number
}

interface DishTemplateAiRaw {
  degraded?: boolean
  rejectReason?: string
  sourceType?: string
  sourceUrl?: string
  coverImage?: string
  overallConfidence?: string
  dish?: DishTemplateAiDishRaw
  uncertainFields?: DishAiUncertainField[]
  reviewHints?: string[]
  aiTruncated?: boolean
}

/** AI 建菜确认页数据（只生成草稿，不落库） */
export interface DishTemplateAiResult {
  degraded: boolean
  rejectReason?: string
  sourceType: DishAiSourceType
  sourceUrl?: string
  coverImage?: string
  overallConfidence: 'HIGH' | 'MEDIUM' | 'LOW'
  dish: {
    name: string
    description: string
    servings?: number
    difficulty?: string
    tags: string[]
    ingredients: DishTemplateIngredient[]
    steps: DishTemplateStep[]
    estimatedTotalSeconds?: number
  }
  uncertainFields: DishAiUncertainField[]
  reviewHints: string[]
  aiTruncated: boolean
}

function normalizeConfidence(value?: string): 'HIGH' | 'MEDIUM' | 'LOW' {
  const upper = (value || '').toUpperCase()
  if (upper === 'HIGH' || upper === 'MEDIUM') return upper
  return 'LOW'
}

function normalizeSourceType(value?: string): DishAiSourceType {
  if (value === 'AI_IMAGE' || value === 'AI_URL') return value
  return 'AI_TEXT'
}

function mapResult(raw: DishTemplateAiRaw): DishTemplateAiResult {
  const dish = raw.dish || {}
  return {
    degraded: Boolean(raw.degraded),
    rejectReason: raw.rejectReason || undefined,
    sourceType: normalizeSourceType(raw.sourceType),
    sourceUrl: raw.sourceUrl || undefined,
    coverImage: toResourceUrl(raw.coverImage),
    overallConfidence: normalizeConfidence(raw.overallConfidence),
    dish: {
      name: dish.name || '',
      description: dish.description || '',
      servings: dish.servings,
      difficulty: dish.difficulty,
      tags: dish.tags || [],
      ingredients: (dish.ingredients || []).map((item) => ({
        ...item,
        required: item.required !== false
      })),
      steps: (dish.steps || []).map((step, index) => ({
        ...step,
        step: step.step ?? step.order ?? index + 1,
        order: step.order ?? step.step ?? index + 1,
        description: step.description || '',
        timerType: step.timerType || 'none',
        required: step.required !== false,
        canUndo: step.canUndo !== false
      })),
      estimatedTotalSeconds: dish.estimatedTotalSeconds
    },
    uncertainFields: raw.uncertainFields || [],
    reviewHints: raw.reviewHints || [],
    aiTruncated: Boolean(raw.aiTruncated)
  }
}

/** 文本或网页链接识别，文本优先 */
export async function extractDishTemplateFromText(params: {
  text?: string
  url?: string
}): Promise<DishTemplateAiResult> {
  const raw = await post<DishTemplateAiRaw>(
    `${AI_URL}/extract`,
    {
      text: params.text,
      url: params.url
    },
    { showError: false }
  )
  return mapResult(raw)
}

function assertUploadBody(body: { code?: number; message?: string; data?: DishTemplateAiRaw } | null) {
  if (body && body.code === 40100) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  if (!body || body.code !== 0 || !body.data) {
    throw new Error(body?.message || 'AI 识别失败')
  }
  return body.data
}

/** 菜品照片识别；text 为可选补充说明 */
export async function extractDishTemplateFromImage(
  filePath: string,
  text?: string
): Promise<DishTemplateAiResult> {
  const token = getToken()
  const formData: Record<string, string> = {}
  if (text && text.trim()) {
    formData.text = text.trim()
  }
  let res: Awaited<ReturnType<typeof Taro.uploadFile>>
  try {
    res = await Taro.uploadFile({
      url: `${BASE_URL}${AI_URL}/extract/image`,
      filePath,
      name: 'file',
      formData,
      header: token ? { Authorization: `Bearer ${token}` } : {}
    })
  } catch (err) {
    const errMsg =
      err && typeof err === 'object' && typeof (err as { errMsg?: string }).errMsg === 'string'
        ? (err as { errMsg: string }).errMsg
        : ''
    throw new Error(/timeout/i.test(errMsg) ? '识别超时，请稍后重试' : '图片上传失败，请检查网络后重试')
  }
  if (res.statusCode === 401) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  let body: { code?: number; message?: string; data?: DishTemplateAiRaw } | null = null
  try {
    body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data as typeof body)
  } catch {
    throw new Error('AI 识别失败')
  }
  return mapResult(assertUploadBody(body))
}
