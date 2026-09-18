import { View, Text, Button, Input, Picker, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useMemo, useState } from 'react'
import { analyzeOrderInsight, draftOrderFromText } from '@/api/ai'
import { fetchMyBinding } from '@/api/kitchen'
import AiPromptSheet from '@/components/AiPromptSheet'
import OrderCard from '@/components/OrderCard'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useOrderStore } from '@/stores/orderStore'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import type { OrderStatus } from '@/types'
import './index.scss'

type TabKey = 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'ALL'

const ACTIVE_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY']

const TABS: { key: TabKey; label: string }[] = [
  { key: 'ACTIVE', label: '进行中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
  { key: 'ALL', label: '全部' }
]

const DISH_EMOJIS = ['🍖', '🥬', '🍅', '🐟', '🥗', '🍲', '🍛', '🥘']
const EMOJI_BGS = ['#FFF1E2', '#E7F7F0', '#FBF3E0', '#EBF3FB', '#FCEEEA']

function todayStr(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function tomorrowStr(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const MEAL_PRESETS = [
  { key: 'today-dinner', label: '今天 · 晚餐', date: () => todayStr(), time: '18:00' },
  { key: 'tomorrow-lunch', label: '明天 · 午餐', date: () => tomorrowStr(), time: '12:00' },
  { key: 'tomorrow-dinner', label: '明天 · 晚餐', date: () => tomorrowStr(), time: '18:00' }
] as const

export default function OrderIndexPage() {
  const {
    orders,
    draft,
    aiMeta,
    aiInsight,
    loading,
    loadError,
    fetchOrders,
    updateDraft,
    updateDraftItemQty,
    setDraftItem,
    submitOrder,
    clearDraft,
    applyAiDraft,
    setAiInsight
  } = useOrderStore()
  const { isLoggedIn, requireLogin, user, refreshProfile } = useUserStore()
  const setProduct = useProductStore((s) => s.setProduct)
  const [tab, setTab] = useState<TabKey>('ACTIVE')
  const [needJoin, setNeedJoin] = useState(false)
  const [pending, setPending] = useState(false)
  const [gateLoading, setGateLoading] = useState(true)
  const [aiOpen, setAiOpen] = useState(false)
  const [aiText, setAiText] = useState('')
  const [aiSubmitting, setAiSubmitting] = useState(false)
  const minDate = todayStr()

  const reload = () => fetchOrders()

  const filteredOrders = useMemo(() => {
    if (tab === 'ALL') return orders
    if (tab === 'ACTIVE') return orders.filter((o) => ACTIVE_STATUSES.includes(o.status))
    return orders.filter((o) => o.status === tab)
  }, [orders, tab])

  const activeMealKey = useMemo(() => {
    return (
      MEAL_PRESETS.find(
        (p) => p.date() === draft.scheduledTime && p.time === draft.scheduledTimeOfDay
      )?.key ?? ''
    )
  }, [draft.scheduledTime, draft.scheduledTimeOfDay])

  const bootstrapGate = async () => {
    setGateLoading(true)
    const profile = (await refreshProfile()) || user
    if (profile?.activeRole === 'CHEF' || (profile?.chef && profile?.activeRole !== 'DINER')) {
      Taro.redirectTo({ url: '/pages/chef/index' })
      return
    }
    try {
      const binding = await fetchMyBinding()
      if (!binding || binding.status !== 'BOUND') {
        setNeedJoin(true)
        setPending(binding?.status === 'PENDING')
        clearDraft()
      } else {
        setNeedJoin(false)
        setPending(false)
        await reload()
      }
    } catch {
      setNeedJoin(true)
    } finally {
      setGateLoading(false)
    }
  }

  useDidShow(() => {
    setProduct('kitchen')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.kitchen.brand })
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    void bootstrapGate()
  })

  useEffect(() => {
    if (isLoggedIn && !needJoin && !gateLoading) void reload()
  }, [isLoggedIn, needJoin, gateLoading])

  usePullDownRefresh(async () => {
    try {
      if (isLoggedIn) await bootstrapGate()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const ensureLogin = async () => {
    if (isLoggedIn) return true
    return requireLogin()
  }

  const handleSubmit = async () => {
    if (needJoin) {
      Taro.showToast({ title: '请先加入厨房', icon: 'none' })
      return
    }
    if (!draft.items.length) {
      Taro.showToast({ title: '先选一道想吃的菜吧', icon: 'none' })
      return
    }
    if (draft.scheduledTime && draft.scheduledTime < minDate) {
      Taro.showToast({ title: '期望日期不能早于今天', icon: 'none' })
      return
    }
    const ok = await ensureLogin()
    if (!ok) return
    const order = await submitOrder()
    if (order) {
      Taro.showToast({ title: order.displayTip || '厨房收到啦', icon: 'success' })
    } else {
      Taro.showToast({ title: '提交失败', icon: 'none' })
    }
  }

  const handleMealPreset = (key: (typeof MEAL_PRESETS)[number]['key']) => {
    const preset = MEAL_PRESETS.find((p) => p.key === key)
    if (!preset) return
    updateDraft({
      scheduledTime: preset.date(),
      scheduledTimeOfDay: preset.time
    })
  }

  const handlePickDate = (e: { detail: { value: string } }) => {
    updateDraft({ scheduledTime: String(e.detail.value) })
  }

  const handlePickTimeOfDay = (e: { detail: { value: string } }) => {
    updateDraft({ scheduledTimeOfDay: String(e.detail.value) })
  }

  const handlePickGuest = (e: { detail: { value: string | number } }) => {
    const guests = [1, 2, 3, 4, 5, 6, 8, 10]
    updateDraft({ guestCount: guests[Number(e.detail.value)] || 2 })
  }

  const goCategory = () => {
    Taro.switchTab({ url: '/pages/category/index' })
  }

  const runAiOrderDraft = async () => {
    const text = aiText.trim()
    if (!text) {
      Taro.showToast({ title: '请先说一句想吃的', icon: 'none' })
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
        Taro.showToast({
          title: (result.ambiguityNote || '没匹配到在售菜品').slice(0, 40),
          icon: 'none'
        })
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
      Taro.showToast({ title: `已生成草稿 ${result.items.length} 样`, icon: 'none' })
    } catch {
      // request toast
    } finally {
      setAiSubmitting(false)
    }
  }

  const applyReplaceSuggestion = (replaceDishId?: string, replaceDishName?: string) => {
    if (!replaceDishId || !replaceDishName) return
    setDraftItem({
      dishId: replaceDishId,
      dishName: replaceDishName,
      quantity: 1
    })
    Taro.showToast({ title: `已加入「${replaceDishName}」`, icon: 'none' })
  }

  const verdictClass = (verdict?: string) => {
    const v = (verdict || '').toUpperCase()
    if (v === 'CAUTION') return 'caution'
    if (v === 'AVOID') return 'avoid'
    return 'ok'
  }

  const verdictLabel = (verdict?: string) => {
    const v = (verdict || '').toUpperCase()
    if (v === 'CAUTION') return '留意'
    if (v === 'AVOID') return '慎选'
    return '合适'
  }

  if (!isLoggedIn) {
    return <Loading fullscreen text='正在前往登录…' />
  }

  if (gateLoading || (loading && !orders.length && !draft.items.length && !loadError && !needJoin)) {
    return <Loading fullscreen text='整理预约单…' />
  }

  if (needJoin) {
    return (
      <View className='order-page'>
        <EmptyState
          emoji='🔑'
          title={pending ? '等待厨师确认' : '先加入厨房'}
          description='没有公开菜单。绑定一位厨师后才能预约。'
          actionText='去加入厨房'
          onAction={() => Taro.navigateTo({ url: '/pages/join/index' })}
        />
      </View>
    )
  }

  return (
    <View className='order-page'>
      <ServiceSwitcher compact className='order-page__switch' />

      <View
        className='order-page__ai-entry ck-pressable'
        onClick={() => setAiOpen(true)}
      >
        <Text className='order-page__ai-entry-ico'>✨</Text>
        <Text className='order-page__ai-entry-txt'>说句话生成预约草稿</Text>
      </View>

      {draft.items.length === 0 && (
        <View className='order-page__cta ck-card'>
          <Text className='order-page__cta-title'>还没有预约菜品</Text>
          <Text className='order-page__cta-desc'>去选菜加入预约，或用 AI 说句话点菜</Text>
          <Button className='ck-btn-primary order-page__cta-btn' onClick={goCategory}>
            去选菜加入预约
          </Button>
        </View>
      )}

      {draft.items.length > 0 && (
        <View className='order-page__draft'>
          <View className='order-page__draft-head'>
            <Text className='order-page__draft-emoji'>🧺</Text>
            <Text className='order-page__draft-title'>预约单 · {draft.items.length} 样</Text>
            <Text className='order-page__draft-clear' onClick={clearDraft}>
              清空
            </Text>
          </View>

          {aiMeta?.ambiguityNote ? (
            <Text className='order-page__ai-note'>{aiMeta.ambiguityNote}</Text>
          ) : null}
          {aiMeta?.unmatched?.length ? (
            <View className='order-page__unmatched'>
              {aiMeta.unmatched.map((u, i) => (
                <Text key={`${u.rawText}-${i}`} className='order-page__unmatched-item'>
                  未匹配「{u.rawText}」：{u.reason}
                </Text>
              ))}
            </View>
          ) : null}

          {draft.items.map((item, idx) => (
            <View key={item.dishId} className='order-page__draft-line'>
              <View
                className='order-page__draft-emoji-box'
                style={{ background: EMOJI_BGS[idx % EMOJI_BGS.length] }}
              >
                <Text>{DISH_EMOJIS[idx % DISH_EMOJIS.length]}</Text>
              </View>
              <View className='order-page__draft-name-wrap'>
                <Text className='order-page__draft-name'>{item.dishName}</Text>
              </View>
              <View className='order-page__qty'>
                <View
                  className='order-page__qty-btn'
                  onClick={() => updateDraftItemQty(item.dishId, item.quantity - 1)}
                >
                  <Text>−</Text>
                </View>
                <Text className='order-page__qty-num'>{item.quantity}</Text>
                <View
                  className='order-page__qty-btn'
                  onClick={() => updateDraftItemQty(item.dishId, item.quantity + 1)}
                >
                  <Text>＋</Text>
                </View>
              </View>
            </View>
          ))}

          <View className='order-page__meal-pick'>
            {MEAL_PRESETS.map((p) => (
              <View
                key={p.key}
                className={`order-page__meal-chip ${
                  activeMealKey === p.key ? 'order-page__meal-chip--on' : ''
                }`}
                onClick={() => handleMealPreset(p.key)}
              >
                <Text>{p.label}</Text>
              </View>
            ))}
          </View>

          <View className='order-page__custom-row'>
            <Picker
              mode='date'
              start={minDate}
              value={draft.scheduledTime || minDate}
              onChange={handlePickDate}
            >
              <View className='order-page__picker order-page__picker--sm'>
                {draft.scheduledTime || '日期'}
              </View>
            </Picker>
            <Picker
              mode='time'
              value={draft.scheduledTimeOfDay || '12:00'}
              onChange={handlePickTimeOfDay}
            >
              <View className='order-page__picker order-page__picker--sm'>
                {draft.scheduledTimeOfDay || '时间'}
              </View>
            </Picker>
            <Picker
              mode='selector'
              range={['1 位', '2 位', '3 位', '4 位', '5 位', '6 位', '8 位', '10 位']}
              onChange={handlePickGuest}
            >
              <View className='order-page__picker order-page__picker--sm'>
                {draft.guestCount} 位
              </View>
            </Picker>
          </View>

          <View className='order-page__submit-row'>
            <Input
              className='order-page__note-input'
              placeholder='备注：口味偏好、忌口等'
              value={draft.note}
              onInput={(e) => updateDraft({ note: e.detail.value })}
            />
            <View className='order-page__submit-btn' onClick={handleSubmit}>
              <Text>提交预约</Text>
            </View>
          </View>
        </View>
      )}

      {aiInsight && !aiInsight.degraded ? (
        <View className={`order-page__insight order-page__insight--${verdictClass(aiInsight.overallVerdict)}`}>
          <View className='order-page__insight-head'>
            <Text className='order-page__insight-badge'>
              {verdictLabel(aiInsight.overallVerdict)}
            </Text>
            <Text className='order-page__insight-title'>饮食参考</Text>
          </View>
          {aiInsight.summary ? (
            <Text className='order-page__insight-summary'>{aiInsight.summary}</Text>
          ) : null}
          {(aiInsight.dishInsights || []).map((d) => (
            <View key={d.dishId} className='order-page__insight-dish'>
              <Text
                className={`order-page__insight-v order-page__insight-v--${verdictClass(d.verdict)}`}
              >
                {verdictLabel(d.verdict)}
              </Text>
              <View className='order-page__insight-dish-body'>
                <Text className='order-page__insight-dish-name'>{d.dishName}</Text>
                {d.reason ? (
                  <Text className='order-page__insight-dish-reason'>{d.reason}</Text>
                ) : null}
              </View>
            </View>
          ))}
          {(aiInsight.suggestions || []).map((s, i) => (
            <View key={`${s.type}-${i}`} className='order-page__insight-sug'>
              <Text className='order-page__insight-sug-txt'>{s.text}</Text>
              {s.type === 'REPLACE' && s.replaceDishId ? (
                <Text
                  className='order-page__insight-sug-act'
                  onClick={() => applyReplaceSuggestion(s.replaceDishId, s.replaceDishName)}
                >
                  换成它
                </Text>
              ) : null}
            </View>
          ))}
          {aiInsight.disclaimer ? (
            <Text className='order-page__insight-disc'>{aiInsight.disclaimer}</Text>
          ) : null}
        </View>
      ) : null}

      <View className='order-page__sec-row'>
        <Text className='order-page__section-title'>我的预约</Text>
      </View>

      <ScrollView scrollX className='order-page__tabs' enhanced showScrollbar={false}>
        {TABS.map((t) => (
          <View
            key={t.key}
            className={`order-page__tab ck-pressable ${tab === t.key ? 'order-page__tab--active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            <Text>{t.label}</Text>
          </View>
        ))}
      </ScrollView>

      <View className='order-page__history'>
        {loadError ? (
          <EmptyState
            emoji='📋'
            title='加载失败'
            description='预约记录暂时拉不下来，请重试'
            actionText='重试'
            onAction={() => void reload()}
          />
        ) : filteredOrders.length === 0 ? (
          <EmptyState
            emoji='📋'
            title='还没有预约记录'
            description='挑一道喜欢的菜，告诉厨房你什么时候来'
            actionText='去看看菜品'
            onAction={goCategory}
          />
        ) : (
          filteredOrders.map((order) => <OrderCard key={order.id} order={order} />)
        )}
      </View>

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
