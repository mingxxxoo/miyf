import {Image, Text, View} from '@tarojs/components'
import Taro, {useDidShow, useRouter} from '@tarojs/taro'
import {useEffect, useRef, useState} from 'react'
import {
    completeCookingTask,
    type CookingStepAction,
    fetchCookingTaskDetail,
    operateCookingStep,
    pauseCookingTask,
    resumeCookingTask
} from '@/api/cookingTask'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import type {CookingTask, CookingTaskStepState} from '@/types'
import {
    addPendingAction,
    cacheCookingTask,
    type CookPendingAction,
    type CookTaskAction,
    hasNetwork,
    loadCachedCookingTask,
    loadPendingActions,
    removePendingAction
} from '@/utils/cookFlowOffline'
import {COOKING_STATUS_META} from '@/utils/cookingTask'
import {timerTypeLabel} from '@/utils/format'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import './flow.scss'

const STEP_STATUS_LABEL: Record<string, string> = {
  PENDING: '待开始',
  RUNNING: '进行中',
  PAUSED: '已暂停',
  DONE: '已完成'
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

/** 秒转 mm:ss / h:mm:ss，用于大字号倒计时 */
function formatClock(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds))
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = seconds % 60
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(rest)}` : `${pad(minutes)}:${pad(rest)}`
}

/** 按服务端时间计算倒计时剩余秒数 */
function stepRemaining(step: CookingTaskStepState, serverNow: number): number | null {
  if (step.timerType !== 'countdown' || step.durationSeconds == null) return null
  if (step.status === 'PENDING') return step.durationSeconds
  if (step.status === 'DONE') return 0
  if (!step.startedTime) return step.remainingSeconds ?? step.durationSeconds
  const started = Date.parse(step.startedTime)
  if (Number.isNaN(started)) return step.remainingSeconds ?? step.durationSeconds
  const end = step.status === 'PAUSED' && step.pausedTime ? Date.parse(step.pausedTime) : serverNow
  const elapsed = Math.max(0, Math.floor((end - started) / 1000) - (step.totalPausedSeconds || 0))
  return Math.max(0, step.durationSeconds - elapsed)
}

/** 按服务端时间计算正计时已用秒数 */
function stepElapsed(step: CookingTaskStepState, serverNow: number): number | null {
  if (!step.startedTime) return null
  const started = Date.parse(step.startedTime)
  if (Number.isNaN(started)) return null
  const end =
    step.status === 'PAUSED' && step.pausedTime
      ? Date.parse(step.pausedTime)
      : step.status === 'DONE' && step.completedTime
        ? Date.parse(step.completedTime)
        : serverNow
  return Math.max(0, Math.floor((end - started) / 1000) - (step.totalPausedSeconds || 0))
}

function isNetworkError(message: string): boolean {
  return /网络|超时|域名|证书|连接/i.test(message)
}

function pickActiveStep(steps: CookingTaskStepState[]): CookingTaskStepState | null {
  const running = steps.find((step) => step.status === 'RUNNING')
  if (running) return running
  const paused = steps.find((step) => step.status === 'PAUSED')
  if (paused) return paused
  const pending = steps.find((step) => step.status === 'PENDING')
  if (pending) return pending
  const done = steps.filter((step) => step.status === 'DONE')
  return done.length > 0 ? done[done.length - 1] : null
}

/**
 * 离线乐观更新后重算步骤按钮可用性。
 * <p>
 * 在线时按钮标记以服务端为准；离线无法回源，按“前置必需步骤已完成”在本地推导，
 * 与后端顺序约束保持一致。并行分组在离线场景下按顺序处理。
 */
function refreshLocalFlags(
  steps: CookingTaskStepState[],
  taskCooking: boolean
): CookingTaskStepState[] {
  let previousRequiredDone = true
  let lastDoneIndex = -1
  let lastActiveIndex = -1
  steps.forEach((step, index) => {
    if (step.status === 'DONE') lastDoneIndex = index
    if (step.status !== 'PENDING') lastActiveIndex = index
  })
  return steps.map((step, index) => {
    const prevDone = previousRequiredDone
    const canStart = taskCooking && step.status === 'PENDING' && prevDone
    const canComplete =
      taskCooking && step.status !== 'DONE' && (step.status !== 'PENDING' || prevDone)
    const canUndoNow =
      taskCooking &&
      step.status === 'DONE' &&
      index === lastDoneIndex &&
      index === lastActiveIndex &&
      step.canUndo !== false
    if (step.required !== false && step.status !== 'DONE') {
      previousRequiredDone = false
    }
    return {
      ...step,
      canStart,
      canPause: taskCooking && step.status === 'RUNNING',
      canResume: taskCooking && step.status === 'PAUSED',
      canComplete,
      canUndoNow
    }
  })
}

/**
 * 做菜流程页。
 * <p>
 * 计时以服务端返回的时间戳为准，本地只负责按秒刷新展示，退出重进或断网后仍可恢复；
 * 断网动作写入本地队列，恢复网络后自动补发。页面在做菜期间保持屏幕常亮，
 * 倒计时归零时震动提醒。
 *
 * @author XieMingJie
 * @since 2026-10-31 17:00
 * @history 1.00 2026-10-31 17:00 XieMingJie Created.
 */
export default function CookFlowPage() {
  const router = useRouter()
  const taskId = router.params.id || ''

  const [task, setTask] = useState<CookingTask | null>(null)
  const [loading, setLoading] = useState(true)
  const [offline, setOffline] = useState(false)
  const [pendingCount, setPendingCount] = useState(0)
  const [acting, setActing] = useState('')
  const [tick, setTick] = useState(Date.now())
  const offsetRef = useRef(0)
  const alertedRef = useRef<Set<number>>(new Set())

  const applyTask = (next: CookingTask) => {
    setTask(next)
    cacheCookingTask(next)
    if (next.serverTime) {
      const parsed = Date.parse(next.serverTime)
      if (!Number.isNaN(parsed)) {
        offsetRef.current = parsed - Date.now()
      }
    }
    const name = next.dishSnapshot?.name
    if (name) {
      void Taro.setNavigationBarTitle({ title: name })
    }
    setPendingCount(loadPendingActions(next.id).length)
  }

  async function callTaskAction(action: CookTaskAction): Promise<void> {
    if (action === 'pause') {
      await pauseCookingTask(taskId)
    } else if (action === 'resume') {
      await resumeCookingTask(taskId)
    } else {
      await completeCookingTask(taskId)
    }
  }

  async function flushPending(): Promise<void> {
    const actions = loadPendingActions(taskId)
    if (actions.length === 0 || !(await hasNetwork())) {
      return
    }
    for (const item of actions) {
      try {
        if (item.type === 'step') {
          await operateCookingStep(taskId, item.stepOrder, item.action)
        } else {
          await callTaskAction(item.action)
        }
        removePendingAction(taskId, item.key)
      } catch {
        break
      }
    }
    const rest = loadPendingActions(taskId)
    setPendingCount(rest.length)
    if (rest.length > 0) {
      return
    }
    try {
      const detail = await fetchCookingTaskDetail(taskId)
      applyTask(detail)
      setOffline(false)
    } catch {
      setOffline(true)
    }
  }

  const load = async () => {
    if (!taskId) {
      setLoading(false)
      return
    }
    try {
      const detail = await fetchCookingTaskDetail(taskId)
      applyTask(detail)
      setOffline(false)
      await flushPending()
    } catch {
      const cached = loadCachedCookingTask(taskId)
      if (cached) {
        setTask(cached)
        setOffline(true)
        if (cached.serverTime) {
          offsetRef.current = Date.parse(cached.serverTime) - Date.now()
        }
      }
    } finally {
      setLoading(false)
    }
  }

  useDidShow(() => {
    void load()
  })

  useEffect(() => {
    const timer = setInterval(() => setTick(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    void Taro.setKeepScreenOn({ keepScreenOn: true }).catch(() => undefined)
    return () => {
      void Taro.setKeepScreenOn({ keepScreenOn: false }).catch(() => undefined)
    }
  }, [])

  useEffect(() => {
    const handler = (res: { isConnected: boolean }) => {
      setOffline(!res.isConnected)
      if (res.isConnected) {
        void load()
      }
    }
    Taro.onNetworkStatusChange(handler)
    return () => {
      Taro.offNetworkStatusChange(handler)
    }
    // load 依赖当前路由任务 ID，页面生命周期内不变
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 倒计时归零提醒：每个步骤只提醒一次
  useEffect(() => {
    if (!task) return
    const step = pickActiveStep(task.steps || [])
    if (!step || step.status !== 'RUNNING' || step.timerType !== 'countdown') return
    const remaining = stepRemaining(step, Date.now() + offsetRef.current)
    if (remaining === 0 && !alertedRef.current.has(step.stepOrder)) {
      alertedRef.current.add(step.stepOrder)
      void Taro.vibrateLong().catch(() => undefined)
      Taro.showToast({ title: '时间到，请检查火候', icon: 'none', duration: 2500 })
    }
  }, [tick, task])

  const serverNow = tick + offsetRef.current

  if (loading) {
    return (
      <View className='cook-flow cook-flow--loading'>
        <Loading text='正在进入做菜流程…' />
      </View>
    )
  }

  if (!task) {
    return (
      <View className='cook-flow cook-flow--loading'>
        <Text className='cook-flow__missing'>任务不存在或无权查看</Text>
      </View>
    )
  }

  const steps = task.steps || []
  const activeStep = pickActiveStep(steps)
  const doneCount = steps.filter((step) => step.status === 'DONE').length
  const progress = steps.length === 0 ? 0 : Math.round((doneCount / steps.length) * 100)
  const meta = COOKING_STATUS_META[task.status] || COOKING_STATUS_META.PENDING
  const lastDone = steps.filter((step) => step.status === 'DONE').slice(-1)[0]
  const taskCooking = task.status === 'COOKING'
  const taskPaused = task.status === 'PAUSED'
  const taskFinished = task.status === 'COMPLETED' || task.status === 'CANCELLED'
  const activeRemaining =
    activeStep == null ? null : stepRemaining(activeStep, serverNow)
  const activeElapsed = activeStep == null ? null : stepElapsed(activeStep, serverNow)

  const timerText = (() => {
    if (!activeStep) return '—'
    if (activeStep.timerType === 'countdown' && activeRemaining != null) {
      return formatClock(activeRemaining)
    }
    if (activeStep.timerType === 'countup' && activeElapsed != null) {
      return formatClock(activeElapsed)
    }
    return '—'
  })()

  const timerLabel = (() => {
    if (!activeStep) return '暂无步骤'
    if (activeStep.timerType === 'countdown') {
      if (activeRemaining === 0 && activeStep.status !== 'DONE') return '时间到，请检查火候'
      return activeStep.status === 'PAUSED' ? '已暂停 · 剩余' : '剩余时间'
    }
    if (activeStep.timerType === 'countup') {
      return activeStep.status === 'PAUSED' ? '已暂停 · 已用' : '已用时'
    }
    return '本步骤不限时'
  })()

  const applyLocalStep = (stepOrder: number, action: CookingStepAction) => {
    if (!task) return
    const nowIso = new Date(Date.now() + offsetRef.current).toISOString()
    const nextSteps = steps.map((step) => {
      if (step.stepOrder !== stepOrder) return step
      if (action === 'start') {
        return {
          ...step,
          status: 'RUNNING' as const,
          startedTime: nowIso,
          pausedTime: undefined,
          totalPausedSeconds: 0,
          remainingSeconds: step.timerType === 'countdown' ? step.durationSeconds : step.remainingSeconds,
          canStart: false,
          canPause: true,
          canResume: false,
          canComplete: true,
          canUndoNow: false
        }
      }
      if (action === 'pause') {
        const remain = stepRemaining(step, Date.now() + offsetRef.current)
        return {
          ...step,
          status: 'PAUSED' as const,
          pausedTime: nowIso,
          remainingSeconds: remain ?? step.remainingSeconds,
          canPause: false,
          canResume: true
        }
      }
      if (action === 'resume') {
        const pausedMs =
          step.pausedTime && !Number.isNaN(Date.parse(step.pausedTime))
            ? Date.parse(nowIso) - Date.parse(step.pausedTime)
            : 0
        return {
          ...step,
          status: 'RUNNING' as const,
          pausedTime: undefined,
          totalPausedSeconds: (step.totalPausedSeconds || 0) + Math.max(0, Math.round(pausedMs / 1000)),
          canPause: true,
          canResume: false
        }
      }
      if (action === 'complete') {
        return {
          ...step,
          status: 'DONE' as const,
          completedTime: nowIso,
          remainingSeconds: step.timerType === 'countdown' ? 0 : step.remainingSeconds,
          canStart: false,
          canPause: false,
          canResume: false,
          canComplete: false,
          canUndoNow: true
        }
      }
      return {
        ...step,
        status: 'PENDING' as const,
        startedTime: undefined,
        pausedTime: undefined,
        completedTime: undefined,
        totalPausedSeconds: 0,
        remainingSeconds: step.timerType === 'countdown' ? step.durationSeconds : undefined,
        canStart: true,
        canPause: false,
        canResume: false,
        canComplete: true,
        canUndoNow: false
      }
    })
    let currentStep = task.currentStep || 0
    if (action === 'start' || action === 'undo') {
      currentStep = stepOrder
    } else if (action === 'complete') {
      const nextPending = nextSteps.find((step) => step.status !== 'DONE')
      currentStep = nextPending ? nextPending.stepOrder : 0
    }
    const allDone = nextSteps.length > 0 && nextSteps.every((step) => step.status === 'DONE')
    const normalizedSteps = refreshLocalFlags(nextSteps, allDone ? false : taskCooking)
    applyTask({
      ...task,
      steps: normalizedSteps,
      currentStep,
      status: allDone ? 'COMPLETED' : task.status,
      canPause: allDone ? false : taskCooking,
      canResume: allDone ? false : taskPaused,
      canComplete: allDone ? false : task.canComplete
    })
    if (action === 'start' || action === 'resume') {
      alertedRef.current.delete(stepOrder)
    }
  }

  const applyLocalTask = (action: CookTaskAction) => {
    if (!task) return
    const nowIso = new Date(Date.now() + offsetRef.current).toISOString()
    const nextSteps = steps.map((step) => {
      if (action === 'pause') {
        if (step.status !== 'RUNNING') return step
        const remain = stepRemaining(step, Date.now() + offsetRef.current)
        return {
          ...step,
          status: 'PAUSED' as const,
          pausedTime: nowIso,
          remainingSeconds: remain ?? step.remainingSeconds,
          canPause: false,
          canResume: true
        }
      }
      if (action === 'resume') {
        if (step.status !== 'PAUSED') return step
        const pausedMs =
          step.pausedTime && !Number.isNaN(Date.parse(step.pausedTime))
            ? Date.parse(nowIso) - Date.parse(step.pausedTime)
            : 0
        return {
          ...step,
          status: 'RUNNING' as const,
          pausedTime: undefined,
          totalPausedSeconds: (step.totalPausedSeconds || 0) + Math.max(0, Math.round(pausedMs / 1000)),
          canPause: true,
          canResume: false
        }
      }
      if (step.status === 'DONE') return step
      return {
        ...step,
        status: 'DONE' as const,
        completedTime: nowIso,
        remainingSeconds: step.timerType === 'countdown' ? 0 : step.remainingSeconds,
        canStart: false,
        canPause: false,
        canResume: false,
        canComplete: false,
        canUndoNow: false
      }
    })
    applyTask({
      ...task,
      steps: refreshLocalFlags(
        nextSteps,
        action === 'resume' || (action !== 'complete' && action !== 'pause' && taskCooking)
      ),
      status:
        action === 'pause'
          ? 'PAUSED'
          : action === 'resume'
            ? 'COOKING'
            : 'COMPLETED',
      currentStep: action === 'complete' ? 0 : task.currentStep,
      canPause: action === 'pause',
      canResume: action === 'resume',
      canComplete: action !== 'complete'
    })
  }

  const queueOffline = (pending: CookPendingAction) => {
    addPendingAction(taskId, pending)
    setPendingCount(loadPendingActions(taskId).length)
    setOffline(true)
    Taro.showToast({ title: '已离线记录，联网后自动同步', icon: 'none' })
  }

  const runStepAction = async (step: CookingTaskStepState, action: CookingStepAction) => {
    const key = `step:${step.stepOrder}:${action}`
    if (acting) return
    setActing(key)
    try {
      if (!(await hasNetwork())) {
        applyLocalStep(step.stepOrder, action)
        queueOffline({ key, type: 'step', stepOrder: step.stepOrder, action })
        return
      }
      const next = await operateCookingStep(taskId, step.stepOrder, action)
      applyTask(next)
      setOffline(false)
      if (action === 'complete') {
        void Taro.vibrateShort({ type: 'medium' }).catch(() => undefined)
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : ''
      if (isNetworkError(message)) {
        applyLocalStep(step.stepOrder, action)
        queueOffline({ key, type: 'step', stepOrder: step.stepOrder, action })
      } else {
        Taro.showToast({ title: message || '操作失败', icon: 'none' })
        await load()
      }
    } finally {
      setActing('')
    }
  }

  const runTaskAction = async (action: CookTaskAction) => {
    const key = `task:${action}`
    if (acting) return
    setActing(key)
    try {
      if (!(await hasNetwork())) {
        applyLocalTask(action)
        queueOffline({ key, type: 'task', action })
        return
      }
      await callTaskAction(action)
      const detail = await fetchCookingTaskDetail(taskId)
      applyTask(detail)
      setOffline(false)
      if (action === 'complete') {
        void Taro.vibrateLong().catch(() => undefined)
        Taro.showToast({ title: '这道菜完成了', icon: 'success' })
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : ''
      if (isNetworkError(message)) {
        applyLocalTask(action)
        queueOffline({ key, type: 'task', action })
      } else {
        Taro.showToast({ title: message || '操作失败', icon: 'none' })
        await load()
      }
    } finally {
      setActing('')
    }
  }

  const confirmFinish = async () => {
    const res = await Taro.showModal({
      title: '完成整道菜',
      content: '确认这道菜已经做好？未完成的步骤会一并标记为完成。',
      confirmText: '完成',
      cancelText: '再等等'
    })
    if (res.confirm) {
      await runTaskAction('complete')
    }
  }

  const handleStepStart = (step: CookingTaskStepState) => void runStepAction(step, 'start')
  const handleStepPause = (step: CookingTaskStepState) => void runStepAction(step, 'pause')
  const handleStepResume = (step: CookingTaskStepState) => void runStepAction(step, 'resume')
  const handleStepComplete = (step: CookingTaskStepState) => void runStepAction(step, 'complete')
  const handleStepUndo = (step: CookingTaskStepState) => void runStepAction(step, 'undo')

  return (
    <View className='cook-flow'>
      {offline ? (
        <View className='cook-flow__offline'>
          <Text className='cook-flow__offline-text'>
            网络不可用，正在使用本地数据
            {pendingCount > 0 ? ` · ${pendingCount} 个操作待同步` : ''}
          </Text>
        </View>
      ) : pendingCount > 0 ? (
        <View className='cook-flow__offline cook-flow__offline--sync'>
          <Text className='cook-flow__offline-text'>{pendingCount} 个操作待同步</Text>
        </View>
      ) : null}

      <View className='cook-flow__top'>
        <View className='cook-flow__title-row'>
          <View className='cook-flow__title-copy'>
            <Text className='cook-flow__dish'>{task.dishSnapshot?.name || '未命名菜品'}</Text>
            <View className='cook-flow__badges'>
              <Text className={`cook-flow__badge cook-flow__badge--${meta.tone}`}>{meta.label}</Text>
              {task.servings ? (
                <Text className='cook-flow__badge'>{task.servings} 人份</Text>
              ) : null}
            </View>
          </View>
          <View className='cook-flow__progress'>
            <Text className='cook-flow__progress-num'>
              {doneCount}/{steps.length}
            </Text>
            <Text className='cook-flow__progress-label'>已完成</Text>
          </View>
        </View>
        <View className='cook-flow__progress-bar'>
          <View className='cook-flow__progress-fill' style={{ width: `${progress}%` }} />
        </View>
        {!taskFinished ? (
          <View className='cook-flow__top-actions'>
            {taskCooking ? (
              <Text
                className={`cook-flow__mini-btn ${acting === 'task:pause' ? 'is-busy' : ''}`}
                onClick={() => void runTaskAction('pause')}
              >
                暂停整单
              </Text>
            ) : null}
            {taskPaused ? (
              <Text
                className={`cook-flow__mini-btn cook-flow__mini-btn--main ${
                  acting === 'task:resume' ? 'is-busy' : ''
                }`}
                onClick={() => void runTaskAction('resume')}
              >
                继续整单
              </Text>
            ) : null}
            {taskCooking || taskPaused ? (
              <Text className='cook-flow__mini-btn' onClick={() => void confirmFinish()}>
                提前完成
              </Text>
            ) : null}
          </View>
        ) : task.status === 'COMPLETED' ? (
          <View className='cook-flow__done-banner'>
            <Text className='cook-flow__done-title'>这道菜已完成</Text>
            <Text className='cook-flow__done-desc'>
              {task.actualDurationSeconds != null
                ? `实际用时 ${formatClock(task.actualDurationSeconds)}`
                : '辛苦了，开饭吧'}
            </Text>
          </View>
        ) : task.status === 'CANCELLED' ? (
          <View className='cook-flow__done-banner cook-flow__done-banner--cancel'>
            <Text className='cook-flow__done-title'>任务已取消</Text>
            <Text className='cook-flow__done-desc'>这单任务不再需要制作</Text>
          </View>
        ) : null}
      </View>

      {activeStep ? (
        <View className='cook-flow__timer-card'>
          <View className='cook-flow__timer-head'>
            <Text className='cook-flow__timer-step'>
              第 {activeStep.stepOrder} 步 / 共 {steps.length} 步
            </Text>
            <Text className='cook-flow__timer-status'>
              {STEP_STATUS_LABEL[activeStep.status] || activeStep.status}
            </Text>
          </View>
          <Text className='cook-flow__timer-title'>
            {activeStep.title || `第 ${activeStep.stepOrder} 步`}
          </Text>
          <Text className='cook-flow__timer-label'>{timerLabel}</Text>
          <Text
            className={`cook-flow__timer-value ${
              activeRemaining === 0 && activeStep.status !== 'DONE' ? 'cook-flow__timer-value--alert' : ''
            }`}
          >
            {timerText}
          </Text>
          {activeStep.timerType !== 'none' && activeStep.durationSeconds ? (
            <Text className='cook-flow__timer-sub'>
              预计 {formatClock(activeStep.durationSeconds)} · {timerTypeLabel(activeStep.timerType)}
            </Text>
          ) : null}

          {activeStep.description ? (
            <Text className='cook-flow__timer-desc'>{activeStep.description}</Text>
          ) : null}
          {activeStep.image ? (
            <Image
              className='cook-flow__timer-image'
              src={toAbsoluteResourceUrl(activeStep.image) || ''}
              mode='widthFix'
              lazyLoad
            />
          ) : null}
          {activeStep.warning ? (
            <View className='cook-flow__warn'>
              <Text className='cook-flow__warn-text'>注意：{activeStep.warning}</Text>
            </View>
          ) : null}
          {activeStep.completionHint ? (
            <Text className='cook-flow__hint'>完成标准：{activeStep.completionHint}</Text>
          ) : null}

          <View className='cook-flow__step-actions'>
            {activeStep.canStart ? (
              <Text
                className='cook-flow__big-btn cook-flow__big-btn--main'
                onClick={() => handleStepStart(activeStep)}
              >
                开始第 {activeStep.stepOrder} 步
              </Text>
            ) : null}
            {activeStep.canPause ? (
              <Text
                className='cook-flow__big-btn cook-flow__big-btn--ghost'
                onClick={() => handleStepPause(activeStep)}
              >
                暂停本步
              </Text>
            ) : null}
            {activeStep.canResume ? (
              <Text
                className='cook-flow__big-btn cook-flow__big-btn--main'
                onClick={() => handleStepResume(activeStep)}
              >
                继续第 {activeStep.stepOrder} 步
              </Text>
            ) : null}
            {activeStep.canComplete ? (
              <Text
                className={`cook-flow__big-btn ${
                  activeStep.status === 'RUNNING' || activeStep.status === 'PAUSED'
                    ? 'cook-flow__big-btn--done'
                    : 'cook-flow__big-btn--ghost'
                }`}
                onClick={() => handleStepComplete(activeStep)}
              >
                完成第 {activeStep.stepOrder} 步
              </Text>
            ) : null}
            {lastDone && lastDone.canUndoNow ? (
              <Text
                className='cook-flow__big-btn cook-flow__big-btn--undo'
                onClick={() => handleStepUndo(lastDone)}
              >
                撤销第 {lastDone.stepOrder} 步
              </Text>
            ) : null}
          </View>
        </View>
      ) : (
        <View className='cook-flow__timer-card cook-flow__timer-card--empty'>
          <MiniIcon name='recipe' size='lg' tone='muted' />
          <Text className='cook-flow__empty-title'>还没有步骤</Text>
          <Text className='cook-flow__empty-desc'>返回任务详情补充菜品步骤后再开始</Text>
        </View>
      )}

      <View className='cook-flow__list'>
        <Text className='cook-flow__list-title'>全部步骤</Text>
        {steps.map((step) => {
          const remaining = stepRemaining(step, serverNow)
          const elapsed = stepElapsed(step, serverNow)
          const isActive = activeStep?.stepOrder === step.stepOrder
          return (
            <View
              key={step.stepOrder}
              className={`cook-flow__row ${isActive ? 'cook-flow__row--active' : ''} ${
                step.status === 'DONE' ? 'cook-flow__row--done' : ''
              }`}
            >
              <View className='cook-flow__row-num'>
                <Text>{step.status === 'DONE' ? '✓' : step.stepOrder}</Text>
              </View>
              <View className='cook-flow__row-body'>
                <View className='cook-flow__row-head'>
                  <Text className='cook-flow__row-title'>
                    {step.title || `第 ${step.stepOrder} 步`}
                  </Text>
                  <Text className='cook-flow__row-state'>{STEP_STATUS_LABEL[step.status] || ''}</Text>
                </View>
                <View className='cook-flow__row-meta'>
                  {step.timerType === 'countdown' && remaining != null ? (
                    <Text className='cook-flow__row-timer'>剩余 {formatClock(remaining)}</Text>
                  ) : null}
                  {step.timerType === 'countup' && elapsed != null ? (
                    <Text className='cook-flow__row-timer'>已用 {formatClock(elapsed)}</Text>
                  ) : null}
                  {step.timerType === 'none' ? (
                    <Text className='cook-flow__row-timer'>不计时</Text>
                  ) : null}
                  {step.required === false ? (
                    <Text className='cook-flow__row-optional'>可选</Text>
                  ) : null}
                </View>
                <View className='cook-flow__row-actions'>
                  {step.canStart ? (
                    <Text className='cook-flow__row-btn' onClick={() => handleStepStart(step)}>
                      开始
                    </Text>
                  ) : null}
                  {step.canPause ? (
                    <Text className='cook-flow__row-btn' onClick={() => handleStepPause(step)}>
                      暂停
                    </Text>
                  ) : null}
                  {step.canResume ? (
                    <Text className='cook-flow__row-btn cook-flow__row-btn--main' onClick={() => handleStepResume(step)}>
                      继续
                    </Text>
                  ) : null}
                  {step.canComplete ? (
                    <Text className='cook-flow__row-btn cook-flow__row-btn--main' onClick={() => handleStepComplete(step)}>
                      完成
                    </Text>
                  ) : null}
                </View>
              </View>
            </View>
          )
        })}
      </View>

      <View className='cook-flow__foot'>
        <Text className='cook-flow__foot-text'>
          计时保存在服务端，退出小程序后重新进入会接着算
        </Text>
      </View>
    </View>
  )
}
