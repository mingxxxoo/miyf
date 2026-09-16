import { View, Text } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import { fetchMyKitchen } from '@/api/kitchen'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import { useUserStore } from '@/stores/userStore'
import { requestChefOrderSubscribe } from '@/utils/wxSubscribe'
import './chef.scss'

const MENUS = [
  { label: '厨房资料', desc: '名称与简介', url: '/pages/chef/kitchen', icon: '🏠' },
  { label: '分类管理', desc: '维护本厨菜品分类', url: '/pages/chef/categories', icon: '📂' },
  { label: '菜品管理', desc: '上传图片、推荐、提审上架', url: '/pages/chef/dishes', icon: '🍽️' },
  { label: '菜谱管理', desc: '食材与步骤', url: '/pages/chef/recipes', icon: '📖' },
  { label: '邀请码', desc: '邀请食客加入厨房', url: '/pages/chef/invite', icon: '🔗' },
  { label: '食客申请', desc: '通过或拒绝绑定', url: '/pages/chef/bindings', icon: '👥' },
  { label: '处理预约', desc: '确认、备餐、完成', url: '/pages/chef/orders', icon: '📋' }
]

export default function ChefHomePage() {
  useChefWorkbench()
  const user = useUserStore((s) => s.user)
  const [kitchenName, setKitchenName] = useState('我的厨房')

  useDidShow(() => {
    void fetchMyKitchen().then((k) => {
      if (k?.name) setKitchenName(k.name)
    })
    // 进入工作台时尝试补充「新预约」订阅额度（用户曾拒绝则静默失败）
    void requestChefOrderSubscribe()
  })

  return (
    <View className='chef-page'>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>{kitchenName}</Text>
        <Text className='chef-page__sub'>
          {user?.nickname || '厨师'}的工作台 · 先建厨房，再上传菜品提审。无价格、无支付。
        </Text>
      </View>
      <View className='chef-page__nav'>
        {MENUS.map((m) => (
          <View
            key={m.url}
            className='chef-page__nav-card ck-pressable'
            onClick={() => {
              const go = () => Taro.navigateTo({ url: m.url })
              // 点「处理预约」时顺带用用户手势拉起订阅授权
              if (m.url.includes('/chef/orders')) {
                void requestChefOrderSubscribe().finally(go)
              } else {
                go()
              }
            }}
          >
            <View className='chef-page__nav-icon'>
              <Text>{m.icon}</Text>
            </View>
            <View className='chef-page__nav-body'>
              <Text className='chef-page__nav-title'>{m.label}</Text>
              <Text className='chef-page__nav-desc'>{m.desc}</Text>
            </View>
            <Text className='chef-page__nav-arrow'>›</Text>
          </View>
        ))}
      </View>
    </View>
  )
}
