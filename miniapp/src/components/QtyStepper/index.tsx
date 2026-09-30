import {Text, View} from '@tarojs/components'
import './QtyStepper.scss'

export interface QtyStepperProps {
  value: number
  disabled?: boolean
  size?: 'sm' | 'md'
  /** 为 0 时只显示加号（点餐场景） */
  showZeroAsAdd?: boolean
  onInc: () => void
  onDec: () => void
}

/** 数量步进：菜品卡 / 草稿行 / 底部预约条共用 */
export default function QtyStepper({
  value,
  disabled = false,
  size = 'md',
  showZeroAsAdd = true,
  onInc,
  onDec
}: QtyStepperProps) {
  const qty = Math.max(0, value)
  const root = `qty-stepper qty-stepper--${size}${disabled ? ' qty-stepper--disabled' : ''}`

  if (showZeroAsAdd && qty <= 0) {
    return (
      <View className={root} onClick={(e) => e.stopPropagation?.()}>
        <View
          className={`qty-stepper__add${disabled ? ' is-disabled' : ''}`}
          onClick={() => {
            if (!disabled) onInc()
          }}
        >
          <Text>＋</Text>
        </View>
      </View>
    )
  }

  return (
    <View className={root} onClick={(e) => e.stopPropagation?.()}>
      <View className='qty-stepper__btn' onClick={() => !disabled && onDec()}>
        <Text>−</Text>
      </View>
      <Text className='qty-stepper__num'>{qty}</Text>
      <View
        className={`qty-stepper__btn qty-stepper__btn--plus${disabled ? ' is-disabled' : ''}`}
        onClick={() => !disabled && onInc()}
      >
        <Text>＋</Text>
      </View>
    </View>
  )
}
