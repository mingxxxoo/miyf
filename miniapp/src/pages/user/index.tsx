import { View, Text, Image, Button, Input, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { fetchMyBinding, unbindBinding, type BindingVo } from '@/api/kitchen'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import { toAbsoluteResourceUrl } from '@/utils/resourceUrl'
import './index.scss'

function bindingStatusText(binding: BindingVo | null): string {
  if (!binding) return '未绑定'
  if (binding.ownerSelf && binding.status === 'BOUND') return '本厨默认食客'
  if (binding.status === 'BOUND') return '绑定中'
  if (binding.status === 'PENDING') return '待确认'
  if (binding.status === 'REJECTED') return '未通过'
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

  const roleDesc =
    user?.activeRole === 'CHEF' ? '当前：厨师' : user?.activeRole === 'DINER' ? '当前：食客' : '未选择'

  const kitchenSub =
    binding?.ownerSelf && binding.status === 'BOUND'
      ? `本厨默认 · ${binding.kitchenName || '厨房'}`
      : binding?.status === 'BOUND'
        ? `绑定中 · ${binding.kitchenName || '厨房'}`
        : binding?.status === 'PENDING'
          ? `待确认 · ${binding.kitchenName || '厨房'}`
          : binding?.status === 'REJECTED'
            ? `未通过 · ${binding.kitchenName || '厨房'}`
            : '用邀请码加入一位厨师的厨房'

  return (
    <ScrollView scrollY className={pageClass} enhanced showScrollbar={false}>
      <ServiceSwitcher compact className='user-page__switch' />

      <View className='user-page__profile'>
        <View className='user-page__avatar-wrap ck-pressable' onClick={() => void onChooseAvatar()}>
          {avatar ? (
            <Image className='user-page__avatar' src={avatar} mode='aspectFill' />
          ) : (
            <View className='user-page__avatar user-page__avatar--empty'>
              <Text className='user-page__avatar-letter'>
                {(user?.nickname || user?.username || '厨').slice(0, 1)}
              </Text>
            </View>
          )}
          <Text className='user-page__avatar-tip'>换头像</Text>
        </View>
        <View className='user-page__info'>
          {editingName ? (
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
            <View className='user-page__name-row'>
              <Text className='user-page__name ck-pressable' onClick={startEditName}>
                {user?.nickname || user?.username || '厨房朋友'}
              </Text>
              <Text className='user-page__role-badge'>{roleBadge}</Text>
            </View>
          )}
          <Text className='user-page__sub'>
            {product === 'kitchen'
              ? kitchenSub
              : `${meta.label} · 点此可切回厨房`}
          </Text>
        </View>
      </View>

      {product === 'kitchen' && (
        <>
          <View className='user-page__kitchen'>
              <View className='user-page__kitchen-ico'>
                <Text>🍳</Text>
              </View>
              <View
                className='user-page__kitchen-body ck-pressable'
                onClick={() => {
                  if (!canUnbind && !isOwnerSelfBound) Taro.navigateTo({ url: '/pages/join/index' })
                }}
              >
                <Text className='user-page__kitchen-name'>
                  {binding?.kitchenName || '尚未绑定厨房'}
                </Text>
                <Text className='user-page__kitchen-status'>
                  {binding ? bindingStatusText(binding) : '邀请码绑定一位厨师'}
                </Text>
              </View>
              {canUnbind ? (
                <Text
                  className='user-page__unbind'
                  onClick={() => void handleUnbind()}
                >
                  {binding?.status === 'PENDING' ? '取消' : '解绑'}
                </Text>
              ) : isOwnerSelfBound ? (
                <Text className='user-page__kitchen-go'>本厨</Text>
              ) : (
                <Text
                  className='user-page__kitchen-go'
                  onClick={() => Taro.navigateTo({ url: '/pages/join/index' })}
                >
                  加入 ›
                </Text>
              )}
            </View>

          <View className='user-page__menu'>
            <View className='user-page__menu-item ck-pressable' onClick={goOrders}>
              <View className='user-page__menu-ico' style={{ background: '#FFF1E2' }}>
                <Text>⭐</Text>
              </View>
              <Text className='user-page__menu-label'>我的预约</Text>
              <Text className='user-page__menu-val'>进行中</Text>
              <Text className='user-page__menu-arrow'>›</Text>
            </View>
            <View
              className='user-page__menu-item ck-pressable'
              onClick={() => Taro.navigateTo({ url: '/pages/join/index' })}
            >
              <View className='user-page__menu-ico' style={{ background: '#EBF3FB' }}>
                <Text>📋</Text>
              </View>
              <Text className='user-page__menu-label'>加入厨房</Text>
              <Text className='user-page__menu-val'>
                {binding?.status === 'BOUND' ? '已绑定' : '邀请码'}
              </Text>
              <Text className='user-page__menu-arrow'>›</Text>
            </View>
          </View>

          <View className='user-page__menu'>
            <View
              className='user-page__menu-item ck-pressable'
              onClick={() => Taro.navigateTo({ url: '/pages/role/select' })}
            >
              <View className='user-page__menu-ico' style={{ background: '#FBF3E0' }}>
                <Text>🎭</Text>
              </View>
              <Text className='user-page__menu-label'>身份</Text>
              <Text className='user-page__menu-val'>{roleDesc}</Text>
              <Text className='user-page__menu-arrow'>›</Text>
            </View>
            <View className='user-page__menu-item ck-pressable' onClick={startEditName}>
              <View className='user-page__menu-ico' style={{ background: '#FBF3E0' }}>
                <Text>✏️</Text>
              </View>
              <Text className='user-page__menu-label'>修改昵称</Text>
              <Text className='user-page__menu-val'>
                {user?.nickname || user?.username || ''}
              </Text>
              <Text className='user-page__menu-arrow'>›</Text>
            </View>
          </View>
        </>
      )}

      {product === 'health' && (
        <View className='user-page__menu'>
          <View
            className='user-page__menu-item ck-pressable'
            onClick={() => Taro.navigateTo({ url: '/pages/health/index' })}
          >
            <View className='user-page__menu-ico' style={{ background: '#E7F7F0' }}>
              <Text>🌿</Text>
            </View>
            <Text className='user-page__menu-label'>进入健康首页</Text>
            <Text className='user-page__menu-val'>指标与趋势</Text>
            <Text className='user-page__menu-arrow'>›</Text>
          </View>
          <View
            className='user-page__menu-item ck-pressable'
            onClick={() => Taro.navigateTo({ url: '/pages/role/select' })}
          >
            <View className='user-page__menu-ico' style={{ background: '#FBF3E0' }}>
              <Text>🎭</Text>
            </View>
            <Text className='user-page__menu-label'>身份</Text>
            <Text className='user-page__menu-val'>{roleDesc}</Text>
            <Text className='user-page__menu-arrow'>›</Text>
          </View>
        </View>
      )}

      <Button className='user-page__logout' onClick={() => void handleLogout()}>
        退出登录
      </Button>

      <Text className='user-page__brand'>miyf</Text>
    </ScrollView>
  )
}
