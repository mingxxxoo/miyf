import { View, Text, Textarea, Button } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { useState } from 'react'
import StarRating from '@/components/StarRating'
import { createComment } from '@/api/comment'
import { useUserStore } from '@/stores/userStore'
import './create.scss'

export default function CommentCreatePage() {
  const router = useRouter()
  const orderId = router.params.orderId || ''
  const dishId = router.params.dishId || ''
  const { isLoggedIn, requireLogin } = useUserStore()
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    if (!orderId || !dishId) {
      Taro.showToast({ title: '缺少预约或菜品信息', icon: 'none' })
      return
    }
    if (!isLoggedIn) {
      const ok = await requireLogin()
      if (!ok) return
    }
    setSubmitting(true)
    try {
      await createComment({
        orderId,
        dishId,
        rating,
        content: content.trim() || undefined
      })
      Taro.showToast({ title: '谢谢你的分享', icon: 'success' })
      setTimeout(() => Taro.navigateBack(), 800)
    } catch {
      // toast in request
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <View className='comment-create'>
      <View className='comment-create__hero'>
        <Text className='comment-create__title'>这顿饭怎么样？</Text>
        <Text className='comment-create__subtitle'>你的反馈会让厨房更有方向</Text>
      </View>

      <View className='comment-create__card ck-card'>
        <Text className='comment-create__label'>整体感受</Text>
        <StarRating value={rating} size='lg' onChange={setRating} />
      </View>

      <View className='comment-create__card ck-card'>
        <Text className='comment-create__label'>想说的话（选填）</Text>
        <Textarea
          className='comment-create__textarea'
          placeholder='味道、摆盘、服务…随便聊聊'
          maxlength={500}
          value={content}
          onInput={(e) => setContent(e.detail.value)}
        />
        <Text className='comment-create__count'>{content.length}/500</Text>
      </View>

      <Button
        className='ck-btn-primary comment-create__submit'
        loading={submitting}
        onClick={handleSubmit}
      >
        提交反馈
      </Button>
    </View>
  )
}
