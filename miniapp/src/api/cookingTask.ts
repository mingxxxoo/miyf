import {get, post} from '@/api/request'
import type {
    CookingDishSnapshot,
    CookingTask,
    CookingTaskStatus,
    CookingTaskStepState,
    DishTimerType,
    PageResult
} from '@/types'
import {pageRecords} from '@/types'
import {asId, asOptionalId} from '@/utils/id'

const TASK_URL = '/api/cooking-tasks'

interface CookingTaskRaw {
  id: string | number
  kitchenId?: string | number
  orgUnitId?: string | number
  dishId?: string | number
  dishSnapshot?: CookingDishSnapshot
  submitterId?: string | number
  submitterName?: string
  chefId?: string | number
  chefName?: string
  servings?: number
  note?: string
  priority?: number
  claimMode?: string
  scheduledStartTime?: string
  status?: string
  currentStep?: number
  startedTime?: string
  pausedTime?: string
  completedTime?: string
  totalPausedSeconds?: number
  actualDurationSeconds?: number
  createTime?: string
  lastModifyTime?: string
  steps?: CookingTaskStepState[]
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

export type CookingTaskScope = 'pool' | 'submitted' | 'assigned' | 'all'

export interface CookingTaskPageParams {
  scope?: CookingTaskScope
  status?: CookingTaskStatus
  keyword?: string
  page?: number
  rows?: number
}

export interface CookingTaskCreatePayload {
  dishId: string
  servings?: number
  note?: string
  priority?: number
  scheduledStartTime?: string
  submitToken?: string
}

function normalizeStatus(status?: string): CookingTaskStatus {
  switch ((status || '').toUpperCase()) {
    case 'CLAIMED':
      return 'CLAIMED'
    case 'PREPARING':
      return 'PREPARING'
    case 'COOKING':
      return 'COOKING'
    case 'PAUSED':
      return 'PAUSED'
    case 'COMPLETED':
      return 'COMPLETED'
    case 'CANCELLED':
      return 'CANCELLED'
    default:
      return 'PENDING'
  }
}

function normalizeTimerType(timerType?: string): DishTimerType {
  if (timerType === 'countdown' || timerType === 'countup') return timerType
  return 'none'
}

function mapStep(raw: CookingTaskStepState): CookingTaskStepState {
  return {
    ...raw,
    id: asOptionalId(raw.id),
    stepOrder: raw.stepOrder ?? 0,
    status: raw.status || 'PENDING',
    timerType: normalizeTimerType(raw.timerType),
    durationSeconds: raw.durationSeconds,
    remainingSeconds: raw.remainingSeconds,
    elapsedSeconds: raw.elapsedSeconds,
    required: raw.required !== false,
    canUndo: raw.canUndo !== false,
    current: Boolean(raw.current),
    canStart: Boolean(raw.canStart),
    canPause: Boolean(raw.canPause),
    canResume: Boolean(raw.canResume),
    canComplete: Boolean(raw.canComplete),
    canUndoNow: Boolean(raw.canUndoNow)
  }
}

function mapTask(raw: CookingTaskRaw): CookingTask {
  return {
    id: asId(raw.id),
    kitchenId: asOptionalId(raw.kitchenId),
    orgUnitId: asOptionalId(raw.orgUnitId),
    dishId: asId(raw.dishId),
    dishSnapshot: {
      ...(raw.dishSnapshot || {}),
      dishId: asOptionalId(raw.dishSnapshot?.dishId),
      tags: raw.dishSnapshot?.tags || [],
      ingredients: raw.dishSnapshot?.ingredients || [],
      seasonings: raw.dishSnapshot?.seasonings || [],
      steps: raw.dishSnapshot?.steps || []
    },
    submitterId: asOptionalId(raw.submitterId),
    submitterName: raw.submitterName,
    chefId: asOptionalId(raw.chefId),
    chefName: raw.chefName,
    servings: raw.servings,
    note: raw.note,
    priority: raw.priority,
    claimMode: raw.claimMode,
    scheduledStartTime: raw.scheduledStartTime,
    status: normalizeStatus(raw.status),
    currentStep: raw.currentStep,
    startedTime: raw.startedTime,
    pausedTime: raw.pausedTime,
    completedTime: raw.completedTime,
    totalPausedSeconds: raw.totalPausedSeconds,
    actualDurationSeconds: raw.actualDurationSeconds,
    createTime: raw.createTime,
    lastModifyTime: raw.lastModifyTime,
    steps: raw.steps?.map(mapStep),
    serverTime: raw.serverTime,
    canClaim: Boolean(raw.canClaim),
    canStart: Boolean(raw.canStart),
    canPause: Boolean(raw.canPause),
    canResume: Boolean(raw.canResume),
    canComplete: Boolean(raw.canComplete),
    canRelease: Boolean(raw.canRelease),
    canCancel: Boolean(raw.canCancel),
    claimedByMe: Boolean(raw.claimedByMe),
    canOpenFlow: Boolean(raw.canOpenFlow)
  }
}

export async function fetchCookingTasks(
  params?: CookingTaskPageParams
): Promise<PageResult<CookingTask>> {
  const page = await get<PageResult<CookingTaskRaw>>(TASK_URL, {
    page: params?.page ?? 1,
    rows: params?.rows ?? 20,
    scope: params?.scope,
    status: params?.status,
    keyword: params?.keyword
  })
  return {
    records: pageRecords(page).map(mapTask),
    total: page.total ?? 0,
    page: page.page ?? params?.page ?? 1,
    pageSize: page.pageSize ?? params?.rows ?? 20
  }
}

export async function fetchCookingTaskDetail(id: string): Promise<CookingTask> {
  const raw = await get<CookingTaskRaw>(`${TASK_URL}/${asId(id)}`)
  return mapTask(raw)
}

export async function createCookingTask(payload: CookingTaskCreatePayload): Promise<string> {
  const id = await post<string | number>(TASK_URL, payload as unknown as Record<string, unknown>)
  return asId(id)
}

/** 生成一次提交令牌：进入提交页时生成，重试复用，避免重复任务 */
export function newSubmitToken(): string {
  const random = Math.random().toString(36).slice(2, 10)
  return `ct-${Date.now()}-${random}`
}

export async function claimCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/claim`)
}

export async function releaseCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/release`)
}

export async function startCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/start`)
}

export async function pauseCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/pause`)
}

export async function resumeCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/resume`)
}

export async function completeCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/complete`)
}

export async function cancelCookingTask(id: string): Promise<void> {
  await post(`${TASK_URL}/${asId(id)}/cancel`)
}

const STEP_ACTIONS = ['start', 'pause', 'resume', 'complete', 'undo'] as const

export type CookingStepAction = (typeof STEP_ACTIONS)[number]

/** 步骤动作：服务端返回最新任务详情，便于流程页一次性刷新 */
export async function operateCookingStep(
  id: string,
  stepOrder: number,
  action: CookingStepAction
): Promise<CookingTask> {
  const raw = await post<CookingTaskRaw>(
    `${TASK_URL}/${asId(id)}/steps/${stepOrder}/${action}`
  )
  return mapTask(raw)
}
