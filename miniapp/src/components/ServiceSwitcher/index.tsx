import { View, Text, Image, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useMemo, useState } from 'react'
import { activateOrSwitchRole } from '@/api/kitchen'
import { useOrderStore } from '@/stores/orderStore'
import { useUserStore } from '@/stores/userStore'
import MiniIcon from '@/components/MiniIcon'
import {
  useProductStore,
  type ProductCode
} from '@/stores/productStore'
import './ServiceSwitcher.scss'

export type SwitchTarget = ProductCode | 'chef'

interface ServiceSwitcherProps {
  compact?: boolean
  navigate?: boolean
  className?: string
  /** 当前所在端：厨师页传 chef */
  activeKey?: SwitchTarget
}

function mealHint(scheduledTime: string, timeOfDay: string, guestCount: number) {
  const parts: string[] = []
  if (scheduledTime && timeOfDay) {
    const today = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
    const t = new Date()
    t.setDate(t.getDate() + 1)
    const tomorrowStr = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
    const dayLabel =
      scheduledTime === todayStr ? '今天' : scheduledTime === tomorrowStr ? '明天' : scheduledTime
    const meal =
      timeOfDay.startsWith('11') || timeOfDay.startsWith('12') || timeOfDay.startsWith('13')
        ? '午餐'
        : timeOfDay.startsWith('17') || timeOfDay.startsWith('18') || timeOfDay.startsWith('19')
          ? '晚餐'
          : ''
    parts.push(meal ? `${dayLabel} ${timeOfDay} ${meal}` : `${dayLabel} ${timeOfDay}`)
  } else if (scheduledTime) {
    parts.push(scheduledTime)
  }
  if (guestCount) parts.push(`${guestCount} 人`)
  return parts.join(' · ')
}

const LABELS: Record<SwitchTarget, { label: string; desc: string }> = {
  kitchen: { label: '胡闹厨房', desc: '点菜 · 预约' },
  health: { label: '胡闹健康', desc: '指标 · 同步' },
  chef: { label: '厨房服务', desc: '厨师工作台' }
}

/** 预约单底部浮条：点击可上展菜品清单 */
export function DraftOrderBar({ className = '' }: { className?: string }) {
  const draft = useOrderStore((s) => s.draft)
  const updateDraftItemQty = useOrderStore((s) => s.updateDraftItemQty)
  const removeDraftItem = useOrderStore((s) => s.removeDraftItem)
  const [expanded, setExpanded] = useState(false)

  const draftCount = useMemo(
    () => draft.items.reduce((sum, it) => sum + (it.quantity || 0), 0),
    [draft.items]
  )
  const hint = useMemo(() => {
    if (!draft.items.length) return ''
    return (
      mealHint(draft.scheduledTime, draft.scheduledTimeOfDay, draft.guestCount) ||
      `${draft.items.length} 样菜等你确认`
    )
  }, [draft])

  if (draftCount <= 0) return null

  const goOrder = (e?: { stopPropagation?: () => void }) => {
    e?.stopPropagation?.()
    setExpanded(false)
    Taro.switchTab({ url: '/pages/order/index' })
  }

  return (
    <View className={`draft-order-bar-wrap ${className}`.trim()}>
      {expanded ? (
        <View className='draft-order-bar__mask' onClick={() => setExpanded(false)} />
      ) : null}

      {expanded ? (
        <View className='draft-order-panel'>
          <View className='draft-order-panel__head'>
            <Text className='draft-order-panel__title'>已选菜品</Text>
            <Text className='draft-order-panel__close' onClick={() => setExpanded(false)}>
              收起
            </Text>
          </View>
          <ScrollView scrollY className='draft-order-panel__list' enhanced showScrollbar={false}>
            {draft.items.map((item) => (
              <View key={item.dishId} className='draft-order-panel__row'>
                {item.coverUrl ? (
                  <Image
                    className='draft-order-panel__cover'
                    src={item.coverUrl}
                    mode='aspectFill'
                    lazyLoad
                  />
                ) : (
                  <View className='draft-order-panel__cover draft-order-panel__cover--empty'>
                    <MiniIcon name='dish' size='sm' tone='muted' />
                  </View>
                )}
                <Text className='draft-order-panel__name'>{item.dishName}</Text>
                <View className='draft-order-panel__qty'>
                  <View
                    className='draft-order-panel__qty-btn'
                    onClick={() => updateDraftItemQty(item.dishId, item.quantity - 1)}
                  >
                    <Text>−</Text>
                  </View>
                  <Text className='draft-order-panel__qty-num'>{item.quantity}</Text>
                  <View
                    className='draft-order-panel__qty-btn'
                    onClick={() => updateDraftItemQty(item.dishId, item.quantity + 1)}
                  >
                    <Text>＋</Text>
                  </View>
                </View>
                <Text
                  className='draft-order-panel__del'
                  onClick={() => removeDraftItem(item.dishId)}
                >
                  删除
                </Text>
              </View>
            ))}
          </ScrollView>
        </View>
      ) : null}

      <View className='draft-order-bar' onClick={() => setExpanded((v) => !v)}>
        <View className='draft-order-bar__ico'>
          <MiniIcon name='basket' size='md' tone='primary' />
          <Text className='draft-order-bar__badge'>{draftCount > 99 ? '99+' : draftCount}</Text>
        </View>
        <View className='draft-order-bar__txt'>
          <Text className='draft-order-bar__title'>预约单 · {draft.items.length} 样菜</Text>
          <Text className='draft-order-bar__sub'>
            {expanded ? '点击收起清单' : hint || '点击查看已选菜品'}
          </Text>
        </View>
        <Text className='draft-order-bar__go' onClick={goOrder}>
          去预约
        </Text>
      </View>
    </View>
  )
}

