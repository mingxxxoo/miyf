import Taro from '@tarojs/taro'
import type {DishAiSourceType, DishAiUncertainField} from '@/api/dishTemplateAi'
import type {DishTemplateIngredient, DishTemplateStep} from '@/types'

/**
 * AI 建菜草稿：AI 页识别成功后暂存，确认页读取后进入编辑。
 * <p>
 * 只保留一份最新草稿，24 小时后自动失效，避免用户下次进入看到过期内容。
 */

const DRAFT_KEY = 'miyf_dish_ai_draft'
const MAX_AGE_MS = 24 * 60 * 60 * 1000

export interface DishAiDraft {
  sourceType: DishAiSourceType
  sourceUrl?: string
  coverImage?: string
  name?: string
  description?: string
  servings?: number
  difficulty?: string
  tags?: string[]
  ingredients?: DishTemplateIngredient[]
  steps?: DishTemplateStep[]
  overallConfidence?: string
  uncertainFields?: DishAiUncertainField[]
  reviewHints?: string[]
  degraded?: boolean
  rejectReason?: string
  createdAt: number
}

/** 暂存最新一次 AI 草稿 */
export function saveDishAiDraft(draft: DishAiDraft): void {
  try {
    Taro.setStorageSync(DRAFT_KEY, draft)
  } catch {
    // 存储失败不阻断确认页
  }
}

/** 读取 AI 草稿；不存在或已过期返回 null */
export function loadDishAiDraft(): DishAiDraft | null {
  try {
    const draft = Taro.getStorageSync(DRAFT_KEY) as DishAiDraft | ''
    if (!draft || typeof draft !== 'object' || !draft.createdAt) {
      return null
    }
    if (Date.now() - draft.createdAt > MAX_AGE_MS) {
      clearDishAiDraft()
      return null
    }
    return draft
  } catch {
    return null
  }
}

/** 保存成功后清理草稿 */
export function clearDishAiDraft(): void {
  try {
    Taro.removeStorageSync(DRAFT_KEY)
  } catch {
    // ignore
  }
}
