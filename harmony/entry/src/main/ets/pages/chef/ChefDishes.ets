import { promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { ChefDish, dishStatusText } from '../../api/Models'

@Entry
@Component
struct ChefDishes {
  @State dishes: ChefDish[] = []
  @State loading: boolean = true
  @State error: string = ''
  @State name: string = ''
  @State stock: string = '10'
  @State creating: boolean = false
  @State filter: string = 'ALL'

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      this.dishes = await MiyfApi.fetchChefDishes()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private shown(): ChefDish[] {
    if (this.filter === 'ON_SALE') {
      return this.dishes.filter((d: ChefDish) => d.status === 'ON_SALE')
    }
    if (this.filter === 'REVIEW') {
      return this.dishes.filter((d: ChefDish) => d.auditStatus === 'PENDING_REVIEW')
    }
    if (this.filter === 'DRAFT') {
      return this.dishes.filter((d: ChefDish) => d.status === 'DRAFT' || d.auditStatus === 'DRAFT')
    }
    if (this.filter === 'OFF') {
      return this.dishes.filter((d: ChefDish) => d.status === 'OFF_SALE')
    }
    return this.dishes
  }

  private async create(): Promise<void> {
    const n = this.name.trim()
    if (n.length === 0) {
      promptAction.showToast({ message: '请填写菜名' })
      return
    }
    this.creating = true
    try {
      const dish = await MiyfApi.createChefDish(n, Number(this.stock) || 0)
      try {
        await MiyfApi.submitChefDish(dish.id)
      } catch (_err) {
      }
      this.name = ''
      promptAction.showToast({ message: '已创建，等待审核通过后再上架' })
      await this.reload()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '创建失败' })
    } finally {
      this.creating = false
    }
  }

  private async toggleSale(dish: ChefDish): Promise<void> {
    try {
      if (dish.status === 'ON_SALE') {
        await MiyfApi.unpublishChefDish(dish.id)
        promptAction.showToast({ message: '已下架' })
      } else {
        await MiyfApi.publishChefDish(dish.id)
        promptAction.showToast({ message: '已上架' })
      }
      await this.reload()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '操作失败' })
    }
  }

  @Builder
  filterChip(key: string, label: string) {
    Text(label)
      .fontSize(12)
      .padding({ left: 14, right: 14, top: 7, bottom: 7 })
      .borderRadius(99)
      .fontColor(this.filter === key ? Theme.cream : Theme.muted)
      .backgroundColor(this.filter === key ? Theme.ink : Theme.white)
      .border({ width: 1, color: Theme.line })
      .onClick(() => {
        this.filter = key
      })
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .onClick(() => {
            router.back()
          })
        Text('菜品管理')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text('AI')
          .fontSize(13)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.orange)
          .onClick(() => {
            router.pushUrl({ url: 'pages/chef/ChefAiExtract' })
          })
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      Scroll() {
        Row({ space: 8 }) {
          this.filterChip('ALL', '全部')
          this.filterChip('ON_SALE', '在售')
          this.filterChip('REVIEW', '审核中')
          this.filterChip('DRAFT', '草稿')
          this.filterChip('OFF', '已下架')
        }
        .padding({ left: 16, right: 16 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .height(44)

      Column({ space: 8 }) {
        TextInput({ placeholder: '新菜名称', text: this.name })
          .height(44)
          .backgroundColor(Theme.white)
          .borderRadius(12)
          .border({ width: 1, color: Theme.line })
          .onChange((v: string) => {
            this.name = v
          })
        Row({ space: 8 }) {
          TextInput({ placeholder: '库存，0 为不限量', text: this.stock })
            .layoutWeight(1)
            .height(44)
            .type(InputType.Number)
            .backgroundColor(Theme.white)
            .borderRadius(12)
            .onChange((v: string) => {
              this.stock = v
            })
          Button(this.creating ? '创建中' : '新建')
            .height(44)
            .padding({ left: 18, right: 18 })
            .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
            .fontColor(Theme.white)
            .borderRadius(12)
            .enabled(!this.creating)
            .onClick(() => {
              void this.create()
            })
        }
      }
      .padding({ left: 16, right: 16, bottom: 8 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 24 })
      } else if (this.error.length > 0) {
        Text(this.error).fontColor(Theme.red)
      } else if (this.shown().length === 0) {
        Text('去建第一道菜').fontColor(Theme.faint).margin({ top: 24 })
      } else {
        List({ space: 12 }) {
          ForEach(this.shown(), (dish: ChefDish) => {
            ListItem() {
              Column({ space: 8 }) {
                Row() {
                  Text(dish.name)
                    .fontSize(15)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(Theme.ink)
                    .layoutWeight(1)
                  Text(dishStatusText(dish.status, dish.auditStatus))
                    .fontSize(11)
                    .fontColor(dish.status === 'ON_SALE' ? Theme.mint : Theme.muted)
                }
                Text((dish.stockType === 'LIMITED' ? ('库存 ' + (dish.stock ?? 0) + (dish.unit || '份')) : '不限量')
                  + (dish.categoryName ? ' · ' + dish.categoryName : ''))
                  .fontSize(12)
                  .fontColor(Theme.faint)
                if (dish.rejectReason) {
                  Text(dish.rejectReason).fontSize(11).fontColor(Theme.red)
                }
                Row({ space: 8 }) {
                  if (dish.status === 'ON_SALE') {
                    Button('下架')
                      .height(30)
                      .fontSize(12)
                      .backgroundColor(Theme.redSoft)
                      .fontColor(Theme.red)
                      .borderRadius(9)
                      .onClick(() => {
                        void this.toggleSale(dish)
                      })
                  } else if (dish.auditStatus === 'APPROVED' || dish.status === 'OFF_SALE' || dish.status === 'DRAFT') {
                    Button('上架')
                      .height(30)
                      .fontSize(12)
                      .backgroundColor(Theme.mintSoft)
                      .fontColor(Theme.mint)
                      .borderRadius(9)
                      .onClick(() => {
                        void this.toggleSale(dish)
                      })
                  }
                }
              }
              .width('100%')
              .padding(14)
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
              .alignItems(HorizontalAlign.Start)
            }
            .padding({ left: 16, right: 16 })
          }, (dish: ChefDish) => dish.id)
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
