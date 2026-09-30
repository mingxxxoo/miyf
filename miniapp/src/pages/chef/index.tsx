import {Text, View} from '@tarojs/components'
import Taro, {useDidShow} from '@tarojs/taro'
import {useMemo, useState} from 'react'
import {type ChefOrder, fetchChefWorkbenchSummary, fetchMyKitchen, updateChefOrderStatus} from '@/api/kitchen'
import ChefRecentOrderCard from '@/components/ChefRecentOrderCard'
import ChefWorkbenchHero from '@/components/ChefWorkbenchHero'
import EmptyState from '@/components/EmptyState'
import MiniIcon, {type MiniIconName} from '@/components/MiniIcon'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import {useChefWorkbench} from '@/hooks/useChefWorkbench'
import {useUserStore} from '@/stores/userStore'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import {prefetchChefWxSubscribeConfig, requestChefOrderSubscribe} from '@/utils/wxSubscribe'
import './chef.scss'

const MENUS: {
  label: string
  url: string
  icon: MiniIconName
  bg: string
  badgeKey?: 'pending' | 'apply'
}[] = [
  { label: '菜品', url: '/pages/chef/dishes', icon: 'dish', bg: '#FFF1E2' },
  { label: '菜谱', url: '/pages/chef/recipes', icon: 'recipe', bg: '#FFF1E2' },
  { label: '分类', url: '/pages/chef/categories', icon: 'category', bg: '#F5E6D8' },
  { label: '预约', url: '/pages/chef/orders', icon: 'order', bg: '#FBF3E0', badgeKey: 'pending' },
  { label: '申请', url: '/pages/chef/bindings', icon: 'people', bg: '#EBF3FB', badgeKey: 'apply' },
  { label: '邀请码', url: '/pages/chef/invite', icon: 'ticket', bg: '#E7F7F0' },
  { label: '资料', url: '/pages/chef/kitchen', icon: 'home', bg: '#F4F0E9' },
  { label: '统计', url: '/pages/chef/stats', icon: 'trend', bg: '#FFE8C8' }
]

const ACTIONABLE = new Set(['PENDING', 'CONFIRMED', 'PREPARING', 'READY'])

function greetByHour() {
  const h = new Date().getHours()
  if (h < 11) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
}

function sortRecent(list: ChefOrder[]) {
  return [...list].sort((a, b) => {
    const aa = ACTIONABLE.has(a.status) ? 0 : 1
    const bb = ACTIONABLE.has(b.status) ? 0 : 1
    if (aa !== bb) return aa - bb
    const ta = a.createTime ? new Date(a.createTime).getTime() : 0
    const tb = b.createTime ? new Date(b.createTime).getTime() : 0
    return tb - ta
  })
}

export default function ChefHomePage() {
  useChefWorkbench()
  const user = useUserStore((s) => s.user)
  const [kitchenName, setKitchenName] = useState('我的厨房')
  const [kitchenStatus, setKitchenStatus] = useState<string | undefined>()
  const [coverUrl, setCoverUrl] = useState<string | undefined>()
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
    const [summary, kitchen] = await Promise.all([
      fetchChefWorkbenchSummary(),
      fetchMyKitchen().catch(() => null)
    ])
    if (summary.kitchenName) setKitchenName(summary.kitchenName)
    setKitchenStatus(summary.kitchenStatus)
    setPending(summary.pendingOrders)
    setPreparing(summary.preparingOrders)
    setOnSale(summary.onSaleDishes)
    setDiners(summary.boundDiners)
    setApplyCount(summary.pendingBindings)
    setRecent(sortRecent(summary.recentOrders).slice(0, 5))
    const cover = kitchen?.coverUrl || toAbsoluteResourceUrl(kitchen?.coverImage)
    setCoverUrl(cover || undefined)
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
    const map: Record<string, { status: string; label: string }> = {
      PENDING: { status: 'CONFIRMED', label: '确认' },
      CONFIRMED: { status: 'PREPARING', label: '开始备餐' },
      PREPARING: { status: 'READY', label: '可以取餐' },
      READY: { status: 'COMPLETED', label: '完成' }
    }
    const next = map[order.status]
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
      title: '驳回预约',
      content: `确认驳回「${order.userNickname || '食客'}」的预约？`,
      success: async (res) => {
        if (!res.confirm) return
        setActingId(order.id)
        try {
          await updateChefOrderStatus(order.id, 'CANCELLED')
          Taro.showToast({ title: '已驳回', icon: 'success' })
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

  const statusText =
    kitchenStatus === 'OPEN' ? '营业中' : kitchenStatus === 'CLOSED' ? '暂停营业' : '我的厨房'

  const nick = user?.nickname || '厨师'

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>

      <ChefWorkbenchHero
        greet={`${greetByHour()}，${nick}`}
        kitchenName={kitchenName}
        statusText={statusText}
        coverUrl={coverUrl}
        stats={[
          {
            value: pending,
            label: '待确认',
            onClick: () => go('/pages/chef/orders', true)
          },
          {
            value: preparing,
            label: '备餐中',
            onClick: () => go('/pages/chef/orders', true)
          },
          {
            value: onSale,
            label: '在售',
            onClick: () => go('/pages/chef/dishes')
          },
          {
            value: diners,
            label: '食客',
            onClick: () => go('/pages/chef/bindings')
          }
        ]}
      />

      <View className='chef-page__sec-row'>
        <Text className='chef-page__sec-title'>快捷入口</Text>
      </View>
      <View className='chef-page__grid'>
        {MENUS.map((m) => {
          const badge = m.badgeKey && badges[m.badgeKey] > 0 ? badges[m.badgeKey] : 0
          return (
            <View
              key={m.label}
              className='chef-page__g8 ck-pressable'
              onClick={() => go(m.url, m.url.includes('/chef/orders'))}
            >
              <View className='chef-page__g8-ico' style={{ background: m.bg }}>
                <MiniIcon
                  name={m.icon}
                  size='md'
                  tone={m.label === '邀请码' ? 'mint' : 'default'}
                />
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
        <Text className='chef-page__sec-more' onClick={() => go('/pages/chef/orders', true)}>
          全部 ›
        </Text>
      </View>

      {recent.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='暂无预约' description='食客下单后会出现在这里' />
        </View>
      ) : (
        <View className='chef-page__book-list'>
          {recent.map((o) => (
            <ChefRecentOrderCard
              key={o.id}
              order={o}
              acting={actingId === o.id}
              onAdvance={(ord) => void advance(ord)}
              onReject={rejectOrder}
              onOpen={() => go('/pages/chef/orders', true)}
            />
          ))}
        </View>
      )}
    </View>
  )
}
