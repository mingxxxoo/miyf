import { View, Text, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import { fetchCategories } from '@/api/category'
import { fetchDishes } from '@/api/dish'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import type { Category, Dish } from '@/types'
import './index.scss'

const CATEGORY_KEY = 'miyf_kitchen_category_id'
const ALL = 'all'

export default function CategoryPage() {
  const { isLoggedIn, bootstrapping } = useAuthGuard()
  const [activeId, setActiveId] = useState(ALL)
  const [categories, setCategories] = useState<Category[]>([])
  const [dishes, setDishes] = useState<Dish[]>([])
  const [loading, setLoading] = useState(true)

  useDidShow(() => {
    if (bootstrapping || !isLoggedIn) return
    const preferred = Taro.getStorageSync(CATEGORY_KEY) as string
    if (preferred) {
      setActiveId(preferred)
      Taro.removeStorageSync(CATEGORY_KEY)
    }
    void loadData(preferred || activeId)
  })

  const loadData = async (categoryId: string) => {
    setLoading(true)
    try {
      const cats = await fetchCategories().catch(() => [] as Category[])
      setCategories(cats)
      const page = await fetchDishes({
        categoryId: categoryId === ALL ? undefined : categoryId,
        page: 1,
        rows: 50
      })
      setDishes(page.records || [])
    } catch {
      setDishes([])
    } finally {
      setLoading(false)
    }
  }

  const handleSelect = (id: string) => {
    setActiveId(id)
    void loadData(id)
  }

  const tabs: Category[] = [{ id: ALL, name: '全部' }, ...categories]

  if (bootstrapping || !isLoggedIn) {
    return <Loading fullscreen text='正在登录…' />
  }

  if (loading && dishes.length === 0) {
    return <Loading fullscreen text='整理菜品柜…' />
  }

  return (
    <View className='category-page'>
      <ScrollView scrollX className='category-page__tabs' enhanced showScrollbar={false}>
        {tabs.map((cat) => (
          <View
            key={cat.id}
            className={`category-page__tab ck-pressable ${
              activeId === cat.id ? 'category-page__tab--active' : ''
            }`}
            onClick={() => handleSelect(cat.id)}
          >
            <Text>{cat.name}</Text>
          </View>
        ))}
      </ScrollView>

      <View className='category-page__content'>
        {!loading && dishes.length > 0 && (
          <Text className='category-page__count'>共 {dishes.length} 道</Text>
        )}
        {dishes.length === 0 ? (
          <EmptyState
            emoji='🥗'
            title='这一栏还没摆满'
            description='换个分类看看，或者告诉厨房你想吃什么'
          />
        ) : (
          <View className='category-page__grid'>
            {dishes.map((dish) => (
              <View key={dish.id} className='category-page__item ck-pressable'>
                <DishCard dish={dish} compact />
              </View>
            ))}
          </View>
        )}
      </View>
    </View>
  )
}
