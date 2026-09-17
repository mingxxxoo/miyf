import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import {
  fetchChefBindings,
  fetchChefDishes,
  fetchChefOrders,
  fetchMyKitchen,
  updateChefOrderStatus,
  type ChefOrder
} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import { useUserStore } from '@/stores/userStore'
import { prefetchChefWxSubscribeConfig, requestChefOrderSubscribe } from '@/utils/wxSubscribe'
import './chef.scss'

const MENUS = [
  { label: '菜品', url: '/pages/chef/dishes', icon: '🥘', bg: '#FFF1E2' },
  { label: '菜谱', url: '/pages/chef/recipes', icon: '📗', bg: '#E7F7F0' },
  { label: '分类', url: '/pages/chef/categories', icon: '🗂️', bg: '#FBF3E0' },
  { label: '预约', url: '/pages/chef/orders', icon: '📝', bg: '#EBF3FB', badgeKey: 'pending' as const },
  { label: '食客申请', url: '/pages/chef/bindings', icon: '🤝', bg: '#FCEEEA', badgeKey: 'apply' as const },
  { label: '邀请码', url: '/pages/chef/invite', icon: '🎟️', bg: '#FFF1E2' },
  { label: '厨房资料', url: '/pages/chef/kitchen', icon: '🏡', bg: '#E7F7F0' },
  { label: '经营统计', url: '', icon: '📈', bg: '#F4F0E9', soon: true }
]

const NEXT: Record<string, { status: string; label: string }> = {
  PENDING: { status: 'CONFIRMED', label: '确认' },
  CONFIRMED: { status: 'PREPARING', label: '开始备餐' },
  PREPARING: { status: 'READY', label: '可以取餐' },
  READY: { status: 'COMPLETED', label: '完成' }
}

const STATUS_CHIP: Record<string, { text: string; cls: string }> = {
  PENDING: { text: '待确认', cls: 'ck-chip--warn' },
  CONFIRMED: { text: '已确认', cls: 'ck-chip--info' },
  PREPARING: { text: '备餐中', cls: 'ck-chip--info' },
  READY: { text: '待取餐', cls: 'ck-chip--ok' },
  COMPLETED: { text: '已完成', cls: 'ck-chip--ok' },
  CANCELLED: { text: '已取消', cls: 'ck-chip--none' }
}

const AVATAR_COLORS = ['#F07B1F', '#18A885', '#4A90D9', '#D99426', '#E15A4B']

function greetByHour() {
  const h = new Date().getHours()
  if (h < 11) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
}

