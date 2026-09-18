import { View, Text, ScrollView, Image } from '@tarojs/components'
import Taro, { useDidHide, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useMemo, useRef, useState } from 'react'
import { analyzeOrderInsight, draftOrderFromText } from '@/api/ai'
import { fetchDishes } from '@/api/dish'
import { fetchMyBinding } from '@/api/kitchen'
import AiPromptSheet from '@/components/AiPromptSheet'
import DishCard from '@/components/DishCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import ServiceSwitcher, { DraftOrderBar } from '@/components/ServiceSwitcher'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { useOrderStore } from '@/stores/orderStore'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import type { Dish } from '@/types'
import './index.scss'

const DISH_EMOJIS = ['🍖', '🥬', '🍅', '🐟', '🥗', '🍲', '🍛', '🥘']

export default function IndexPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const user = useUserStore((s) => s.user)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const refreshProfile = useUserStore((s) => s.refreshProfile)
  const draft = useOrderStore((s) => s.draft)
  const setDraftItem = useOrderStore((s) => s.setDraftItem)
  const updateDraftItemQty = useOrderStore((s) => s.updateDraftItemQty)
  const applyAiDraft = useOrderStore((s) => s.applyAiDraft)
  const setAiInsight = useOrderStore((s) => s.setAiInsight)
  const [loading, setLoading] = useState(true)
  const [dishes, setDishes] = useState<Dish[]>([])
  const [kitchenName, setKitchenName] = useState('')
  const [needJoin, setNeedJoin] = useState(false)
  const [pending, setPending] = useState(false)
  const [rejected, setRejected] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [aiOpen, setAiOpen] = useState(false)
  const [aiText, setAiText] = useState('')
  const [aiSubmitting, setAiSubmitting] = useState(false)
  const hasLoadedRef = useRef(false)
  const scrollTopRef = useRef(0)

  const recommended = useMemo(
    () => dishes.filter((d) => d.recommend).slice(0, 6),
    [dishes]
  )

  const draftQtyMap = useMemo(() => {
    const map: Record<string, number> = {}
    draft.items.forEach((it) => {
      map[it.dishId] = it.quantity
    })
    return map
  }, [draft.items])

  const saveScroll = () => {
    try {
      Taro.createSelectorQuery()
        .selectViewport()
        .scrollOffset()
        .exec((res) => {
          const top = res?.[0]?.scrollTop
          if (typeof top === 'number') scrollTopRef.current = top
        })
    } catch {
      // ignore
    }
  }

  const restoreScroll = () => {
    const top = scrollTopRef.current
    if (top <= 0) return
    setTimeout(() => {
      Taro.pageScrollTo({ scrollTop: top, duration: 0 })
    }, 30)
  }

  useDidHide(() => {
    saveScroll()
  })

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (bootstrapping) return
    // 已加载过：从详情返回不请求、不重绘列表，只恢复滚动
    if (hasLoadedRef.current) {
      restoreScroll()
      return
    }
    void bootstrapHome({ soft: false })
  })

  // 首次进入时若正值登录引导，等 bootstrapping 结束后再拉首页
  useEffect(() => {
    if (bootstrapping || !isLoggedIn || hasLoadedRef.current) return
    void bootstrapHome({ soft: false })
  }, [bootstrapping, isLoggedIn])

  usePullDownRefresh(async () => {
    try {
      if (!bootstrapping) await bootstrapHome({ soft: false })
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const bootstrapHome = async (opts?: { soft?: boolean }) => {
    if (!isLoggedIn) {
      setLoading(false)
      hasLoadedRef.current = false
      return
    }
    const soft = Boolean(opts?.soft && hasLoadedRef.current)
    if (soft) return
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
      hasLoadedRef.current = true
    } catch {
      setNeedJoin(true)
      setDishes([])
    } finally {
      setLoading(false)
    }
  }

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

  const runAiOrderDraft = async () => {
    const text = aiText.trim()
    if (!text) {
      Taro.showToast({ title: '请先说一句想吃的', icon: 'none' })
      return
    }
    if (needJoin) {
      Taro.showToast({ title: '请先加入厨房', icon: 'none' })
      return
    }
    if (aiSubmitting) return
    setAiSubmitting(true)
    try {
      const result = await draftOrderFromText(text)
      applyAiDraft(result, text)
      setAiOpen(false)
      setAiText('')

      if (result.degraded && result.items.length === 0) {
        Taro.showToast({ title: 'AI 暂不可用，请手动选菜', icon: 'none' })
        return
      }
      if (result.items.length === 0) {
        const tip =
          result.unmatched?.[0]?.reason ||
          result.ambiguityNote ||
          '没匹配到在售菜品，请手动选菜'
        Taro.showToast({ title: tip.slice(0, 40), icon: 'none' })
        return
      }

      // 异步拉饮食参考，失败静默
      void analyzeOrderInsight({
        dinerText: text,
        draftJson: JSON.stringify(result)
      })
        .then((insight) => {
          if (!insight.degraded) setAiInsight(insight)
        })
        .catch(() => {
          // ignore
        })

      const unmatchedHint =
        result.unmatched?.length > 0 ? `，${result.unmatched.length} 项未匹配` : ''
      Taro.showToast({
        title: `已生成草稿 ${result.items.length} 样${unmatchedHint}`,
        icon: 'none'
      })
      Taro.switchTab({ url: '/pages/order/index' })
    } catch {
      // request 层已 toast
    } finally {
      setAiSubmitting(false)
    }
  }

  const nick = user?.nickname || user?.username || '朋友'

  if (bootstrapping || (loading && !hasLoadedRef.current)) {
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
          <Text className='index-page__greeting'>
            {pending
              ? '申请已提交，等厨师确认'
              : rejected
                ? '申请未通过'
                : rejectReason
                  ? '厨房暂不可用'
                  : `${nick}，先加入一位厨师的厨房`}
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
        <Text className='index-page__greeting'>{nick}，今天想吃点什么？</Text>
        <Text className='index-page__subtitle'>专属菜单 · 无价格无支付 · 约到就是赚到</Text>
        {kitchenName ? (
          <View className='index-page__kitchen-chip'>
            <MiniIcon name='kitchen' size='sm' tone='mint' />
            <Text>{kitchenName} · 营业中</Text>
          </View>
        ) : null}
        <View
          className='index-page__ai-ask ck-pressable'
          onClick={() => {
            if (needJoin) {
              Taro.showToast({ title: '请先加入厨房', icon: 'none' })
              return
            }
            setAiOpen(true)
          }}
        >
          <MiniIcon name='spark' size='sm' tone='primary' className='index-page__ai-ico' />
          <Text className='index-page__ai-txt'>说句话就点菜：「明天中午两个人，想吃清淡点…」</Text>
        </View>
      </View>

      {recommended.length > 0 ? (
        <View className='index-page__section'>
          <View className='index-page__sec-row'>
            <Text className='index-page__section-title'>厨师推荐</Text>
          </View>
          <ScrollView scrollX className='index-page__rec-scroll' enhanced showScrollbar={false}>
            {recommended.map((d, i) => (
              <View
                key={d.id}
                className={`index-page__rec-card ck-pressable ${
                  i % 2 === 1 ? 'index-page__rec-card--alt' : ''
                }`}
                onClick={() => {
                  saveScroll()
                  Taro.navigateTo({ url: `/pages/dish/detail?id=${d.id}` })
                }}
              >
                <Text className='index-page__rec-tag'>{d.recommend ? '招牌' : '推荐'}</Text>
                {d.coverUrl ? (
                  <Image
                    className='index-page__rec-img'
                    src={d.coverUrl}
                    mode='aspectFill'
                    lazyLoad
                  />
                ) : (
                  <Text className='index-page__rec-emoji'>
                    {DISH_EMOJIS[i % DISH_EMOJIS.length]}
                  </Text>
                )}
                <Text className='index-page__rec-name'>{d.name}</Text>
                <Text className='index-page__rec-meta'>
                  {d.rating != null ? `评分 ${d.rating}` : '厨师力荐'}
                  {d.stockType === 'LIMITED' && d.stock != null ? ` · 今日剩 ${d.stock} 份` : ''}
                </Text>
              </View>
            ))}
          </ScrollView>
        </View>
      ) : null}

      <View className='index-page__section'>
        <View className='index-page__sec-row'>
          <Text className='index-page__section-title'>今日菜单</Text>
          <Text
            className='index-page__sec-more'
            onClick={() => Taro.switchTab({ url: '/pages/category/index' })}
          >
            全部 {dishes.length} ›
          </Text>
        </View>
        {dishes.length === 0 ? (
          <EmptyState emoji='🥘' title='厨房还没上菜' description='等厨师审核通过并上架后再来' />
        ) : (
          dishes.map((d) => (
            <DishCard
              key={d.id}
              dish={d}
              quantity={draftQtyMap[d.id] || 0}
              onAdd={addToDraft}
              onDec={decDraft}
            />
          ))
        )}
      </View>

      <DraftOrderBar />

      <AiPromptSheet
        visible={aiOpen}
        title='✨ 说句话点菜'
        hint='只匹配当前厨房在售菜品，生成预约草稿，不会自动提交。'
        placeholder='例如：明天中午两个人，想吃清淡点，来个鱼和素菜，少辣'
        submitting={aiSubmitting}
        text={aiText}
        onTextChange={setAiText}
        onClose={() => setAiOpen(false)}
        onSubmit={() => void runAiOrderDraft()}
      />
    </View>
  )
}
