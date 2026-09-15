import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useState } from 'react'
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
        <Text className='index-page__greeting'>{kitchenName || '我的厨房'}</Text>
        <Text className='index-page__subtitle'>专属菜单 · 无价格无支付</Text>
      </View>
      {dishes.length === 0 ? (
        <EmptyState emoji='🥘' title='厨房还没上菜' description='等厨师审核通过并上架后再来' />
      ) : (
        <View className='index-page__section'>
          <ScrollView scrollY>
            {dishes.map((d) => (
              <DishCard key={d.id} dish={d} />
            ))}
          </ScrollView>
        </View>
      )}
    </View>
  )
}
