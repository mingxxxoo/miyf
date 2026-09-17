import { View, Text, Input, Textarea, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchInvite, fetchMyKitchen, saveMyKitchen, type InviteVo } from '@/api/kitchen'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

export default function ChefKitchenPage() {
  useChefWorkbench()
  const [name, setName] = useState('')
  const [intro, setIntro] = useState('')
  const [invite, setInvite] = useState<InviteVo | null>(null)
  const [saving, setSaving] = useState(false)

  useDidShow(() => {
    void fetchMyKitchen().then((k) => {
      if (!k) return
      setName(k.name || '')
      setIntro(k.intro || '')
    })
    void fetchInvite()
      .then(setInvite)
      .catch(() => setInvite(null))
  })

  const save = async () => {
    if (!name.trim()) {
      Taro.showToast({ title: '请填写厨房名', icon: 'none' })
      return
    }
    if (saving) return
    setSaving(true)
    try {
      await saveMyKitchen({ name: name.trim(), intro: intro.trim() })
      Taro.showToast({ title: '已保存', icon: 'success' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>
      <View className='chef-page__cover'>
        <View className='chef-page__big-avatar'>
          <Text>🍳</Text>
        </View>
      </View>

      <View className='chef-page__card'>
        <View className='chef-page__field'>
          <Text className='chef-page__label'>厨房名称</Text>
          <Input
            className='chef-page__input'
            value={name}
            placeholder='例如：老王的灶'
            onInput={(e) => setName(e.detail.value)}
          />
        </View>
        <View className='chef-page__field'>
          <Text className='chef-page__label'>简介</Text>
          <Textarea
            className='chef-page__textarea'
            value={intro}
            placeholder='风格、擅长菜系、取餐说明等'
            maxlength={200}
            onInput={(e) => setIntro(e.detail.value)}
          />
        </View>
        <Button className='ck-btn-primary chef-page__btn' loading={saving} onClick={() => void save()}>
          保存资料
        </Button>
      </View>

      {invite?.code ? (
        <View className='chef-page__invite-mini'>
          <View style={{ flex: 1 }}>
            <Text className='chef-page__invite-code'>{invite.code}</Text>
            <Text className='chef-page__oc-time' style={{ display: 'block', marginTop: '6px' }}>
              邀请码 · 可复制分享给食客
            </Text>
          </View>
          <Button
            className='chef-page__btn-xs chef-page__btn-xs--solid'
            size='mini'
            onClick={() => void Taro.setClipboardData({ data: invite.code })}
          >
            复制
          </Button>
        </View>
      ) : null}
    </View>
  )
}
