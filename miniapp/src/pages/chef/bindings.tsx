import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useState } from 'react'
import {
  approveBinding,
  fetchChefBindings,
  rejectBinding,
  unbindBinding,
  type BindingVo
} from '@/api/kitchen'
import EmptyState from '@/components/EmptyState'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import { useChefWorkbench } from '@/hooks/useChefWorkbench'
import './chef.scss'

const STATUS_TEXT: Record<string, string> = {
  PENDING: '待确认',
  BOUND: '已绑定',
  REJECTED: '已拒绝',
  UNBOUND: '已解除'
}

export default function ChefBindingsPage() {
  useChefWorkbench()
  const [list, setList] = useState<BindingVo[]>([])

  const load = async () => {
    const page = await fetchChefBindings()
    setList(page.records || [])
  }

  useDidShow(() => {
    void load().catch(() => {
      Taro.showToast({ title: '请先创建厨房', icon: 'none' })
    })
  })

  const unbind = async (b: BindingVo) => {
    const tip =
      b.status === 'PENDING'
        ? `取消「${b.dinerNickname || '食客'}」的待确认申请？`
        : `解除与「${b.dinerNickname || '食客'}」的绑定？有进行中的预约时无法解除。`
    const res = await Taro.showModal({ title: '确认', content: tip })
    if (!res.confirm) return
    await unbindBinding(b.id)
    Taro.showToast({ title: '已处理', icon: 'success' })
    void load()
  }

  return (
    <View className='chef-page'>
      <View className='chef-page__svc-switch'>
        <ServiceSwitcher compact activeKey='chef' />
      </View>
      <View className='chef-page__hero'>
        <Text className='chef-page__title'>食客申请</Text>
        <Text className='chef-page__sub'>通过后对方即可浏览你的菜单并预约</Text>
      </View>
      {list.length === 0 ? (
        <View className='chef-page__empty'>
          <EmptyState title='还没有申请' description='把邀请码发给食客后，申请会出现在这里' />
        </View>
      ) : (
        list.map((b) => (
          <View key={b.id} className='chef-page__card'>
            <Text className='chef-dish__name'>{b.dinerNickname || '食客'}</Text>
            <Text className='chef-dish__desc'>{STATUS_TEXT[b.status] || b.status}</Text>
            {b.rejectReason && <Text className='chef-dish__desc'>原因：{b.rejectReason}</Text>}
            <View className='chef-page__actions'>
              {b.status === 'PENDING' && !b.ownerSelf && (
                <>
                  <Button
                    className='chef-page__action'
                    size='mini'
                    onClick={() => void approveBinding(b.id).then(load)}
                  >
                    通过
                  </Button>
                  <Button
                    className='chef-page__action chef-page__action--danger'
                    size='mini'
                    onClick={() => void rejectBinding(b.id, '暂不接收').then(load)}
                  >
                    拒绝
                  </Button>
                  <Button
                    className='chef-page__action chef-page__action--ghost'
                    size='mini'
                    onClick={() => void unbind(b)}
                  >
                    取消申请
                  </Button>
                </>
              )}
              {b.status === 'BOUND' && b.ownerSelf && (
                <Text className='chef-dish__desc'>本厨默认食客 · 不可单独解除</Text>
              )}
              {b.status === 'BOUND' && !b.ownerSelf && b.removable !== false && (
                <Button
                  className='chef-page__action chef-page__action--danger'
                  size='mini'
                  onClick={() => void unbind(b)}
                >
                  解除绑定
                </Button>
              )}
            </View>
          </View>
        ))
      )}
    </View>
  )
}
