import { PropsWithChildren } from 'react'
import Taro, { useLaunch } from '@tarojs/taro'
import { useUserStore } from './stores/userStore'
import './app.scss'

function App({ children }: PropsWithChildren) {
  const hydrate = useUserStore((s) => s.hydrate)
  const logout = useUserStore((s) => s.logout)

  useLaunch(() => {
    hydrate()
    Taro.eventCenter.on('miyf:auth-expired', () => {
      logout()
    })
  })

  return children
}

export default App
