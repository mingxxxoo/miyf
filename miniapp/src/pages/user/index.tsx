import { View, Text, Image, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function UserPage() {
  const { user, isLoggedIn, loading, login, logout } = useUserStore()

  const handleLogin = async () => {
    await login()
  }

  const goOrders = () => {
    Taro.switchTab({ url: '/pages/order/index' })
  }

  return (
    <View className='user-page'>
      <View className='user-page__profile ck-card'>
        {isLoggedIn && user ? (
          <>
            {user.avatarUrl ? (
              <Image className='user-page__avatar' src={user.avatarUrl} mode='aspectFill' />
            ) : (
              <View className='user-page__avatar user-page__avatar--placeholder'>
                <Text>🍳</Text>
              </View>
            )}
            <View className='user-page__info'>
              <Text className='user-page__name'>{user.nickname}</Text>
              {user.bio && <Text className='user-page__bio'>{user.bio}</Text>}
            </View>
          </>
        ) : (
          <View className='user-page__guest'>
            <Text className='user-page__guest-emoji'>👋</Text>
            <Text className='user-page__guest-title'>欢迎使用 miyf 厨房</Text>
            <Text className='user-page__guest-desc'>登录后可以查看预约与评价</Text>
            <Button
              className='ck-btn-primary user-page__login-btn'
              loading={loading}
              onClick={handleLogin}
            >
              进入厨房
            </Button>
          </View>
        )}
      </View>

      <View className='user-page__menu ck-card'>
        <View className='user-page__menu-item' onClick={goOrders}>
          <Text className='user-page__menu-label'>我的预约</Text>
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

      {isLoggedIn && (
        <Button className='user-page__logout' onClick={logout}>
          退出登录
        </Button>
      )}

      <Text className='user-page__brand'>miyf / kitchen</Text>
    </View>
  )
}
