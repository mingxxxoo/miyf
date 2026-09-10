import { View, Text, Image, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function UserPage() {
  const { user, isLoggedIn, logout, requireLogin } = useUserStore()

  useDidShow(() => {
    if (!isLoggedIn) {
      void requireLogin()
    }
  })

  const goOrders = () => {
    Taro.switchTab({ url: '/pages/order/index' })
  }

  if (!isLoggedIn || !user) {
    return (
      <View className='user-page'>
        <Text className='user-page__brand'>正在前往登录…</Text>
      </View>
    )
  }

  return (
    <View className='user-page'>
      <View className='user-page__profile ck-card'>
        {user.avatarUrl ? (
          <Image className='user-page__avatar' src={user.avatarUrl} mode='aspectFill' />
        ) : (
          <View className='user-page__avatar user-page__avatar--placeholder'>
            <Text>🍳</Text>
          </View>
        )}
        <View className='user-page__info'>
          <Text className='user-page__name'>{user.nickname || user.username}</Text>
          {user.username && <Text className='user-page__meta'>用户名 {user.username}</Text>}
          {user.phone && <Text className='user-page__meta'>手机 {user.phone}</Text>}
          {user.wechatId && <Text className='user-page__meta'>微信 {user.wechatId}</Text>}
          {user.bio && <Text className='user-page__bio'>{user.bio}</Text>}
        </View>
      </View>

      <View className='user-page__menu ck-card'>
        <View className='user-page__menu-item' onClick={goOrders}>
          <Text className='user-page__menu-label'>我的预约</Text>
          <Text className='user-page__menu-arrow'>→</Text>
        </View>
        <View
          className='user-page__menu-item'
          onClick={() => Taro.navigateTo({ url: '/pages/health/index' })}
        >
          <Text className='user-page__menu-label'>健康管理</Text>
          <Text className='user-page__menu-arrow'>→</Text>
        </View>
        <View
          className='user-page__menu-item'
          onClick={() => Taro.switchTab({ url: '/pages/category/index' })}
        >
          <Text className='user-page__menu-label'>浏览菜品</Text>
          <Text className='user-page__menu-arrow'>→</Text>
        </View>
      </View>

      <Button className='user-page__logout' onClick={logout}>
        退出登录
      </Button>

      <Text className='user-page__brand'>miyf / kitchen</Text>
    </View>
  )
}
