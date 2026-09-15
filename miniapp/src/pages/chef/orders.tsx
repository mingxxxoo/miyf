import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchChefOrders, updateChefOrderStatus } from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import StatusBadge from '@/components/StatusBadge'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

const NEXT: Record<string, { status: string; label: string }> = {
  PENDING: { status: 'CONFIRMED', label: '确认预约' },
  CONFIRMED: { status: 'PREPARING', label: '开始备餐' },
  PREPARING: { status: 'READY', label: '可以取餐' },
  READY: { status: 'COMPLETED', label: '完成' }
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
    <View className='chef-page'>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>处理预约</Text>
        <Text className='chef-page__sub'>按流程推进：确认 → 备餐 → 取餐 → 完成</Text>
      </View>
      {list.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='暂无预约' description='食客下单后会出现在这里' />
        </View>
      ) : (
        list.map((o) => {
          const next = NEXT[o.status]
          return (
            <View key={o.id} className='chef-page__card'>
              <View className='chef-page__row'>
                <Text className='chef-dish__name'>{o.orderNo}</Text>
                <StatusBadge status={o.status} />
              </View>
              {next && (
                <View className='chef-page__actions'>
                  <Button
                    className='chef-page__action'
                    size='mini'
                    onClick={() => void updateChefOrderStatus(o.id, next.status).then(load)}
                  >
                    {next.label}
                  </Button>
                </View>
              )}
            </View>
          )
        })
      )}
    </View>
  )
}
