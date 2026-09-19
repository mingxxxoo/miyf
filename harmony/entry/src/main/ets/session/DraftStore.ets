import { DraftLine } from '../api/Models'

export class DraftStore {
  private static lines: DraftLine[] = []

  static list(): DraftLine[] {
    return DraftStore.lines.slice()
  }

  static totalQty(): number {
    let sum = 0
    DraftStore.lines.forEach((line: DraftLine) => {
      sum += line.quantity
    })
    return sum
  }

  static qtyOf(dishId: string): number {
    const found = DraftStore.lines.find((line: DraftLine) => line.dishId === dishId)
    return found === undefined ? 0 : found.quantity
  }

  static setQty(dishId: string, dishName: string, quantity: number): void {
    const next: DraftLine[] = []
    let replaced = false
    DraftStore.lines.forEach((line: DraftLine) => {
      if (line.dishId === dishId) {
        replaced = true
        if (quantity > 0) {
          next.push({ dishId: dishId, dishName: dishName, quantity: quantity })
        }
      } else {
        next.push(line)
      }
    })
    if (!replaced && quantity > 0) {
      next.push({ dishId: dishId, dishName: dishName, quantity: quantity })
    }
    DraftStore.lines = next
  }

  static addOne(dishId: string, dishName: string): void {
    DraftStore.setQty(dishId, dishName, DraftStore.qtyOf(dishId) + 1)
  }

  static clear(): void {
    DraftStore.lines = []
  }
}
