import { View, Text } from '@tarojs/components'
import {
  PRODUCT_META,
  useProductStore,
  type ProductCode
} from '@/stores/productStore'
import './ServiceSwitcher.scss'

interface ServiceSwitcherProps {
  /** 紧凑样式，适合英雄区 / 卡片内 */
  compact?: boolean
  /** 切换后是否自动跳转对应首页，默认 true */
  navigate?: boolean
  className?: string
}

const OPTIONS: ProductCode[] = ['kitchen', 'health']

export default function ServiceSwitcher({
  compact = false,
  navigate = true,
  className = ''
}: ServiceSwitcherProps) {
  const product = useProductStore((s) => s.product)
  const switchTo = useProductStore((s) => s.switchTo)
  const setProduct = useProductStore((s) => s.setProduct)

  const onSelect = (code: ProductCode) => {
    if (code === product) return
    if (navigate) {
      switchTo(code)
    } else {
      setProduct(code)
    }
  }

  return (
    <View
      className={`service-switcher ${compact ? 'service-switcher--compact' : ''} ${className}`.trim()}
    >
      {OPTIONS.map((code) => {
        const meta = PRODUCT_META[code]
        const active = product === code
        return (
          <View
            key={code}
            className={`service-switcher__item service-switcher__item--${code} ${
              active ? 'service-switcher__item--active' : ''
            }`}
            onClick={() => onSelect(code)}
          >
            <Text className='service-switcher__label'>{meta.label}</Text>
          </View>
        )
      })}
    </View>
  )
}
