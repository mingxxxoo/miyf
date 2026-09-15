import Taro, { useDidShow } from '@tarojs/taro'
import { activateOrSwitchRole } from '@/api/kitchen'
import { useUserStore } from '@/stores/userStore'

/**
 * 厨师工作台守卫：须具备厨师身份，且 activeRole 为 CHEF。
 * 双身份若当前在食客台，自动切回厨师台。
 */
export function useChefWorkbench() {
  const user = useUserStore((s) => s.user)
  const refreshProfile = useUserStore((s) => s.refreshProfile)

  useDidShow(() => {
    void (async () => {
      let profile = (await refreshProfile()) || user
      if (!profile?.chef) {
        Taro.showToast({ title: '请先选择厨师身份', icon: 'none' })
        Taro.redirectTo({ url: '/pages/role/select' })
        return
      }
      if (profile.activeRole !== 'CHEF') {
        try {
          await activateOrSwitchRole('CHEF', profile)
          profile = (await refreshProfile()) || profile
        } catch {
          Taro.redirectTo({ url: '/pages/role/select' })
          return
        }
      }
      if (profile?.activeRole !== 'CHEF') {
        Taro.redirectTo({ url: '/pages/role/select' })
      }
    })()
  })
}
