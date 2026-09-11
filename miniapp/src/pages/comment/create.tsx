import { View, Text, Textarea, Button, Image } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { useState } from 'react'
import StarRating from '@/components/StarRating'
import { createComment } from '@/api/comment'
import { getToken, clearToken } from '@/api/request'
import { useUserStore } from '@/stores/userStore'
import { markCommented } from '@/utils/commented'
import './create.scss'

const BASE_URL = process.env.TARO_APP_API_BASE || 'https://www.miyf.cn'
const MAX_IMAGES = 3

async function uploadCommentImage(filePath: string): Promise<string> {
  const token = getToken()
  const res = await Taro.uploadFile({
    url: `${BASE_URL}/api/comments/images/upload`,
    filePath,
    name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (res.statusCode === 401) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  let body: { code?: number; message?: string; data?: { url?: string } } | null = null
  try {
    body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data as typeof body)
  } catch {
    throw new Error('图片上传失败')
  }
  if (body && body.code === 40100) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  const url = body?.data?.url
  if (!body || body.code !== 0 || !url) {
    throw new Error(body?.message || '图片上传失败')
  }
  return url
}

export default function CommentCreatePage() {
  const router = useRouter()
  const orderId = router.params.orderId || ''
  const dishId = router.params.dishId || ''
  const dishName = (() => {
    try {
      return decodeURIComponent(router.params.dishName || '')
    } catch {
      return router.params.dishName || ''
    }
  })()
  const { isLoggedIn, requireLogin } = useUserStore()
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [images, setImages] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)

  const chooseImages = async () => {
    const remain = MAX_IMAGES - images.length
    if (remain <= 0) {
      Taro.showToast({ title: `最多 ${MAX_IMAGES} 张`, icon: 'none' })
      return
    }
    try {
      const res = await Taro.chooseImage({
        count: remain,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      })
      const paths = res.tempFilePaths || []
      if (paths.length) {
        setImages((prev) => [...prev, ...paths].slice(0, MAX_IMAGES))
      }
    } catch {
      // user cancel
    }
  }

  const removeImage = (index: number) => {
    setImages((prev) => prev.filter((_, i) => i !== index))
  }

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
      const uploaded: string[] = []
      for (const path of images) {
        const url = await uploadCommentImage(path)
        uploaded.push(url)
      }
      await createComment({
        orderId,
        dishId,
        rating,
        content: content.trim() || undefined,
        images: uploaded.length ? uploaded : undefined
      })
      markCommented(orderId, dishId)
      Taro.showToast({ title: '谢谢你的分享', icon: 'success' })
      setTimeout(() => Taro.navigateBack(), 800)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      if (msg && !msg.includes('fail')) {
        Taro.showToast({ title: msg.slice(0, 40), icon: 'none' })
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <View className='comment-create'>
      <View className='comment-create__hero'>
        <Text className='comment-create__title'>这顿饭怎么样？</Text>
        <Text className='comment-create__subtitle'>
          {dishName ? `正在评价：${dishName}` : '你的反馈会让厨房更有方向'}
        </Text>
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

      <View className='comment-create__card ck-card'>
        <Text className='comment-create__label'>图片（选填，最多 {MAX_IMAGES} 张）</Text>
        <View className='comment-create__images'>
          {images.map((src, index) => (
            <View key={`${src}-${index}`} className='comment-create__thumb-wrap'>
              <Image className='comment-create__thumb' src={src} mode='aspectFill' />
              <Text
                className='comment-create__thumb-del'
                onClick={() => removeImage(index)}
              >
                ×
              </Text>
            </View>
          ))}
          {images.length < MAX_IMAGES && (
            <View className='comment-create__add ck-pressable' onClick={() => void chooseImages()}>
              <Text className='comment-create__add-icon'>+</Text>
              <Text className='comment-create__add-text'>添加</Text>
            </View>
          )}
        </View>
      </View>

      <Button
        className='ck-btn-primary comment-create__submit'
        loading={submitting}
        onClick={() => void handleSubmit()}
      >
        提交反馈
      </Button>
    </View>
  )
}
