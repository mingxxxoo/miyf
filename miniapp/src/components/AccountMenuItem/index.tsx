import {Text, View} from '@tarojs/components'
import MiniIcon, {type MiniIconName} from '@/components/MiniIcon'
import './AccountMenuItem.scss'

export interface AccountMenuItemProps {
  icon: MiniIconName
  title: string
  hint?: string
  danger?: boolean
  iconBg?: string
  onClick?: () => void
}

/** 账户列表行：身份切换 / 预约 / 退出等 */
export default function AccountMenuItem({
  icon,
  title,
  hint,
  danger = false,
  iconBg,
  onClick
}: AccountMenuItemProps) {
  return (
    <View
      className={`account-menu-item${danger ? ' account-menu-item--danger' : ''} ck-pressable`}
      onClick={onClick}
    >
      <View
        className='account-menu-item__ico'
        style={iconBg ? { background: iconBg } : undefined}
      >
        <MiniIcon name={icon} size='sm' tone={danger ? 'muted' : 'default'} />
      </View>
      <View className='account-menu-item__body'>
        <Text className='account-menu-item__title'>{title}</Text>
        {hint ? <Text className='account-menu-item__hint'>{hint}</Text> : null}
      </View>
      <Text className='account-menu-item__arrow'>›</Text>
    </View>
  )
}
