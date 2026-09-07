import Taro from '@tarojs/taro'
import { create } from 'zustand'
import { wxLogin, type WxLoginProfile } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'
import type { User } from '@/types'

const USER_KEY = 'ck_user'
const PROFILE_KEY = 'ck_login_profile'

interface UserState {
  user: User | null
  loading: boolean
  bootstrapping: boolean
  isLoggedIn: boolean
  hydrate: () => void
  bootstrap: () => Promise<boolean>
  login: (profile: WxLoginProfile) => Promise<boolean>
  requireLogin: () => Promise<boolean>
  setUser: (user: User | null) => void
  logout: () => void
  goHome: () => void
  goLogin: () => void
}

function persistUser(user: User | null) {
  if (user) {
    Taro.setStorageSync(USER_KEY, user)
  } else {
    Taro.removeStorageSync(USER_KEY)
  }
}

function persistProfile(profile: WxLoginProfile | null) {
  if (profile) {
    Taro.setStorageSync(PROFILE_KEY, {
      username: profile.username,
      phone: profile.phone,
      wechatId: profile.wechatId,
      nickname: profile.nickname || profile.username
    })
  } else {
    Taro.removeStorageSync(PROFILE_KEY)
  }
}

function readCachedProfile(): WxLoginProfile | null {
  const cached = Taro.getStorageSync(PROFILE_KEY) as WxLoginProfile | ''
  if (!cached || typeof cached !== 'object') return null
  if (!cached.username || !cached.phone || !cached.wechatId) return null
  return cached
}

function currentRoute(): string {
  const pages = Taro.getCurrentPages()
  const cur = pages[pages.length - 1] as { route?: string } | undefined
  return cur?.route || ''
}

export const useUserStore = create<UserState>((set, get) => ({
  user: null,
  loading: false,
  bootstrapping: true,
  isLoggedIn: false,

  hydrate: () => {
    const token = getToken()
    const cached = Taro.getStorageSync(USER_KEY) as User | ''
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
   * 进入应用时先完成登录态恢复 / 静默重新登录。
   */
  bootstrap: async () => {
    set({ bootstrapping: true })
    get().hydrate()
    if (get().isLoggedIn) {
      set({ bootstrapping: false })
      get().goHome()
      return true
    }

    const profile = readCachedProfile()
    if (!profile) {
      set({ bootstrapping: false })
      get().goLogin()
      return false
    }

    set({ loading: true })
    try {
      const { code } = await Taro.login()
      if (!code) {
        set({ loading: false, bootstrapping: false })
        get().goLogin()
        return false
      }
      const { login, user } = await wxLogin(code, profile)
      setToken(login.token)
      persistUser(user)
      persistProfile(profile)
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

  login: async (profile) => {
    const username = profile.username?.trim()
    const phone = profile.phone?.trim()
    const wechatId = profile.wechatId?.trim()
    if (!username) {
      Taro.showToast({ title: '请填写用户名', icon: 'none' })
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(phone || '')) {
      Taro.showToast({ title: '请填写正确手机号', icon: 'none' })
      return false
    }
    if (!wechatId) {
      Taro.showToast({ title: '请填写微信号', icon: 'none' })
      return false
    }

    set({ loading: true })
    try {
      const { code } = await Taro.login()
      if (!code) {
        Taro.showToast({ title: '获取登录凭证失败', icon: 'none' })
        set({ loading: false })
        return false
      }
      const payload: WxLoginProfile = {
        username,
        phone: phone!,
        wechatId,
        nickname: profile.nickname?.trim() || username,
        avatarUrl: profile.avatarUrl
      }
      const { login, user } = await wxLogin(code, payload)
      setToken(login.token)
      persistUser(user)
      persistProfile(payload)
      set({ user, isLoggedIn: true, loading: false })
      setTimeout(() => {
        Taro.showToast({ title: '欢迎回来', icon: 'success' })
      }, 80)
      get().goHome()
      return true
    } catch {
      set({ loading: false })
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
