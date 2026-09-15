import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchChefOrders, updateChefOrderStatus } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'

const NEXT: Record<string, string> = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'PREPARING',
  PREPARING: 'READY',
  READY: 'COMPLETED'
}

export default function ChefOrdersPage() {
  useChefWorkbench()
  const [list, setList] = useState<{ id: string; orderNo: string; status: string }[]>([])

  const load = async () => {
    const page = await fetchChefOrders()
    setList(page.records || [])
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  return (
    <View style={{ padding: '32px' }}>
      {list.map((o) => (
        <View key={o.id} style={{ background: '#fff', padding: '24px', borderRadius: '16px', marginBottom: '16px' }}>
          <Text style={{ display: 'block' }}>{o.orderNo}</Text>
          <Text style={{ display: 'block', color: '#888' }}>{o.status}</Text>
          {NEXT[o.status] && (
            <Button size='mini' onClick={() => void updateChefOrderStatus(o.id, NEXT[o.status]).then(load)}>
              下一步：{NEXT[o.status]}
            </Button>
          )}
        </View>
      ))}
      {list.length === 0 && <Text>暂无预约</Text>}
    </View>
  )
}
