import {Image, Input, ScrollView, Text, View} from '@tarojs/components'
import Taro, {useDidShow, usePullDownRefresh, useReachBottom} from '@tarojs/taro'
import {useRef, useState} from 'react'
import {fetchDishTemplates} from '@/api/dishTemplate'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import type {DishTemplate, DishTemplateStatus} from '@/types'
import {formatDuration} from '@/utils/format'
import './library.scss'

const PAGE_SIZE = 20

type TabKey = 'all' | 'pre_created' | 'published' | 'mine' | 'recent'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'pre_created', label: '预创建' },
  { key: 'published', label: '已发布' },
  { key: 'mine', label: '我创建' },
  { key: 'recent', label: '最近用' }
]

const STATUS_LABEL: Record<DishTemplateStatus, string> = {
  draft: '草稿',
  pre_created: '预创建',
  published: '已发布',
  archived: '已归档'
}

interface LibraryFilters {
  tab: TabKey
  made?: boolean
  keyword: string
}

export default function DishLibraryPage() {
  const [list, setList] = useState<DishTemplate[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [tab, setTab] = useState<TabKey>('all')
  const [made, setMade] = useState<boolean | undefined>(undefined)
  const [keywordInput, setKeywordInput] = useState('')
  const filtersRef = useRef<LibraryFilters>({tab: 'all', made: undefined, keyword: ''})
  const requestIdRef = useRef(0)

  const load = async (reset: boolean, filters: LibraryFilters = filtersRef.current) => {
    if (!reset && (loadingMore || loading)) return
    const requestId = ++requestIdRef.current
    const nextPage = reset ? 1 : page + 1
    if (reset) {
      setLoading(true)
    } else {
      setLoadingMore(true)
    }
    try {
      const status: DishTemplateStatus | undefined =
        filters.tab === 'pre_created' || filters.tab === 'published' ? filters.tab : undefined
      const result = await fetchDishTemplates({
        page: nextPage,
        rows: PAGE_SIZE,
        status,
        mine: filters.tab === 'mine',
        recent: filters.tab === 'recent',
        made: filters.made,
        keyword: filters.keyword || undefined
      })
      if (requestId !== requestIdRef.current) return
      const records = result.records || []
      setList((prev) => (reset ? records : [...prev, ...records]))
      setTotal(result.total)
      setPage(nextPage)
    } catch {
      if (reset && requestId === requestIdRef.current) setList([])
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false)
        setLoadingMore(false)
      }
    }
  }

  useDidShow(() => {
    void load(true, filtersRef.current)
  })

  usePullDownRefresh(async () => {
    try {
      await load(true, filtersRef.current)
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  useReachBottom(() => {
    if (list.length >= total || loadingMore || loading) return
    void load(false, filtersRef.current)
  })

  const openDetail = (item: DishTemplate) => {
    Taro.navigateTo({ url: `/pages/dish/template-detail?id=${item.id}` })
  }

  const openCreate = () => {
    Taro.navigateTo({ url: '/pages/dish/edit' })
  }

  const openAiCreate = () => {
    Taro.navigateTo({ url: '/pages/dish/ai' })
  }

  const applyFilters = (patch: Partial<LibraryFilters>) => {
    const next: LibraryFilters = {...filtersRef.current, ...patch}
    filtersRef.current = next
    setTab(next.tab)
    setMade(next.made)
    setList([])
    setTotal(0)
    void load(true, next)
  }

  const switchTab = (key: TabKey) => {
    if (tab === key) return
    applyFilters({tab: key})
  }

  const switchMade = (value: boolean | undefined) => {
    if (made === value) return
    applyFilters({made: value})
  }

  const submitSearch = () => {
    const next = keywordInput.trim()
    if (next === filtersRef.current.keyword) {
      void load(true, filtersRef.current)
      return
    }
    applyFilters({keyword: next})
  }

  return (
    <View className='tpl-page'>
      <View className='tpl-page__hero'>
        <View className='tpl-page__hero-copy'>
          <Text className='tpl-page__hero-title'>组织菜品库</Text>
          <Text className='tpl-page__hero-desc'>全员共建 · 做菜前先选菜</Text>
        </View>
        <View className='tpl-page__hero-count'>
          <Text className='tpl-page__hero-num'>{total}</Text>
          <Text className='tpl-page__hero-unit'>道菜</Text>
        </View>
      </View>

      <View className='tpl-page__ai' onClick={openAiCreate}>
        <View className='tpl-page__ai-icon'>
          <MiniIcon name='spark' size='md' tone='primary' />
        </View>
        <View className='tpl-page__ai-copy'>
          <Text className='tpl-page__ai-title'>AI 帮我建菜</Text>
          <Text className='tpl-page__ai-desc'>拍照、贴文字或链接，自动整理成草稿</Text>
        </View>
        <Text className='tpl-page__ai-arrow'>→</Text>
      </View>

      <View className='tpl-page__search'>
        <MiniIcon name='dish' size='sm' tone='muted' />
        <Input
          className='tpl-page__search-input'
          value={keywordInput}
          placeholder='搜菜名、食材或做法'
          confirmType='search'
          onInput={(e) => setKeywordInput(e.detail.value)}
          onConfirm={submitSearch}
        />
        <Text className='tpl-page__search-btn' onClick={submitSearch}>
          搜索
        </Text>
      </View>

      <ScrollView scrollX className='tpl-page__tabs' showScrollbar={false}>
        <View className='tpl-page__tabs-inner'>
          {TABS.map((item) => (
            <Text
              key={item.key}
              className={`tpl-page__tab ${tab === item.key ? 'tpl-page__tab--on' : ''}`}
              onClick={() => switchTab(item.key)}
            >
              {item.label}
            </Text>
          ))}
        </View>
      </ScrollView>

      <View className='tpl-page__made'>
        {[
          { value: undefined, label: '全部状态' },
          { value: false, label: '还没做过' },
          { value: true, label: '已经做过' }
        ].map((item) => (
          <Text
            key={item.label}
            className={`tpl-page__chip ${made === item.value ? 'tpl-page__chip--on' : ''}`}
            onClick={() => switchMade(item.value)}
          >
            {item.label}
          </Text>
        ))}
      </View>

      {loading && list.length === 0 ? (
        <Loading text='正在读取菜品库…' />
      ) : list.length === 0 ? (
        <EmptyState
          title='菜品库还是空的'
          description='把常做的菜建进来，下次直接选，不用再想'
          actionText='创建第一道菜'
          onAction={openCreate}
        />
      ) : (
        <View className='tpl-page__list'>
          {list.map((item) => (
            <View key={item.id} className='tpl-card' onClick={() => openDetail(item)}>
              <View className='tpl-card__cover'>
                {item.coverUrl ? (
                  <Image className='tpl-card__img' src={item.coverUrl} mode='aspectFill' lazyLoad />
                ) : (
                  <View className='tpl-card__empty'>
                    <MiniIcon name='dish' size='lg' tone='muted' />
                  </View>
                )}
                <Text className={`tpl-card__status tpl-card__status--${item.status}`}>
                  {STATUS_LABEL[item.status]}
                </Text>
                {item.made ? <Text className='tpl-card__made'>做过</Text> : null}
              </View>
              <View className='tpl-card__body'>
                <Text className='tpl-card__name'>{item.name}</Text>
                {item.description ? (
                  <Text className='tpl-card__desc'>{item.description}</Text>
                ) : null}
                <View className='tpl-card__meta'>
                  <Text className='tpl-card__meta-item'>
                    {item.steps.length > 0 ? `${item.steps.length} 步` : '未配置步骤'}
                  </Text>
                  <Text className='tpl-card__meta-item'>{formatDuration(item.estimatedTotalSeconds)}</Text>
                  {item.servings ? (
                    <Text className='tpl-card__meta-item'>{item.servings} 人份</Text>
                  ) : null}
                </View>
                <View className='tpl-card__foot'>
                  <View className='tpl-card__tags'>
                    {item.tags.slice(0, 3).map((tag) => (
                      <Text key={tag} className='tpl-card__tag'>
                        {tag}
                      </Text>
                    ))}
                  </View>
                  <Text className='tpl-card__usage'>
                    {item.usageCount > 0 ? `做过 ${item.usageCount} 次` : item.creatorName || '待首次开做'}
                  </Text>
                </View>
              </View>
            </View>
          ))}
          {loadingMore ? (
            <Text className='tpl-page__more'>加载中…</Text>
          ) : list.length >= total ? (
            <Text className='tpl-page__more'>已经到底啦</Text>
          ) : null}
        </View>
      )}

      <View className='tpl-fab' onClick={openCreate}>
        <Text className='tpl-fab__plus'>＋</Text>
        <Text className='tpl-fab__text'>建菜</Text>
      </View>
    </View>
  )
}
