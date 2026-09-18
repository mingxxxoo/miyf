import { post } from '@/api/request'
import { asId, asOptionalId } from '@/utils/id'

/** 厨师端 AI 建菜抽取结果 */
export interface ChefDishAiExtractResult {
  degraded: boolean
  rejectReason?: string
  dishes: ChefDishAiDraft[]
}

export interface ChefDishAiDraft {
  name: string
  subtitle?: string
  categoryName?: string
  categoryId?: string
  newCategory?: boolean
  description?: string
  tags?: string[]
  stockType?: 'LIMITED' | 'UNLIMITED' | string
  stock?: number | null
  recipe?: ChefDishAiRecipeDraft
  nutrition?: Record<string, unknown>
  aiTruncated?: boolean
}

export interface ChefDishAiRecipeDraft {
  ingredients?: { name: string; amount?: string }[]
  seasonings?: { name: string; amount?: string }[]
  steps?: { step?: number; content?: string; durationMinutes?: number }[]
  difficulty?: string
  prepareMinutes?: number
  cookMinutes?: number
  servings?: number
}

/** 食客端 AI 预约草稿 */
export interface OrderDraftAiResult {
  degraded: boolean
  items: OrderDraftAiItem[]
  mealDate?: string
  mealType?: string
  guestCount?: number
  note?: string
  unmatched: OrderDraftAiUnmatched[]
  ambiguityNote?: string
}

export interface OrderDraftAiItem {
  dishId: string
  dishName: string
  quantity: number
  confidence?: string
  itemNote?: string
}

export interface OrderDraftAiUnmatched {
  rawText: string
  reason: string
}

/** 食客端饮食参考分析 */
export interface OrderInsightAiResult {
  degraded: boolean
  overallVerdict?: string
  summary?: string
  dishInsights: OrderInsightDish[]
  suggestions: OrderInsightSuggestion[]
  disclaimer?: string
}

export interface OrderInsightDish {
  dishId: string
  dishName: string
  verdict: string
  reason?: string
}

export interface OrderInsightSuggestion {
  type: string
  text: string
  replaceDishId?: string
  replaceDishName?: string
}

function mapExtract(raw: ChefDishAiExtractResult): ChefDishAiExtractResult {
  return {
    degraded: Boolean(raw?.degraded),
    rejectReason: raw?.rejectReason || undefined,
    dishes: (raw?.dishes || []).map((d) => ({
      ...d,
      categoryId: asOptionalId(d.categoryId),
      name: d.name || '',
      tags: d.tags || [],
      recipe: d.recipe
        ? {
            ...d.recipe,
            ingredients: d.recipe.ingredients || [],
            seasonings: d.recipe.seasonings || [],
            steps: d.recipe.steps || []
          }
        : undefined
    }))
  }
}

function mapDraft(raw: OrderDraftAiResult): OrderDraftAiResult {
  return {
    degraded: Boolean(raw?.degraded),
    items: (raw?.items || [])
      .filter((i) => i?.dishId)
      .map((i) => ({
        dishId: asId(i.dishId),
        dishName: i.dishName || '',
        quantity: Math.max(1, Number(i.quantity) || 1),
        confidence: i.confidence,
        itemNote: i.itemNote
      })),
    mealDate: raw?.mealDate || undefined,
    mealType: raw?.mealType || undefined,
    guestCount: raw?.guestCount ?? undefined,
    note: raw?.note || undefined,
    unmatched: (raw?.unmatched || []).map((u) => ({
      rawText: u.rawText || '',
      reason: u.reason || ''
    })),
    ambiguityNote: raw?.ambiguityNote || undefined
  }
}

function mapInsight(raw: OrderInsightAiResult): OrderInsightAiResult {
  return {
    degraded: Boolean(raw?.degraded),
    overallVerdict: raw?.overallVerdict || undefined,
    summary: raw?.summary || undefined,
    dishInsights: (raw?.dishInsights || []).map((d) => ({
      dishId: asId(d.dishId),
      dishName: d.dishName || '',
      verdict: d.verdict || 'OK',
      reason: d.reason
    })),
    suggestions: (raw?.suggestions || []).map((s) => ({
      type: s.type || 'GENERAL',
      text: s.text || '',
      replaceDishId: asOptionalId(s.replaceDishId),
      replaceDishName: s.replaceDishName
    })),
    disclaimer: raw?.disclaimer || undefined
  }
}

/** 文本/链接 → 菜品草稿（不落库） */
export async function extractChefDishes(payload: {
  text?: string
  url?: string
}): Promise<ChefDishAiExtractResult> {
  const raw = await post<ChefDishAiExtractResult>(
    '/api/chef/ai/dishes/extract',
    payload,
    { showLoading: true }
  )
  return mapExtract(raw || { degraded: true, dishes: [] })
}

/** 自然语言 → 预约草稿（不下单） */
export async function draftOrderFromText(text: string): Promise<OrderDraftAiResult> {
  const raw = await post<OrderDraftAiResult>(
    '/api/ai/orders/draft',
    { text },
    { showLoading: true }
  )
  return mapDraft(raw || { degraded: true, items: [], unmatched: [] })
}

/** 草稿 + 健康指标 → 饮食参考（失败可静默） */
export async function analyzeOrderInsight(payload: {
  dinerText: string
  draftJson: string
}): Promise<OrderInsightAiResult> {
  const raw = await post<OrderInsightAiResult>('/api/ai/orders/insight', payload, {
    showLoading: false,
    showError: false
  })
  return mapInsight(raw || { degraded: true, dishInsights: [], suggestions: [] })
}
