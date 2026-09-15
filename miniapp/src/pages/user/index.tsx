import { View, Text, Image, Button, Input, ScrollView } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { fetchMyBinding, unbindBinding, type BindingVo } from '@/api/kitchen'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import { toAbsoluteResourceUrl } from '@/utils/resourceUrl'
import './index.scss'

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

  const goHealthHome = () => {
    switchTo('health')
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
  const canUnbind = binding && (binding.status === 'BOUND' || binding.status === 'PENDING')
  const pageClass = product === 'health' ? 'user-page user-page--health' : 'user-page'

  const roleLabel =
    user?.activeRole === 'CHEF' ? '当前：厨师' : user?.activeRole === 'DINER' ? '当前：食客' : '未选择'

  const bindDesc =
    binding?.status === 'BOUND'
      ? `已绑定：${binding.kitchenName || '厨房'}`
      : binding?.status === 'PENDING'
        ? `待确认：${binding.kitchenName || '厨房'}`
        : binding?.status === 'REJECTED'
          ? `未通过：${binding.kitchenName || '厨房'}`
          : '邀请码绑定一位厨师'

  return (
    <ScrollView scrollY className={pageClass} enhanced showScrollbar={false}>
      <ServiceSwitcher compact className='user-page__switch' />

      <View className='user-page__profile ck-card'>
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
            <View className='user-page__name-row ck-pressable' onClick={startEditName}>
              <Text className='user-page__name'>
                {user?.nickname || user?.username || '厨房朋友'}
              </Text>
              <Text className='user-page__name-edit-tip'>改昵称</Text>
            </View>
          )}
          <Text className='user-page__service'>{meta.label}</Text>
        </View>
      </View>

      {product === 'kitchen' && (
        <View className='user-page__menu ck-card'>
          <View className='user-page__menu-item ck-pressable' onClick={goOrders}>
            <View className='user-page__menu-main'>
              <Text className='user-page__menu-label'>我的预约</Text>
              <Text className='user-page__menu-desc'>查看与管理预约单</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
          <View
            className='user-page__menu-item ck-pressable'
            onClick={() => Taro.navigateTo({ url: '/pages/join/index' })}
          >
            <View className='user-page__menu-main'>
              <Text className='user-page__menu-label'>加入厨房</Text>
              <Text className='user-page__menu-desc'>{bindDesc}</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
          {canUnbind && (
            <View className='user-page__menu-item ck-pressable' onClick={() => void handleUnbind()}>
              <View className='user-page__menu-main'>
                <Text className='user-page__menu-label'>
                  {binding?.status === 'PENDING' ? '取消申请' : '解除绑定'}
                </Text>
                <Text className='user-page__menu-desc'>换厨须先解除当前绑定</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
          )}
          <View
            className='user-page__menu-item ck-pressable'
            onClick={() => Taro.navigateTo({ url: '/pages/role/select' })}
          >
            <View className='user-page__menu-main'>
              <Text className='user-page__menu-label'>身份</Text>
              <Text className='user-page__menu-desc'>{roleLabel}</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
          {user?.chef && (
            <View
              className='user-page__menu-item ck-pressable'
              onClick={() => Taro.navigateTo({ url: '/pages/chef/index' })}
            >
              <View className='user-page__menu-main'>
                <Text className='user-page__menu-label'>厨师工作台</Text>
                <Text className='user-page__menu-desc'>厨房、菜品、邀请与接单</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
          )}
        </View>
      )}

      {product === 'health' && (
        <View className='user-page__menu ck-card'>
          <View className='user-page__menu-item ck-pressable' onClick={goHealthHome}>
            <View className='user-page__menu-main'>
              <Text className='user-page__menu-label'>进入健康首页</Text>
              <Text className='user-page__menu-desc'>指标统计与明细</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
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
