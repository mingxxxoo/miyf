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

function isNotFoundError(err: unknown): boolean {
  const msg = err instanceof Error ? err.message : String(err || '')
  return /不存在|找不到|404|NOT_FOUND/i.test(msg)
}

function formatNutrition(nutrition?: Record<string, unknown>): string {
  if (!nutrition || typeof nutrition !== 'object') return ''
  return Object.entries(nutrition)
    .filter(([, v]) => v != null && String(v).trim() !== '')
    .map(([k, v]) => `${k} ${v}`)
    .join(' · ')
}

export default function DishDetailPage() {
  const router = useRouter()
  const dishId = router.params.id || ''
  const setDraftItem = useOrderStore((s) => s.setDraftItem)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const requireLogin = useUserStore((s) => s.requireLogin)

  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [notFound, setNotFound] = useState(false)
  const [dish, setDish] = useState<Dish | null>(null)
  const [recipe, setRecipe] = useState<Recipe | null>(null)
  const [comments, setComments] = useState<Comment[]>([])

  useDidShow(() => {
    if (dishId) void loadDetail(dishId)
  })

  const soldOut =
    dish?.stockType === 'LIMITED' && (dish.stock == null || dish.stock <= 0)

  const maxQty = soldOut
    ? 0
    : dish?.stockType === 'LIMITED'
      ? Math.max(0, dish.stock ?? 0)
      : 20

  const loadDetail = async (id: string) => {
    setLoading(true)
    setLoadError(false)
    setNotFound(false)
    try {
      const [d, r, c] = await Promise.all([
        fetchDishDetail(id),
        fetchDishRecipe(id),
        fetchDishComments(id, { page: 1, rows: 10, sortMode: 'latest' }).catch(
          () => ({ records: [] as Comment[] })
        )
      ])
      const prepMinutes = r?.prepareMinutes
      setDish(
        prepMinutes != null
          ? { ...d, prepMinutes, difficulty: d.difficulty || r?.difficulty }
          : { ...d, difficulty: d.difficulty || r?.difficulty }
      )
      setRecipe(r)
      setComments(c.records || [])
      const out =
        d.stockType === 'LIMITED' && (d.stock == null || d.stock <= 0)
      const cap = out
        ? 0
        : d.stockType === 'LIMITED'
          ? Math.max(0, d.stock ?? 0)
          : 20
      setQuantity((q) => (cap <= 0 ? 0 : Math.min(cap, Math.max(1, q))))
    } catch (err) {
      setDish(null)
      setRecipe(null)
      setComments([])
      if (isNotFoundError(err)) {
        setNotFound(true)
      } else {
        setLoadError(true)
      }
    } finally {
      setLoading(false)
    }
  }

  const bumpQty = (delta: number) => {
    if (soldOut || maxQty <= 0) return
    setQuantity((q) => {
      const next = q + delta
      if (next > maxQty) {
        Taro.showToast({ title: `最多预约 ${maxQty} 份`, icon: 'none' })
        return maxQty
      }
      return Math.max(1, Math.min(maxQty, next))
    })
  }

  const previewImages = (urls: string[], current: string) => {
    const list = urls.filter(Boolean)
    if (!list.length) return
    void Taro.previewImage({ urls: list, current })
  }

  const handleReserve = async () => {
    if (!dish) return
    if (soldOut || maxQty <= 0 || quantity < 1) {
      Taro.showToast({ title: '今日已约满', icon: 'none' })
      return
    }
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

  if (loadError) {
    return (
      <EmptyState
        emoji='📡'
        title='加载失败'
        description='菜品详情暂时拉不下来，请检查网络后重试'
        actionText='重试'
        onAction={() => dishId && void loadDetail(dishId)}
      />
    )
  }

  if (notFound || !dish) {
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
  const gallery = (dish.images || []).filter(Boolean)
  const showGallery = gallery.length > 1
  const nutritionText = formatNutrition(recipe?.nutrition)
  const prepMinutes = dish.prepMinutes ?? recipe?.prepareMinutes

  return (
    <View className='dish-detail'>
      <Image
        className='dish-detail__cover'
        src={dish.coverUrl}
        mode='aspectFill'
        onClick={() => {
          const urls = gallery.length ? gallery : dish.coverUrl ? [dish.coverUrl] : []
          if (urls.length) previewImages(urls, dish.coverUrl || urls[0])
        }}
      />
      {showGallery && (
        <ScrollView scrollX className='dish-detail__gallery' enhanced showScrollbar={false}>
          {gallery.map((src) => (
            <Image
              key={src}
              className='dish-detail__gallery-item'
              src={src}
              mode='aspectFill'
              onClick={() => previewImages(gallery, src)}
            />
          ))}
        </ScrollView>
      )}
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

        {(recipe?.difficulty || prepMinutes != null || recipe?.cookMinutes != null) && (
          <View className='dish-detail__tags'>
            {recipe?.difficulty && (
              <Text className='dish-detail__tag'>难度 {recipe.difficulty}</Text>
            )}
            {prepMinutes != null && (
              <Text className='dish-detail__tag'>准备 {prepMinutes} 分钟</Text>
            )}
            {recipe?.cookMinutes != null && (
              <Text className='dish-detail__tag'>烹饪 {recipe.cookMinutes} 分钟</Text>
            )}
          </View>
        )}

        {dish.stockType === 'LIMITED' && (
          <Text className='dish-detail__rating-count'>
            {soldOut
              ? '今日已约满'
              : `今日可约 ${Math.max(0, dish.stock ?? 0)} ${dish.unit || '份'}`}
          </Text>
        )}

        <View className={`dish-detail__qty ${soldOut ? 'dish-detail__qty--disabled' : ''}`}>
          <Text className='dish-detail__qty-label'>预约份数</Text>
          <View className='dish-detail__qty-ctrl'>
            <View
              className='dish-detail__qty-btn'
              onClick={() => bumpQty(-1)}
            >
              <Text>−</Text>
            </View>
            <Text className='dish-detail__qty-num'>{soldOut ? 0 : quantity}</Text>
            <View
              className='dish-detail__qty-btn'
              onClick={() => bumpQty(1)}
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
                  食材：
                  {recipe.ingredients!.map((i) => `${i.name}${i.amount ? `(${i.amount})` : ''}`).join('、')}
                </Text>
              </View>
            )}
            {(recipe.seasonings?.length || 0) > 0 && (
              <View className='dish-detail__tip'>
                <Text>
                  调料：
                  {recipe.seasonings!.map((i) => `${i.name}${i.amount ? `(${i.amount})` : ''}`).join('、')}
                </Text>
              </View>
            )}
            {nutritionText && (
              <View className='dish-detail__tip'>
                <Text>营养：{nutritionText}</Text>
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
              {comments.map((c) => {
                const imgs = (c.images || []).filter(Boolean)
                return (
                  <View key={c.id} className='dish-detail__tip' style={{ marginBottom: '16px' }}>
                    <StarRating value={c.rating} size='sm' readonly />
                    <Text>{c.content || '（只打了分）'}</Text>
                    {imgs.length > 0 && (
                      <View className='dish-detail__comment-imgs'>
                        {imgs.map((src) => (
                          <Image
                            key={src}
                            className='dish-detail__comment-img'
                            src={src}
                            mode='aspectFill'
                            onClick={() => previewImages(imgs, src)}
                          />
                        ))}
                      </View>
                    )}
                    <Text className='dish-detail__rating-count'>
                      {c.userNickname || '厨房朋友'} · {String(c.createTime || '').slice(0, 10)}
                    </Text>
                  </View>
                )
              })}
            </ScrollView>
          )}
        </View>
      </View>

      <View className='dish-detail__footer'>
        <Button
          className={`ck-btn-primary dish-detail__btn ${soldOut ? 'dish-detail__btn--disabled' : ''}`}
          disabled={soldOut}
          onClick={() => void handleReserve()}
        >
          {soldOut ? '今日已约满' : '预约这道菜'}
        </Button>
      </View>
    </View>
  )
}
