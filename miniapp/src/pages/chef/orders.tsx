import { View, Text, Button, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import { fetchChefOrders, updateChefOrderStatus, type ChefOrder } from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import { prefetchChefWxSubscribeConfig, requestChefOrderSubscribe } from '@/utils/wxSubscribe'
import './chef.scss'

const TABS: { key: string; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待确认' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'PREPARING', label: '备餐中' },
  { key: 'READY', label: '待取餐' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

const NEXT: Record<string, { status: string; label: string }> = {
  PENDING: { status: 'CONFIRMED', label: '确认预约' },
  CONFIRMED: { status: 'PREPARING', label: '开始备餐' },
  PREPARING: { status: 'READY', label: '可以取餐' },
  READY: { status: 'COMPLETED', label: '完成' }
}

const FLOW = [
  { key: 'PENDING', label: '确认' },
  { key: 'PREPARING', label: '备餐' },
  { key: 'READY', label: '取餐' },
  { key: 'COMPLETED', label: '完成' }
]

const STATUS_CHIP: Record<string, { text: string; cls: string }> = {
  PENDING: { text: '待确认', cls: 'ck-chip--warn' },
  CONFIRMED: { text: '已确认', cls: 'ck-chip--info' },
  PREPARING: { text: '备餐中', cls: 'ck-chip--info' },
  READY: { text: '待取餐', cls: 'ck-chip--ok' },
  COMPLETED: { text: '已完成', cls: 'ck-chip--ok' },
  CANCELLED: { text: '已取消', cls: 'ck-chip--none' }
}

const AVATAR_COLORS = ['#F07B1F', '#18A885', '#4A90D9', '#D99426', '#E15A4B']

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function itemSummary(order: ChefOrder) {
  const items = order.items || []
  if (!items.length) return '暂无菜品明细'
  return items.map((it) => `${it.dishName} ×${it.quantity}`).join(' · ')
}

function flowIndex(status: string) {
  if (status === 'PENDING' || status === 'CONFIRMED') return 0
  if (status === 'PREPARING') return 1
  if (status === 'READY') return 2
  if (status === 'COMPLETED') return 3
  return -1
}

export default function ChefOrdersPage() {
  useChefWorkbench()
  const [tab, setTab] = useState('ALL')
  const [list, setList] = useState<ChefOrder[]>([])
  const [allForSum, setAllForSum] = useState<ChefOrder[]>([])
  const [actingId, setActingId] = useState<string | null>(null)

  const sums = useMemo(() => {
    const pending = allForSum.filter((o) => o.status === 'PENDING').length
    const preparing = allForSum.filter((o) => o.status === 'PREPARING').length
    const ready = allForSum.filter((o) => o.status === 'READY').length
    return { pending, preparing, ready }
  }, [allForSum])

  const load = async (status = tab) => {
    const [page, allPage] = await Promise.all([
      fetchChefOrders(status === 'ALL' ? undefined : status),
      status === 'ALL' ? Promise.resolve(null) : fetchChefOrders().catch(() => null)
    ])
    setList(page.records || [])
    if (status === 'ALL') {
      setAllForSum(page.records || [])
    } else if (allPage) {
      setAllForSum(allPage.records || [])
    }
  }

  useDidShow(() => {
    void prefetchChefWxSubscribeConfig()
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const enableNotify = () => {
    void requestChefOrderSubscribe().then((ok) => {
      Taro.showToast({
        title: ok ? '已开启新预约提醒' : '未获得提醒授权',
        icon: 'none'
      })
    })
  }

  const switchTab = (key: string) => {
    setTab(key)
    void load(key).catch(() => {
      Taro.showToast({ title: '加载失败', icon: 'none' })
    })
  }

  const advance = async (order: ChefOrder) => {
    const next = NEXT[order.status]
    if (!next || actingId) return
    setActingId(order.id)
    try {
      await updateChefOrderStatus(order.id, next.status)
      Taro.showToast({ title: next.label + '成功', icon: 'success' })
      await load()
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '操作失败',
        icon: 'none'
      })
    } finally {
      setActingId(null)
    }
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>处理预约</Text>
        <Text className='chef-page__sub'>确认 → 备餐 → 取餐 → 完成</Text>
        <Button className='chef-page__btn-ghost' size='mini' onClick={enableNotify}>
          开启新预约提醒
        </Button>
      </View>

      <View className='chef-page__sum-strip'>
        <View className='chef-page__sum-cell' style={{ background: '#FBF3E0', color: '#D99426' }}>
          <Text className='chef-page__sum-num'>{sums.pending}</Text>
          <Text className='chef-page__sum-label'>待确认</Text>
        </View>
        <View className='chef-page__sum-cell' style={{ background: '#EBF3FB', color: '#4A90D9' }}>
          <Text className='chef-page__sum-num'>{sums.preparing}</Text>
          <Text className='chef-page__sum-label'>备餐中</Text>
        </View>
        <View className='chef-page__sum-cell' style={{ background: '#E7F7F0', color: '#18A885' }}>
          <Text className='chef-page__sum-num'>{sums.ready}</Text>
          <Text className='chef-page__sum-label'>待取餐</Text>
        </View>
      </View>

      <ScrollView scrollX className='chef-page__tabs'>
        <View className='chef-page__tabs-inner'>
          {TABS.map((t) => (
            <Text
              key={t.key}
              className={`chef-page__tab ${tab === t.key ? 'chef-page__tab--on' : ''}`}
              onClick={() => switchTab(t.key)}
            >
              {t.label}
            </Text>
          ))}
        </View>
      </ScrollView>

      {list.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='暂无预约' description='食客下单后会出现在这里' />
        </View>
      ) : (
        list.map((o, idx) => {
          const next = NEXT[o.status]
          const chip = STATUS_CHIP[o.status] || STATUS_CHIP.PENDING
          const fi = flowIndex(o.status)
          const nick = o.userNickname || '厨房朋友'
          return (
            <View key={o.id} className='chef-page__order-card'>
              <View className='chef-page__oc-top'>
                <View className='chef-page__oc-user'>
                  <View
                    className='chef-page__avatar'
                    style={{ background: AVATAR_COLORS[idx % AVATAR_COLORS.length] }}
                  >
                    <Text>{nick.slice(0, 1)}</Text>
                  </View>
                  <View>
                    <Text className='chef-page__oc-name'>{nick}</Text>
                    <Text className='chef-page__oc-time'>#{o.orderNo} · {formatTime(o.createTime)}</Text>
                  </View>
                </View>
                <Text className={`ck-chip ${chip.cls}`}>{chip.text}</Text>
              </View>

              {fi >= 0 ? (
                <View className='chef-page__flow' style={{ margin: '20px 0 8px' }}>
                  {FLOW.flatMap((step, i) => {
                    const nodes = [
                      <View
                        key={step.key}
                        className={`chef-page__fstep ${i === fi ? 'chef-page__fstep--cur' : ''}`}
                      >
                        <View
                          className={`chef-page__fdot ${
                            i < fi ? 'chef-page__fdot--done' : i === fi ? 'chef-page__fdot--cur' : ''
                          }`}
                        >
                          <Text>{i < fi ? '✓' : i + 1}</Text>
                        </View>
                        <Text className='chef-page__fstep-label'>{step.label}</Text>
                      </View>
                    ]
                    if (i < FLOW.length - 1) {
                      nodes.push(
                        <View
                          key={`${step.key}-line`}
                          className={`chef-page__fline ${i < fi ? 'chef-page__fline--done' : ''}`}
                        />
                      )
                    }
                    return nodes
                  })}
                </View>
              ) : null}

              <Text className='chef-page__oc-dishes'>{itemSummary(o)}</Text>
              {o.remark ? <Text className='chef-page__oc-meta' style={{ marginTop: '12px' }}>备注：{o.remark}</Text> : null}
              {next ? (
                <View className='chef-page__oc-foot'>
                  <View />
                  <View className='chef-page__oc-actions'>
                    <Button
                      className='chef-page__btn-xs chef-page__btn-xs--solid'
                      size='mini'
                      loading={actingId === o.id}
                      onClick={() => void advance(o)}
                    >
                      {next.label}
                    </Button>
                  </View>
                </View>
              ) : null}
            </View>
          )
        })
      )}
    </View>
  )
}
