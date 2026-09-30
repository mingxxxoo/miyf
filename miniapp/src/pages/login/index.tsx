import {Button, Image, Input, Text, View} from '@tarojs/components'
import Taro, {useDidShow} from '@tarojs/taro'
import {useState} from 'react'
import {useUserStore} from '@/stores/userStore'
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
      <View className='login-page__brand'>
        <View className='login-page__logo'>
          <Text>m</Text>
        </View>
        <Text className='login-page__name'>miyf</Text>
        <Text className='login-page__tagline'>
          家庭厨房预约 · 健康数据{'\n'}今天吃什么，不外卖
        </Text>
        <View className='login-page__rooms'>
          <Text className='login-page__room'>胡闹厨房</Text>
          <Text className='login-page__room login-page__room--health'>胡闹健康</Text>
        </View>
      </View>

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

      <View className='login-page__foot'>
        <Button
          className='ck-btn-primary login-page__btn'
          loading={loading}
          disabled={!canSubmit}
          onClick={handleLogin}
        >
          {nickname.trim() ? '微信授权登录' : '请先填写昵称'}
        </Button>
        <Text className='login-page__hint'>登录即表示同意用户协议与隐私政策</Text>
      </View>
    </View>
  )
}
