import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import {
  approveBinding,
  fetchChefBindings,
  rejectBinding,
  unbindBinding,
  type BindingVo
} from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'

export default function ChefBindingsPage() {
  useChefWorkbench()
  const [list, setList] = useState<BindingVo[]>([])

  const load = async () => {
    const page = await fetchChefBindings()
    setList(page.records || [])
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const unbind = async (b: BindingVo) => {
    const tip =
      b.status === 'PENDING'
        ? `取消「${b.dinerNickname || '食客'}」的待确认申请？`
        : `解除与「${b.dinerNickname || '食客'}」的绑定？有进行中的预约时无法解除。`
    const res = await Taro.showModal({ title: '确认', content: tip })
    if (!res.confirm) return
    await unbindBinding(b.id)
    Taro.showToast({ title: '已处理', icon: 'success' })
    void load()
  }

  return (
    <View style={{ padding: '32px' }}>
      {list.map((b) => (
        <View key={b.id} style={{ background: '#fff', padding: '24px', borderRadius: '16px', marginBottom: '16px' }}>
          <Text style={{ display: 'block' }}>{b.dinerNickname || b.id}</Text>
          <Text style={{ display: 'block', color: '#888' }}>{b.status}</Text>
          {b.status === 'PENDING' && (
            <View>
              <Button size='mini' onClick={() => void approveBinding(b.id).then(load)}>
                通过
              </Button>
              <Button size='mini' onClick={() => void rejectBinding(b.id, '暂不接收').then(load)}>
                拒绝
              </Button>
              <Button size='mini' onClick={() => void unbind(b)}>
                取消申请
              </Button>
            </View>
          )}
          {b.status === 'BOUND' && (
            <Button size='mini' onClick={() => void unbind(b)}>
              解除绑定
            </Button>
          )}
        </View>
      ))}
      {list.length === 0 && <Text>还没有申请</Text>}
    </View>
  )
}
