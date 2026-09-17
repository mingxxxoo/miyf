import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { fetchDishes } from '@/api/dish'
import { fetchMyBinding } from '@/api/kitchen'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import type { Dish } from '@/types'
import './index.scss'

export default function IndexPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const user = useUserStore((s) => s.user)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const refreshProfile = useUserStore((s) => s.refreshProfile)
  const [loading, setLoading] = useState(true)
  const [dishes, setDishes] = useState<Dish[]>([])
  const [kitchenName, setKitchenName] = useState('')
  const [needJoin, setNeedJoin] = useState(false)
  const [pending, setPending] = useState(false)
  const [rejected, setRejected] = useState(false)
  const [rejectReason, setRejectReason] = useState('')

  const recommended = useMemo(
    () => dishes.filter((d) => d.recommend).slice(0, 6),
    [dishes]
  )

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (!bootstrapping) void bootstrapHome()
  })

  usePullDownRefresh(async () => {
    try {
      if (!bootstrapping) await bootstrapHome()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const bootstrapHome = async () => {
    if (!isLoggedIn) {
      setLoading(false)
      return
    }
    setLoading(true)
    const profile = (await refreshProfile()) || user
    if (!profile?.activeRole && !profile?.chef && !profile?.diner) {
      Taro.navigateTo({ url: '/pages/role/select' })
      setLoading(false)
      return
    }
    if (profile?.activeRole === 'CHEF' || (profile?.chef && profile?.activeRole !== 'DINER')) {
      Taro.redirectTo({ url: '/pages/chef/index' })
      return
    }
    try {
      const binding = await fetchMyBinding()
      if (!binding || binding.status !== 'BOUND') {
        setNeedJoin(true)
        setPending(binding?.status === 'PENDING')
        setRejected(binding?.status === 'REJECTED')
        setRejectReason(binding?.rejectReason || '')
        setDishes([])
        setKitchenName(binding?.kitchenName || '')
      } else {
        setNeedJoin(false)
        setPending(false)
        setRejected(false)
        setRejectReason('')
        setKitchenName(binding.kitchenName || '')
        try {
          const page = await fetchDishes({ page: 1, rows: 20 })
          setDishes(page.records || [])
        } catch (err) {
          const msg = err instanceof Error ? err.message : String(err || '')
          if (/停业|封禁|暂不可用/i.test(msg)) {
            setNeedJoin(true)
            setDishes([])
            setKitchenName(binding.kitchenName || '')
            setRejectReason(msg)
          } else {
            throw err
          }
        }
      }
    } catch {
      setNeedJoin(true)
      setDishes([])
    }
    setLoading(false)
  }

  if (bootstrapping || loading) {
    return <Loading fullscreen text='打开冰箱看看…' />
  }

  if (!isLoggedIn) {
    return (
      <View className='index-page'>
        <EmptyState
          emoji='🍳'
          title='胡闹厨房'
          description='登录后选择厨师或食客身份。没有公开菜，也没有价格和支付。'
          actionText='去登录'
          onAction={() => Taro.navigateTo({ url: '/pages/login/index' })}
        />
      </View>
    )
  }

  if (needJoin) {
    const title = pending
      ? `等待「${kitchenName}」确认`
      : rejected
        ? `「${kitchenName || '厨房'}」未通过申请`
        : rejectReason
          ? kitchenName || '厨房暂不可用'
          : '没有公开菜单'
    const desc = pending
      ? '一位食客同时只能绑定一位厨师。用邀请码、链接加入后才能看菜和下单。'
      : rejected
        ? rejectReason
          ? `原因：${rejectReason}。可换码重新申请，或联系厨师。`
          : '申请被拒绝。可换邀请码重新申请，或联系厨师。'
        : rejectReason
          ? rejectReason
          : '一位食客同时只能绑定一位厨师。用邀请码、链接加入后才能看菜和下单。'
    return (
      <View className='index-page'>
        <View className='index-page__hero'>
          <ServiceSwitcher compact className='index-page__switch' />
          <Text className='index-page__brand'>胡闹厨房</Text>
          <Text className='index-page__greeting'>
            {pending
              ? '申请已提交，等厨师确认'
              : rejected
                ? '申请未通过'
                : rejectReason
                  ? '厨房暂不可用'
                  : '先加入一位厨师的厨房'}
          </Text>
        </View>
        <EmptyState
          emoji='🔑'
          title={title}
          description={desc}
          actionText='去加入厨房'
          onAction={() => Taro.navigateTo({ url: '/pages/join/index' })}
        />
      </View>
    )
  }

  return (
    <View className='index-page'>
      <View className='index-page__hero'>
        <ServiceSwitcher compact className='index-page__switch' />
        <Text className='index-page__brand'>胡闹厨房</Text>
        <Text className='index-page__subtitle'>专属菜单 · 无价格无支付</Text>
        {kitchenName ? (
          <Text className='index-page__kitchen-chip'>🏠 {kitchenName}</Text>
        ) : null}
      </View>

      {recommended.length > 0 ? (
        <View className='index-page__section'>
          <Text className='index-page__section-title'>今日推荐</Text>
          <ScrollView scrollX className='index-page__rec-scroll'>
            {recommended.map((d, i) => (
              <View
                key={d.id}
                className={`index-page__rec-card ck-pressable ${i % 2 === 1 ? 'index-page__rec-card--alt' : ''}`}
                onClick={() => Taro.navigateTo({ url: `/pages/dish/detail?id=${d.id}` })}
              >
                <Text className='index-page__rec-emoji'>🍽️</Text>
                <Text className='index-page__rec-name'>{d.name}</Text>
                <Text className='index-page__rec-meta'>
                  {d.rating != null ? `评分 ${d.rating}` : '厨师力荐'}
                </Text>
              </View>
            ))}
          </ScrollView>
        </View>
      ) : null}

      <View className='index-page__section'>
        <Text className='index-page__section-title'>全部菜品</Text>
        {dishes.length === 0 ? (
          <EmptyState emoji='🥘' title='厨房还没上菜' description='等厨师审核通过并上架后再来' />
        ) : (
          dishes.map((d) => <DishCard key={d.id} dish={d} />)
        )}
      </View>
    </View>
  )
}
