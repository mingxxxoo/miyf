import { PropsWithChildren } from 'react'
import { useLaunch } from '@tarojs/taro'
import { useUserStore } from './stores/userStore'
import './app.scss'

function App({ children }: PropsWithChildren) {
  const hydrate = useUserStore((s) => s.hydrate)

  useLaunch(() => {
    hydrate()
  })

  return children
}

export default App
