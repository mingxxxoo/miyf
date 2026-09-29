import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { Theme } from '../common/AppConfig'
import { ApiError } from '../api/ApiClient'
import { MiyfApi } from '../api/MiyfApi'
import { Order, OrderItem, orderStatusText } from '../api/Models'
import { FormCardUpdater } from '../platform/FormCardUpdater'
import { common } from '@kit.AbilityKit'

@Entry
@Component
struct OrderDetail {
  @State order: Order | null = null
  @State loading: boolean = true
  @State error: string = ''
  private orderId: string = ''

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>
    this.orderId = params !== undefined && params['id'] !== undefined ? String(params['id']) : ''
    void this.reload()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private async reload(): Promise<void> {
    if (this.orderId.length === 0) {
      this.error = '缺少预约'
      this.loading = false
      return
    }
    this.loading = true
    this.error = ''
    try {
      this.order = await MiyfApi.fetchOrderDetail(this.orderId)
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
      this.order = null
    } finally {
      this.loading = false
    }
  }

  private confirmCancel(): void {
    if (this.order === null || this.order.status !== 'PENDING') {
      return
    }
    AlertDialog.show({
      title: '取消预约',
      message: '确认取消这张待确认的预约？',
      primaryButton: {
        value: '取消预约',
        fontColor: Theme.red,
        action: () => {
          void this.cancel()
        }
      },
      secondaryButton: {
        value: '再想想',
        action: () => {
        }
      }
    })
  }

  private async cancel(): Promise<void> {
    if (this.order === null) {
      return
    }
    try {
      this.order = await MiyfApi.cancelOrder(this.order.id)
      promptAction.showToast({ message: '已取消' })
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '取消失败' })
    }
  }

  private flowDone(step: string): boolean {
    if (this.order === null) {
      return false
    }
    const s = this.order.status
    if (step === 'PENDING') {
      return s !== 'CANCELLED'
    }
    if (step === 'CONFIRMED') {
      return s === 'CONFIRMED' || s === 'PREPARING' || s === 'READY' || s === 'COMPLETED'
    }
    if (step === 'PREPARING') {
      return s === 'PREPARING' || s === 'READY' || s === 'COMPLETED'
    }
    if (step === 'READY') {
      return s === 'READY' || s === 'COMPLETED'
    }
    if (step === 'COMPLETED') {
      return s === 'COMPLETED'
    }
    return false
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .onClick(() => {
            router.back()
          })
        Text('预约详情')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ').width(22)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 40 })
      } else if (this.error.length > 0 || this.order === null) {
        Column({ space: 12 }) {
          Text(this.error || '预约不存在').fontColor(Theme.red)
          Button('重试').onClick(() => {
            void this.reload()
          })
        }
        .margin({ top: 40 })
      } else {
        Scroll() {
          Column({ space: 14 }) {
            Column({ space: 8 }) {
              Row() {
                Text(this.order.orderNo)
                  .fontSize(14)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(Theme.ink)
                  .layoutWeight(1)
                Text(orderStatusText(this.order.status))
                  .fontSize(11)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(this.order.status === 'READY' || this.order.status === 'PREPARING'
                    ? Theme.orange : Theme.mint)
                  .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                  .borderRadius(99)
                  .backgroundColor(this.order.status === 'PENDING' ? Theme.amberSoft : Theme.orangeSoft)
              }
              .width('100%')
              Text((this.order.createTime || '').replace('T', ' ').substring(0, 16))
                .fontSize(11)
                .fontColor(Theme.faint)
                .width('100%')
            }
            .width('100%')
            .padding(16)
            .borderRadius(20)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .alignItems(HorizontalAlign.Start)

            Column({ space: 8 }) {
              Text('菜品')
                .fontSize(14)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
                .width('100%')
              ForEach(this.order.items || [], (item: OrderItem) => {
                Text(item.dishName + ' ×' + item.quantity)
                  .fontSize(13)
                  .fontColor(Theme.ink)
                  .width('100%')
                  .padding(10)
                  .borderRadius(12)
                  .backgroundColor(Theme.orangeSofter)
              }, (item: OrderItem) => item.dishId + '_' + item.quantity)
              if (this.order.remark) {
                Text(this.order.remark)
                  .fontSize(12)
                  .fontColor(Theme.muted)
                  .width('100%')
              }
            }
            .width('100%')
            .padding(16)
            .borderRadius(20)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .alignItems(HorizontalAlign.Start)

            Column({ space: 10 }) {
              Text('进度')
                .fontSize(14)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
                .width('100%')
              Row({ space: 4 }) {
                this.flowDot('确认', this.flowDone('CONFIRMED'))
                Column().layoutWeight(1).height(2)
                  .backgroundColor(this.flowDone('PREPARING') ? Theme.mint : Theme.line)
                this.flowDot('备餐', this.flowDone('PREPARING'))
                Column().layoutWeight(1).height(2)
                  .backgroundColor(this.flowDone('READY') ? Theme.mint : Theme.line)
                this.flowDot('可取', this.flowDone('READY'))
                Column().layoutWeight(1).height(2)
                  .backgroundColor(this.flowDone('COMPLETED') ? Theme.mint : Theme.line)
                this.flowDot('完成', this.flowDone('COMPLETED'))
              }
              .width('100%')
              .alignItems(VerticalAlign.Center)
            }
            .width('100%')
            .padding(16)
            .borderRadius(20)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })
            .alignItems(HorizontalAlign.Start)

            if (this.order.status === 'PENDING') {
              Button('取消预约')
                .width('100%')
                .height(48)
                .backgroundColor(Theme.white)
                .fontColor(Theme.red)
                .border({ width: 1, color: Theme.line })
                .borderRadius(16)
                .onClick(() => {
                  this.confirmCancel()
                })
            }
            Column().height(24)
          }
          .padding({ left: 16, right: 16 })
        }
        .layoutWeight(1)
        .width('100%')
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.cream)
  }

  @Builder
  flowDot(label: string, done: boolean) {
    Column({ space: 4 }) {
      Column()
        .width(10)
        .height(10)
        .borderRadius(5)
        .backgroundColor(done ? Theme.mint : Theme.line)
      Text(label)
        .fontSize(10)
        .fontColor(done ? Theme.ink : Theme.faint)
    }
  }
}
