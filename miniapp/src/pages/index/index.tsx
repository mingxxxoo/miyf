import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import { fetchCategories } from '@/api/category'
import { fetchHotDishes, fetchRecommendDishes } from '@/api/dish'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import type { Category, Dish } from '@/types'
import './index.scss'

const CATEGORY_KEY = 'miyf_kitchen_category_id'

export default function IndexPage() {
  const { isLoggedIn, bootstrapping } = useAuthGuard()
  const [loading, setLoading] = useState(true)
  const [recommend, setRecommend] = useState<Dish[]>([])
  const [hotDishes, setHotDishes] = useState<Dish[]>([])
  const [categories, setCategories] = useState<Category[]>([])

  useDidShow(() => {
    if (!bootstrapping && isLoggedIn) void loadHome()
  })

  const loadHome = async () => {
    setLoading(true)
    try {
      const [cats, rec, hot] = await Promise.all([
        fetchCategories().catch(() => [] as Category[]),
        fetchRecommendDishes(6).catch(() => [] as Dish[]),
        fetchHotDishes(8).catch(() => [] as Dish[])
      ])
      setCategories(cats)
      setRecommend(rec)
      setHotDishes(hot)
    } finally {
      setLoading(false)
    }
  }

  const goCategory = (categoryId?: string) => {
    if (categoryId) {
      Taro.setStorageSync(CATEGORY_KEY, categoryId)
    } else {
      Taro.removeStorageSync(CATEGORY_KEY)
    }
    Taro.switchTab({ url: '/pages/category/index' })
  }

  if (bootstrapping || !isLoggedIn) {
    return <Loading fullscreen text='正在登录…' />
  }

  if (loading) {
    return <Loading fullscreen text='打开冰箱看看…' />
  }

  return (
    <View className='index-page'>
      <View className='index-page__hero'>
        <Text className='index-page__brand'>miyf 厨房</Text>
        <Text className='index-page__greeting'>今天吃点什么呢？</Text>
        <Text className='index-page__subtitle'>把心意端上餐桌</Text>
      </View>

      <View className='index-page__section'>
        <View className='index-page__section-head'>
          <Text className='index-page__section-title'>今日推荐</Text>
          <Text className='index-page__section-more' onClick={() => goCategory()}>全部菜品 →</Text>
        </View>
        {recommend.length === 0 ? (
          <EmptyState emoji='🥄' title='厨房还在备菜中' description='稍后再来看看' />
        ) : (
          <View className='index-page__list'>
            {recommend.slice(0, 3).map((dish) => (
              <View key={dish.id} className='index-page__item'>
                <DishCard dish={dish} />
              </View>
            ))}
          </View>
        )}
      </View>

      <View className='index-page__section'>
        <Text className='index-page__section-title'>分类逛逛</Text>
        <ScrollView scrollX className='index-page__cats' enhanced showScrollbar={false}>
          {categories.map((cat) => (
            <View
              key={cat.id}
              className='index-page__cat'
              onClick={() => goCategory(cat.id)}
            >
              <Text>{cat.name}</Text>
            </View>
          ))}
        </ScrollView>
      </View>

      <View className='index-page__section'>
        <Text className='index-page__section-title'>热门菜品</Text>
        {hotDishes.length === 0 ? (
          <EmptyState emoji='🔥' title='还没有热门榜' description='多预约几道，厨房就热闹了' />
        ) : (
          <View className='index-page__list'>
            {hotDishes.slice(0, 4).map((dish) => (
              <View key={dish.id} className='index-page__item'>
                <DishCard dish={dish} compact />
              </View>
            ))}
          </View>
        )}
      </View>
    </View>
  )
}
