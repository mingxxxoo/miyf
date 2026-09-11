import { PropsWithChildren } from 'react'
import Taro, { useLaunch } from '@tarojs/taro'
import { useProductStore } from './stores/productStore'
import { useUserStore } from './stores/userStore'
import './app.scss'

function App({ children }: PropsWithChildren) {
  const bootstrap = useUserStore((s) => s.bootstrap)
  const logout = useUserStore((s) => s.logout)
  const hydrateProduct = useProductStore((s) => s.hydrate)

  useLaunch(() => {
    hydrateProduct()
    void bootstrap()
    const onExpired = () => {
      logout()
    }
    Taro.eventCenter.on('miyf:auth-expired', onExpired)
  })

  return children
}

export default App
