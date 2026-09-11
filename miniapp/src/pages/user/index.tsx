import { View, Text, Image, Button, Input } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import ServiceSwitcher from '@/components/ServiceSwitcher'
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

  useDidShow(() => {
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    Taro.setNavigationBarTitle({
      title: product === 'health' ? '我的 · 健康' : '我的 · 厨房'
    })
    void refreshProfile()
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
        <Text className='user-page__brand'>正在前往登录…</Text>
      </View>
    )
  }

  const meta = PRODUCT_META[product]
  const avatar = toAbsoluteResourceUrl(user?.avatarUrl)

  return (
    <View className='user-page'>
      <ServiceSwitcher compact className='user-page__switch' />

      <View className='user-page__profile ck-card'>
        <View className='user-page__avatar-wrap ck-pressable' onClick={() => void onChooseAvatar()}>
          {avatar ? (
            <Image className='user-page__avatar' src={avatar} mode='aspectFill' />
          ) : (
            <View className='user-page__avatar user-page__avatar--empty'>
              <Text>头像</Text>
            </View>
          )}
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
              <Button
                className='ck-btn-primary user-page__name-save'
                size='mini'
                loading={saving}
                onClick={() => void saveName()}
              >
                保存
              </Button>
              <Text
                className='user-page__name-cancel'
                onClick={() => setEditingName(false)}
              >
                取消
              </Text>
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
            <View>
              <Text className='user-page__menu-label'>我的预约</Text>
              <Text className='user-page__menu-desc'>查看与管理预约单</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
        </View>
      )}

      {product === 'health' && (
        <View className='user-page__menu ck-card'>
          <View className='user-page__menu-item ck-pressable' onClick={goHealthHome}>
            <View>
              <Text className='user-page__menu-label'>进入健康首页</Text>
              <Text className='user-page__menu-desc'>记录、趋势与资料</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
          <View className='user-page__menu-item ck-pressable' onClick={goHealthHome}>
            <View>
              <Text className='user-page__menu-label'>完善健康资料</Text>
              <Text className='user-page__menu-desc'>在健康页编辑称呼、性别、生日与身高</Text>
            </View>
            <Text className='user-page__menu-arrow'>→</Text>
          </View>
        </View>
      )}

      <Button className='user-page__logout' onClick={() => void handleLogout()}>
        退出登录
      </Button>

      <Text className='user-page__brand'>miyf</Text>
    </View>
  )
}
