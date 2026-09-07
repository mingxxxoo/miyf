import { useDidShow } from '@tarojs/taro'
import { useUserStore } from '@/stores/userStore'

/**
 * 业务页进入前校验登录；未登录立即跳转登录页。
 */
export function useAuthGuard() {
  const isLoggedIn = useUserStore((s) => s.isLoggedIn)
  const bootstrapping = useUserStore((s) => s.bootstrapping)
  const requireLogin = useUserStore((s) => s.requireLogin)

  useDidShow(() => {
    if (bootstrapping) return
    if (!isLoggedIn) {
      void requireLogin()
    }
  })

  return { isLoggedIn, bootstrapping }
}
