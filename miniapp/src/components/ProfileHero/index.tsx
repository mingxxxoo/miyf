import type {ReactNode} from 'react'
import {Image, Text, View} from '@tarojs/components'
import './ProfileHero.scss'

export interface ProfileStat {
  value: string | number
  label: string
  onClick?: () => void
}

export interface ProfileHeroProps {
  avatarUrl?: string
  avatarLetter?: string
  displayName: string
  roleBadge?: string
  subtitle?: string
  /** 可选背景图（厨房封面等）；无则用渐变 */
  coverUrl?: string
  tone?: 'kitchen' | 'health'
  stats?: ProfileStat[]
  /** 覆盖名称区（编辑昵称等） */
  nameSlot?: ReactNode
  onAvatarClick?: () => void
}

/** 我的页头图：头像 / 身份 / 统计，与产品页解耦 */
export default function ProfileHero({
  avatarUrl,
  avatarLetter = '厨',
  displayName,
  roleBadge,
  subtitle,
  coverUrl,
  tone = 'kitchen',
  stats,
  nameSlot,
  onAvatarClick
}: ProfileHeroProps) {
  return (
    <View className={`profile-hero profile-hero--${tone}`}>
      {coverUrl ? (
        <Image className='profile-hero__bg' src={coverUrl} mode='aspectFill' lazyLoad />
      ) : (
        <View className='profile-hero__bg profile-hero__bg--grad' />
      )}
      <View className={`profile-hero__shade${coverUrl ? '' : ' profile-hero__shade--soft'}`} />
      <View className='profile-hero__body'>
        <View className='profile-hero__top'>
          <View className='profile-hero__avatar-wrap ck-pressable' onClick={onAvatarClick}>
            {avatarUrl ? (
              <Image className='profile-hero__avatar' src={avatarUrl} mode='aspectFill' />
            ) : (
              <View className='profile-hero__avatar profile-hero__avatar--empty'>
                <Text className='profile-hero__letter'>{avatarLetter.slice(0, 1)}</Text>
              </View>
            )}
            <Text className='profile-hero__avatar-tip'>{avatarUrl ? '换' : '上传'}</Text>
          </View>
          <View className='profile-hero__info'>
            {nameSlot || (
              <View className='profile-hero__name-row'>
                <Text className='profile-hero__name'>{displayName}</Text>
                {roleBadge ? <Text className='profile-hero__role'>{roleBadge}</Text> : null}
              </View>
            )}
            {subtitle ? <Text className='profile-hero__sub'>{subtitle}</Text> : null}
          </View>
        </View>
        {stats && stats.length > 0 ? (
          <View className='profile-hero__stats'>
            {stats.map((s) => (
              <View
                key={s.label}
                className='profile-hero__stat ck-pressable'
                onClick={s.onClick}
              >
                <Text className='profile-hero__stat-val'>{s.value}</Text>
                <Text className='profile-hero__stat-label'>{s.label}</Text>
              </View>
            ))}
          </View>
        ) : null}
      </View>
    </View>
  )
}
