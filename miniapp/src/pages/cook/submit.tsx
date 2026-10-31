import {Image, Picker, Text, Textarea, View} from '@tarojs/components'
import Taro, {useRouter} from '@tarojs/taro'
import {useEffect, useState} from 'react'
import {createCookingTask, newSubmitToken} from '@/api/cookingTask'
import {fetchDishTemplateDetail, fetchDishTemplates} from '@/api/dishTemplate'
import EmptyState from '@/components/EmptyState'
import MiniIcon from '@/components/MiniIcon'
import type {DishTemplate} from '@/types'
import {formatDuration} from '@/utils/format'
import './submit.scss'

const PRIORITIES = [
  { value: 0, label: '普通' },
  { value: 5, label: '加急' },
  { value: 9, label: '最急' }
]

export default function CookSubmitPage() {
  const router = useRouter()
  const initialDishId = router.params.dishId || ''

  const [dish, setDish] = useState<DishTemplate | null>(null)
  const [candidates, setCandidates] = useState<DishTemplate[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [servings, setServings] = useState('2')
  const [note, setNote] = useState('')
  const [priority, setPriority] = useState(0)
  const [planDate, setPlanDate] = useState('')
  const [planTime, setPlanTime] = useState('')
  const [submitToken] = useState(() => newSubmitToken())

  useEffect(() => {
    let alive = true
    const load = async () => {
      setLoading(true)
      try {
        if (initialDishId) {
          const detail = await fetchDishTemplateDetail(initialDishId)
          if (!alive) return
          setDish(detail)
          setServings(String(detail.servings || 1))
        } else {
          const page = await fetchDishTemplates({ page: 1, rows: 50 })
          if (!alive) return
          const list = (page.records || []).filter(
            (item) => item.status === 'published' || item.status === 'pre_created'
          )
          setCandidates(list)
        }
      } catch {
        // request 层已提示
      } finally {
        if (alive) setLoading(false)
      }
    }
    void load()
    return () => {
      alive = false
    }
  }, [initialDishId])

  const pickDish = (item: DishTemplate) => {
    setDish(item)
    setServings(String(item.servings || 1))
  }

  const resetDish = () => {
    setDish(null)
  }

  const buildScheduledTime = (): string | undefined => {
    if (!planDate || !planTime) return undefined
    const date = new Date(`${planDate}T${planTime}:00`)
    if (Number.isNaN(date.getTime())) return undefined
    return date.toISOString()
  }

  const submit = async () => {
    if (submitting || !dish) return
    if (dish.steps.length === 0) {
      Taro.showToast({ title: '该菜品还没有步骤', icon: 'none' })
      return
    }
    setSubmitting(true)
    Taro.showLoading({ title: '提交中…', mask: true })
    try {
      const id = await createCookingTask({
        dishId: dish.id,
        servings: Math.max(1, Number(servings) || 1),
        note: note.trim() || undefined,
        priority,
        scheduledStartTime: buildScheduledTime(),
        submitToken
      })
      Taro.showToast({ title: '已提交到任务池', icon: 'success' })
      setTimeout(() => {
        Taro.redirectTo({ url: `/pages/cook/detail?id=${id}` })
      }, 400)
    } catch (err) {
      const message = err instanceof Error ? err.message : '提交失败'
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      Taro.hideLoading()
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <View className='cook-submit cook-submit--loading'>
        <Text className='cook-submit__loading-text'>正在读取菜品…</Text>
      </View>
    )
  }

  if (!dish) {
    return (
      <View className='cook-submit'>
        <View className='cook-submit__head'>
          <Text className='cook-submit__title'>选择要做的菜</Text>
          <Text className='cook-submit__sub'>从组织菜品库中选一道，提交后进入任务池</Text>
        </View>
        {candidates.length === 0 ? (
          <EmptyState
            title='暂无可提交的菜品'
            description='先到菜品库创建并发布菜品，再来提交做菜任务'
            actionText='去菜品库'
            onAction={() => Taro.redirectTo({ url: '/pages/dish/library' })}
          />
        ) : (
          <View className='cook-submit__picks'>
            {candidates.map((item) => (
              <View key={item.id} className='cook-submit__pick' onClick={() => pickDish(item)}>
                {item.coverUrl ? (
                  <Image className='cook-submit__pick-img' src={item.coverUrl} mode='aspectFill' />
                ) : (
                  <View className='cook-submit__pick-empty'>
                    <MiniIcon name='dish' size='md' tone='muted' />
                  </View>
                )}
                <View className='cook-submit__pick-copy'>
                  <Text className='cook-submit__pick-name'>{item.name}</Text>
                  <Text className='cook-submit__pick-meta'>
                    {item.steps.length} 步 · {formatDuration(item.estimatedTotalSeconds)}
                  </Text>
                </View>
                <Text className='cook-submit__pick-go'>选它</Text>
              </View>
            ))}
          </View>
        )}
      </View>
    )
  }

  return (
    <View className='cook-submit'>
      <View className='cook-submit__dish'>
        {dish.coverUrl ? (
          <Image className='cook-submit__dish-img' src={dish.coverUrl} mode='aspectFill' />
        ) : (
          <View className='cook-submit__dish-empty'>
            <MiniIcon name='dish' size='lg' tone='muted' />
          </View>
        )}
        <View className='cook-submit__dish-copy'>
          <Text className='cook-submit__dish-name'>{dish.name}</Text>
          <Text className='cook-submit__dish-meta'>
            {dish.steps.length} 步 · {formatDuration(dish.estimatedTotalSeconds)}
          </Text>
          {dish.description ? (
            <Text className='cook-submit__dish-desc'>{dish.description}</Text>
          ) : null}
        </View>
        {!initialDishId ? (
          <Text className='cook-submit__dish-change' onClick={resetDish}>
            换一道
          </Text>
        ) : null}
      </View>

      <View className='cook-submit__card'>
        <Text className='cook-submit__label'>做几份</Text>
        <View className='cook-submit__stepper'>
          <Text
            className='cook-submit__step-btn'
            onClick={() => setServings(String(Math.max(1, (Number(servings) || 1) - 1)))}
          >
            −
          </Text>
          <Text className='cook-submit__step-num'>{servings}</Text>
          <Text
            className='cook-submit__step-btn'
            onClick={() => setServings(String(Math.min(99, (Number(servings) || 1) + 1)))}
          >
            ＋
          </Text>
        </View>

        <Text className='cook-submit__label'>优先级</Text>
        <View className='cook-submit__chips'>
          {PRIORITIES.map((item) => (
            <Text
              key={item.value}
              className={`cook-submit__chip ${priority === item.value ? 'cook-submit__chip--on' : ''}`}
              onClick={() => setPriority(item.value)}
            >
              {item.label}
            </Text>
          ))}
        </View>

        <Text className='cook-submit__label'>计划开始（选填）</Text>
        <View className='cook-submit__plan'>
          <Picker
            mode='date'
            value={planDate}
            onChange={(e) => setPlanDate(String(e.detail.value))}
          >
            <View className='cook-submit__plan-item'>
              <Text>{planDate || '选择日期'}</Text>
            </View>
          </Picker>
          <Picker
            mode='time'
            value={planTime}
            onChange={(e) => setPlanTime(String(e.detail.value))}
          >
            <View className='cook-submit__plan-item'>
              <Text>{planTime || '选择时间'}</Text>
            </View>
          </Picker>
          {planDate || planTime ? (
            <Text
              className='cook-submit__plan-clear'
              onClick={() => {
                setPlanDate('')
                setPlanTime('')
              }}
            >
              清除
            </Text>
          ) : null}
        </View>

        <Text className='cook-submit__label'>备注</Text>
        <Textarea
          className='cook-submit__textarea'
          value={note}
          maxlength={500}
          placeholder='口味偏好、忌口、什么时候要吃'
          onInput={(e) => setNote(e.detail.value)}
        />

        <Text className='cook-submit__tip'>
          提交后进入组织任务池，任何成员都可以领取并开始做菜
        </Text>
      </View>

      <View className='cook-submit__actions'>
        <Text className='cook-submit__btn' onClick={() => void submit()}>
          {submitting ? '提交中…' : '提交做菜任务'}
        </Text>
      </View>
    </View>
  )
}
