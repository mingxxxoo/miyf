import { promptAction } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { DraftLine, Order, OrderItem, orderStatusText } from '../../api/Models'
import { DraftStore } from '../../session/DraftStore'

@Component
export struct OrderTab {
  @State draft: DraftLine[] = []
  @State orders: Order[] = []
  @State remark: string = ''
  @State loading: boolean = true
  @State submitting: boolean = false
  @State error: string = ''

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    this.draft = DraftStore.list()
    try {
      this.orders = await MiyfApi.fetchOrders()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private async submit(): Promise<void> {
    const lines = DraftStore.list()
    if (lines.length === 0) {
      promptAction.showToast({ message: '请先加入菜品' })
      return
    }
    this.submitting = true
    try {
      await MiyfApi.createOrder(lines, this.remark.trim())
      DraftStore.clear()
      this.draft = []
      this.remark = ''
      promptAction.showToast({ message: '预约已提交，等待厨师确认' })
      this.orders = await MiyfApi.fetchOrders()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '提交失败' })
    } finally {
      this.submitting = false
    }
  }

  private async cancel(order: Order): Promise<void> {
    if (order.status !== 'PENDING') {
      return
    }
    try {
      await MiyfApi.cancelOrder(order.id)
      promptAction.showToast({ message: '已取消' })
      this.orders = await MiyfApi.fetchOrders()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '取消失败' })
    }
  }

  build() {
    Column() {
      Text('预约')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
        .width('100%')
        .padding({ left: 18, right: 18, top: 18, bottom: 8 })

      Scroll() {
        Column({ space: 12 }) {
          Column({ space: 10 }) {
            Text('当前草稿')
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(Theme.ink)
            if (this.draft.length === 0) {
              Text('还没有菜，去首页或菜品页加入')
                .fontSize(12)
                .fontColor(Theme.faint)
            } else {
              ForEach(this.draft, (line: DraftLine) => {
                Row() {
                  Text(line.dishName)
                    .fontColor(Theme.ink)
                    .layoutWeight(1)
                  Text('×' + line.quantity)
                    .fontColor(Theme.muted)
                }
              }, (line: DraftLine) => line.dishId)
              TextInput({ placeholder: '整单备注（可选）', text: this.remark })
                .height(40)
                .backgroundColor(Theme.orangeSoft)
                .borderRadius(12)
                .onChange((value: string) => {
                  this.remark = value
                })
              Button(this.submitting ? '提交中…' : '提交预约')
                .width('100%')
                .height(44)
                .backgroundColor(Theme.orange)
                .fontColor(Theme.white)
                .borderRadius(14)
                .enabled(!this.submitting)
                .onClick(async () => {
                  await this.submit()
                })
            }
          }
          .width('100%')
          .padding(16)
          .borderRadius(20)
          .backgroundColor(Theme.white)
          .border({ width: 1, color: Theme.line })

          Text('历史预约')
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.ink)
            .width('100%')

          if (this.loading) {
            Text('加载中…').fontColor(Theme.muted)
          } else if (this.error.length > 0) {
            Text(this.error).fontColor(Theme.red)
          } else if (this.orders.length === 0) {
            Text('暂无预约').fontColor(Theme.faint)
          } else {
            ForEach(this.orders, (order: Order) => {
              Column({ space: 8 }) {
                Row() {
                  Text(order.orderNo)
                    .fontSize(13)
                    .fontColor(Theme.ink)
                    .layoutWeight(1)
                  Text(orderStatusText(order.status))
                    .fontSize(12)
                    .fontColor(Theme.orange)
                }
                ForEach(order.items || [], (item: OrderItem) => {
                  Text(item.dishName + ' ×' + item.quantity)
                    .fontSize(12)
                    .fontColor(Theme.muted)
                }, (item: OrderItem) => item.dishId + '_' + item.quantity)
                if (order.status === 'PENDING') {
                  Button('取消预约')
                    .height(32)
                    .fontSize(12)
                    .backgroundColor(Theme.white)
                    .fontColor(Theme.red)
                    .border({ width: 1, color: Theme.line })
                    .borderRadius(10)
                    .onClick(async () => {
                      await this.cancel(order)
                    })
                }
              }
              .width('100%')
              .padding(14)
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
              .alignItems(HorizontalAlign.Start)
            }, (order: Order) => order.id)
          }
        }
        .padding({ left: 18, right: 18, bottom: 24 })
      }
      .layoutWeight(1)
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
    .onVisibleAreaChange([0.0, 1.0], (visible: boolean) => {
      if (visible) {
        this.draft = DraftStore.list()
      }
    })
  }
}
