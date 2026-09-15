import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchMyKitchen } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import { useUserStore } from '@/stores/userStore'

export default function ChefHomePage() {
  useChefWorkbench()
  const user = useUserStore((s) => s.user)
  const [kitchenName, setKitchenName] = useState('我的厨房')

  useDidShow(() => {
    void fetchMyKitchen().then((k) => {
      if (k?.name) setKitchenName(k.name)
    })
  })

  const go = (url: string) => Taro.navigateTo({ url })

  return (
    <View style={{ padding: '40px 32px' }}>
      <Text style={{ display: 'block', fontSize: '40px', fontWeight: 700 }}>{kitchenName}</Text>
      <Text style={{ display: 'block', color: '#888', margin: '12px 0 32px' }}>
        {user?.nickname || '厨师'}，先建厨房，再上传菜品提交审核。无价格、无支付。
      </Text>
      {[
        ['厨房资料', '/pages/chef/kitchen'],
        ['菜品管理', '/pages/chef/dishes'],
        ['邀请码', '/pages/chef/invite'],
        ['食客申请', '/pages/chef/bindings'],
        ['处理预约', '/pages/chef/orders']
      ].map(([label, url]) => (
        <View
          key={url}
          style={{ background: '#fff', padding: '28px', borderRadius: '20px', marginBottom: '16px' }}
          onClick={() => go(url)}
        >
          <Text>{label}</Text>
        </View>
      ))}
    </View>
  )
}
