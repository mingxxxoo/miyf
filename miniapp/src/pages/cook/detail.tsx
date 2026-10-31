import {Image, Text, View} from '@tarojs/components'
import Taro, {useDidShow, useRouter} from '@tarojs/taro'
import {useState} from 'react'
import {
    cancelCookingTask,
    claimCookingTask,
    completeCookingTask,
    fetchCookingTaskDetail,
    pauseCookingTask,
    releaseCookingTask,
    resumeCookingTask,
    startCookingTask
} from '@/api/cookingTask'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import type {CookingTask, CookingTaskStepState} from '@/types'
import {COOKING_STATUS_META, formatSeconds, formatTaskTime} from '@/utils/cookingTask'
import {timerTypeLabel} from '@/utils/format'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import './detail.scss'

const STEP_STATUS_LABEL: Record<string, string> = {
  PENDING: '待开始',
  RUNNING: '进行中',
  PAUSED: '已暂停',
  DONE: '已完成'
}

export default function CookTaskDetailPage() {
  const router = useRouter()
  const taskId = router.params.id || ''
  const [task, setTask] = useState<CookingTask | null>(null)
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState(false)

  const load = async () => {
    if (!taskId) {
      setLoading(false)
      return
    }
    try {
      const detail = await fetchCookingTaskDetail(taskId)
      setTask(detail)
    } catch {
      // request 层已提示
    } finally {
      setLoading(false)
    }
  }

  useDidShow(() => {
    void load()
  })

  const run = async (label: string, action: () => Promise<void>) => {
    if (acting) return
    setActing(true)
    Taro.showLoading({ title: `${label}中…`, mask: true })
    try {
      await action()
      Taro.showToast({ title: `${label}成功`, icon: 'success' })
      await load()
    } catch (err) {
      const message = err instanceof Error ? err.message : `${label}失败`
      Taro.showToast({ title: message, icon: 'none' })
      await load()
    } finally {
      Taro.hideLoading()
      setActing(false)
    }
  }

  const confirmRun = async (title: string, content: string, label: string, action: () => Promise<void>) => {
    const res = await Taro.showModal({ title, content, confirmText: label, cancelText: '取消' })
    if (!res.confirm) return
    await run(label, action)
  }

  const enterFlow = () => {
    void Taro.navigateTo({ url: `/pages/cook/flow?id=${taskId}` })
  }

  const startAndEnterFlow = async () => {
    if (acting) return
    setActing(true)
    Taro.showLoading({ title: '开始中…', mask: true })
    try {
      await startCookingTask(taskId)
      Taro.showToast({ title: '开始做菜', icon: 'success' })
      setTimeout(() => {
        void Taro.navigateTo({ url: `/pages/cook/flow?id=${taskId}` })
      }, 300)
    } catch (err) {
      const message = err instanceof Error ? err.message : '开始失败'
      Taro.showToast({ title: message, icon: 'none' })
      await load()
    } finally {
      Taro.hideLoading()
      setActing(false)
    }
  }

  if (loading) {
    return (
      <View className='cook-detail cook-detail--loading'>
        <Loading text='正在读取任务…' />
      </View>
    )
  }

  if (!task) {
    return (
      <View className='cook-detail cook-detail--loading'>
        <Text className='cook-detail__missing'>任务不存在或无权查看</Text>
      </View>
    )
  }

  const meta = COOKING_STATUS_META[task.status] || COOKING_STATUS_META.PENDING
  const snapshot = task.dishSnapshot || {}
  const cover = toAbsoluteResourceUrl(snapshot.coverImage)
  const stepStates: CookingTaskStepState[] =
    task.steps && task.steps.length > 0
      ? task.steps
      : (snapshot.steps || []).map((step, index) => ({
          stepOrder: step.order ?? step.step ?? index + 1,
          status: 'PENDING' as const,
          timerType: step.timerType,
          durationSeconds: step.durationSeconds,
          title: step.title,
          description: step.description,
          image: step.image,
          warning: step.warning,
          completionHint: step.completionHint,
          required: step.required,
          canUndo: step.canUndo
        }))

  const showActions =
    task.canClaim ||
    task.canStart ||
    task.canPause ||
    task.canResume ||
    task.canComplete ||
    task.canRelease ||
    task.canCancel

  return (
    <View className={`cook-detail ${showActions ? 'cook-detail--with-actions' : ''}`}>
      <View className='cook-detail__hero'>
        {cover ? (
          <Image className='cook-detail__cover' src={cover} mode='aspectFill' />
        ) : (
          <View className='cook-detail__cover-empty'>
            <MiniIcon name='dish' size='lg' tone='muted' />
          </View>
        )}
        <View className='cook-detail__hero-mask' />
        <View className='cook-detail__hero-copy'>
          <View className='cook-detail__badges'>
            <Text className={`cook-detail__badge cook-detail__badge--${meta.tone}`}>{meta.label}</Text>
            {task.priority && task.priority >= 5 ? (
              <Text className='cook-detail__badge cook-detail__badge--urgent'>
                {task.priority >= 9 ? '最急' : '加急'}
              </Text>
            ) : null}
            {task.claimedByMe ? (
              <Text className='cook-detail__badge cook-detail__badge--mine'>我掌勺</Text>
            ) : null}
          </View>
          <Text className='cook-detail__name'>{snapshot.name || '未命名菜品'}</Text>
          {snapshot.description ? (
            <Text className='cook-detail__desc'>{snapshot.description}</Text>
          ) : null}
        </View>
      </View>

      <View className='cook-detail__meta'>
        <View className='cook-detail__meta-item'>
          <Text className='cook-detail__meta-num'>{task.servings || 1}</Text>
          <Text className='cook-detail__meta-label'>人份</Text>
        </View>
        <View className='cook-detail__meta-item'>
          <Text className='cook-detail__meta-num'>{stepStates.length}</Text>
          <Text className='cook-detail__meta-label'>步骤</Text>
        </View>
        <View className='cook-detail__meta-item'>
          <Text className='cook-detail__meta-num'>
            {task.actualDurationSeconds != null
              ? formatSeconds(task.actualDurationSeconds)
              : formatTaskTime(task.scheduledStartTime) || '随到随做'}
          </Text>
          <Text className='cook-detail__meta-label'>
            {task.actualDurationSeconds != null ? '实际用时' : '计划开始'}
          </Text>
        </View>
      </View>

      <View className='cook-detail__section'>
        <Text className='cook-detail__section-title'>任务信息</Text>
        <View className='cook-detail__info'>
          <View className='cook-detail__info-row'>
            <Text className='cook-detail__info-label'>提交人</Text>
            <Text className='cook-detail__info-value'>{task.submitterName || '成员'}</Text>
          </View>
          <View className='cook-detail__info-row'>
            <Text className='cook-detail__info-label'>掌勺</Text>
            <Text className='cook-detail__info-value'>{task.chefName || '等待领取'}</Text>
          </View>
          <View className='cook-detail__info-row'>
            <Text className='cook-detail__info-label'>提交时间</Text>
            <Text className='cook-detail__info-value'>{formatTaskTime(task.createTime) || '—'}</Text>
          </View>
          {task.scheduledStartTime ? (
            <View className='cook-detail__info-row'>
              <Text className='cook-detail__info-label'>计划开始</Text>
              <Text className='cook-detail__info-value'>{formatTaskTime(task.scheduledStartTime)}</Text>
            </View>
          ) : null}
          {task.startedTime ? (
            <View className='cook-detail__info-row'>
              <Text className='cook-detail__info-label'>开始制作</Text>
              <Text className='cook-detail__info-value'>{formatTaskTime(task.startedTime)}</Text>
            </View>
          ) : null}
        </View>
        {task.note ? (
          <View className='cook-detail__note'>
            <Text className='cook-detail__note-label'>备注</Text>
            <Text className='cook-detail__note-text'>{task.note}</Text>
          </View>
        ) : null}
      </View>

      {(snapshot.ingredients || []).length > 0 ? (
        <View className='cook-detail__section'>
          <View className='cook-detail__section-head'>
            <Text className='cook-detail__section-title'>原材料</Text>
            <Text className='cook-detail__section-count'>{snapshot.ingredients?.length} 样</Text>
          </View>
          <View className='cook-detail__materials'>
            {(snapshot.ingredients || []).map((item, index) => (
              <View key={`${item.name}-${index}`} className='cook-detail__material'>
                <Text className='cook-detail__material-name'>{item.name}</Text>
                <Text className='cook-detail__material-amount'>
                  {[item.amount, item.unit].filter(Boolean).join(' ') || '适量'}
                </Text>
                {item.processing ? (
                  <Text className='cook-detail__material-sub'>{item.processing}</Text>
                ) : null}
              </View>
            ))}
          </View>
        </View>
      ) : null}

      <View className='cook-detail__section'>
        <View className='cook-detail__section-head'>
          <Text className='cook-detail__section-title'>做菜步骤</Text>
          <Text className='cook-detail__section-count'>{stepStates.length} 步</Text>
        </View>
        <View className='cook-detail__steps'>
          {stepStates.map((step, index) => {
            const done = step.status === 'DONE'
            const running = step.status === 'RUNNING'
            const image = toAbsoluteResourceUrl(step.image)
            return (
              <View
                key={`${step.stepOrder}-${index}`}
                className={`cook-detail__step ${running ? 'cook-detail__step--running' : ''}`}
              >
                <View className='cook-detail__step-rail'>
                  <Text
                    className={`cook-detail__step-num ${done ? 'cook-detail__step-num--done' : ''} ${
                      running ? 'cook-detail__step-num--running' : ''
                    }`}
                  >
                    {done ? '✓' : step.stepOrder}
                  </Text>
                  {index < stepStates.length - 1 ? <View className='cook-detail__step-line' /> : null}
                </View>
                <View className='cook-detail__step-body'>
                  <View className='cook-detail__step-head'>
                    <Text className='cook-detail__step-title'>
                      {step.title || `第 ${step.stepOrder} 步`}
                    </Text>
                    <Text className='cook-detail__step-state'>{STEP_STATUS_LABEL[step.status] || ''}</Text>
                  </View>
                  <Text className='cook-detail__step-desc'>{step.description}</Text>
                  {image ? (
                    <Image className='cook-detail__step-img' src={image} mode='widthFix' lazyLoad />
                  ) : null}
                  {step.durationSeconds ? (
                    <Text className='cook-detail__step-timer'>
                      预计 {formatSeconds(step.durationSeconds)} ·{' '}
                      {timerTypeLabel(step.timerType)}
                      {step.remainingSeconds != null && step.timerType === 'countdown'
                        ? ` · 剩余 ${formatSeconds(step.remainingSeconds)}`
                        : ''}
                    </Text>
                  ) : null}
                  {step.warning ? (
                    <Text className='cook-detail__step-warn'>注意：{step.warning}</Text>
                  ) : null}
                  {step.completionHint ? (
                    <Text className='cook-detail__step-hint'>完成标准：{step.completionHint}</Text>
                  ) : null}
                </View>
              </View>
            )
          })}
        </View>
      </View>

      {showActions ? (
        <View className='cook-detail__actions'>
          {task.canCancel ? (
            <Text
              className='cook-detail__btn cook-detail__btn--danger'
              onClick={() =>
                void confirmRun('取消任务', '取消后不可恢复，确定取消这单做菜任务？', '取消', () =>
                  cancelCookingTask(task.id)
                )
              }
            >
              取消
            </Text>
          ) : null}
          {task.canRelease ? (
            <Text
              className='cook-detail__btn cook-detail__btn--ghost'
              onClick={() =>
                void confirmRun('放回任务池', '放回后其他成员可以领取', '放回', () =>
                  releaseCookingTask(task.id)
                )
              }
            >
              放回
            </Text>
          ) : null}
          {task.canClaim ? (
            <Text className='cook-detail__btn cook-detail__btn--main' onClick={() => void run('领取', () => claimCookingTask(task.id))}>
              领取任务
            </Text>
          ) : null}
          {task.canStart ? (
            <Text className='cook-detail__btn cook-detail__btn--main' onClick={() => void startAndEnterFlow()}>
              开始做菜
            </Text>
          ) : null}
          {task.canOpenFlow && (task.status === 'COOKING' || task.status === 'PAUSED') ? (
            <Text className='cook-detail__btn cook-detail__btn--main' onClick={enterFlow}>
              进入做菜流程
            </Text>
          ) : null}
          {task.canPause ? (
            <Text className='cook-detail__btn cook-detail__btn--main' onClick={() => void run('暂停', () => pauseCookingTask(task.id))}>
              暂停
            </Text>
          ) : null}
          {task.canResume ? (
            <Text className='cook-detail__btn cook-detail__btn--main' onClick={() => void run('恢复', () => resumeCookingTask(task.id))}>
              继续做菜
            </Text>
          ) : null}
          {task.canComplete ? (
            <Text
              className='cook-detail__btn cook-detail__btn--main'
              onClick={() =>
                void confirmRun('完成任务', '确认这道菜已经做好？', '完成', () =>
                  completeCookingTask(task.id)
                )
              }
            >
              完成做菜
            </Text>
          ) : null}
        </View>
      ) : null}
    </View>
  )
}
