import { useDidShow } from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'

interface AuthGuardOptions {
  /** 默认 true：未登录跳转登录；false 仅暴露登录态，不强制跳转 */
  required?: boolean
}

/**
 * 业务页进入前校验登录。
 * required=false 时支持游客浏览公开页。
 */
export function useAuthGuard(options?: AuthGuardOptions) {
  const required = options?.required !== false
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const bootstrapping = useUserStore((s) => s.bootstrapping)
  const requireLogin = useUserStore((s) => s.requireLogin)

  useDidShow(() => {
    if (bootstrapping) return
    if (required && !isLoggedIn) {
      void requireLogin()
    }
  })

  return { isLoggedIn, bootstrapping }
}
