import {Text, View} from '@tarojs/components'
import Taro from '@tarojs/taro'
import {useState} from 'react'
import MiniIcon from '@/components/MiniIcon'
import {useUserStore} from '@/stores/userStore'
import {activateOrSwitchRole} from '@/api/kitchen'
import './select.scss'

type Role = 'CHEF' | 'DINER'

export default function RoleSelectPage() {
  const user = useUserStore((s) => s.user)
  const refreshProfile = useUserStore((s) => s.refreshProfile)
  const [picked, setPicked] = useState<Role>(
    user?.activeRole === 'CHEF' ? 'CHEF' : 'DINER'
  )
  const [busy, setBusy] = useState(false)

  const confirm = async () => {
    if (busy) return
    setBusy(true)
    try {
      await activateOrSwitchRole(picked, user)
      await refreshProfile()
      Taro.showToast({ title: '已确认身份', icon: 'success' })
      if (picked === 'CHEF') {
        Taro.redirectTo({ url: '/pages/chef/index' })
      } else {
        Taro.switchTab({ url: '/pages/index/index' })
      }
    } catch {
      // toast from request
    } finally {
      setBusy(false)
    }
  }

  const hasBoth = Boolean(user?.chef && user?.diner)

  return (
    <View className='role-select'>
      <Text className='role-select__title'>选择身份</Text>
      <Text className='role-select__sub'>
        {hasBoth
          ? '同一账号可切换。先选一个工作台开始。'
          : '先选身份，再开始做饭或吃饭。这里没有公开菜单，也没有价格和支付。'}
      </Text>

      <View
        className={`role-select__card${picked === 'DINER' ? ' role-select__card--on' : ''}`}
        onClick={() => setPicked('DINER')}
      >
        <View className='role-select__ico role-select__ico--diner'>
          <MiniIcon name='dish' size='md' tone='primary' />
        </View>
        <View className='role-select__copy'>
          <Text className='role-select__name'>我是吃饭的</Text>
          <Text className='role-select__desc'>
            {user?.diner ? '切换到食客菜单' : '用邀请码加入厨房，浏览在售菜，预约今天吃什么。'}
          </Text>
        </View>
      </View>

      <View
        className={`role-select__card${picked === 'CHEF' ? ' role-select__card--on' : ''}`}
        onClick={() => setPicked('CHEF')}
      >
        <View className='role-select__ico role-select__ico--chef'>
          <MiniIcon name='kitchen' size='md' tone='primary' />
        </View>
        <View className='role-select__copy'>
          <Text className='role-select__name'>我是干活的</Text>
          <Text className='role-select__desc'>
            {user?.chef ? '切换到厨师工作台' : '经营一个厨房：菜品、预约处理、邀请食客。'}
          </Text>
        </View>
      </View>

      <View
        className={`role-select__confirm${busy ? ' is-off' : ''}`}
        onClick={() => void confirm()}
      >
        <Text>{busy ? '处理中…' : '确认身份'}</Text>
      </View>
    </View>
  )
}
