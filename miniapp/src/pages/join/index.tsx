import { View, Text, Input, Button } from '@tarojs/components'
import Taro, { useRouter, useDidShow } from '@tarojs/taro'
import { useRef, useState } from 'react'
import { applyBinding, activateOrSwitchRole } from '@/api/kitchen'
import { useUserStore } from '@/stores/userStore'

export default function JoinKitchenPage() {
  const router = useRouter()
  const { isLoggedIn, requireLogin, user, refreshProfile } = useUserStore()
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const tokenTried = useRef<string | null>(null)

  useDidShow(() => {
    const qCode = router.params.code
    if (qCode) setCode(qCode)
    const token = router.params.token
    if (token && tokenTried.current !== token) {
      tokenTried.current = token
      void doApply({ token })
    }
  })

  const ensureDinerWorkbench = async () => {
    if (!isLoggedIn) {
      const ok = await requireLogin()
      if (!ok) return false
    }
    let profile = (await refreshProfile()) || user
    // 开通食客身份，或已开通则切到食客工作台（避免双身份仍停在厨师台）
    await activateOrSwitchRole('DINER', profile)
    profile = (await refreshProfile()) || profile
    return Boolean(profile?.diner)
  }

  const doApply = async (payload: { code?: string; token?: string }) => {
    if (busy) return
    setBusy(true)
    try {
      const ok = await ensureDinerWorkbench()
      if (!ok) {
        tokenTried.current = null
        return
      }
      await applyBinding(payload)
      Taro.showToast({ title: '已提交申请', icon: 'success' })
      setTimeout(() => Taro.switchTab({ url: '/pages/index/index' }), 500)
    } catch {
      tokenTried.current = null
    } finally {
      setBusy(false)
    }
  }

  const submit = async () => {
    if (!code.trim()) {
      Taro.showToast({ title: '请输入邀请码', icon: 'none' })
      return
    }
    await doApply({ code: code.trim() })
  }

  return (
    <View style={{ padding: '48px 32px' }}>
      <Text style={{ display: 'block', fontSize: '36px', fontWeight: 700, marginBottom: '16px' }}>
        加入厨房
      </Text>
      <Text style={{ display: 'block', color: '#888', marginBottom: '32px' }}>
        没有公开菜品。输入厨师邀请码，等待确认后才能看到菜单和下单。
      </Text>
      <Input
        placeholder='邀请码'
        value={code}
        onInput={(e) => setCode(e.detail.value)}
        style={{ background: '#fff', padding: '20px', borderRadius: '16px', marginBottom: '24px' }}
      />
      <Button type='primary' loading={busy} onClick={() => void submit()}>
        申请加入
      </Button>
    </View>
  )
}
