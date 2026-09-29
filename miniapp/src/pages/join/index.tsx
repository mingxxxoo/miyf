import {Button, Input, Text, View} from '@tarojs/components'
import Taro, {useDidShow, useRouter} from '@tarojs/taro'
import {useRef, useState} from 'react'
import {activateOrSwitchRole, applyBinding, type BindingVo, fetchMyBinding} from '@/api/kitchen'
import {useUserStore} from '@/stores/userStore'
import './index.scss'

export default function JoinKitchenPage() {
  const router = useRouter()
  const { isLoggedIn, requireLogin, user, refreshProfile } = useUserStore()
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [binding, setBinding] = useState<BindingVo | null>(null)
  const tokenTried = useRef<string | null>(null)

  const refreshBinding = async () => {
    const b = await fetchMyBinding().catch(() => null)
    setBinding(b)
    return b
  }

  useDidShow(() => {
    const qCode = router.params.code
    if (qCode) setCode(qCode)
    void refreshBinding()
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
      await refreshBinding()
      setTimeout(() => Taro.switchTab({ url: '/pages/index/index' }), 600)
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

  const status = binding?.status
  const statusLabel =
    status === 'PENDING'
      ? '待确认 · 已提交申请'
      : status === 'BOUND'
        ? '已加入 · ' + (binding?.kitchenName || '厨房')
        : status === 'REJECTED'
          ? '未通过'
          : null

  return (
    <View className='join-page'>
      <Text className='join-page__title'>加入厨房</Text>
      <Text className='join-page__sub'>
        向厨师要 6 位邀请码。提交后需厨师确认，通过后即可浏览菜品与预约。
      </Text>

      {statusLabel ? (
        <View
          className={`join-page__status ${
            status === 'REJECTED'
              ? 'join-page__status--bad'
              : status === 'BOUND'
                ? 'join-page__status--ok'
                : 'join-page__status--warn'
          }`}
        >
          <Text className='join-page__status-dot'>●</Text>
          <View className='join-page__status-body'>
            <Text className='join-page__status-title'>{statusLabel}</Text>
            {status === 'REJECTED' && binding?.rejectReason ? (
              <Text className='join-page__status-desc'>原因：{binding.rejectReason}</Text>
            ) : null}
            {status === 'PENDING' ? (
              <Text className='join-page__status-desc'>
                等待「{binding?.kitchenName || '厨房'}」的厨师确认
              </Text>
            ) : null}
          </View>
        </View>
      ) : null}

      {status === 'BOUND' ? (
        <Button
          className='join-page__btn'
          onClick={() => Taro.switchTab({ url: '/pages/index/index' })}
        >
          去首页看看
        </Button>
      ) : (
        <>
          <View className='join-page__code-wrap'>
            <Input
              className='join-page__code'
              placeholder='输入邀请码'
              maxlength={8}
              value={code}
              onInput={(e) => setCode(e.detail.value.toUpperCase())}
            />
          </View>
          <Button
            className='join-page__btn'
            loading={busy}
            onClick={() => void submit()}
          >
            {status === 'REJECTED' ? '重新申请' : '提交申请'}
          </Button>
        </>
      )}
    </View>
  )
}
