import { get } from '@/api/request'
import type { Category } from '@/types'
import { asId } from '@/utils/id'

export async function fetchCategories(): Promise<Category[]> {
  const list = await get<Category[]>('/api/categories')
  return (list || []).map((c) => ({
    ...c,
    id: asId(c.id)
  }))
}
