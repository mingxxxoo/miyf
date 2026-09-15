import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'
import { activateOrSwitchRole } from '@/api/kitchen'
import './select.scss'

export default function RoleSelectPage() {
  const user = useUserStore((s) => s.user)
  const refreshProfile = useUserStore((s) => s.refreshProfile)

  const pick = async (role: 'CHEF' | 'DINER') => {
    try {
      await activateOrSwitchRole(role, user)
      await refreshProfile()
      Taro.showToast({ title: '已切换', icon: 'success' })
      if (role === 'CHEF') {
        Taro.redirectTo({ url: '/pages/chef/index' })
      } else {
        Taro.switchTab({ url: '/pages/index/index' })
      }
    } catch {
      // toast from request
    }
  }

  const hasBoth = Boolean(user?.chef && user?.diner)

  return (
    <View className='role-select'>
      <Text className='role-select__title'>欢迎来到胡闹厨房</Text>
      <Text className='role-select__sub'>
        {hasBoth
          ? '切换当前工作台。同一账号可兼厨师与食客，这里没有公开菜单，也没有价格和支付。'
          : '先选身份，再开始做饭或吃饭。这里没有公开菜单，也没有价格和支付。'}
      </Text>
      <View className='role-select__card' onClick={() => void pick('CHEF')}>
        <Text className='role-select__name'>我是干活的</Text>
        <Text className='role-select__desc'>
          {user?.chef ? '切换到厨师工作台' : '创建厨房、上传菜品、处理预约'}
        </Text>
      </View>
      <View className='role-select__card' onClick={() => void pick('DINER')}>
        <Text className='role-select__name'>我是吃饭的</Text>
        <Text className='role-select__desc'>
          {user?.diner ? '切换到食客菜单' : '用邀请码加入一位厨师的厨房后点餐'}
        </Text>
      </View>
    </View>
  )
}
