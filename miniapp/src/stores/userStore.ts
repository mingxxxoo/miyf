import Taro from '@tarojs/taro'
import { create } from 'zustand'
import {
  fetchMyProfile,
  updateMyProfile,
  uploadMyAvatar,
  wxLogin,
  type WxLoginProfile
} from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import type { User } from '@/types'

const USER_KEY = 'miyf_user'
const LEGACY_USER_KEY = 'ck_user'
const LEGACY_PROFILE_KEY = 'ck_login_profile'
const PROFILE_KEY = 'miyf_login_profile'

interface UserState {
  user: User | null
  loading: boolean
  bootstrapping: boolean
  isLoggedIn: boolean
  hydrate: () => void
  bootstrap: () => Promise<boolean>
  login: (profile?: WxLoginProfile) => Promise<boolean>
  refreshProfile: () => Promise<User | null>
  updateProfile: (payload: { nickname?: string; avatarUrl?: string }) => Promise<User | null>
  uploadAvatar: (filePath: string) => Promise<User | null>
  requireLogin: () => Promise<boolean>
  setUser: (user: User | null) => void
  logout: () => void
  goHome: () => void
  goLogin: () => void
}

function persistUser(user: User | null) {
  if (user) {
    Taro.setStorageSync(USER_KEY, user)
    try {
      Taro.removeStorageSync(LEGACY_USER_KEY)
    } catch {
      // ignore
    }
  } else {
    Taro.removeStorageSync(USER_KEY)
    try {
      Taro.removeStorageSync(LEGACY_USER_KEY)
    } catch {
      // ignore
    }
  }
}

function clearLegacyProfileCache() {
  try {
    Taro.removeStorageSync(PROFILE_KEY)
  } catch {
    // ignore
  }
  try {
    Taro.removeStorageSync(LEGACY_PROFILE_KEY)
  } catch {
    // ignore
  }
}

function currentRoute(): string {
  const pages = Taro.getCurrentPages()
  const cur = pages[pages.length - 1] as { route?: string } | undefined
  return cur?.route || ''
}

async function exchangeWxCode(profile?: WxLoginProfile): Promise<{ user: User; token: string; expireSeconds?: number }> {
  const { code } = await Taro.login()
  if (!code) {
    throw new Error('empty wx login code')
  }
  const { login, user } = await wxLogin(code, profile)
  return { user, token: login.token, expireSeconds: login.expireSeconds }
}

export const useUserStore = create<UserState>((set, get) => ({
  user: null,
  loading: false,
  bootstrapping: true,
  isLoggedIn: false,

  hydrate: () => {
    const token = getToken()
    let cached = Taro.getStorageSync(USER_KEY) as User | ''
    if (!cached || typeof cached !== 'object') {
      cached = Taro.getStorageSync(LEGACY_USER_KEY) as User | ''
      if (cached && typeof cached === 'object') {
        persistUser(cached)
      }
    }
    if (token && cached && typeof cached === 'object') {
      set({ user: cached, isLoggedIn: true })
    } else if (token) {
      // 有 token 无缓存：保留 token，标记已登录，由 bootstrap/refresh 补资料
      set({ user: null, isLoggedIn: true })
    } else {
      clearToken()
      persistUser(null)
      set({ user: null, isLoggedIn: false })
    }
  },

  goHome: () => {
    const product = useProductStore.getState().product
    const meta = PRODUCT_META[product]
    const route = currentRoute()
    const target = meta.homeUrl.replace(/^\//, '')
    if (route === target) return
    if (meta.homeIsTab) {
      Taro.switchTab({ url: meta.homeUrl })
    } else {
      Taro.reLaunch({ url: meta.homeUrl })
    }
  },

  goLogin: () => {
    const route = currentRoute()
    if (route === 'pages/login/index') return
    Taro.reLaunch({ url: '/pages/login/index' })
  },

  /**
   * 仅恢复本地登录态；无 token 时以游客进入厨房首页，不再自动跳登录。
   */
  bootstrap: async () => {
    set({ bootstrapping: true })
    get().hydrate()
    if (get().isLoggedIn) {
      set({ bootstrapping: false })
      // 后台刷新资料（含头像签名 URL），失败不打断
      void get().refreshProfile()
      get().goHome()
      return true
    }
    set({ loading: false, bootstrapping: false })
    useProductStore.getState().setProduct('kitchen')
    Taro.switchTab({ url: '/pages/index/index' })
    return false
  },

  login: async (profile) => {
    set({ loading: true })
    try {
      const { user, token, expireSeconds } = await exchangeWxCode(profile)
      setToken(token, expireSeconds)
      persistUser(user)
      clearLegacyProfileCache()
      set({ user, isLoggedIn: true, loading: false })

      // 本地临时头像：登录后再上传落库
      const localAvatar = profile?.avatarUrl
      if (localAvatar && !/^https?:\/\//i.test(localAvatar) && !localAvatar.startsWith('/r/')) {
        try {
          const updated = await uploadMyAvatar(localAvatar)
          persistUser(updated)
          set({ user: updated })
        } catch {
          // 昵称登录已成功，头像可稍后再改
        }
      } else {
        void get().refreshProfile()
      }

      setTimeout(() => {
        Taro.showToast({ title: '登录成功', icon: 'success' })
      }, 80)
      get().goHome()
      return true
    } catch {
      set({ loading: false })
      Taro.showToast({ title: '微信登录失败，请重试', icon: 'none' })
      return false
    }
  },

  refreshProfile: async () => {
    if (!get().isLoggedIn) return null
    try {
      const user = await fetchMyProfile()
      persistUser(user)
      set({ user, isLoggedIn: true })
      return user
    } catch {
      return get().user
    }
  },

  updateProfile: async (payload) => {
    try {
      const user = await updateMyProfile(payload)
      persistUser(user)
      set({ user })
      return user
    } catch {
      Taro.showToast({ title: '更新失败', icon: 'none' })
      return null
    }
  },

  uploadAvatar: async (filePath) => {
    try {
      const user = await uploadMyAvatar(filePath)
      persistUser(user)
      set({ user })
      return user
    } catch {
      Taro.showToast({ title: '头像上传失败', icon: 'none' })
      return null
    }
  },

  setUser: (user) => {
    persistUser(user)
    set({ user, isLoggedIn: !!user })
  },

  logout: () => {
    clearToken()
    persistUser(null)
    set({ user: null, isLoggedIn: false })
    useProductStore.getState().setProduct('kitchen')
    get().goHome()
  },

  requireLogin: async () => {
    if (get().isLoggedIn) return true
    Taro.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => get().goLogin(), 300)
    return false
  }
}))
