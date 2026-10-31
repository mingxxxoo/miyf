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
  /** 服务端是否已评价；本地缓存作乐观兜底 */
  commented?: boolean
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
  mealDate?: string
  mealType?: string
  mealTime?: string
  guestCount?: number
  /** @deprecated 草稿本地字段，提交后见 mealDate */
  scheduledTime?: string
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

/** 菜品模板状态：草稿 / 预创建 / 已发布 / 已归档 */
export type DishTemplateStatus = 'draft' | 'pre_created' | 'published' | 'archived'

/** 步骤计时方式：倒计时 / 正计时 / 不计时 */
export type DishTimerType = 'countdown' | 'countup' | 'none'

export interface DishTemplateIngredient {
  name: string
  amount?: string
  unit?: string
  processing?: string
  required?: boolean
  substitute?: string
  note?: string
}

export interface DishTemplateStep {
  step?: number
  order?: number
  title?: string
  description: string
  image?: string
  video?: string
  durationSeconds?: number
  timerType?: DishTimerType
  parallelGroup?: string
  completionHint?: string
  warning?: string
  required?: boolean
  canUndo?: boolean
}

/** 菜品库中的菜品模板（候选菜），与点菜菜品 Dish 严格分离 */
export interface DishTemplate {
  id: string
  kitchenId?: string
  orgUnitId?: string
  name: string
  subtitle?: string
  coverUrl: string
  description?: string
  servings?: number
  difficulty?: string
  tags: string[]
  status: DishTemplateStatus
  sourceType?: string
  sourceUrl?: string
  estimatedTotalSeconds?: number
  creatorId?: string
  creatorName?: string
  createTime?: string
  lastModifyTime?: string
  editable?: boolean
  publishable?: boolean
  archivable?: boolean
  usageCount: number
  made: boolean
  ingredients: DishTemplateIngredient[]
  steps: DishTemplateStep[]
}

/** 做菜任务状态 */
export type CookingTaskStatus =
  | 'PENDING'
  | 'CLAIMED'
  | 'PREPARING'
  | 'COOKING'
  | 'PAUSED'
  | 'COMPLETED'
  | 'CANCELLED'

/** 任务创建时固化的菜品快照 */
export interface CookingDishSnapshot {
  dishId?: string
  name?: string
  coverImage?: string
  description?: string
  servings?: number
  difficulty?: string
  tags?: string[]
  estimatedTotalSeconds?: number
  sourceType?: string
  sourceUrl?: string
  ingredients?: DishTemplateIngredient[]
  seasonings?: DishTemplateIngredient[]
  steps?: DishTemplateStep[]
  capturedAt?: string
}

/** 任务步骤计时状态（内容来自快照） */
export interface CookingTaskStepState {
  id?: string
  stepOrder: number
  status: 'PENDING' | 'RUNNING' | 'PAUSED' | 'DONE'
  timerType?: DishTimerType
  durationSeconds?: number
  remainingSeconds?: number
  elapsedSeconds?: number
  startedTime?: string
  pausedTime?: string
  totalPausedSeconds?: number
  completedTime?: string
  title?: string
  description?: string
  image?: string
  video?: string
  parallelGroup?: string
  completionHint?: string
  warning?: string
  required?: boolean
  canUndo?: boolean
  current?: boolean
  canStart?: boolean
  canPause?: boolean
  canResume?: boolean
  canComplete?: boolean
  canUndoNow?: boolean
}

/** 组织协作做菜任务 */
export interface CookingTask {
  id: string
  kitchenId?: string
  orgUnitId?: string
  dishId: string
  dishSnapshot: CookingDishSnapshot
  submitterId?: string
  submitterName?: string
  chefId?: string
  chefName?: string
  servings?: number
  note?: string
  priority?: number
  claimMode?: string
  scheduledStartTime?: string
  status: CookingTaskStatus
  currentStep?: number
  startedTime?: string
  pausedTime?: string
  completedTime?: string
  totalPausedSeconds?: number
  actualDurationSeconds?: number
  createTime?: string
  lastModifyTime?: string
  steps?: CookingTaskStepState[]
  /** 服务端当前时间，用于校准本地倒计时 */
  serverTime?: string
  canClaim?: boolean
  canStart?: boolean
  canPause?: boolean
  canResume?: boolean
  canComplete?: boolean
  canRelease?: boolean
  canCancel?: boolean
  claimedByMe?: boolean
  canOpenFlow?: boolean
}

export interface User {
  id: string
  username?: string
  nickname: string
  avatarUrl?: string
  phone?: string
  wechatId?: string
  bio?: string
  activeRole?: 'CHEF' | 'DINER' | string
  chef?: boolean
  diner?: boolean
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
