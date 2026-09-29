import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { Dish, Recipe, RecipeStep, stockLabel } from '../api/Models'
import { DraftStore } from '../session/DraftStore'

@Entry
@Component
struct DishDetail {
  @State dish: Dish | null = null
  @State recipe: Recipe | null = null
  @State loading: boolean = true
  @State error: string = ''
  @State qty: number = 1
  private dishId: string = ''

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>
    this.dishId = params !== undefined && params['id'] !== undefined ? String(params['id']) : ''
    const existing = DraftStore.qtyOf(this.dishId)
    if (existing > 0) {
      this.qty = existing
    }
    void this.reload()
  }

  private async reload(): Promise<void> {
    if (this.dishId.length === 0) {
      this.error = '缺少菜品'
      this.loading = false
      return
    }
    this.loading = true
    try {
      this.dish = await MiyfApi.fetchDishDetail(this.dishId)
      this.recipe = await MiyfApi.fetchRecipe(this.dishId)
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private soldOut(): boolean {
    if (this.dish === null) {
      return true
    }
    return this.dish.stockType !== 'UNLIMITED' && (this.dish.stock ?? 0) <= 0
  }

  build() {
    Column() {
      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 80 })
      } else if (this.error.length > 0 || this.dish === null) {
        Row() {
          Text('‹ 返回').onClick(() => {
            router.back()
          })
        }
        .padding(16)
        Text(this.error || '菜品不存在').fontColor(Theme.red)
      } else {
        Scroll() {
          Column() {
            Stack() {
              Column() {
                Text(this.dish.name.substring(0, 1))
                  .fontSize(64)
              }
              .width('100%')
              .height(180)
              .linearGradient({ angle: 135, colors: [['#FFD9AE', 0], ['#FF9A3E', 1]] })
              .justifyContent(FlexAlign.Center)
              Text('‹')
                .fontSize(18)
                .fontColor(Theme.ink)
                .width(32)
                .height(32)
                .textAlign(TextAlign.Center)
                .borderRadius(16)
                .backgroundColor('rgba(255,255,255,0.92)')
                .position({ x: 14, y: 14 })
                .onClick(() => {
                  router.back()
                })
            }
            .width('100%')

            Column() {
              Row({ space: 8 }) {
                Text(this.dish.name)
                  .fontSize(18)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.ink)
                if (this.dish.recommend) {
                  Text('荐')
                    .fontSize(10)
                    .fontColor(Theme.orange)
                    .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                    .borderRadius(99)
                    .backgroundColor(Theme.orangeSoft)
                }
              }
              Text(this.dish.subtitle || this.dish.description || '')
                .fontSize(12)
                .fontColor(Theme.muted)
                .margin({ top: 8 })
              Row({ space: 8 }) {
                if (this.dish.categoryName) {
                  Text(this.dish.categoryName)
                    .fontSize(10)
                    .fontColor(Theme.muted)
                    .padding({ left: 11, right: 11, top: 5, bottom: 5 })
                    .borderRadius(99)
                    .backgroundColor(Theme.orangeSofter)
                    .border({ width: 1, color: Theme.line })
                }
                Text(stockLabel(this.dish.stockType, this.dish.stock, this.dish.unit))
                  .fontSize(10)
                  .fontColor(Theme.mint)
                  .padding({ left: 11, right: 11, top: 5, bottom: 5 })
                  .borderRadius(99)
                  .backgroundColor(Theme.mintSoft)
              }
              .margin({ top: 12 })
            }
            .width('100%')
            .padding(16)
            .margin({ left: 16, right: 16, top: -20 })
            .borderRadius(22)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .alignItems(HorizontalAlign.Start)

            if (this.recipe !== null && this.recipe.steps !== undefined && this.recipe.steps.length > 0) {
              Text('怎么做')
                .fontSize(13)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
                .width('100%')
                .padding({ left: 16, top: 16, bottom: 8 })
              ForEach(this.recipe.steps, (step: RecipeStep, index: number) => {
                Row({ space: 10 }) {
                  Text(String(step.step ?? index + 1))
                    .fontSize(10)
                    .fontColor(Theme.white)
                    .fontWeight(FontWeight.Bold)
                    .width(20)
                    .height(20)
                    .textAlign(TextAlign.Center)
                    .borderRadius(10)
                    .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                  Text(step.content || step.description || '')
                    .fontSize(12)
                    .fontColor(Theme.ink)
                    .layoutWeight(1)
                }
                .width('100%')
                .padding({ left: 16, right: 16, top: 8, bottom: 8 })
                .alignItems(VerticalAlign.Top)
              }, (_step: RecipeStep, index: number) => String(index))
            }
            Column().height(24)
          }
        }
        .layoutWeight(1)
        .width('100%')
        .scrollBar(BarState.Off)

        Row({ space: 12 }) {
          Row({ space: 2 }) {
            Text('−')
              .width(28)
              .height(28)
              .textAlign(TextAlign.Center)
              .backgroundColor(Theme.white)
              .borderRadius(9)
              .fontColor(Theme.orange)
              .onClick(() => {
                if (this.qty > 1) {
                  this.qty -= 1
                }
              })
            Text(String(this.qty))
              .width(26)
              .textAlign(TextAlign.Center)
              .fontSize(14)
            Text('+')
              .width(28)
              .height(28)
              .textAlign(TextAlign.Center)
              .backgroundColor(Theme.white)
              .borderRadius(9)
              .fontColor(Theme.orange)
              .onClick(() => {
                this.qty += 1
              })
          }
          .padding(3)
          .borderRadius(12)
          .backgroundColor(Theme.orangeSofter)
          .border({ width: 1, color: Theme.line })

          Button(this.soldOut() ? '已售罄' : '加入预约')
            .layoutWeight(1)
            .height(44)
            .linearGradient({
              angle: 135,
              colors: this.soldOut()
                ? [['#E8E2D8', 0], ['#E8E2D8', 1]]
                : [[Theme.orange2, 0], [Theme.orange, 1]]
            })
            .fontColor(Theme.white)
            .borderRadius(14)
            .enabled(!this.soldOut())
            .onClick(() => {
              if (this.dish !== null) {
                DraftStore.setQty(this.dish.id, this.dish.name, this.qty)
                promptAction.showToast({ message: '已加入预约' })
              }
            })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 10, bottom: 14 })
        .backgroundColor(Theme.white)
        .border({ width: { top: 1 }, color: Theme.line })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }
}