export default function ServiceSwitcher({
  compact = false,
  navigate = true,
  className = '',
  activeKey
}: ServiceSwitcherProps) {
  const product = useProductStore((s) => s.product)
  const switchTo = useProductStore((s) => s.switchTo)
  const setProduct = useProductStore((s) => s.setProduct)
  const user = useUserStore((s) => s.user)
  const refreshProfile = useUserStore((s) => s.refreshProfile)
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const isChef = Boolean(user?.chef)
  const current: SwitchTarget = activeKey || product
  const currentMeta = LABELS[current]
  const dotClass =
    current === 'health'
      ? 'service-switcher__dot--health'
      : current === 'chef'
        ? 'service-switcher__dot--chef'
        : 'service-switcher__dot--kitchen'

  const options: SwitchTarget[] = ['kitchen', 'health']
  if (isChef) options.push('chef')

  const onPick = async (key: SwitchTarget) => {
    if (busy) return
    setOpen(false)
    if (key === current) return

    if (key === 'chef') {
      setBusy(true)
      try {
        if (user?.activeRole !== 'CHEF') {
          await activateOrSwitchRole('CHEF', user)
          await refreshProfile()
        }
        Taro.navigateTo({ url: '/pages/chef/index' })
      } catch (err) {
        Taro.showToast({
          title: err instanceof Error ? err.message : '无法进入厨房服务',
          icon: 'none'
        })
      } finally {
        setBusy(false)
      }
      return
    }

    setBusy(true)
    try {
      // 从厨师端切回食客厨房时，先切到 DINER，避免首页再跳回厨师台
      if (current === 'chef' && key === 'kitchen' && user?.diner) {
        await activateOrSwitchRole('DINER', user)
        await refreshProfile()
      }
      if (navigate) {
        switchTo(key)
      } else {
        setProduct(key)
      }
    } catch (err) {
      Taro.showToast({
        title: err instanceof Error ? err.message : '切换失败',
        icon: 'none'
      })
    } finally {
      setBusy(false)
    }
  }

  return (
    <View
      className={`service-switcher ${compact ? 'service-switcher--compact' : ''} ${className}`.trim()}
    >
      <View
        className='service-switcher__trigger ck-pressable'
        onClick={() => setOpen((v) => !v)}
      >
        <View className={`service-switcher__dot ${dotClass}`} />
        <Text className='service-switcher__current'>{currentMeta.label}</Text>
        <Text className={`service-switcher__chevron ${open ? 'is-open' : ''}`}>⌄</Text>
      </View>

      {open ? (
        <>
          <View className='service-switcher__mask' onClick={() => setOpen(false)} />
          <View className='service-switcher__menu'>
            {options.map((key) => {
              const opt = LABELS[key]
              const active = key === current
              return (
                <View
                  key={key}
                  className={`service-switcher__option ck-pressable ${
                    active ? 'service-switcher__option--active' : ''
                  }`}
                  onClick={() => void onPick(key)}
                >
                  <View>
                    <Text className='service-switcher__option-label'>{opt.label}</Text>
                    <Text className='service-switcher__option-desc'>{opt.desc}</Text>
                  </View>
                  {active ? <MiniIcon name='check' size='sm' tone='primary' /> : null}
                </View>
              )
            })}
          </View>
        </>
      ) : null}
    </View>
  )
}
