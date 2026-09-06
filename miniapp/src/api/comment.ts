import { post } from '@/api/request'
import type { Comment } from '@/types'
import { asId, asOptionalId } from '@/utils/id'

export async function createComment(payload: {
  orderId: string
  dishId: string
  rating: number
  content?: string
  images?: string[]
}): Promise<Comment> {
  const body = {
    ...payload,
    orderId: asId(payload.orderId),
    dishId: asId(payload.dishId)
  }
  const raw = await post<Comment>('/api/comments', body, { showLoading: true })
  return {
    ...raw,
    id: asId(raw.id),
    orderId: asId(raw.orderId),
    dishId: asOptionalId(raw.dishId) ?? body.dishId
  }
}
