import {Input, ScrollView, Text, View} from '@tarojs/components'
import Taro, {useDidShow} from '@tarojs/taro'
import {useMemo, useState} from 'react'
import AccountMenuItem from '@/components/AccountMenuItem'
import KitchenBindingCard from '@/components/KitchenBindingCard'
import ProfileHero from '@/components/ProfileHero'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import ServiceTile from '@/components/ServiceTile'
import {activateOrSwitchRole, type BindingVo, fetchMyBinding, unbindBinding} from '@/api/kitchen'
import {PRODUCT_META, useProductStore} from '@/stores/productStore'
import {useOrderStore} from '@/stores/orderStore'
import {useUserStore} from '@/stores/userStore'
import {toAbsoluteResourceUrl} from '@/utils/resourceUrl'
import './index.scss'

const ACTIVE = new Set(['PENDING', 'CONFIRMED', 'PREPARING', 'READY'])

function bindingStatusText(binding: BindingVo | null): string {
  if (!binding) return '邀请码绑定一位厨师'
  if (binding.ownerSelf && binding.status === 'BOUND') return '本厨默认 · 邀请码加入'
  if (binding.status === 'BOUND') return '已绑定 · 邀请码加入'
  if (binding.status === 'PENDING') return '待确认 · 等待厨师通过'
  if (binding.status === 'REJECTED') return '未通过 · 可换码重试'
  return binding.status
}

