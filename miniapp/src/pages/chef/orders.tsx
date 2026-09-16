import { View, Text, Button, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchChefOrders, updateChefOrderStatus, type ChefOrder } from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import StatusBadge from '@/components/StatusBadge'
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

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

function itemSummary(order: ChefOrder) {
  const items = order.items || []
  if (!items.length) return '暂无菜品明细'
  if (items.length === 1) {
    const it = items[0]
    return `${it.dishName} × ${it.quantity}${it.unit || ''}`
  }
  return items.map((it) => `${it.dishName}×${it.quantity}`).join('、')
}

export default function ChefOrdersPage() {
  useChefWorkbench()
  const [tab, setTab] = useState('ALL')
  const [list, setList] = useState<ChefOrder[]>([])
  const [actingId, setActingId] = useState<string | null>(null)

  const load = async (status = tab) => {
    const page = await fetchChefOrders(status === 'ALL' ? undefined : status)
    setList(page.records || [])
  }

  useDidShow(() => {
    // 预取模板；授权改由「开启提醒」按钮在用户手势内触发
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
        <Text className='chef-page__sub'>按流程推进：确认 → 备餐 → 取餐 → 完成</Text>
        <Button className='chef-page__btn-ghost' size='mini' onClick={enableNotify}>
          开启新预约提醒
        </Button>
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
        list.map((o) => {
          const next = NEXT[o.status]
          return (
            <View key={o.id} className='chef-page__card'>
              <View className='chef-page__row'>
                <Text className='chef-dish__name'>#{o.orderNo}</Text>
                <StatusBadge status={o.status} />
              </View>
              <Text className='chef-dish__desc'>食客：{o.userNickname || '厨房朋友'}</Text>
              <Text className='chef-dish__desc'>菜品：{itemSummary(o)}</Text>
              <Text className='chef-dish__desc'>下单：{formatTime(o.createTime)}</Text>
              {o.remark ? <Text className='chef-dish__desc'>备注：{o.remark}</Text> : null}
              {next ? (
                <View className='chef-page__actions'>
                  <Button
                    className='chef-page__action'
                    size='mini'
                    loading={actingId === o.id}
                    onClick={() => void advance(o)}
                  >
                    {next.label}
                  </Button>
                </View>
              ) : null}
            </View>
          )
        })
      )}
    </View>
  )
}
