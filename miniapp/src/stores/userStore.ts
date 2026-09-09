import Taro from '@tarojs/taro'
import { create } from 'zustand'
import { wxLogin } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'
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
  login: () => Promise<boolean>
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

async function exchangeWxCode(): Promise<{ user: User; token: string }> {
  const { code } = await Taro.login()
  if (!code) {
    throw new Error('empty wx login code')
  }
  const { login, user } = await wxLogin(code)
  return { user, token: login.token }
}

export const useUserStore = create<UserState>((set, get) => ({
  user: null,
  loading: false,
  bootstrapping: true,
  isLoggedIn: false,

  hydrate: () => {
    const token = getToken()
    let cached = Taro.getStorageSync(USER_KEY) as User | ''
    if ((!cached || typeof cached !== 'object')) {
      cached = Taro.getStorageSync(LEGACY_USER_KEY) as User | ''
      if (cached && typeof cached === 'object') {
        persistUser(cached)
      }
    }
    if (token && cached && typeof cached === 'object') {
      set({ user: cached, isLoggedIn: true })
    } else {
      clearToken()
      persistUser(null)
      set({ user: null, isLoggedIn: false })
    }
  },

  goHome: () => {
    const route = currentRoute()
    if (route === 'pages/index/index') return
    Taro.switchTab({ url: '/pages/index/index' })
  },

  goLogin: () => {
    const route = currentRoute()
    if (route === 'pages/login/index') return
    Taro.reLaunch({ url: '/pages/login/index' })
  },

  /**
   * 进入应用时恢复登录态；无 token 时尝试微信静默登录。
   */
  bootstrap: async () => {
    set({ bootstrapping: true })
    get().hydrate()
    if (get().isLoggedIn) {
      set({ bootstrapping: false })
      get().goHome()
      return true
    }

    set({ loading: true })
    try {
      const { user, token } = await exchangeWxCode()
      setToken(token)
      persistUser(user)
      clearLegacyProfileCache()
      set({ user, isLoggedIn: true, loading: false, bootstrapping: false })
      get().goHome()
      return true
    } catch {
      clearToken()
      persistUser(null)
      set({ user: null, isLoggedIn: false, loading: false, bootstrapping: false })
      get().goLogin()
      return false
    }
  },

  login: async () => {
    set({ loading: true })
    try {
      const { user, token } = await exchangeWxCode()
      setToken(token)
      persistUser(user)
      clearLegacyProfileCache()
      set({ user, isLoggedIn: true, loading: false })
      setTimeout(() => {
        Taro.showToast({ title: '欢迎回来', icon: 'success' })
      }, 80)
      get().goHome()
      return true
    } catch {
      set({ loading: false })
      Taro.showToast({ title: '微信登录失败，请重试', icon: 'none' })
      return false
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
    get().goLogin()
  },

  requireLogin: async () => {
    if (get().isLoggedIn) return true
    Taro.showToast({ title: '请先登录', icon: 'none' })
    setTimeout(() => get().goLogin(), 300)
    return false
  }
}))
