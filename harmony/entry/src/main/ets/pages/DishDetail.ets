import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { Dish, Recipe } from '../api/Models'
import { DraftStore } from '../session/DraftStore'

@Entry
@Component
struct DishDetail {
  @State dish: Dish | null = null
  @State recipe: Recipe | null = null
  @State loading: boolean = true
  @State error: string = ''
  private dishId: string = ''

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>
    this.dishId = params !== undefined && params['id'] !== undefined ? String(params['id']) : ''
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

  build() {
    Column() {
      Row() {
        Text('‹ 返回')
          .fontSize(16)
          .fontColor(Theme.ink)
          .onClick(() => {
            router.back()
          })
        Text('菜品详情')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.ink)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ')
          .width(48)
      }
      .width('100%')
      .padding(16)

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted)
      } else if (this.error.length > 0 || this.dish === null) {
        Text(this.error || '菜品不存在').fontColor(Theme.red)
      } else {
        Scroll() {
          Column({ space: 12 }) {
            Text(this.dish.name)
              .fontSize(22)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.ink)
            if (this.dish.subtitle) {
              Text(this.dish.subtitle).fontSize(14).fontColor(Theme.muted)
            }
            Text(this.dish.description || '')
              .fontSize(13)
              .fontColor(Theme.muted)
            Text(this.dish.stockType === 'UNLIMITED'
              ? '不限量'
              : ('今日还剩 ' + (this.dish.stock ?? 0) + ' 份'))
              .fontSize(12)
              .fontColor(Theme.faint)

            if (this.recipe !== null && this.recipe.steps !== undefined && this.recipe.steps.length > 0) {
              Text('做法')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor(Theme.ink)
                .margin({ top: 8 })
              ForEach(this.recipe.steps, (step, index: number) => {
                Text((step.step ?? index + 1) + '. ' + (step.content || step.description || ''))
                  .fontSize(13)
                  .fontColor(Theme.muted)
              }, (_step, index: number) => String(index))
            }

            Button('加入预约')
              .width('100%')
              .height(48)
              .margin({ top: 16, bottom: 24 })
              .backgroundColor(Theme.orange)
              .fontColor(Theme.white)
              .borderRadius(16)
              .onClick(() => {
                if (this.dish !== null) {
                  DraftStore.addOne(this.dish.id, this.dish.name)
                  promptAction.showToast({ message: '已加入预约' })
                }
              })
          }
          .padding({ left: 18, right: 18 })
          .alignItems(HorizontalAlign.Start)
        }
        .layoutWeight(1)
        .width('100%')
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }
}
