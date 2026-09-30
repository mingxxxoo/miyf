import {useMemo} from 'react'
import Taro from '@tarojs/taro'
import type {Dish} from '@/types'
import {useOrderStore} from '@/stores/orderStore'

/** 首页 / 菜品页共用的草稿加减逻辑 */
export function useDraftDishActions() {
  const draft = useOrderStore((s) => s.draft)
  const setDraftItem = useOrderStore((s) => s.setDraftItem)
  const updateDraftItemQty = useOrderStore((s) => s.updateDraftItemQty)

  const draftQtyMap = useMemo(() => {
    const map: Record<string, number> = {}
    draft.items.forEach((it) => {
      map[it.dishId] = it.quantity
    })
    return map
  }, [draft.items])

  const addToDraft = (dish: Dish) => {
    const soldOut = dish.stockType === 'LIMITED' && (dish.stock == null || dish.stock <= 0)
    if (soldOut) {
      Taro.showToast({ title: '今日已约满', icon: 'none' })
      return
    }
    const existing = draft.items.find((i) => i.dishId === dish.id)
    const nextQty = (existing?.quantity || 0) + 1
    if (dish.stockType === 'LIMITED' && dish.stock != null && nextQty > dish.stock) {
      Taro.showToast({ title: `最多预约 ${dish.stock} 份`, icon: 'none' })
      return
    }
    setDraftItem({
      dishId: dish.id,
      dishName: dish.name,
      coverUrl: dish.coverUrl || undefined,
      quantity: nextQty
    })
  }

  const decDraft = (dish: Dish) => {
    const existing = draft.items.find((i) => i.dishId === dish.id)
    if (!existing) return
    updateDraftItemQty(dish.id, existing.quantity - 1)
  }

  return { draft, draftQtyMap, addToDraft, decDraft, updateDraftItemQty }
}
