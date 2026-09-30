import {Image, Text, View} from '@tarojs/components'
import './ChefWorkbenchHero.scss'

export interface ChefWorkbenchStat {
  value: number | string
  label: string
  onClick?: () => void
}

export interface ChefWorkbenchHeroProps {
  greet: string
  kitchenName: string
  statusText: string
  /** 厨房封面绝对 URL；无则橙色渐变 */
  coverUrl?: string
  stats: ChefWorkbenchStat[]
}

/** 厨师工作台头图：封面 + 问候 + 四格统计 */
export default function ChefWorkbenchHero({
  greet,
  kitchenName,
  statusText,
  coverUrl,
  stats
}: ChefWorkbenchHeroProps) {
  return (
    <View className='chef-wb-hero'>
      {coverUrl ? (
        <Image className='chef-wb-hero__bg' src={coverUrl} mode='aspectFill' lazyLoad />
      ) : (
        <View className='chef-wb-hero__bg chef-wb-hero__bg--grad' />
      )}
      <View className={`chef-wb-hero__shade${coverUrl ? '' : ' chef-wb-hero__shade--soft'}`} />
      <Text className='chef-wb-hero__hello'>{greet}</Text>
      <View className='chef-wb-hero__kname'>
        <Text className='chef-wb-hero__name'>{kitchenName}</Text>
        <Text className='chef-wb-hero__verify'>{statusText}</Text>
      </View>
      <View className='chef-wb-hero__stats'>
        {stats.map((s) => (
          <View key={s.label} className='chef-wb-hero__stat ck-pressable' onClick={s.onClick}>
            <Text className='chef-wb-hero__stat-num'>{s.value}</Text>
            <Text className='chef-wb-hero__stat-label'>{s.label}</Text>
          </View>
        ))}
      </View>
    </View>
  )
}
