import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { Binding, Dish } from '../api/Models'
import { DraftStore } from '../session/DraftStore'

@Component
export struct HomeTab {
  @State kitchenName: string = '厨房'
  @State dishes: Dish[] = []
  @State loading: boolean = true
  @State error: string = ''
  @State draftQty: number = 0

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      const binding: Binding | null = await MiyfApi.fetchMyBinding()
      if (binding === null || binding.status !== 'BOUND') {
        router.replaceUrl({ url: 'pages/Join' })
        return
      }
      this.kitchenName = binding.kitchenName || '厨房'
      this.dishes = await MiyfApi.fetchRecommendDishes(6)
      this.draftQty = DraftStore.totalQty()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  build() {
    Column() {
      Row() {
        Column() {
          Text('胡闹厨房')
            .fontSize(13)
            .fontColor(Theme.muted)
          Text(this.kitchenName)
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
            .fontColor(Theme.ink)
            .margin({ top: 4 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 18, right: 18, top: 18, bottom: 8 })

      if (this.loading) {
        Text('加载中…')
          .fontColor(Theme.muted)
          .margin({ top: 40 })
      } else if (this.error.length > 0) {
        Text(this.error)
          .fontColor(Theme.red)
          .margin({ top: 40 })
        Button('重试')
          .margin({ top: 12 })
          .onClick(() => {
            void this.reload()
          })
      } else {
        List({ space: 12 }) {
          ListItem() {
            Text('今日推荐')
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor(Theme.ink)
              .margin({ left: 18, top: 8 })
          }
          ForEach(this.dishes, (dish: Dish) => {
            ListItem() {
              Row({ space: 12 }) {
                Column() {
                  Text(dish.name)
                    .fontSize(15)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(Theme.ink)
                  if (dish.subtitle) {
                    Text(dish.subtitle)
                      .fontSize(12)
                      .fontColor(Theme.muted)
                      .margin({ top: 4 })
                  }
                  Text(dish.stockType === 'UNLIMITED'
                    ? '不限量'
                    : ('今日还剩 ' + (dish.stock ?? 0) + ' 份'))
                    .fontSize(11)
                    .fontColor(Theme.faint)
                    .margin({ top: 6 })
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)

                Button('加入')
                  .height(32)
                  .backgroundColor(Theme.orange)
                  .fontColor(Theme.white)
                  .borderRadius(10)
                  .fontSize(12)
                  .onClick(() => {
                    DraftStore.addOne(dish.id, dish.name)
                    this.draftQty = DraftStore.totalQty()
                    promptAction.showToast({ message: '已加入预约' })
                  })
              }
              .width('100%')
              .padding(14)
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
              .onClick(() => {
                router.pushUrl({ url: 'pages/DishDetail', params: { id: dish.id } })
              })
            }
            .padding({ left: 18, right: 18 })
          }, (dish: Dish) => dish.id)
        }
        .layoutWeight(1)
        .width('100%')
      }

      if (this.draftQty > 0) {
        Row() {
          Text('已选 ' + this.draftQty + ' 样')
            .fontColor(Theme.white)
            .fontSize(14)
            .layoutWeight(1)
          Text('去预约')
            .fontColor(Theme.white)
            .fontWeight(FontWeight.Medium)
        }
        .width('100%')
        .height(48)
        .padding({ left: 18, right: 18 })
        .margin({ left: 18, right: 18, bottom: 12 })
        .borderRadius(16)
        .backgroundColor(Theme.orange)
        .onClick(() => {
          promptAction.showToast({ message: '请切换到「预约」提交' })
        })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }
}
