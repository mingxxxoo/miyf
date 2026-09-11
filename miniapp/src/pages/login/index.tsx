import { View, Text, Button, Input, Image } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function LoginPage() {
  const { loading, bootstrapping, isLoggedIn, login, goHome } = useUserStore()
  const [nickname, setNickname] = useState('')
  const [avatarPath, setAvatarPath] = useState('')

  useDidShow(() => {
    if (isLoggedIn) {
      goHome()
    }
  })

  const onChooseAvatar = (e: { detail: { avatarUrl?: string } }) => {
    const url = e.detail?.avatarUrl || ''
    if (url) setAvatarPath(url)
  }

  const handleLogin = () => {
    const name = nickname.trim()
    if (!name) {
      Taro.showToast({ title: '请填写微信昵称', icon: 'none' })
      return
    }
    void login({
      nickname: name,
      avatarUrl: avatarPath || undefined
    })
  }

  if (bootstrapping) {
    return (
      <View className='login-page login-page--loading'>
        <Text className='login-page__loading-text'>正在恢复登录…</Text>
      </View>
    )
  }

  const canSubmit = Boolean(nickname.trim()) && !loading

  return (
    <View className='login-page'>
      <View className='login-page__card ck-card'>
        <Text className='login-page__brand'>miyf</Text>
        <Text className='login-page__title'>授权登录</Text>
        <Text className='login-page__desc'>
          请填写微信昵称（头像可选），我们仅用于展示你的个人资料
        </Text>

        <View className='login-page__profile'>
          <Button
            className='login-page__avatar-btn'
            openType='chooseAvatar'
            onChooseAvatar={onChooseAvatar}
          >
            {avatarPath ? (
              <Image className='login-page__avatar' src={avatarPath} mode='aspectFill' />
            ) : (
              <Text className='login-page__avatar-placeholder'>选头像</Text>
            )}
          </Button>
          <View className='login-page__nickname-wrap'>
            <Text className='login-page__field-label'>微信昵称</Text>
            <Input
              className='login-page__nickname'
              type='nickname'
              placeholder='点击获取微信昵称'
              value={nickname}
              onInput={(e) => setNickname(e.detail.value)}
              onBlur={(e) => setNickname(e.detail.value)}
            />
          </View>
        </View>

        <View className='login-page__switch'>
          <ServiceSwitcher compact navigate={false} />
        </View>
        <Text className='login-page__switch-hint'>登录后将进入所选服务</Text>

        <Button
          className='ck-btn-primary login-page__btn login-page__btn--wx'
          loading={loading}
          disabled={!canSubmit}
          onClick={handleLogin}
        >
          {nickname.trim() ? '授权并登录' : '请先填写昵称'}
        </Button>
        <Button
          className='ck-btn-secondary login-page__btn'
          disabled={loading}
          onClick={() => {
            useProductStore.getState().setProduct('kitchen')
            Taro.switchTab({ url: '/pages/index/index' })
          }}
        >
          先逛逛菜品
        </Button>
      </View>
    </View>
  )
}
