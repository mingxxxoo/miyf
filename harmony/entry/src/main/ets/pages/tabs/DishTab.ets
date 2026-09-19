import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { Category, Dish } from '../../api/Models'
import { DraftStore } from '../../session/DraftStore'

@Component
export struct DishTab {
  @State categories: Category[] = [{ id: 'all', name: '全部' }]
  @State selected: string = 'all'
  @State dishes: Dish[] = []
  @State loading: boolean = true
  @State error: string = ''

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      const list = await MiyfApi.fetchCategories()
      this.categories = [{ id: 'all', name: '全部' }].concat(list)
      await this.loadDishes()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private async loadDishes(): Promise<void> {
    this.dishes = await MiyfApi.fetchDishes(this.selected === 'all' ? '' : this.selected)
  }

  build() {
    Column() {
      Text('菜品')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .padding({ left: 18, right: 18, top: 18, bottom: 8 })

      Scroll() {
        Row({ space: 8 }) {
          ForEach(this.categories, (item: Category) => {
            Text(item.name)
              .fontSize(12)
              .padding({ left: 14, right: 14, top: 7, bottom: 7 })
              .borderRadius(99)
              .fontColor(this.selected === item.id ? Theme.cream : Theme.muted)
              .backgroundColor(this.selected === item.id ? Theme.ink : Theme.white)
              .border({ width: 1, color: Theme.line })
              .onClick(async () => {
                this.selected = item.id
                try {
                  await this.loadDishes()
                } catch (err) {
                  promptAction.showToast({
                    message: err instanceof ApiError ? err.message : '加载失败'
                  })
                }
              })
          }, (item: Category) => item.id)
        }
        .padding({ left: 18, right: 18 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .height(44)

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 40 })
      } else if (this.error.length > 0) {
        Text(this.error).fontColor(Theme.red).margin({ top: 40 })
      } else if (this.dishes.length === 0) {
        Text('暂无在售菜品').fontColor(Theme.muted).margin({ top: 40 })
      } else {
        List({ space: 12 }) {
          ForEach(this.dishes, (dish: Dish) => {
            ListItem() {
              Row({ space: 12 }) {
                Column() {
                  Text(dish.name)
                    .fontSize(15)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(Theme.ink)
                  Text(dish.categoryName || '')
                    .fontSize(12)
                    .fontColor(Theme.faint)
                    .margin({ top: 4 })
                  Text(dish.stockType === 'UNLIMITED'
                    ? '不限量'
                    : ('今日还剩 ' + (dish.stock ?? 0) + ' 份'))
                    .fontSize(11)
                    .fontColor(Theme.muted)
                    .margin({ top: 6 })
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)

                Button('+')
                  .width(36)
                  .height(36)
                  .backgroundColor(Theme.orangeSoft)
                  .fontColor(Theme.orange)
                  .borderRadius(10)
                  .onClick(() => {
                    DraftStore.addOne(dish.id, dish.name)
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
        .margin({ top: 8 })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }
}
