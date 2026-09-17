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

function isBindingRequiredError(err: unknown): boolean {
  const msg = err instanceof Error ? err.message : String(err || '')
  return /请先加入厨房|BINDING_REQUIRED|41013/i.test(msg)
}

function formatNutrition(nutrition?: Record<string, unknown>): string {
  if (!nutrition || typeof nutrition !== 'object') return ''
  return Object.entries(nutrition)
    .filter(([, v]) => v != null && String(v).trim() !== '')
    .map(([k, v]) => `${k} ${v}`)
    .join(' · ')
}

function avatarLetter(name?: string): string {
  const n = (name || '厨').trim()
  return n.slice(0, 1) || '厨'
}

function avatarTone(name?: string): string {
  const tones = ['#F07B1F', '#18A885', '#4A90D9', '#D99426', '#E15A4B']
  const s = name || ''
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h + s.charCodeAt(i) * (i + 1)) % tones.length
  return tones[h]
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
  const [needJoin, setNeedJoin] = useState(false)
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
    setNeedJoin(false)
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
      if (isBindingRequiredError(err)) {
        setNeedJoin(true)
      } else if (isNotFoundError(err)) {
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

  if (needJoin) {
    return (
      <EmptyState
        emoji='🔑'
        title='先加入厨房'
        description='没有公开菜品。绑定厨师后才能查看详情与预约。'
        actionText='去加入厨房'
        onAction={() => Taro.navigateTo({ url: '/pages/join/index' })}
      />
    )
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
  const cookMinutes = recipe?.cookMinutes
  const duration =
    cookMinutes != null
      ? cookMinutes
      : prepMinutes != null
        ? prepMinutes
        : null

  const stockChip =
    dish.stockType === 'LIMITED'
      ? soldOut
        ? { text: '今日已约满', ok: false }
        : { text: `今日剩 ${Math.max(0, dish.stock ?? 0)} ${dish.unit || '份'}`, ok: true }
      : dish.stockType === 'UNLIMITED'
        ? { text: '不限量', ok: true }
        : null

  const chips: { text: string; ok?: boolean }[] = []
  if (stockChip) chips.push(stockChip)
  if (dish.categoryName) chips.push({ text: dish.categoryName })
  if (recipe?.difficulty) chips.push({ text: `难度 ${recipe.difficulty}` })
  if (dish.tags?.length) {
    dish.tags.slice(0, 2).forEach((t) => chips.push({ text: t }))
  }
  chips.push({ text: '可备注口味' })

  const ingredientCapsules: string[] = []
  ;(recipe?.ingredients || []).forEach((i) => {
    ingredientCapsules.push(`${i.name}${i.amount ? ` ${i.amount}` : ''}`)
  })
  ;(recipe?.seasonings || []).forEach((i) => {
    ingredientCapsules.push(`${i.name}${i.amount ? ` ${i.amount}` : ''}`)
  })

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
        <View className='dish-detail__title-row'>
          <Text className='dish-detail__name'>{dish.name}</Text>
          {dish.recommend ? (
            <Text className='dish-detail__rec-pill'>厨师推荐</Text>
          ) : null}
        </View>

        {dish.subtitle && <Text className='dish-detail__desc'>{dish.subtitle}</Text>}

        <View className='dish-detail__meta'>
          {hasRating ? (
            <Text className='dish-detail__meta-item'>
              <Text className='dish-detail__meta-star'>★{dish.rating}</Text>
              （{dish.ratingCount} 条评价）
            </Text>
          ) : (
            <Text className='dish-detail__meta-item'>暂无评分</Text>
          )}
          {duration != null ? (
            <Text className='dish-detail__meta-item'>约 {duration} 分钟</Text>
          ) : null}
        </View>

        {chips.length > 0 && (
          <View className='dish-detail__chips'>
            {chips.map((c) => (
              <Text
                key={c.text}
                className={`dish-detail__chip${c.ok ? ' dish-detail__chip--ok' : ''}`}
              >
                {c.text}
              </Text>
            ))}
          </View>
        )}

        {dish.description && dish.description !== dish.subtitle ? (
          <Text className='dish-detail__desc'>{dish.description}</Text>
        ) : null}
      </View>

      {recipe && (
        <View className='dish-detail__card'>
          <Text className='dish-detail__section-title'>食材</Text>
          {ingredientCapsules.length > 0 ? (
            <View className='dish-detail__ing-grid'>
              {ingredientCapsules.map((label) => (
                <Text key={label} className='dish-detail__ing-chip'>
                  {label}
                </Text>
              ))}
            </View>
          ) : (
            <Text className='dish-detail__muted'>厨师暂未填写食材</Text>
          )}
          {nutritionText ? (
            <Text className='dish-detail__nutrition'>营养：{nutritionText}</Text>
          ) : null}
          {recipe.tips ? (
            <View className='dish-detail__tip'>
              <Text>💡 {recipe.tips}</Text>
            </View>
          ) : null}
        </View>
      )}

      {recipe && recipe.steps.length > 0 && (
        <View className='dish-detail__card'>
          <Text className='dish-detail__section-title'>做法步骤</Text>
          {recipe.steps.map((step, index) => (
            <RecipeStep
              key={step.step}
              step={step}
              isLast={index === recipe.steps.length - 1}
            />
          ))}
        </View>
      )}

      <View className='dish-detail__card'>
        <View className='dish-detail__section-head'>
          <Text className='dish-detail__section-title dish-detail__section-title--inline'>
            食客评价
          </Text>
          {comments.length > 0 ? (
            <Text className='dish-detail__section-more'>{comments.length} 条</Text>
          ) : null}
        </View>
        {comments.length === 0 ? (
          <Text className='dish-detail__muted'>还没有评价，完成预约后来分享吧</Text>
        ) : (
          comments.map((c) => {
            const imgs = (c.images || []).filter(Boolean)
            const nick = c.userNickname || '厨房朋友'
            return (
              <View key={c.id} className='dish-detail__cmt'>
                <View className='dish-detail__cmt-top'>
                  <View
                    className='dish-detail__cmt-avatar'
                    style={{ background: avatarTone(nick) }}
                  >
                    <Text>{avatarLetter(nick)}</Text>
                  </View>
                  <Text className='dish-detail__cmt-name'>{nick}</Text>
                  <View className='dish-detail__cmt-stars'>
                    <StarRating value={c.rating} size='sm' readonly />
                  </View>
                </View>
                <Text className='dish-detail__cmt-txt'>{c.content || '（只打了分）'}</Text>
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
                <Text className='dish-detail__cmt-time'>
                  {String(c.createTime || '').slice(0, 10)}
                </Text>
              </View>
            )
          })
        )}
      </View>

      <View className='dish-detail__footer'>
        <View
          className={`dish-detail__stepper${soldOut ? ' dish-detail__stepper--disabled' : ''}`}
        >
          <View className='dish-detail__stepper-op' onClick={() => bumpQty(-1)}>
            <Text>−</Text>
          </View>
          <Text className='dish-detail__stepper-num'>{soldOut ? 0 : quantity}</Text>
          <View className='dish-detail__stepper-op' onClick={() => bumpQty(1)}>
            <Text>＋</Text>
          </View>
        </View>
        <Button
          className={`dish-detail__cta${soldOut ? ' dish-detail__cta--disabled' : ''}`}
          disabled={soldOut}
          onClick={() => void handleReserve()}
        >
          {soldOut ? '今日已约满' : '加入预约单'}
        </Button>
      </View>
    </View>
  )
}
