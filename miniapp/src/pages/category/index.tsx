import { View, Text, ScrollView, Input } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh, useReachBottom } from '@tarojs/taro'
import { useEffect, useRef, useState } from 'react'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { fetchCategories } from '@/api/category'
import { fetchDishes } from '@/api/dish'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import type { Category, Dish } from '@/types'
import './index.scss'

const CATEGORY_KEY = 'miyf_kitchen_category_id'
const ALL = 'all'
const SEARCH_DEBOUNCE_MS = 400
const PAGE_SIZE = 20

export default function CategoryPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const [activeId, setActiveId] = useState(ALL)
  const [categories, setCategories] = useState<Category[]>([])
  const [dishes, setDishes] = useState<Dish[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [searchInput, setSearchInput] = useState('')
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
    if (append) {
      if (loadingMoreRef.current) return
      setLoadingMore(true)
    } else {
      setLoading(true)
      setLoadError(false)
    }
    try {
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
    if (loadingMoreRef.current || loading) return
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

  if (bootstrapping) {
    return <Loading fullscreen text='整理菜品柜…' />
  }

  if (loading && dishes.length === 0 && !loadError) {
    return <Loading fullscreen text='整理菜品柜…' />
  }

  return (
    <View className='category-page'>
      <View className='category-page__top'>
        <ServiceSwitcher compact className='category-page__switch' />
        <Input
          className='category-page__search'
          type='text'
          confirmType='search'
          placeholder='搜索菜品'
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
            <Text>{cat.name}</Text>
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
                  <View key={dish.id} className='category-page__item ck-pressable'>
                    <DishCard dish={dish} compact />
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
    </View>
  )
}