export default function UserPage() {
  const {
    user,
    isLoggedIn,
    logout,
    requireLogin,
    refreshProfile,
    updateProfile,
    uploadAvatar
  } = useUserStore()
  const product = useProductStore((s) => s.product)
  const setProduct = useProductStore((s) => s.setProduct)
  const switchTo = useProductStore((s) => s.switchTo)
  const orders = useOrderStore((s) => s.orders)
  const draft = useOrderStore((s) => s.draft)
  const fetchOrders = useOrderStore((s) => s.fetchOrders)
  const [editingName, setEditingName] = useState(false)
  const [nameDraft, setNameDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [binding, setBinding] = useState<BindingVo | null>(null)

  useDidShow(() => {
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    Taro.setNavigationBarTitle({
      title: product === 'health' ? '我的 · 健康' : '我的 · 胡闹厨房'
    })
    void refreshProfile()
    void fetchOrders().catch(() => undefined)
    if (product === 'kitchen') {
      void fetchMyBinding().then(setBinding)
    } else {
      setBinding(null)
    }
  })

  const goOrders = () => {
    setProduct('kitchen')
    Taro.switchTab({ url: '/pages/order/index' })
  }

  const startEditName = () => {
    setNameDraft(user?.nickname || user?.username || '')
    setEditingName(true)
  }

  const saveName = async () => {
    const name = nameDraft.trim()
    if (!name) {
      Taro.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    setSaving(true)
    const updated = await updateProfile({ nickname: name })
    setSaving(false)
    if (updated) {
      setEditingName(false)
      Taro.showToast({ title: '已更新', icon: 'success' })
    }
  }

  const onChooseAvatar = async () => {
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const path = res.tempFilePaths?.[0]
      if (!path) return
      const updated = await uploadAvatar(path)
      if (updated) {
        Taro.showToast({ title: '头像已更新', icon: 'success' })
      }
    } catch {
      // cancel
    }
  }

  const handleUnbind = async () => {
    if (!binding?.id) return
    const tip =
      binding.status === 'PENDING'
        ? `取消对「${binding.kitchenName || '厨房'}」的申请？`
        : `解除与「${binding.kitchenName || '厨房'}」的绑定？有进行中的预约时无法解除，历史预约与评价会保留。`
    const res = await Taro.showModal({ title: '确认', content: tip })
    if (!res.confirm) return
    await unbindBinding(binding.id)
    setBinding(null)
    Taro.showToast({ title: '已解除', icon: 'success' })
  }

  const handleLogout = async () => {
    const res = await Taro.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？'
    })
    if (!res.confirm) return
    logout()
  }

  const enterChef = async () => {
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
    }
  }

  const stats = useMemo(() => {
    const active = orders.filter((o) => ACTIVE.has(o.status)).length
    const done = orders.filter((o) => o.status === 'COMPLETED').length
    const draftN = draft.items.reduce((s, it) => s + (it.quantity || 0), 0)
    return [
      { value: active, label: '进行中', onClick: goOrders },
      { value: done, label: '已完成', onClick: goOrders },
      {
        value: draftN,
        label: '草稿菜',
        onClick: () => Taro.switchTab({ url: '/pages/category/index' })
      }
    ]
  }, [orders, draft.items])

  if (!isLoggedIn) {
    return (
      <View className='user-page'>
        <Text className='user-page__hint'>正在前往登录…</Text>
      </View>
    )
  }

  const meta = PRODUCT_META[product]
  const avatar = toAbsoluteResourceUrl(user?.avatarUrl)
  const canUnbind =
    binding &&
    (binding.status === 'BOUND' || binding.status === 'PENDING') &&
    !binding.ownerSelf &&
    binding.removable !== false
  const isOwnerSelfBound = Boolean(binding?.ownerSelf && binding.status === 'BOUND')
  const pageClass = product === 'health' ? 'user-page user-page--health' : 'user-page'

  const roleBadge =
    user?.activeRole === 'CHEF' ? '厨师' : user?.activeRole === 'DINER' ? '食客' : '未选身份'

  const kitchenSub =
    binding?.ownerSelf && binding.status === 'BOUND'
      ? `微信已登录 · ${binding.kitchenName || '本厨'}`
      : binding?.status === 'BOUND'
        ? `微信已登录 · ${binding.kitchenName || '厨房'}`
        : binding?.status === 'PENDING'
          ? `待确认 · ${binding.kitchenName || '厨房'}`
          : binding?.status === 'REJECTED'
            ? `未通过 · ${binding.kitchenName || '厨房'}`
            : product === 'health'
              ? `${meta.label} · 可切回厨房`
              : '微信已登录 · 用邀请码加入厨房'

  const bindingAction = canUnbind
    ? binding?.status === 'PENDING'
      ? '取消'
      : '解绑'
    : isOwnerSelfBound
      ? '本厨'
      : binding?.status === 'BOUND'
        ? '查看'
        : '加入'

  const onBindingAction = () => {
    if (canUnbind) {
      void handleUnbind()
      return
    }
    if (isOwnerSelfBound) return
    Taro.navigateTo({ url: '/pages/join/index' })
  }

  const nameSlot = editingName ? (
    <View className='user-page__name-edit'>
      <Input
        className='user-page__name-input'
        value={nameDraft}
        maxlength={32}
        onInput={(e) => setNameDraft(e.detail.value)}
      />
      <View className='user-page__name-actions'>
        <Text
          className={`user-page__name-action user-page__name-action--primary${
            saving ? ' is-disabled' : ''
          }`}
          onClick={() => {
            if (!saving) void saveName()
          }}
        >
          {saving ? '保存中…' : '保存'}
        </Text>
        <Text className='user-page__name-action' onClick={() => setEditingName(false)}>
          取消
        </Text>
      </View>
    </View>
  ) : (
    <View className='profile-hero__name-row' onClick={startEditName}>
      <Text className='profile-hero__name'>{user?.nickname || user?.username || '厨房朋友'}</Text>
      <Text className='profile-hero__role'>{roleBadge}</Text>
    </View>
  )

  return (
    <ScrollView scrollY className={pageClass} enhanced showScrollbar={false}>
      <ServiceSwitcher compact className='user-page__switch' />

      <ProfileHero
        avatarUrl={avatar || undefined}
        avatarLetter={(user?.nickname || user?.username || '厨').slice(0, 1)}
        displayName={user?.nickname || user?.username || '厨房朋友'}
        roleBadge={roleBadge}
        subtitle={kitchenSub}
        tone={product === 'health' ? 'health' : 'kitchen'}
        stats={product === 'kitchen' ? stats : undefined}
        nameSlot={nameSlot}
        onAvatarClick={() => void onChooseAvatar()}
      />

      {product === 'kitchen' && (
        <>
          <KitchenBindingCard
            kitchenName={binding?.kitchenName || '尚未绑定厨房'}
            statusText={bindingStatusText(binding)}
            actionLabel={bindingAction}
            onAction={onBindingAction}
            onClick={() => {
              if (!canUnbind && !isOwnerSelfBound) {
                Taro.navigateTo({ url: '/pages/join/index' })
              }
            }}
          />

          <View className='user-page__sec'>
            <Text className='user-page__sec-title'>服务入口</Text>
            <Text className='user-page__sec-hint'>同一账号可切换</Text>
          </View>
          <View className='user-page__tiles'>
            <ServiceTile
              variant='chef'
              icon='kitchen'
              title='厨房工作台'
              desc='切厨师身份，处理预约与菜品'
              onClick={() => void enterChef()}
            />
            <ServiceTile
              variant='health'
              icon='heart'
              title='胡闹健康'
              desc='评分、指标与数据源'
              onClick={() => switchTo('health')}
            />
          </View>

          <View className='user-page__sec'>
            <Text className='user-page__sec-title'>账户</Text>
          </View>
          <View className='user-page__list'>
            <AccountMenuItem
              icon='switch'
              title='切换身份'
              hint='吃饭的 / 干活的'
              iconBg='#FFF1E2'
              onClick={() => Taro.navigateTo({ url: '/pages/role/select' })}
            />
            <AccountMenuItem
              icon='order'
              title='我的预约'
              hint='草稿与历史状态'
              iconBg='#FBF3E0'
              onClick={goOrders}
            />
            <AccountMenuItem
              icon='logout'
              title='退出登录'
              hint='清除本机登录态'
              danger
              onClick={() => void handleLogout()}
            />
          </View>
        </>
      )}

      {product === 'health' && (
        <>
          <View className='user-page__sec'>
            <Text className='user-page__sec-title'>服务入口</Text>
          </View>
          <View className='user-page__tiles'>
            <ServiceTile
              variant='health'
              icon='heart'
              title='健康首页'
              desc='指标、趋势与数据源'
              onClick={() => Taro.navigateTo({ url: '/pages/health/index' })}
            />
            <ServiceTile
              variant='chef'
              icon='kitchen'
              title='胡闹厨房'
              desc='点菜与预约'
              onClick={() => switchTo('kitchen')}
            />
          </View>
          <View className='user-page__sec'>
            <Text className='user-page__sec-title'>账户</Text>
          </View>
          <View className='user-page__list'>
            <AccountMenuItem
              icon='switch'
              title='切换身份'
              hint='吃饭的 / 干活的'
              iconBg='#FFF1E2'
              onClick={() => Taro.navigateTo({ url: '/pages/role/select' })}
            />
            <AccountMenuItem
              icon='logout'
              title='退出登录'
              hint='清除本机登录态'
              danger
              onClick={() => void handleLogout()}
            />
          </View>
        </>
      )}

      <Text className='user-page__brand'>miyf · 家庭厨房预约</Text>
    </ScrollView>
  )
}
