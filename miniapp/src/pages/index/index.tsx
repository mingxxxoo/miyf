import {ScrollView, Text, View} from '@tarojs/components'
import Taro, {useDidHide, useDidShow, usePullDownRefresh} from '@tarojs/taro'
import {useEffect, useMemo, useRef, useState} from 'react'
import {analyzeOrderInsight, draftOrderFromText} from '@/api/ai'
import {fetchDishes, fetchRecommendDishes} from '@/api/dish'
import {fetchMyBinding} from '@/api/kitchen'
import AiPromptSheet from '@/components/AiPromptSheet'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import MiniIcon from '@/components/MiniIcon'
import RecommendDishCard from '@/components/RecommendDishCard'
import ServiceSwitcher, {DraftOrderBar} from '@/components/ServiceSwitcher'
import {useAuthGuard} from '@/hooks/useAuthGuard'
import {useDraftDishActions} from '@/hooks/useDraftDishActions'
import {useOrderStore} from '@/stores/orderStore'
import {useProductStore} from '@/stores/productStore'
import {useUserStore} from '@/stores/userStore'
import type {Dish} from '@/types'
import './index.scss'

export default function IndexPage() {
  const { bootstrapping } = useAuthGuard({ required: false })
  const setProduct = useProductStore((s) => s.setProduct)
  const user = useUserStore((s) => s.user)
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const refreshProfile = useUserStore((s) => s.refreshProfile)
  const { addToDraft } = useDraftDishActions()
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

  const recommended = useMemo(() => {
    const marked = dishes.filter((d) => d.recommend)
    return (marked.length ? marked : dishes).slice(0, 3)
  }, [dishes])

  const heroDish = recommended[0]
  const miniDishes = recommended.slice(1, 3)

  const lowStockTip = useMemo(() => {
    const hot = dishes.find(
      (d) => d.stockType === 'LIMITED' && d.stock != null && d.stock > 0 && d.stock <= 2
    )
    if (!hot) return ''
    return `${hot.name}今日还剩 ${hot.stock} 份，想吃抓紧预约`
  }, [dishes])

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
    Taro.setNavigationBarTitle({ title: '首页' })
    if (bootstrapping) return
    if (hasLoadedRef.current) {
      restoreScroll()
      return
    }
    void bootstrapHome({ soft: false })
  })

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
          const [rec, page] = await Promise.all([
            fetchRecommendDishes(6).catch(() => [] as Dish[]),
            fetchDishes({ page: 1, rows: 20 })
          ])
          const all = page.records || []
          setDishes(
            rec.length ? [...rec, ...all.filter((d) => !rec.some((r) => r.id === d.id))] : all
          )
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

  const openDish = (dish: Dish) => {
    saveScroll()
    Taro.navigateTo({ url: `/pages/dish/detail?id=${dish.id}` })
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

      void analyzeOrderInsight({
        dinerText: text,
        draftJson: JSON.stringify(result)
      })
        .then((insight) => {
          if (!insight.degraded) setAiInsight(insight)
        })
        .catch(() => undefined)

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
          icon='kitchen'
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
          icon='ticket'
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
        <View className='index-page__top'>
          <ServiceSwitcher compact className='index-page__switch' />
        </View>

        {kitchenName ? (
          <View className='index-page__kitchen-chip'>
            <MiniIcon name='kitchen' size='sm' tone='default' className='index-page__kitchen-ico' />
            <Text className='index-page__kitchen-name'>{kitchenName}</Text>
            <Text className='index-page__kitchen-sep'>·</Text>
            <Text className='index-page__kitchen-ok'>已加入</Text>
          </View>
        ) : null}

        <Text className='index-page__greeting'>今天想吃点什么？</Text>
        <Text className='index-page__subtitle'>
          {nick ? `${nick}，` : ''}厨师刚上新了几道，先看看推荐
        </Text>

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
          <View className='index-page__ai-spark'>
            <MiniIcon name='spark' size='sm' tone='primary' />
          </View>
          <Text className='index-page__ai-txt'>说一句话点菜</Text>
          <Text className='index-page__ai-go'>去试试</Text>
        </View>
      </View>

      <View className='index-page__section'>
        <View className='index-page__sec-row'>
          <Text className='index-page__section-title'>今日推荐</Text>
          <Text
            className='index-page__sec-more'
            onClick={() => Taro.switchTab({ url: '/pages/category/index' })}
          >
            全部 ›
          </Text>
        </View>

        {!heroDish ? (
          <EmptyState icon='dish' title='厨房还没上菜' description='等厨师审核通过并上架后再来' />
        ) : (
          <>
            <RecommendDishCard
              dish={heroDish}
              variant='hero'
              onOpen={openDish}
              onAdd={addToDraft}
            />
            {miniDishes.length > 0 ? (
              <ScrollView scrollX className='index-page__rec-scroll' enhanced showScrollbar={false}>
                <View className='index-page__rec-row'>
                  {miniDishes.map((d) => (
                    <View key={d.id} className='index-page__rec-item'>
                      <RecommendDishCard
                        dish={d}
                        variant='mini'
                        onOpen={openDish}
                        onAdd={addToDraft}
                      />
                    </View>
                  ))}
                </View>
              </ScrollView>
            ) : null}
          </>
        )}

        {lowStockTip ? (
          <View className='index-page__warn'>
            <MiniIcon name='alert' size='sm' tone='muted' />
            <Text className='index-page__warn-txt'>{lowStockTip}</Text>
          </View>
        ) : null}
      </View>

      <DraftOrderBar />

      <AiPromptSheet
        visible={aiOpen}
        title='说句话点菜'
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
