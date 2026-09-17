import { View, Text, ScrollView, Input } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh, useReachBottom } from '@tarojs/taro'
import { useEffect, useRef, useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher, { DraftOrderBar } from '@/components/ServiceSwitcher'
import { fetchCategories } from '@/api/category'
import { fetchDishes } from '@/api/dish'
import { fetchMyBinding } from '@/api/kitchen'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { useOrderStore } from '@/stores/orderStore'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import type { Category, Dish } from '@/types'
import './index.scss'

const CAT_EMOJI = ['🥩', '🥬', '🍲', '🍚', '🍰', '🥗', '🍜', '🍵']

function catLabel(name: string, index: number) {
  if (name === '全部') return '全部'
  if (/荤/.test(name)) return `🥩 ${name}`
  if (/素/.test(name)) return `🥬 ${name}`
  if (/汤/.test(name)) return `🍲 ${name}`
  if (/主食|饭/.test(name)) return `🍚 ${name}`
  if (/甜|点心/.test(name)) return `🍰 ${name}`
  return `${CAT_EMOJI[index % CAT_EMOJI.length]} ${name}`
}

const CATEGORY_KEY = 'miyf_kitchen_category_id'
const ALL = 'all'
const SEARCH_DEBOUNCE_MS = 400
const PAGE_SIZE = 20

export default function CategoryPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const user = useUserStore((s) => s.user)
  const [activeId, setActiveId] = useState(ALL)
  const [categories, setCategories] = useState<Category[]>([])
  const [dishes, setDishes] = useState<Dish[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [needJoin, setNeedJoin] = useState(false)
  const [pending, setPending] = useState(false)
  const [rejected, setRejected] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [kitchenName, setKitchenName] = useState('')
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const draft = useOrderStore((s) => s.draft)
  const setDraftItem = useOrderStore((s) => s.setDraftItem)
  const updateDraftItemQty = useOrderStore((s) => s.updateDraftItemQty)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const activeIdRef = useRef(activeId)
  const keywordRef = useRef(keyword)
  const pageRef = useRef(1)
  const totalRef = useRef(0)
  const loadingMoreRef = useRef(false)

  activeIdRef.current = activeId
  keywordRef.current = keyword
  pageRef.current = page
  totalRef.current = total
  loadingMoreRef.current = loadingMore

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (bootstrapping) return
    const preferred = Taro.getStorageSync(CATEGORY_KEY) as string
    if (preferred) {
      setActiveId(preferred)
      Taro.removeStorageSync(CATEGORY_KEY)
      void loadData(preferred, keywordRef.current, 1, false)
    } else {
      void loadData(activeIdRef.current, keywordRef.current, 1, false)
    }
  })

  usePullDownRefresh(async () => {
    try {
      if (!bootstrapping) {
        await loadData(activeIdRef.current, keywordRef.current, 1, false)
      }
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  useReachBottom(() => {
    void loadMore()
  })

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [])

  const loadData = async (
    categoryId: string,
    kw: string,
    pageNo: number,
    append: boolean
  ) => {
    if (!isLoggedIn) {
      setNeedJoin(false)
      setDishes([])
      setLoading(false)
      return
    }
    if (user?.activeRole === 'CHEF' || (user?.chef && user?.activeRole !== 'DINER')) {
      setNeedJoin(false)
      setDishes([])
      setLoading(false)
      Taro.redirectTo({ url: '/pages/chef/index' })
      return
    }
    if (append) {
      if (loadingMoreRef.current) return
      setLoadingMore(true)
    } else {
      setLoading(true)
      setLoadError(false)
    }
    try {
      const binding = await fetchMyBinding()
      if (!binding || binding.status !== 'BOUND') {
        setNeedJoin(true)
        setPending(binding?.status === 'PENDING')
        setRejected(binding?.status === 'REJECTED')
        setRejectReason(binding?.rejectReason || '')
        setKitchenName(binding?.kitchenName || '')
        setDishes([])
        setTotal(0)
        setCategories([])
        return
      }
      setNeedJoin(false)
      setPending(false)
      setRejected(false)
      setRejectReason('')
      setKitchenName(binding.kitchenName || '')
      if (!append) {
        const cats = await fetchCategories().catch(() => [] as Category[])
        setCategories(cats)
      }
      const result = await fetchDishes({
        categoryId: categoryId === ALL ? undefined : categoryId,
        keyword: kw.trim() || undefined,
        page: pageNo,
        rows: PAGE_SIZE
      })
      const records = result.records || []
      const nextTotal = result.total ?? records.length
      setTotal(nextTotal)
      setPage(pageNo)
      setDishes((prev) => (append ? [...prev, ...records] : records))
    } catch {
      if (!append) {
        setDishes([])
        setTotal(0)
        setLoadError(true)
      } else {
        Taro.showToast({ title: '加载更多失败', icon: 'none' })
      }
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }

  const loadMore = async () => {
    if (needJoin || loadingMoreRef.current || loading) return
    if (dishes.length >= totalRef.current) return
    await loadData(activeIdRef.current, keywordRef.current, pageRef.current + 1, true)
  }

  const handleSelect = (id: string) => {
    setActiveId(id)
    void loadData(id, keyword, 1, false)
  }

  const applyKeyword = (value: string) => {
    setKeyword(value)
    void loadData(activeId, value, 1, false)
  }

  const onSearchInput = (value: string) => {
    setSearchInput(value)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      applyKeyword(value)
    }, SEARCH_DEBOUNCE_MS)
  }

  const onSearchConfirm = () => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    applyKeyword(searchInput)
  }

  const tabs: Category[] = [{ id: ALL, name: '全部' }, ...categories]
  const hasMore = dishes.length < total

  const addToDraft = (dish: Dish) => {
    const soldOut = dish.stockType === 'LIMITED' && (dish.stock == null || dish.stock <= 0)
    if (soldOut) {
      Taro.showToast({ title: '今日已约满', icon: 'none' })
      return
    }
    const existing = draft.items.find((i) => i.dishId === dish.id)
    const nextQty = (existing?.quantity || 0) + 1
    if (dish.stockType === 'LIMITED' && dish.stock != null && nextQty > dish.stock) {
      Taro.showToast({ title: `最多预约 ${dish.stock} 份`, icon: 'none' })
      return
    }
    setDraftItem({
      dishId: dish.id,
      dishName: dish.name,
      coverUrl: dish.coverUrl,
      quantity: nextQty
    })
  }

  const decDraft = (dish: Dish) => {
    const existing = draft.items.find((i) => i.dishId === dish.id)
    if (!existing) return
    updateDraftItemQty(dish.id, existing.quantity - 1)
  }

  const draftQtyMap = (() => {
    const map: Record<string, number> = {}
    draft.items.forEach((it) => {
      map[it.dishId] = it.quantity
    })
    return map
  })()

  if (bootstrapping) {
    return <Loading fullscreen text='整理菜品柜…' />
  }

  if (loading && dishes.length === 0 && !loadError && !needJoin) {
    return <Loading fullscreen text='整理菜品柜…' />
  }

  if (!isLoggedIn) {
    return (
      <View className='category-page'>
        <EmptyState
          emoji='🔑'
          title='登录后看专属菜单'
          description='胡闹厨房没有公开菜，加入厨师厨房后才能浏览。'
          actionText='去登录'
          onAction={() => Taro.navigateTo({ url: '/pages/login/index' })}
        />
      </View>
    )
  }

  if (needJoin) {
    return (
      <View className='category-page'>
        <EmptyState
          emoji='🔑'
          title={
            pending
              ? `等待「${kitchenName || '厨房'}」确认`
              : rejected
                ? `「${kitchenName || '厨房'}」未通过申请`
                : '先加入厨房'
          }
          description={
            pending
              ? '申请已提交，厨师确认后才能按分类看菜。'
              : rejected
                ? rejectReason
                  ? `原因：${rejectReason}。可换码重新申请。`
                  : '申请被拒绝。可换邀请码重新申请。'
                : '没有公开菜单。用邀请码绑定一位厨师后，才能按分类看菜。'
          }
          actionText={pending ? '查看首页' : '去加入厨房'}
          onAction={() =>
            pending
              ? Taro.switchTab({ url: '/pages/index/index' })
              : Taro.navigateTo({ url: '/pages/join/index' })
          }
        />
      </View>
    )
  }

  return (
    <View className='category-page'>
      <View className='category-page__top'>
        <ServiceSwitcher compact className='category-page__switch' />
        <Input
          className='category-page__search'
          type='text'
          confirmType='search'
          placeholder='想吃什么？搜搜看'
          value={searchInput}
          onInput={(e) => onSearchInput(e.detail.value)}
          onConfirm={onSearchConfirm}
        />
      </View>

      <ScrollView scrollX className='category-page__tabs' enhanced showScrollbar={false}>
        {tabs.map((cat) => (
          <View
            key={cat.id}
            className={`category-page__tab ck-pressable ${
              activeId === cat.id ? 'category-page__tab--active' : ''
            }`}
            onClick={() => handleSelect(cat.id)}
          >
            <Text>{catLabel(cat.name, tabs.indexOf(cat))}</Text>
          </View>
        ))}
      </ScrollView>

      <View className='category-page__content'>
        {loading && dishes.length > 0 && (
          <Text className='category-page__count'>刷新中…</Text>
        )}
        {loadError ? (
          <EmptyState
            emoji='🥗'
            title='加载失败'
            description='菜品列表暂时拉不下来，请重试'
            actionText='重试'
            onAction={() => void loadData(activeId, keyword, 1, false)}
          />
        ) : (
          <>
            {!loading && dishes.length > 0 && (
              <Text className='category-page__count'>
                共 {total} 道{hasMore ? ` · 已显示 ${dishes.length}` : ''}
              </Text>
            )}
            {dishes.length === 0 ? (
              <EmptyState
                emoji='🥗'
                title='这一栏还没摆满'
                description={
                  keyword.trim()
                    ? '换个关键词试试，或者看看其他分类'
                    : '换个分类看看，或者告诉厨房你想吃什么'
                }
              />
            ) : (
              <View className='category-page__grid'>
                {dishes.map((dish) => (
                  <View key={dish.id} className='category-page__item'>
                    <DishCard
                      dish={dish}
                      compact
                      quantity={draftQtyMap[dish.id] || 0}
                      onAdd={addToDraft}
                      onDec={decDraft}
                    />
                  </View>
                ))}
              </View>
            )}
            {hasMore && (
              <View
                className='category-page__more ck-pressable'
                onClick={() => void loadMore()}
              >
                <Text>{loadingMore ? '加载中…' : '加载更多'}</Text>
              </View>
            )}
          </>
        )}
      </View>
      <DraftOrderBar />
    </View>
  )
}
