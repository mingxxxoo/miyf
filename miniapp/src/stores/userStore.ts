import Taro from '@tarojs/taro'
import { create } from 'zustand'
import { wxLogin } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/api/request'
import type { User } from '@/types'

const USER_KEY = 'ck_user'

interface UserState {
  user: User | null
  loading: boolean
  isLoggedIn: boolean
  hydrate: () => void
  login: () => Promise<boolean>
  setUser: (user: User | null) => void
  logout: () => void
}

function persistUser(user: User | null) {
  if (user) {
    Taro.setStorageSync(USER_KEY, user)
  } else {
    Taro.removeStorageSync(USER_KEY)
  }
}

export const useUserStore = create<UserState>((set) => ({
  user: null,
  loading: false,
  isLoggedIn: false,

  hydrate: () => {
    const token = getToken()
    const cached = Taro.getStorageSync(USER_KEY) as User | ''
    if (token && cached && typeof cached === 'object') {
      set({ user: cached, isLoggedIn: true })
    } else {
      set({ user: null, isLoggedIn: false })
    }
  },

  login: async () => {
    set({ loading: true })
    try {
      const { code } = await Taro.login()
      if (!code) {
        Taro.showToast({ title: '获取登录凭证失败', icon: 'none' })
        set({ loading: false })
        return false
      }
      const { login, user } = await wxLogin(code)
      setToken(login.token)
      persistUser(user)
      set({ user, isLoggedIn: true, loading: false })
      setTimeout(() => {
        Taro.showToast({ title: '欢迎回来', icon: 'success' })
      }, 80)
      return true
    } catch {
      // 具体错误已由 request 弹出；此处仅复位 loading
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
  }
}))
