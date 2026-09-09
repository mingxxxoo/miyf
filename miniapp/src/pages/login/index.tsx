import { View, Text, Button } from '@tarojs/components'
import { useDidShow } from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function LoginPage() {
  const { loading, bootstrapping, isLoggedIn, login, goHome } = useUserStore()

  useDidShow(() => {
    if (isLoggedIn) {
      goHome()
    }
  })

  if (bootstrapping) {
    return (
      <View className='login-page login-page--loading'>
        <Text className='login-page__loading-text'>正在登录…</Text>
      </View>
    )
  }

  return (
    <View className='login-page'>
      <View className='login-page__card ck-card'>
        <Text className='login-page__brand'>miyf 厨房</Text>
        <Text className='login-page__title'>微信快捷登录</Text>
        <Text className='login-page__desc'>使用微信账号安全进入厨房</Text>

        <Button
          className='ck-btn-primary login-page__btn login-page__btn--wx'
          loading={loading}
          onClick={() => void login()}
        >
          微信一键登录
        </Button>
      </View>
    </View>
  )
}
