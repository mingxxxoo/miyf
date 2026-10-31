import {Text, View} from '@tarojs/components'
import Taro, {useDidShow, usePullDownRefresh, useReachBottom} from '@tarojs/taro'
import {useRef, useState} from 'react'
import {claimCookingTask, type CookingTaskScope, fetchCookingTasks} from '@/api/cookingTask'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import type {CookingTask} from '@/types'
import {COOKING_STATUS_META, formatTaskTime} from '@/utils/cookingTask'
import './pool.scss'

const PAGE_SIZE = 20

const TABS: { key: CookingTaskScope; label: string }[] = [
  { key: 'pool', label: '任务池' },
  { key: 'submitted', label: '我提交' },
  { key: 'assigned', label: '我领取' },
  { key: 'all', label: '全部' }
]

export default function CookPoolPage() {
  const [list, setList] = useState<CookingTask[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [tab, setTab] = useState<CookingTaskScope>('pool')
  const [claimingId, setClaimingId] = useState<string | null>(null)
  const scopeRef = useRef<CookingTaskScope>('pool')
  const requestIdRef = useRef(0)

  const load = async (reset: boolean, scope: CookingTaskScope = scopeRef.current) => {
    if (!reset && (loadingMore || loading)) return
    const requestId = ++requestIdRef.current
    const nextPage = reset ? 1 : page + 1
    if (reset) {
      setLoading(true)
    } else {
      setLoadingMore(true)
    }
    try {
      const result = await fetchCookingTasks({ page: nextPage, rows: PAGE_SIZE, scope })
      if (requestId !== requestIdRef.current) return
      const records = result.records || []
      setList((prev) => (reset ? records : [...prev, ...records]))
      setTotal(result.total)
      setPage(nextPage)
    } catch {
      if (reset && requestId === requestIdRef.current) setList([])
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
        setLoadingMore(false)
      }
    }
  }

  useDidShow(() => {
    void load(true, scopeRef.current)
  })

  usePullDownRefresh(async () => {
    try {
      await load(true, scopeRef.current)
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  useReachBottom(() => {
    if (list.length >= total || loadingMore || loading) return
    void load(false, scopeRef.current)
  })

  const switchTab = (key: CookingTaskScope) => {
    if (tab === key) return
    scopeRef.current = key
    setTab(key)
    setList([])
    setTotal(0)
    void load(true, key)
  }

  const openDetail = (task: CookingTask) => {
    Taro.navigateTo({ url: `/pages/cook/detail?id=${task.id}` })
  }

  const openFlow = (task: CookingTask) => {
    Taro.navigateTo({ url: `/pages/cook/flow?id=${task.id}` })
  }

  const claim = async (task: CookingTask) => {
    if (claimingId) return
    setClaimingId(task.id)
    Taro.showLoading({ title: '领取中…', mask: true })
    try {
      await claimCookingTask(task.id)
      Taro.showToast({ title: '领取成功，开始做菜吧', icon: 'success' })
      setTimeout(() => {
        Taro.navigateTo({ url: `/pages/cook/detail?id=${task.id}` })
      }, 300)
    } catch (err) {
      const message = err instanceof Error ? err.message : '领取失败'
      Taro.showToast({ title: message, icon: 'none' })
      await load(true, scopeRef.current)
    } finally {
      Taro.hideLoading()
      setClaimingId(null)
    }
  }

  return (
    <View className='cook-pool'>
      <View className='cook-pool__hero'>
        <View>
          <Text className='cook-pool__hero-title'>做菜任务池</Text>
          <Text className='cook-pool__hero-desc'>按顺序领取，做完一步再做下一步</Text>
        </View>
        <View className='cook-pool__hero-count'>
          <Text className='cook-pool__hero-num'>{total}</Text>
          <Text className='cook-pool__hero-unit'>单</Text>
        </View>
      </View>

      <View className='cook-pool__tabs'>
        {TABS.map((item) => (
          <Text
            key={item.key}
            className={`cook-pool__tab ${tab === item.key ? 'cook-pool__tab--on' : ''}`}
            onClick={() => switchTab(item.key)}
          >
            {item.label}
          </Text>
        ))}
      </View>

      {loading && list.length === 0 ? (
        <Loading text='正在读取任务…' />
      ) : list.length === 0 ? (
        <EmptyState
          icon='recipe'
          title='暂时没有任务'
          description='去菜品库选一道菜，提交做菜任务'
          actionText='去提交任务'
          onAction={() => Taro.navigateTo({ url: '/pages/cook/submit' })}
        />
      ) : (
        <View className='cook-pool__list'>
          {list.map((task) => {
            const meta = COOKING_STATUS_META[task.status] || COOKING_STATUS_META.PENDING
            const snapshot = task.dishSnapshot || {}
            const stepCount = task.steps?.length || snapshot.steps?.length || 0
            return (
              <View key={task.id} className='cook-card' onClick={() => openDetail(task)}>
                <View className='cook-card__head'>
                  <View className='cook-card__title-wrap'>
                    <Text className='cook-card__title'>{snapshot.name || '未命名菜品'}</Text>
                    <Text className={`cook-card__status cook-card__status--${meta.tone}`}>
                      {meta.label}
                    </Text>
                    {task.priority && task.priority >= 5 ? (
                      <Text className='cook-card__priority'>
                        {task.priority >= 9 ? '最急' : '加急'}
                      </Text>
                    ) : null}
                  </View>
                  <Text className='cook-card__time'>{formatTaskTime(task.createTime)}</Text>
                </View>

                <View className='cook-card__meta'>
                  <Text className='cook-card__meta-item'>{stepCount} 步</Text>
                  <Text className='cook-card__meta-item'>{task.servings || 1} 人份</Text>
                  {task.scheduledStartTime ? (
                    <Text className='cook-card__meta-item'>
                      计划 {formatTaskTime(task.scheduledStartTime)}
                    </Text>
                  ) : null}
                </View>

                {task.note ? <Text className='cook-card__note'>{task.note}</Text> : null}

                <View className='cook-card__foot'>
                  <View className='cook-card__people'>
                    <MiniIcon name='user' size='sm' tone='muted' />
                    <Text className='cook-card__people-text'>
                      {task.submitterName || '成员'} 提交
                      {task.chefName ? ` · ${task.chefName} 掌勺` : ''}
                    </Text>
                  </View>
                  {task.canClaim ? (
                    <Text
                      className='cook-card__action'
                      onClick={(e) => {
                        e.stopPropagation()
                        void claim(task)
                      }}
                    >
                      {claimingId === task.id ? '领取中…' : '领取'}
                    </Text>
                  ) : task.canOpenFlow && (task.status === 'COOKING' || task.status === 'PAUSED') ? (
                    <Text
                      className='cook-card__action'
                      onClick={(e) => {
                        e.stopPropagation()
                        openFlow(task)
                      }}
                    >
                      继续做菜
                    </Text>
                  ) : (
                    <Text className='cook-card__action cook-card__action--ghost'>查看 ›</Text>
                  )}
                </View>
              </View>
            )
          })}
          {loadingMore ? (
            <Text className='cook-pool__more'>加载中…</Text>
          ) : list.length >= total ? (
            <Text className='cook-pool__more'>已经到底啦</Text>
          ) : null}
        </View>
      )}

      <View className='cook-pool__fab' onClick={() => Taro.navigateTo({ url: '/pages/cook/submit' })}>
        <Text className='cook-pool__fab-plus'>＋</Text>
        <Text className='cook-pool__fab-text'>提任务</Text>
      </View>
    </View>
  )
}
