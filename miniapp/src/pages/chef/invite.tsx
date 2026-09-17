import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchInvite, rotateInvite, type InviteVo } from '@/api/kitchen'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

export default function ChefInvitePage() {
  useChefWorkbench()
  const [invite, setInvite] = useState<InviteVo | null>(null)

  useDidShow(() => {
    void fetchInvite()
      .then(setInvite)
      .catch(() => Taro.showToast({ title: '请先创建厨房', icon: 'none' }))
  })

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>邀请食客</Text>
        <Text className='chef-page__sub'>把邀请码或链接发给食客，对方申请并由你确认后才能看到菜单。</Text>
      </View>
      <View className='chef-page__card'>
        <Text className='chef-page__label' style={{ textAlign: 'center' }}>
          当前邀请码
        </Text>
        <Text className='chef-page__code'>{invite?.code || '——'}</Text>
        {invite?.joinPath && <Text className='chef-page__link'>{invite.joinPath}</Text>}
        <Button
          className='ck-btn-primary chef-page__btn'
          onClick={() => {
            if (!invite?.code) return
            void Taro.setClipboardData({ data: invite.code })
          }}
        >
          复制邀请码
        </Button>
        <Button
          className='chef-page__btn-ghost'
          onClick={() => {
            if (!invite?.joinPath) return
            void Taro.setClipboardData({ data: invite.joinPath })
          }}
        >
          复制邀请链接
        </Button>
        <Button
          className='chef-page__btn-ghost'
          onClick={async () => {
            setInvite(await rotateInvite())
            Taro.showToast({ title: '已换新码', icon: 'success' })
          }}
        >
          作废并换码
        </Button>
      </View>
    </View>
  )
}
