import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { fetchCategories } from '@/api/category'
import { fetchHotDishes, fetchRecommendDishes } from '@/api/dish'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import type { Category, Dish } from '@/types'
import './index.scss'

const CATEGORY_KEY = 'miyf_kitchen_category_id'

function greetingByHour(): string {
  const h = new Date().getHours()
  if (h < 11) return '早上好，今天想吃点什么？'
  if (h < 14) return '中午好，来点热乎的？'
  if (h < 18) return '下午好，给晚餐找点灵感'
  return '晚上好，夜晚也值得好好吃一顿'
}

export default function IndexPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [partialFail, setPartialFail] = useState(false)
  const [recommend, setRecommend] = useState<Dish[]>([])
  const [hotDishes, setHotDishes] = useState<Dish[]>([])
  const [categories, setCategories] = useState<Category[]>([])

  const greeting = useMemo(() => greetingByHour(), [])

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (!bootstrapping) void loadHome()
  })

  usePullDownRefresh(async () => {
    try {
      if (!bootstrapping) await loadHome()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const loadHome = async () => {
    setLoading(true)
    setLoadError(false)
    setPartialFail(false)
    let failCount = 0
    const [cats, rec, hot] = await Promise.all([
      fetchCategories().catch(() => {
        failCount += 1
        return null as Category[] | null
      }),
      fetchRecommendDishes(6).catch(() => {
        failCount += 1
        return null as Dish[] | null
      }),
      fetchHotDishes(8).catch(() => {
        failCount += 1
        return null as Dish[] | null
      })
    ])
    if (failCount === 3) {
      setLoadError(true)
      setCategories([])
      setRecommend([])
      setHotDishes([])
    } else {
      setCategories(cats || [])
      setRecommend(rec || [])
      setHotDishes(hot || [])
      if (failCount > 0) {
        setPartialFail(true)
        Taro.showToast({ title: '部分内容加载失败', icon: 'none' })
      }
    }
    setLoading(false)
  }

  const goCategory = (categoryId?: string) => {
    if (categoryId) {
      Taro.setStorageSync(CATEGORY_KEY, categoryId)
    } else {
      Taro.removeStorageSync(CATEGORY_KEY)
    }
    Taro.switchTab({ url: '/pages/category/index' })
  }

  if (bootstrapping) {
    return <Loading fullscreen text='打开冰箱看看…' />
  }

  if (loading && !categories.length && !recommend.length && !hotDishes.length) {
    return <Loading fullscreen text='打开冰箱看看…' />
  }

  if (loadError) {
    return (
      <View className='index-page'>
        <EmptyState
          emoji='🥄'
          title='加载失败'
          description='首页内容暂时拉不下来，请检查网络后重试'
          actionText='重试'
          onAction={() => void loadHome()}
        />
      </View>
    )
  }

  return (
    <View className='index-page'>
      <View className='index-page__hero'>
        <ServiceSwitcher compact className='index-page__switch' />
        <Text className='index-page__brand'>miyf 厨房</Text>
        <Text className='index-page__greeting'>{greeting}</Text>
        <Text className='index-page__subtitle'>把心意端上餐桌</Text>
        {partialFail && (
          <Text className='index-page__subtitle' onClick={() => void loadHome()}>
            部分内容未加载，点此重试
          </Text>
        )}
      </View>

      {categories.length > 0 && (
        <View className='index-page__section'>
          <View className='index-page__section-head'>
            <Text className='index-page__section-title'>分类逛逛</Text>
            <Text className='index-page__section-more' onClick={() => goCategory()}>
              全部 →
            </Text>
          </View>
          <ScrollView scrollX className='index-page__cats' enhanced showScrollbar={false}>
            {categories.map((c) => (
              <View
                key={c.id}
                className='index-page__cat ck-pressable'
                onClick={() => goCategory(c.id)}
              >
                <Text>{c.name}</Text>
              </View>
            ))}
          </ScrollView>
        </View>
      )}

      {recommend.length > 0 && (
        <View className='index-page__section'>
          <Text className='index-page__section-title'>今日推荐</Text>
          <View className='index-page__list'>
            {recommend.map((dish) => (
              <DishCard key={dish.id} dish={dish} />
            ))}
          </View>
        </View>
      )}

      {hotDishes.length > 0 && (
        <View className='index-page__section'>
          <Text className='index-page__section-title'>大家爱点</Text>
          <View className='index-page__list'>
            {hotDishes.map((dish) => (
              <DishCard key={dish.id} dish={dish} />
            ))}
          </View>
        </View>
      )}

      {!recommend.length && !hotDishes.length && !categories.length && (
        <EmptyState
          emoji='🥗'
          title='厨房还在备菜'
          description='稍后再来看看，或下拉刷新'
          actionText='刷新'
          onAction={() => void loadHome()}
        />
      )}
    </View>
  )
}
