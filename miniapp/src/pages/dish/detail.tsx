import { View, Text, Image, Button, ScrollView } from '@tarojs/components'
import Taro, { useRouter, useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import StarRating from '@/components/StarRating'
import RecipeStep from '@/components/RecipeStep'
import Loading from '@/components/Loading'
import EmptyState from '@/components/EmptyState'
import { fetchDishComments, fetchDishDetail, fetchDishRecipe } from '@/api/dish'
import { useOrderStore } from '@/stores/orderStore'
import { useUserStore } from '@/stores/userStore'
import type { Comment, Dish, Recipe } from '@/types'
import './detail.scss'

export default function DishDetailPage() {
  const router = useRouter()
  const dishId = router.params.id || ''
  const setDraftItem = useOrderStore((s) => s.setDraftItem)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const requireLogin = useUserStore((s) => s.requireLogin)

  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [dish, setDish] = useState<Dish | null>(null)
  const [recipe, setRecipe] = useState<Recipe | null>(null)
  const [comments, setComments] = useState<Comment[]>([])

  useDidShow(() => {
    if (dishId) void loadDetail(dishId)
  })

  const loadDetail = async (id: string) => {
    setLoading(true)
    try {
      const [d, r, c] = await Promise.all([
        fetchDishDetail(id),
        fetchDishRecipe(id),
        fetchDishComments(id, { page: 1, rows: 10, sortMode: 'latest' }).catch(() => ({ records: [] as Comment[] }))
      ])
      setDish(d)
      setRecipe(r)
      setComments(c.records || [])
    } catch {
      setDish(null)
    } finally {
      setLoading(false)
    }
  }

  const handleReserve = async () => {
    if (!dish) return
    if (!isLoggedIn) {
      const ok = await requireLogin()
      if (!ok) return
    }
    setDraftItem({
      dishId: dish.id,
      dishName: dish.name,
      coverUrl: dish.coverUrl,
      quantity
    })
    Taro.showToast({ title: '已加入预约单', icon: 'success' })
    setTimeout(() => {
      Taro.switchTab({ url: '/pages/order/index' })
    }, 600)
  }

  if (loading) {
    return <Loading fullscreen text='端上这道菜…' />
  }

  if (!dish) {
    return (
      <EmptyState
        emoji='🍲'
        title='找不到这道菜'
        description='可能已下架，换一道尝尝'
        actionText='回首页'
        onAction={() => Taro.switchTab({ url: '/pages/index/index' })}
      />
    )
  }

  const hasRating = dish.ratingCount != null && dish.ratingCount > 0 && dish.rating != null

  return (
    <View className='dish-detail'>
      <Image className='dish-detail__cover' src={dish.coverUrl} mode='aspectFill' />
      <View className='dish-detail__body'>
        <Text className='dish-detail__name'>{dish.name}</Text>
        {dish.subtitle && <Text className='dish-detail__desc'>{dish.subtitle}</Text>}
        <View className='dish-detail__meta'>
          {hasRating ? (
            <>
              <StarRating value={dish.rating!} readonly />
              <Text className='dish-detail__rating-count'>{dish.ratingCount} 人觉得不错</Text>
            </>
          ) : (
            <Text className='dish-detail__rating-count'>暂无评分</Text>
          )}
        </View>
        {dish.description && <Text className='dish-detail__desc'>{dish.description}</Text>}

        {(dish.stockType === 'LIMITED') && (
          <Text className='dish-detail__rating-count'>
            今日可约 {dish.stock ?? 0} {dish.unit || '份'}
          </Text>
        )}

        <View className='dish-detail__qty'>
          <Text className='dish-detail__qty-label'>预约份数</Text>
          <View className='dish-detail__qty-ctrl'>
            <View
              className='dish-detail__qty-btn'
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            >
              <Text>−</Text>
            </View>
            <Text className='dish-detail__qty-num'>{quantity}</Text>
            <View
              className='dish-detail__qty-btn'
              onClick={() => setQuantity((q) => Math.min(20, q + 1))}
            >
              <Text>＋</Text>
            </View>
          </View>
        </View>

        {recipe && (
          <View className='dish-detail__recipe'>
            <Text className='dish-detail__section-title'>菜谱</Text>
            {recipe.tips && (
              <View className='dish-detail__tip'>
                <Text>💡 {recipe.tips}</Text>
              </View>
            )}
            {(recipe.ingredients?.length || 0) > 0 && (
              <View className='dish-detail__tip'>
                <Text>
                  食材：{recipe.ingredients!.map((i) => `${i.name}${i.amount ? `(${i.amount})` : ''}`).join('、')}
                </Text>
              </View>
            )}
            {recipe.steps.map((step, index) => (
              <RecipeStep
                key={step.step}
                step={step}
                isLast={index === recipe.steps.length - 1}
              />
            ))}
          </View>
        )}

        <View className='dish-detail__recipe'>
          <Text className='dish-detail__section-title'>大家怎么说</Text>
          {comments.length === 0 ? (
            <Text className='dish-detail__rating-count'>还没有评价，完成预约后来分享吧</Text>
          ) : (
            <ScrollView scrollY style={{ maxHeight: '480px' }}>
              {comments.map((c) => (
                <View key={c.id} className='dish-detail__tip' style={{ marginBottom: '16px' }}>
                  <StarRating value={c.rating} size='sm' readonly />
                  <Text>{c.content || '（只打了分）'}</Text>
                  <Text className='dish-detail__rating-count'>
                    {c.userNickname || '厨房朋友'} · {String(c.createTime || '').slice(0, 10)}
                  </Text>
                </View>
              ))}
            </ScrollView>
          )}
        </View>
      </View>

      <View className='dish-detail__footer'>
        <Button className='ck-btn-primary dish-detail__btn' onClick={handleReserve}>
          预约这道菜
        </Button>
      </View>
    </View>
  )
}
