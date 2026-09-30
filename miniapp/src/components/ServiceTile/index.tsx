import {Text, View} from '@tarojs/components'
import MiniIcon, {type MiniIconName} from '@/components/MiniIcon'
import './ServiceTile.scss'

export type ServiceTileVariant = 'chef' | 'health' | 'default'

export interface ServiceTileProps {
  variant?: ServiceTileVariant
  icon: MiniIconName
  title: string
  desc: string
  onClick?: () => void
}

/** 服务入口瓷砖：厨房工作台 / 健康等，可复用于我的与引导页 */
export default function ServiceTile({
  variant = 'default',
  icon,
  title,
  desc,
  onClick
}: ServiceTileProps) {
  return (
    <View
      className={`service-tile service-tile--${variant} ck-pressable`}
      onClick={onClick}
    >
      <View className='service-tile__ico'>
        <MiniIcon
          name={icon}
          size='md'
          tone={variant === 'health' ? 'mint' : variant === 'chef' ? 'primary' : 'default'}
        />
      </View>
      <Text className='service-tile__title'>{title}</Text>
      <Text className='service-tile__desc'>{desc}</Text>
    </View>
  )
}
