import { View, Text, Image, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function UserPage() {
  const { user, isLoggedIn, logout, requireLogin } = useUserStore()
  const product = useProductStore((s) => s.product)
  const setProduct = useProductStore((s) => s.setProduct)
  const switchTo = useProductStore((s) => s.switchTo)

  useDidShow(() => {
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    // 从健康页返回「我的」时保持当前服务文案，不强制改 product
    Taro.setNavigationBarTitle({
      title: product === 'health' ? '我的 · 健康' : '我的 · 厨房'
    })
  })

  const goOrders = () => {
    setProduct('kitchen')
    Taro.switchTab({ url: '/pages/order/index' })
  }

  const goCategory = () => {
    setProduct('kitchen')
    Taro.switchTab({ url: '/pages/category/index' })
  }

  const goHealth = () => {
    switchTo('health')
  }

  const goKitchenHome = () => {
    switchTo('kitchen')
  }

  if (!isLoggedIn || !user) {
    return (
      <View className='user-page'>
        <Text className='user-page__brand'>正在前往登录…</Text>
      </View>
    )
  }

  const meta = PRODUCT_META[product]

  return (
    <View className={`user-page user-page--${product}`}>
      <View className='user-page__switch'>
        <ServiceSwitcher />
      </View>

      <View className='user-page__profile ck-card'>
        {user.avatarUrl ? (
          <Image className='user-page__avatar' src={user.avatarUrl} mode='aspectFill' />
        ) : (
          <View className='user-page__avatar user-page__avatar--placeholder'>
            <Text>{product === 'health' ? '💚' : '🍳'}</Text>
          </View>
        )}
        <View className='user-page__info'>
          <Text className='user-page__name'>{user.nickname || user.username}</Text>
          <Text className='user-page__service'>{meta.label}</Text>
          {user.username && <Text className='user-page__meta'>用户名 {user.username}</Text>}
          {user.phone && <Text className='user-page__meta'>手机 {user.phone}</Text>}
          {user.bio && <Text className='user-page__bio'>{user.bio}</Text>}
        </View>
      </View>

      <View className='user-page__menu ck-card'>
        {product === 'kitchen' ? (
          <>
            <View className='user-page__menu-item ck-pressable' onClick={goOrders}>
              <View>
                <Text className='user-page__menu-label'>我的预约</Text>
                <Text className='user-page__menu-desc'>查看与管理预约单</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
            <View className='user-page__menu-item ck-pressable' onClick={goCategory}>
              <View>
                <Text className='user-page__menu-label'>浏览菜品</Text>
                <Text className='user-page__menu-desc'>挑一道想吃的</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
            <View className='user-page__menu-item ck-pressable' onClick={goHealth}>
              <View>
                <Text className='user-page__menu-label'>进入健康服务</Text>
                <Text className='user-page__menu-desc'>体征记录与趋势</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
          </>
        ) : (
          <>
            <View className='user-page__menu-item ck-pressable' onClick={goHealth}>
              <View>
                <Text className='user-page__menu-label'>健康首页</Text>
                <Text className='user-page__menu-desc'>趋势、均值与手动录入</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
            <View className='user-page__menu-item ck-pressable' onClick={goKitchenHome}>
              <View>
                <Text className='user-page__menu-label'>返回厨房服务</Text>
                <Text className='user-page__menu-desc'>菜品浏览与预约</Text>
              </View>
              <Text className='user-page__menu-arrow'>→</Text>
            </View>
          </>
        )}
      </View>

      <Button className='user-page__logout' onClick={logout}>
        退出登录
      </Button>

      <Text className='user-page__brand'>miyf / {product}</Text>
    </View>
  )
}
