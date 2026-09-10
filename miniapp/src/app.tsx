import { PropsWithChildren } from 'react'
import Taro, { useLaunch } from '@tarojs/taro'
import { useProductStore } from './stores/productStore'
import { useUserStore } from './stores/userStore'
import './app.scss'

function App({ children }: PropsWithChildren) {
  const bootstrap = useUserStore((s) => s.bootstrap)
  const logout = useUserStore((s) => s.logout)
  const goLogin = useUserStore((s) => s.goLogin)
  const hydrateProduct = useProductStore((s) => s.hydrate)

  useLaunch(() => {
    hydrateProduct()
    // 进入应用立刻恢复/请求登录，未登录不进入业务页
    void bootstrap()
    Taro.eventCenter.on('miyf:auth-expired', () => {
      logout()
      goLogin()
    })
  })

  return children
}

export default App
