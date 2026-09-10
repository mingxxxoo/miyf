import { View, Text, Button } from '@tarojs/components'
import { useDidShow } from '@tarojs/taro'
import ServiceSwitcher from '@/components/ServiceSwitcher'
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
        <Text className='login-page__brand'>miyf</Text>
        <Text className='login-page__title'>微信快捷登录</Text>
        <Text className='login-page__desc'>一套账号，畅用厨房服务与健康服务</Text>

        <View className='login-page__switch'>
          <ServiceSwitcher compact navigate={false} />
        </View>
        <Text className='login-page__switch-hint'>登录后将进入所选服务</Text>

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