function relativeTime(value?: string) {
  if (!value) return ''
  const t = new Date(value).getTime()
  if (Number.isNaN(t)) return value.replace('T', ' ').slice(0, 16)
  const diff = Date.now() - t
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚提交'
  if (m < 60) return `${m} 分钟前提交`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前提交`
  return value.replace('T', ' ').slice(0, 16)
}

function itemSummary(order: ChefOrder) {
  const items = order.items || []
  if (!items.length) return '暂无菜品明细'
  return items.map((it) => `${it.dishName} ×${it.quantity}`).join(' · ')
}

function nickInitial(name?: string) {
  const n = (name || '客').trim()
  return n.slice(0, 1)
}

export default function ChefHomePage() {
  useChefWorkbench()
  const user = useUserStore((s) => s.user)
  const [kitchenName, setKitchenName] = useState('我的厨房')
  const [pending, setPending] = useState(0)
  const [preparing, setPreparing] = useState(0)
  const [onSale, setOnSale] = useState(0)
  const [diners, setDiners] = useState(0)
  const [applyCount, setApplyCount] = useState(0)
  const [recent, setRecent] = useState<ChefOrder[]>([])
  const [actingId, setActingId] = useState<string | null>(null)

  const badges = useMemo(
    () => ({ pending, apply: applyCount }),
    [pending, applyCount]
  )

  const load = async () => {
    const [kitchen, pendingPage, preparingPage, dishes, bound, apply, recentPage] =
      await Promise.all([
        fetchMyKitchen(),
        fetchChefOrders('PENDING').catch(() => null),
        fetchChefOrders('PREPARING').catch(() => null),
        fetchChefDishes().catch(() => null),
        fetchChefBindings('BOUND').catch(() => null),
        fetchChefBindings('PENDING').catch(() => null),
        fetchChefOrders().catch(() => null)
      ])
    if (kitchen?.name) setKitchenName(kitchen.name)
    setPending(Number(pendingPage?.total || pendingPage?.records?.length || 0))
    setPreparing(Number(preparingPage?.total || preparingPage?.records?.length || 0))
    setOnSale((dishes?.records || []).filter((d) => d.status === 'ON_SALE').length)
    setDiners(Number(bound?.total || bound?.records?.length || 0))
    setApplyCount(Number(apply?.total || apply?.records?.length || 0))
    setRecent((recentPage?.records || []).slice(0, 5))
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
    void prefetchChefWxSubscribeConfig()
  })

  const go = (url: string, withSubscribe = false) => {
    const nav = () => Taro.navigateTo({ url })
    if (withSubscribe) {
      void requestChefOrderSubscribe().finally(nav)
    } else {
      nav()
    }
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

  const rejectOrder = (order: ChefOrder) => {
    if (actingId) return
    Taro.showModal({
      title: '拒绝预约',
      content: `确认拒绝「${order.userNickname || '食客'}」的预约？`,
      success: async (res) => {
        if (!res.confirm) return
        setActingId(order.id)
        try {
          await updateChefOrderStatus(order.id, 'CANCELLED')
          Taro.showToast({ title: '已拒绝', icon: 'success' })
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
    })
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>
      <View className='chef-page__hero-card'>
        <Text className='chef-page__hello'>
          {greetByHour()}，{user?.nickname || '厨师'} 👋
        </Text>
        <View className='chef-page__kname'>
          <Text className='chef-page__kname-text'>{kitchenName}</Text>
          <Text className='chef-page__verify'>已认证厨房</Text>
        </View>
        <View className='chef-page__stats'>
          <View className='chef-page__stat'>
            <Text className='chef-page__stat-num'>{pending}</Text>
            <Text className='chef-page__stat-label'>待确认</Text>
          </View>
          <View className='chef-page__stat'>
            <Text className='chef-page__stat-num'>{preparing}</Text>
            <Text className='chef-page__stat-label'>备餐中</Text>
          </View>
          <View className='chef-page__stat'>
            <Text className='chef-page__stat-num'>{onSale}</Text>
            <Text className='chef-page__stat-label'>在售菜品</Text>
          </View>
          <View className='chef-page__stat'>
            <Text className='chef-page__stat-num'>{diners}</Text>
            <Text className='chef-page__stat-label'>食客</Text>
          </View>
        </View>
      </View>

      <View className='chef-page__sec-row'>
        <Text className='chef-page__sec-title'>厨房管理</Text>
      </View>
      <View className='chef-page__grid'>
        {MENUS.map((m) => {
          const badge =
            m.badgeKey && badges[m.badgeKey] > 0 ? badges[m.badgeKey] : 0
          return (
            <View
              key={m.label}
              className='chef-page__g8 ck-pressable'
              onClick={() => {
                if (m.soon || !m.url) {
                  Taro.showToast({ title: '经营统计即将开放', icon: 'none' })
                  return
                }
                go(m.url, m.url.includes('/chef/orders'))
              }}
            >
              <View className='chef-page__g8-ico' style={{ background: m.bg }}>
                <Text>{m.icon}</Text>
              </View>
              <Text className='chef-page__g8-label'>{m.label}</Text>
              {badge > 0 ? (
                <Text className='chef-page__badge'>{badge > 99 ? '99+' : badge}</Text>
              ) : null}
            </View>
          )
        })}
      </View>

      <View className='chef-page__sec-row'>
        <Text className='chef-page__sec-title'>最新预约</Text>
        <Text
          className='chef-page__sec-more'
          onClick={() => go('/pages/chef/orders', true)}
        >
          全部 ›
        </Text>
      </View>

      {recent.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='暂无预约' description='食客下单后会出现在这里' />
        </View>
      ) : (
        recent.map((o, idx) => {
          const next = NEXT[o.status]
          const chip = STATUS_CHIP[o.status] || STATUS_CHIP.PENDING
          const nick = o.userNickname || '厨房朋友'
          return (
            <View key={o.id} className='chef-page__order-card'>
              <View className='chef-page__oc-top'>
                <View className='chef-page__oc-user'>
                  <View
                    className='chef-page__avatar'
                    style={{ background: AVATAR_COLORS[idx % AVATAR_COLORS.length] }}
                  >
                    <Text>{nickInitial(nick)}</Text>
                  </View>
                  <View>
                    <Text className='chef-page__oc-name'>{nick}</Text>
                    <Text className='chef-page__oc-time'>{relativeTime(o.createTime)}</Text>
                  </View>
                </View>
                <Text className={`ck-chip ${chip.cls}`}>{chip.text}</Text>
              </View>
              <Text className='chef-page__oc-dishes'>{itemSummary(o)}</Text>
              {o.remark ? (
                <Text className='chef-page__oc-meta' style={{ marginTop: '8px' }}>
                  备注：{o.remark}
                </Text>
              ) : null}
              <View className='chef-page__oc-foot'>
                <Text className='chef-page__oc-meta'>🕐 单号 #{o.orderNo}</Text>
                {next ? (
                  <View className='chef-page__oc-actions'>
                    {o.status === 'PENDING' ? (
                      <Button
                        className='chef-page__btn-xs chef-page__btn-xs--ghost'
                        size='mini'
                        disabled={actingId === o.id}
                        onClick={() => rejectOrder(o)}
                      >
                        拒绝
                      </Button>
                    ) : null}
                    <Button
                      className='chef-page__btn-xs chef-page__btn-xs--solid'
                      size='mini'
                      loading={actingId === o.id}
                      onClick={() => void advance(o)}
                    >
                      {o.status === 'PENDING' ? '确认' : next.label}
                    </Button>
                  </View>
                ) : null}
              </View>
            </View>
          )
        })
      )}
    </View>
  )
}
