import { View, Text, Button, Input } from '@tarojs/components'
import Taro, { useDidShow, useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function LoginPage() {
  const { loading, bootstrapping, isLoggedIn, login, goHome } = useUserStore()
  const [username, setUsername] = useState('')
  const [phone, setPhone] = useState('')
  const [wechatId, setWechatId] = useState('')
  const [preparing, setPreparing] = useState(true)

  useLoad(() => {
    // 进入应用即预取微信登录 code，避免点按钮后再请求
    void Taro.login()
      .catch(() => undefined)
      .finally(() => setPreparing(false))
  })

  useDidShow(() => {
    if (isLoggedIn) {
      goHome()
    }
  })

  const handleLogin = async () => {
    await login({ username, phone, wechatId })
  }

  if (bootstrapping || preparing) {
    return (
      <View className='login-page login-page--loading'>
        <Text className='login-page__loading-text'>正在获取登录信息…</Text>
      </View>
    )
  }

  return (
    <View className='login-page'>
      <View className='login-page__card ck-card'>
        <Text className='login-page__brand'>miyf 厨房</Text>
        <Text className='login-page__title'>登录后继续</Text>
        <Text className='login-page__desc'>请填写用户名、手机号与微信号</Text>

        <View className='login-page__form'>
          <Input
            className='login-page__input'
            placeholder='用户名'
            maxlength={64}
            value={username}
            onInput={(e) => setUsername(e.detail.value)}
          />
          <Input
            className='login-page__input'
            type='number'
            placeholder='手机号'
            maxlength={11}
            value={phone}
            onInput={(e) => setPhone(e.detail.value)}
          />
          <Input
            className='login-page__input'
            placeholder='微信号'
            maxlength={64}
            value={wechatId}
            onInput={(e) => setWechatId(e.detail.value)}
          />
        </View>

        <Button className='ck-btn-primary login-page__btn' loading={loading} onClick={handleLogin}>
          进入厨房
        </Button>
      </View>
    </View>
  )
}
