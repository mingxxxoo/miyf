import { View, Text, Button, Input, Picker, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useState } from 'react'
import OrderCard from '@/components/OrderCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useOrderStore } from '@/stores/orderStore'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
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

function todayStr(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export default function OrderIndexPage() {
  const {
    orders,
    draft,
    loading,
    loadError,
    fetchOrders,
    updateDraft,
    updateDraftItemQty,
    removeDraftItem,
    submitOrder,
    clearDraft
  } = useOrderStore()
  const { isLoggedIn, requireLogin } = useUserStore()
  const setProduct = useProductStore((s) => s.setProduct)
  const [tab, setTab] = useState<'ALL' | OrderStatus>('ALL')
  const minDate = todayStr()

  const reload = () => fetchOrders(tab === 'ALL' ? undefined : tab)

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    void reload()
  })

  useEffect(() => {
    if (isLoggedIn) void reload()
  }, [tab, isLoggedIn])

  usePullDownRefresh(async () => {
    try {
      if (isLoggedIn) await reload()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const ensureLogin = async () => {
    if (isLoggedIn) return true
    return requireLogin()
  }

  const handleSubmit = async () => {
    if (!draft.items.length) {
      Taro.showToast({ title: '先选一道想吃的菜吧', icon: 'none' })
      return
    }
    if (draft.scheduledTime && draft.scheduledTime < minDate) {
      Taro.showToast({ title: '期望日期不能早于今天', icon: 'none' })
      return
    }
    const ok = await ensureLogin()
    if (!ok) return
    const order = await submitOrder()
    if (order) {
      Taro.showToast({ title: order.displayTip || '厨房收到啦', icon: 'success' })
    } else {
      Taro.showToast({ title: '提交失败', icon: 'none' })
    }
  }

  const handlePickDate = (e: { detail: { value: string } }) => {
    updateDraft({ scheduledTime: String(e.detail.value) })
  }

  const handlePickTimeOfDay = (e: { detail: { value: string } }) => {
    updateDraft({ scheduledTimeOfDay: String(e.detail.value) })
  }

  const handlePickGuest = (e: { detail: { value: string | number } }) => {
    const guests = [1, 2, 3, 4, 5, 6, 8, 10]
    updateDraft({ guestCount: guests[Number(e.detail.value)] || 2 })
  }

  const goCategory = () => {
    Taro.switchTab({ url: '/pages/category/index' })
  }

  if (!isLoggedIn) {
    return <Loading fullscreen text='正在前往登录…' />
  }

  if (loading && !orders.length && !draft.items.length && !loadError) {
    return <Loading fullscreen text='整理预约单…' />
  }

  return (
    <View className='order-page'>
      <ServiceSwitcher compact className='order-page__switch' />

      {draft.items.length === 0 && (
        <View className='order-page__cta ck-card'>
          <Text className='order-page__cta-title'>还没有预约菜品</Text>
          <Text className='order-page__cta-desc'>去选菜加入预约</Text>
          <Button className='ck-btn-primary order-page__cta-btn' onClick={goCategory}>
            去选菜加入预约
          </Button>
        </View>
      )}

      {draft.items.length > 0 && (
        <View className='order-page__draft ck-card'>
          <Text className='order-page__draft-title'>当前预约</Text>
          {draft.items.map((item) => (
            <View key={item.dishId} className='order-page__draft-item'>
              <Text className='order-page__draft-name'>{item.dishName}</Text>
              <View className='order-page__draft-ops'>
                <View className='order-page__qty'>
                  <View
                    className='order-page__qty-btn'
                    onClick={() => updateDraftItemQty(item.dishId, item.quantity - 1)}
                  >
                    <Text>−</Text>
                  </View>
                  <Text className='order-page__qty-num'>{item.quantity}</Text>
                  <View
                    className='order-page__qty-btn'
                    onClick={() => updateDraftItemQty(item.dishId, item.quantity + 1)}
                  >
                    <Text>＋</Text>
                  </View>
                </View>
                <Text
                  className='order-page__draft-del'
                  onClick={() => removeDraftItem(item.dishId)}
                >
                  删除
                </Text>
              </View>
            </View>
          ))}

          <View className='order-page__field'>
            <Text className='order-page__label'>期望用餐日期（选填，写入给厨房的备注）</Text>
            <Picker
              mode='date'
              start={minDate}
              value={draft.scheduledTime || minDate}
              onChange={handlePickDate}
            >
              <View className='order-page__picker'>
                {draft.scheduledTime || '选择日期'}
              </View>
            </Picker>
          </View>

          <View className='order-page__field'>
            <Text className='order-page__label'>期望用餐时间（选填）</Text>
            <Picker
              mode='time'
              value={draft.scheduledTimeOfDay || '12:00'}
              onChange={handlePickTimeOfDay}
            >
              <View className='order-page__picker'>
                {draft.scheduledTimeOfDay || '选择时间'}
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
            <Text className='order-page__label'>给厨房的备注</Text>
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

      <ScrollView scrollX className='order-page__tabs' enhanced showScrollbar={false}>
        {TABS.map((t) => (
          <View
            key={t.key}
            className={`order-page__tab ck-pressable ${tab === t.key ? 'order-page__tab--active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            <Text>{t.label}</Text>
          </View>
        ))}
      </ScrollView>

      <View className='order-page__history'>
        <Text className='order-page__section-title'>预约记录</Text>
        {loadError ? (
          <EmptyState
            emoji='📋'
            title='加载失败'
            description='预约记录暂时拉不下来，请重试'
            actionText='重试'
            onAction={() => void reload()}
          />
        ) : orders.length === 0 ? (
          <EmptyState
            emoji='📋'
            title='还没有预约记录'
            description='挑一道喜欢的菜，告诉厨房你什么时候来'
            actionText='去看看菜品'
            onAction={goCategory}
          />
        ) : (
          orders.map((order) => <OrderCard key={order.id} order={order} />)
        )}
      </View>
    </View>
  )
}
