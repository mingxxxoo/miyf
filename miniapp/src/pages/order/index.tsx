import { View, Text, Button, Input, Picker } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useEffect, useState } from 'react'
import OrderCard from '@/components/OrderCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import { useOrderStore } from '@/stores/orderStore'
import { useUserStore } from '@/stores/userStore'
import type { OrderStatus } from '@/types'
import './index.scss'

const TABS: { key: 'ALL' | OrderStatus; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待确认' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'PREPARING', label: '制作中' },
  { key: 'READY', label: '待取餐' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

export default function OrderIndexPage() {
  const {
    orders,
    draft,
    loading,
    fetchOrders,
    updateDraft,
    submitOrder,
    clearDraft
  } = useOrderStore()
  const { isLoggedIn, login } = useUserStore()
  const [tab, setTab] = useState<'ALL' | OrderStatus>('ALL')

  useDidShow(() => {
    if (isLoggedIn) void fetchOrders(tab === 'ALL' ? undefined : tab)
  })

  useEffect(() => {
    if (isLoggedIn) void fetchOrders(tab === 'ALL' ? undefined : tab)
  }, [tab, isLoggedIn, fetchOrders])

  const ensureLogin = async () => {
    if (isLoggedIn) return true
    return login()
  }

  const handleSubmit = async () => {
    if (!draft.items.length) {
      Taro.showToast({ title: '先选一道想吃的菜吧', icon: 'none' })
      return
    }
    const ok = await ensureLogin()
    if (!ok) return
    const order = await submitOrder()
    if (order) {
      Taro.showToast({ title: order.displayTip || '厨房收到啦', icon: 'success' })
    }
  }

  const handlePickTime = (e: { detail: { value: string } }) => {
    updateDraft({ scheduledAt: String(e.detail.value) })
  }

  const handlePickGuest = (e: { detail: { value: string | number } }) => {
    const guests = [1, 2, 3, 4, 5, 6, 8, 10]
    updateDraft({ guestCount: guests[Number(e.detail.value)] || 2 })
  }

  if (!isLoggedIn && !draft.items.length) {
    return (
      <EmptyState
        emoji='🔑'
        title='登录后查看预约'
        description='登录后可以提交与管理你的预约单'
        actionText='去登录'
        onAction={() => void login()}
      />
    )
  }

  if (loading && !orders.length && !draft.items.length) {
    return <Loading fullscreen text='整理预约单…' />
  }

  return (
    <View className='order-page'>
      {draft.items.length > 0 && (
        <View className='order-page__draft ck-card'>
          <Text className='order-page__draft-title'>当前预约</Text>
          {draft.items.map((item) => (
            <View key={item.dishId} className='order-page__draft-item'>
              <Text className='order-page__draft-name'>{item.dishName}</Text>
              <Text className='order-page__draft-qty'>× {item.quantity}</Text>
            </View>
          ))}

          <View className='order-page__field'>
            <Text className='order-page__label'>期望日期（选填）</Text>
            <Picker mode='date' value={draft.scheduledAt || ''} onChange={handlePickTime}>
              <View className='order-page__picker'>
                {draft.scheduledAt || '选择日期'}
              </View>
            </Picker>
          </View>

          <View className='order-page__field'>
            <Text className='order-page__label'>用餐人数（选填）</Text>
            <Picker
              mode='selector'
              range={['1 位', '2 位', '3 位', '4 位', '5 位', '6 位', '8 位', '10 位']}
              onChange={handlePickGuest}
            >
              <View className='order-page__picker'>{draft.guestCount} 位</View>
            </Picker>
          </View>

          <View className='order-page__field'>
            <Text className='order-page__label'>备注</Text>
            <Input
              className='order-page__input'
              placeholder='口味偏好、忌口等（选填）'
              value={draft.note}
              onInput={(e) => updateDraft({ note: e.detail.value })}
            />
          </View>

          <View className='order-page__actions'>
            <Button className='ck-btn-secondary order-page__btn' onClick={clearDraft}>
              清空
            </Button>
            <Button className='ck-btn-primary order-page__btn' onClick={handleSubmit}>
              提交预约
            </Button>
          </View>
        </View>
      )}

      <View className='order-page__tabs'>
        {TABS.map((t) => (
          <View
            key={t.key}
            className={`order-page__tab ${tab === t.key ? 'order-page__tab--active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            <Text>{t.label}</Text>
          </View>
        ))}
      </View>

      <View className='order-page__history'>
        <Text className='order-page__section-title'>预约记录</Text>
        {orders.length === 0 ? (
          <EmptyState
            emoji='📋'
            title='还没有预约记录'
            description='挑一道喜欢的菜，告诉厨房你什么时候来'
            actionText='去看看菜品'
            onAction={() => Taro.switchTab({ url: '/pages/category/index' })}
          />
        ) : (
          orders.map((order) => <OrderCard key={order.id} order={order} />)
        )}
      </View>
    </View>
  )
}
