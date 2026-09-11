import { View, Text, Image, Button } from '@tarojs/components'
import Taro, { useRouter, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useState } from 'react'
import StatusBadge from '@/components/StatusBadge'
import Loading from '@/components/Loading'
import EmptyState from '@/components/EmptyState'
import { useOrderStore } from '@/stores/orderStore'
import { isCommented, markCommented } from '@/utils/commented'
import './detail.scss'

function statusTip(status: string): string | null {
  switch (status) {
    case 'PENDING':
      return '等待厨房确认'
    case 'CONFIRMED':
      return '厨房已确认，即将开始准备'
    case 'PREPARING':
      return '厨房制作中，请稍候'
    case 'READY':
      return '可到柜台取餐'
    case 'COMPLETED':
      return '已完成，欢迎评价'
    case 'CANCELLED':
      return '已取消'
    default:
      return null
  }
}

export default function OrderDetailPage() {
  const router = useRouter()
  const {
    currentOrder,
    loading,
    detailError,
    fetchOrderDetail,
    cancelOrder
  } = useOrderStore()
  const orderId = router.params.id
  const [commentTick, setCommentTick] = useState(0)

  const reload = () => {
    if (orderId) void fetchOrderDetail(orderId)
  }

  useDidShow(() => {
    setCommentTick((n) => n + 1)
    reload()
  })

  usePullDownRefresh(async () => {
    try {
      reload()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  useEffect(() => {
    if (!currentOrder?.items) return
    for (const item of currentOrder.items) {
      if (item.commented === true) {
        markCommented(currentOrder.id, item.dishId)
      }
    }
  }, [currentOrder])

  const goComment = (dishId: string, dishName: string) => {
    if (!currentOrder) return
    const name = encodeURIComponent(dishName || '')
    Taro.navigateTo({
      url: `/pages/comment/create?orderId=${currentOrder.id}&dishId=${dishId}&dishName=${name}`
    })
  }

  const handleCancel = async () => {
    if (!currentOrder) return
    const res = await Taro.showModal({
      title: '取消预约',
      content: '确定取消这份预约吗？'
    })
    if (!res.confirm) return
    const order = await cancelOrder(currentOrder.id)
    if (order) {
      Taro.showToast({ title: order.displayTip || '这次预约取消啦', icon: 'none' })
    }
  }

  if (loading && !currentOrder) {
    return <Loading fullscreen text='查看预约详情…' />
  }

  if (detailError && !currentOrder) {
    return (
      <EmptyState
        emoji='📡'
        title='加载失败'
        description='预约详情暂时拉不下来，请重试'
        actionText='重试'
        onAction={reload}
      />
    )
  }

  if (!currentOrder) {
    return (
      <EmptyState
        emoji='🔍'
        title='找不到这份预约'
        description='可能已被取消，或链接有误'
        actionText='返回预约列表'
        onAction={() => Taro.navigateBack()}
      />
    )
  }

  const status = String(currentOrder.status).toUpperCase()
  const canCancel = status === 'PENDING'
  const canComment = status === 'COMPLETED'
  const tip = statusTip(status)

  return (
    <View className='order-detail'>
      <View className='order-detail__header ck-card'>
        <View className='order-detail__row'>
          <Text className='order-detail__no'>预约单 #{currentOrder.orderNo}</Text>
          <StatusBadge status={currentOrder.status} />
        </View>
        <Text className='order-detail__time'>
          下单时间：{String(currentOrder.createTime || '').replace('T', ' ').slice(0, 16)}
        </Text>
        {tip && <Text className='order-detail__tip'>{tip}</Text>}
      </View>

      <View className='order-detail__items ck-card'>
        <Text className='order-detail__section-title'>菜品清单</Text>
        {currentOrder.items.map((item) => {
          const commented =
            item.commented === true ||
            (commentTick >= 0 && isCommented(currentOrder.id, item.dishId))
          return (
            <View key={`${item.dishId}-${item.id || item.quantity}`} className='order-detail__item'>
              {item.coverUrl && (
                <Image className='order-detail__cover' src={item.coverUrl} mode='aspectFill' />
              )}
              <View className='order-detail__item-info'>
                <Text className='order-detail__item-name'>{item.dishName}</Text>
                <Text className='order-detail__item-qty'>
                  × {item.quantity}{item.unit ? ` ${item.unit}` : ''}
                </Text>
                {(item.note || item.remark) && (
                  <Text className='order-detail__item-note'>{item.note || item.remark}</Text>
                )}
                {canComment &&
                  (commented ? (
                    <Text className='order-detail__item-note'>已评价</Text>
                  ) : (
                    <Text
                      className='order-detail__item-note'
                      onClick={() => goComment(item.dishId, item.dishName)}
                    >
                      评价这道菜 →
                    </Text>
                  ))}
              </View>
            </View>
          )
        })}
      </View>

      {currentOrder.note && (
        <View className='order-detail__note ck-card'>
          <Text className='order-detail__section-title'>备注</Text>
          <Text className='order-detail__note-text'>{currentOrder.note}</Text>
        </View>
      )}

      {canCancel && (
        <View className='order-detail__footer'>
          <Button className='ck-btn-secondary order-detail__btn' onClick={handleCancel}>
            取消预约
          </Button>
        </View>
      )}
    </View>
  )
}
