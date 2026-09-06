import { get } from '@/api/request'
import type { Comment, Dish, PageResult, Recipe, RecipeStep } from '@/types'
import { pageRecords } from '@/types'
import { asId, asOptionalId } from '@/utils/id'

interface DishRaw {
  id: string
  name: string
  subtitle?: string
  description?: string
  coverImage?: string
  coverUrl?: string
  categoryId?: string
  categoryName?: string
  status?: string
  recommend?: boolean
  stock?: number
  stockType?: string
  unit?: string
  sortOrder?: number
  rating?: number
  ratingCount?: number
  images?: string[]
}

interface RecipeRaw {
  id: string
  dishId: string
  description?: string
  difficulty?: string
  prepareMinutes?: number
  cookMinutes?: number
  servings?: number
  tips?: string
  ingredients?: { name: string; amount?: string }[]
  seasonings?: { name: string; amount?: string }[]
  steps?: RecipeStep[]
  nutrition?: Record<string, unknown>
}

function mapDish(raw: DishRaw): Dish {
  const ratingCount = raw.ratingCount ?? 0
  return {
    id: asId(raw.id),
    name: raw.name,
    subtitle: raw.subtitle,
    description: raw.description || raw.subtitle || '',
    coverUrl: raw.coverImage || raw.coverUrl || raw.images?.[0] || '',
    categoryId: asId(raw.categoryId),
    categoryName: raw.categoryName,
    status: raw.status,
    recommend: raw.recommend,
    stock: raw.stock,
    stockType: raw.stockType,
    unit: raw.unit,
    prepMinutes: undefined,
    rating: ratingCount > 0 && raw.rating != null ? Number(raw.rating) : undefined,
    ratingCount,
    images: raw.images
  }
}

function mapRecipe(raw: RecipeRaw): Recipe {
  return {
    id: asId(raw.id),
    dishId: asId(raw.dishId),
    description: raw.description,
    difficulty: raw.difficulty,
    prepareMinutes: raw.prepareMinutes,
    cookMinutes: raw.cookMinutes,
    servings: raw.servings,
    tips: raw.tips,
    ingredients: raw.ingredients,
    seasonings: raw.seasonings,
    steps: (raw.steps || []).map((s) => ({
      ...s,
      description: s.description || s.content,
      imageUrl: s.imageUrl || s.image
    })),
    nutrition: raw.nutrition
  }
}

export async function fetchDishes(params?: {
  categoryId?: string
  keyword?: string
  recommend?: boolean
  page?: number
  rows?: number
}): Promise<PageResult<Dish>> {
  const query: Record<string, unknown> = {
    page: params?.page,
    rows: params?.rows
  }
  if (params?.keyword) query.keyword = params.keyword
  if (params?.recommend != null) query.recommend = params.recommend
  const categoryId = asOptionalId(params?.categoryId)
  if (categoryId && categoryId !== 'all') query.categoryId = categoryId

  const page = await get<PageResult<DishRaw>>('/api/dishes', query)
  return {
    records: pageRecords(page).map(mapDish),
    total: page.total ?? 0,
    page: page.page ?? 1,
    pageSize: page.pageSize ?? params?.rows ?? 20
  }
}

export async function fetchHotDishes(limit = 10): Promise<Dish[]> {
  const list = await get<DishRaw[]>('/api/dishes/hot', { limit })
  return (list || []).map(mapDish)
}

export async function fetchRecommendDishes(limit = 10): Promise<Dish[]> {
  const list = await get<DishRaw[]>('/api/dishes/recommend', { limit })
  return (list || []).map(mapDish)
}

export async function fetchDishDetail(id: string): Promise<Dish> {
  const raw = await get<DishRaw>(`/api/dishes/${asId(id)}`)
  return mapDish(raw)
}

export async function fetchDishRecipe(id: string): Promise<Recipe | null> {
  try {
    const raw = await get<RecipeRaw>(`/api/dishes/${asId(id)}/recipe`, undefined, { showError: false })
    return raw ? mapRecipe(raw) : null
  } catch {
    return null
  }
}

export async function fetchDishComments(
  id: string,
  params?: { page?: number; rows?: number; sortMode?: string; rating?: number }
): Promise<PageResult<Comment>> {
  const page = await get<PageResult<Comment>>(`/api/dishes/${asId(id)}/comments`, params as Record<string, unknown>)
  return {
    records: pageRecords(page).map((c) => ({
      ...c,
      id: asId(c.id),
      orderId: asId(c.orderId),
      dishId: asOptionalId(c.dishId)
    })),
    total: page.total ?? 0,
    page: page.page ?? 1,
    pageSize: page.pageSize ?? params?.rows ?? 20
  }
}
