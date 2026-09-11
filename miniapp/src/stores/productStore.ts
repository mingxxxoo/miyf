import Taro from '@tarojs/taro'
import { create } from 'zustand'

export type ProductCode = 'kitchen' | 'health'

const PRODUCT_KEY = 'miyf_active_product'

const TAB_ROUTES = new Set([
  'pages/index/index',
  'pages/category/index',
  'pages/order/index',
  'pages/user/index'
])

export const PRODUCT_META: Record<
  ProductCode,
  {
    code: ProductCode
    label: string
    shortLabel: string
    brand: string
    homeUrl: string
    homeIsTab: boolean
    accent: 'kitchen' | 'health'
  }
> = {
  kitchen: {
    code: 'kitchen',
    label: '厨房服务',
    shortLabel: '厨房',
    brand: 'miyf 厨房',
    homeUrl: '/pages/index/index',
    homeIsTab: true,
    accent: 'kitchen'
  },
  health: {
    code: 'health',
    label: '健康服务',
    shortLabel: '健康',
    brand: 'miyf 健康',
    homeUrl: '/pages/health/index',
    homeIsTab: false,
    accent: 'health'
  }
}

interface ProductState {
  product: ProductCode
  hydrate: () => void
  setProduct: (product: ProductCode) => void
  /** 切换服务并跳转到对应首页 */
  switchTo: (product: ProductCode) => void
  goProductHome: () => void
}

function currentRoute(): string {
  const pages = Taro.getCurrentPages()
  const cur = pages[pages.length - 1] as { route?: string } | undefined
  return cur?.route || ''
}

function routeInStack(target: string): boolean {
  return Taro.getCurrentPages().some((p) => (p as { route?: string }).route === target)
}

function readStoredProduct(): ProductCode {
  const raw = Taro.getStorageSync(PRODUCT_KEY) as string
  return raw === 'health' ? 'health' : 'kitchen'
}

export const useProductStore = create<ProductState>((set, get) => ({
  product: 'kitchen',

  hydrate: () => {
    set({ product: readStoredProduct() })
  },

  setProduct: (product) => {
    Taro.setStorageSync(PRODUCT_KEY, product)
    set({ product })
    const meta = PRODUCT_META[product]
    try {
      Taro.setNavigationBarTitle({ title: meta.brand })
    } catch {
      // 部分页面未就绪时忽略
    }
  },

  goProductHome: () => {
    const meta = PRODUCT_META[get().product]
    const route = currentRoute()
    const target = meta.homeUrl.replace(/^\//, '')
    if (route === target) return
    if (meta.homeIsTab) {
      Taro.switchTab({ url: meta.homeUrl })
    } else if (routeInStack(target)) {
      Taro.redirectTo({ url: meta.homeUrl })
    } else {
      Taro.navigateTo({ url: meta.homeUrl })
    }
  },

  switchTo: (product) => {
    const meta = PRODUCT_META[product]
    get().setProduct(product)
    try {
      Taro.vibrateShort({ type: 'light' })
    } catch {
      // 部分端不支持震动
    }
    const route = currentRoute()
    const target = meta.homeUrl.replace(/^\//, '')
    if (route === target) return
    if (meta.homeIsTab) {
      // switchTab 会关闭非 tab 页（含健康页），回到厨房主流程
      Taro.switchTab({ url: meta.homeUrl })
    } else if (TAB_ROUTES.has(route)) {
      // 从 tab 页进入健康：navigateTo
      Taro.navigateTo({ url: meta.homeUrl })
    } else {
      // 从非 tab 栈页（菜品/预约详情等）切到健康：redirectTo 避免堆叠
      Taro.redirectTo({ url: meta.homeUrl })
    }
  }
}))
