import {Button, Input, Text, View} from '@tarojs/components'
import Taro, {useDidShow, useRouter} from '@tarojs/taro'
import {useMemo, useRef, useState} from 'react'
import {activateOrSwitchRole, applyBinding, type BindingVo, fetchMyBinding} from '@/api/kitchen'
import {useUserStore} from '@/stores/userStore'
import './index.scss'

const CODE_LEN = 6

export default function JoinKitchenPage() {
  const router = useRouter()
  const { isLoggedIn, requireLogin, user, refreshProfile } = useUserStore()
  const [code, setCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [binding, setBinding] = useState<BindingVo | null>(null)
  const tokenTried = useRef<string | null>(null)

  const cells = useMemo(() => {
    const chars = code.toUpperCase().slice(0, CODE_LEN).split('')
    return Array.from({ length: CODE_LEN }, (_, i) => chars[i] || '')
  }, [code])

  const refreshBinding = async () => {
    const b = await fetchMyBinding().catch(() => null)
    setBinding(b)
    return b
  }

  useDidShow(() => {
    const qCode = router.params.code
    if (qCode) setCode(String(qCode).toUpperCase().slice(0, CODE_LEN))
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
          ? '未绑定 · 未通过'
          : '未绑定 · 填写邀请码'

  return (
    <View className='join-page'>
      <View className='join-page__hero'>
        <View className='join-page__hero-shade' />
        <View className='join-page__hero-body'>
          <Text className='join-page__hero-title'>输入 6 位邀请码</Text>
          <Text className='join-page__hero-desc'>
            向厨师要邀请码。提交后需厨师确认，通过后即可浏览菜品与预约。
          </Text>
        </View>
      </View>

      <View
        className={`join-page__pill ${
          status === 'REJECTED'
            ? 'join-page__pill--bad'
            : status === 'BOUND'
              ? 'join-page__pill--ok'
              : status === 'PENDING'
                ? 'join-page__pill--warn'
                : ''
        }`}
      >
        <Text>{statusLabel}</Text>
      </View>

      {status === 'REJECTED' && binding?.rejectReason ? (
        <Text className='join-page__reject'>原因：{binding.rejectReason}</Text>
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
          <View className='join-page__boxes'>
            {cells.map((ch, i) => (
              <View
                key={i}
                className={`join-page__cell${ch ? ' join-page__cell--filled' : ''}`}
              >
                <Text>{ch}</Text>
              </View>
            ))}
            <Input
              className='join-page__hidden-input'
              maxlength={CODE_LEN}
              value={code}
              focus
              onInput={(e) => setCode(e.detail.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))}
            />
          </View>
          <Text className='join-page__hint'>状态：未绑定 → 待确认 → 已绑定</Text>
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
