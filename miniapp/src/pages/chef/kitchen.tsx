import { View, Text, Input, Textarea, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchMyKitchen, saveMyKitchen } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

export default function ChefKitchenPage() {
  useChefWorkbench()
  const [name, setName] = useState('')
  const [intro, setIntro] = useState('')
  const [saving, setSaving] = useState(false)

  useDidShow(() => {
    void fetchMyKitchen().then((k) => {
      if (!k) return
      setName(k.name || '')
      setIntro(k.intro || '')
    })
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
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>厨房资料</Text>
        <Text className='chef-page__sub'>食客加入后会看到这些介绍</Text>
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
          保存
        </Button>
      </View>
    </View>
  )
}
