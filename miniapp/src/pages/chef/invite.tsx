import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchInvite, rotateInvite, type InviteVo } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'

export default function ChefInvitePage() {
  useChefWorkbench()
  const [invite, setInvite] = useState<InviteVo | null>(null)

  useDidShow(() => {
    void fetchInvite()
      .then(setInvite)
      .catch(() => Taro.showToast({ title: '请先创建厨房', icon: 'none' }))
  })

  return (
    <View style={{ padding: '48px 32px' }}>
      <Text style={{ display: 'block', marginBottom: '16px' }}>
        把邀请码或链接发给食客，确认后才能看到你的菜单。
      </Text>
      <Text style={{ display: 'block', fontSize: '56px', fontWeight: 800, letterSpacing: '8px' }}>
        {invite?.code || '——'}
      </Text>
      {invite?.joinPath && (
        <Text style={{ display: 'block', color: '#888', marginTop: '16px', wordBreak: 'break-all' }}>
          链接：{invite.joinPath}
        </Text>
      )}
      <Button
        style={{ marginTop: '24px' }}
        onClick={() => {
          if (!invite?.code) return
          void Taro.setClipboardData({ data: invite.code })
        }}
      >
        复制邀请码
      </Button>
      <Button
        onClick={() => {
          if (!invite?.joinPath) return
          void Taro.setClipboardData({ data: invite.joinPath })
        }}
      >
        复制邀请链接
      </Button>
      <Button
        onClick={async () => {
          setInvite(await rotateInvite())
          Taro.showToast({ title: '已换新码', icon: 'success' })
        }}
      >
        作废并换码
      </Button>
    </View>
  )
}
