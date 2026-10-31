import Taro from '@tarojs/taro'
import type {CookingStepAction} from '@/api/cookingTask'
import type {CookingTask} from '@/types'

/**
 * 做菜流程离线缓存与待同步动作。
 * <p>
 * 做菜时网络可能不稳定：最后一次任务详情写入本地，断网时流程页仍可显示倒计时；
 * 操作先入队，恢复网络后按顺序补发。服务端步骤动作本身幂等，重复补发不会产生副作用。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */

export type CookTaskAction = 'pause' | 'resume' | 'complete'

export type CookPendingAction =
  | { key: string; type: 'step'; stepOrder: number; action: CookingStepAction }
  | { key: string; type: 'task'; action: CookTaskAction }

const CACHE_PREFIX = 'miyf_cook_task_'
const QUEUE_PREFIX = 'miyf_cook_queue_'

interface CachedTask {
  task: CookingTask
  cachedAt: number
}

/**
 * 缓存任务详情，供断网时展示。
 *
 * @param task 任务详情
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function cacheCookingTask(task: CookingTask): void {
  try {
    const payload: CachedTask = { task, cachedAt: Date.now() }
    Taro.setStorageSync(`${CACHE_PREFIX}${task.id}`, payload)
  } catch {
    // 本地存储失败不影响在线流程
  }
}

/**
 * 读取缓存的任务详情。
 *
 * @param taskId 任务 ID
 * @return 缓存任务，不存在返回 null
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function loadCachedCookingTask(taskId: string): CookingTask | null {
  try {
    const cached = Taro.getStorageSync(`${CACHE_PREFIX}${taskId}`) as CachedTask | ''
    if (!cached || !cached.task) return null
    return cached.task
  } catch {
    return null
  }
}

/**
 * 读取待同步动作队列。
 *
 * @param taskId 任务 ID
 * @return 动作列表（按加入顺序）
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function loadPendingActions(taskId: string): CookPendingAction[] {
  try {
    const list = Taro.getStorageSync(`${QUEUE_PREFIX}${taskId}`) as CookPendingAction[] | ''
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

/**
 * 入队一个动作；相同 key 只保留一次，避免离线反复点击造成重复补发。
 *
 * @param taskId 任务 ID
 * @param action 动作
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function addPendingAction(taskId: string, action: CookPendingAction): void {
  const list = loadPendingActions(taskId)
  if (list.some((item) => item.key === action.key)) {
    return
  }
  list.push(action)
  savePendingActions(taskId, list)
}

/**
 * 移除已完成同步的动作。
 *
 * @param taskId 任务 ID
 * @param key    动作 key
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function removePendingAction(taskId: string, key: string): void {
  const list = loadPendingActions(taskId).filter((item) => item.key !== key)
  savePendingActions(taskId, list)
}

/**
 * 清空待同步动作。
 *
 * @param taskId 任务 ID
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export function clearPendingActions(taskId: string): void {
  try {
    Taro.removeStorageSync(`${QUEUE_PREFIX}${taskId}`)
  } catch {
    // ignore
  }
}

/**
 * 当前是否有网络。
 *
 * @return true 表示已联网
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export async function hasNetwork(): Promise<boolean> {
  try {
    const res = await Taro.getNetworkType()
    return res.networkType !== 'none'
  } catch {
    return true
  }
}

function savePendingActions(taskId: string, list: CookPendingAction[]): void {
  try {
    if (list.length === 0) {
      Taro.removeStorageSync(`${QUEUE_PREFIX}${taskId}`)
      return
    }
    Taro.setStorageSync(`${QUEUE_PREFIX}${taskId}`, list)
  } catch {
    // ignore
  }
}
