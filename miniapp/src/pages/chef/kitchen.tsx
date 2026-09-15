import { View, Input, Button, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchMyKitchen, saveMyKitchen } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'

export default function ChefKitchenPage() {
  useChefWorkbench()
  const [name, setName] = useState('')
  const [intro, setIntro] = useState('')

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
    await saveMyKitchen({ name: name.trim(), intro: intro.trim() })
    Taro.showToast({ title: '已保存', icon: 'success' })
  }

  return (
    <View style={{ padding: '32px' }}>
      <Text>厨房名称</Text>
      <Input value={name} onInput={(e) => setName(e.detail.value)} placeholder='例如：老王的灶' />
      <Text>简介</Text>
      <Input value={intro} onInput={(e) => setIntro(e.detail.value)} placeholder='给食客看的介绍' />
      <Button type='primary' onClick={() => void save()} style={{ marginTop: '24px' }}>
        保存
      </Button>
    </View>
  )
}
