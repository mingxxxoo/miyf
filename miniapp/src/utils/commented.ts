import Taro from '@tarojs/taro'

const STORAGE_KEY = 'miyf_commented_order_dishes'

function readMap(): Record<string, true> {
  try {
    const raw = Taro.getStorageSync(STORAGE_KEY)
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      return raw as Record<string, true>
    }
  } catch {
    // ignore
  }
  return {}
}

function keyOf(orderId: string, dishId: string): string {
  return `${orderId}:${dishId}`
}

/** 本地标记：该预约的菜品已评价（后端一单一菜一次，用于详情页展示） */
export function markCommented(orderId: string, dishId: string): void {
  if (!orderId || !dishId) return
  const map = readMap()
  map[keyOf(orderId, dishId)] = true
  try {
    Taro.setStorageSync(STORAGE_KEY, map)
  } catch {
    // ignore
  }
}

export function isCommented(orderId: string, dishId: string): boolean {
  if (!orderId || !dishId) return false
  return Boolean(readMap()[keyOf(orderId, dishId)])
}
