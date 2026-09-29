import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import {
  ChefOrder,
  itemSummary,
  nextChefAction,
  nextChefStatus,
  nickInitial,
  orderStatusText
} from '../../api/Models'
import { FormCardUpdater } from '../../platform/FormCardUpdater'
import { common } from '@kit.AbilityKit'

@Entry
@Component
struct ChefOrders {
  @State tab: string = 'ALL'
  @State list: ChefOrder[] = []
  @State all: ChefOrder[] = []
  @State loading: boolean = true
  @State error: string = ''
  private tabs: string[] = ['ALL', 'PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'COMPLETED']

  aboutToAppear(): void {
    void this.reload()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private tabLabel(key: string): string {
    if (key === 'ALL') {
      return '全部'
    }
    return orderStatusText(key)
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      this.all = await MiyfApi.fetchChefOrders()
      if (this.tab === 'ALL') {
        this.list = this.all
      } else {
        this.list = await MiyfApi.fetchChefOrders(this.tab)
      }
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private count(status: string): number {
    return this.all.filter((o: ChefOrder) => o.status === status).length
  }

  private confirmAdvance(order: ChefOrder): void {
    const next = nextChefStatus(order.status)
    const label = nextChefAction(order.status)
    if (next.length === 0) {
      return
    }
    AlertDialog.show({
      title: label,
      message: '确认执行「' + label + '」？',
      primaryButton: {
        value: label,
        action: () => {
          void this.act(order, next, label)
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private async act(order: ChefOrder, status: string, tip: string): Promise<void> {
    try {
      await MiyfApi.updateChefOrderStatus(order.id, status)
      promptAction.showToast({ message: tip })
      await this.reload()
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '操作失败' })
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).onClick(() => {
          router.back()
        })
        Text('处理预约')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ').width(22)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      Row({ space: 10 }) {
        Column() {
          Text(String(this.count('PENDING'))).fontSize(22).fontWeight(FontWeight.Bold).fontColor(Theme.amber)
          Text('待确认').fontSize(11).fontColor(Theme.muted)
        }
        .layoutWeight(1)
        .padding(14)
        .borderRadius(18)
        .backgroundColor(Theme.amberSoft)
        Column() {
          Text(String(this.count('PREPARING'))).fontSize(22).fontWeight(FontWeight.Bold).fontColor(Theme.orange)
          Text('备餐中').fontSize(11).fontColor(Theme.muted)
        }
        .layoutWeight(1)
        .padding(14)
        .borderRadius(18)
        .backgroundColor(Theme.orangeSoft)
        Column() {
          Text(String(this.count('READY'))).fontSize(22).fontWeight(FontWeight.Bold).fontColor(Theme.mint)
          Text('可取餐').fontSize(11).fontColor(Theme.muted)
        }
        .layoutWeight(1)
        .padding(14)
        .borderRadius(18)
        .backgroundColor(Theme.mintSoft)
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 6 })

      Scroll() {
        Row({ space: 8 }) {
          ForEach(this.tabs, (key: string) => {
            Text(this.tabLabel(key))
              .fontSize(12)
              .padding({ left: 14, right: 14, top: 7, bottom: 7 })
              .borderRadius(99)
              .fontColor(this.tab === key ? Theme.cream : Theme.muted)
              .backgroundColor(this.tab === key ? Theme.ink : Theme.white)
              .border({ width: 1, color: Theme.line })
              .onClick(() => {
                this.tab = key
                void this.reload()
              })
          }, (key: string) => key)
        }
        .padding({ left: 16, right: 16 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .height(48)

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted)
      } else if (this.error.length > 0) {
        Text(this.error).fontColor(Theme.red)
      } else if (this.list.length === 0) {
        Text('这一栏暂时没有预约').fontColor(Theme.faint).margin({ top: 24 })
      } else {
        List({ space: 12 }) {
          ForEach(this.list, (order: ChefOrder) => {
            ListItem() {
              Column({ space: 10 }) {
                Row() {
                  Text(nickInitial(order.userNickname))
                    .width(34).height(34).textAlign(TextAlign.Center).borderRadius(17)
                    .fontColor(Theme.white).backgroundColor(Theme.orange)
                  Column() {
                    Text(order.userNickname || '食客').fontSize(13).fontWeight(FontWeight.Medium)
                    Text(order.orderNo).fontSize(10).fontColor(Theme.faint).margin({ top: 2 })
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)
                  .margin({ left: 8 })
                  Text(orderStatusText(order.status))
                    .fontSize(11)
                    .fontColor(order.status === 'PENDING' ? Theme.amber : Theme.mint)
                }
                Text(itemSummary(order.items))
                  .fontSize(12)
                  .width('100%')
                  .padding(10)
                  .borderRadius(12)
                  .backgroundColor(Theme.orangeSofter)
                if (order.remark) {
                  Text(order.remark).fontSize(11).fontColor(Theme.muted)
                }
                if (nextChefAction(order.status).length > 0) {
                  Row({ space: 8 }) {
                    if (order.status === 'PENDING') {
                      Button('驳回')
                        .height(32)
                        .fontSize(12)
                        .backgroundColor(Theme.white)
                        .fontColor(Theme.red)
                        .border({ width: 1, color: Theme.line })
                        .borderRadius(10)
                        .onClick(() => {
                          AlertDialog.show({
                            title: '拒绝预约',
                            message: '确认拒绝这张预约？',
                            primaryButton: {
                              value: '拒绝',
                              fontColor: Theme.red,
                              action: () => {
                                void this.act(order, 'CANCELLED', '已拒绝')
                              }
                            },
                            secondaryButton: { value: '取消', action: () => {} }
                          })
                        })
                    }
                    Button(nextChefAction(order.status))
                      .height(32)
                      .fontSize(12)
                      .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                      .fontColor(Theme.white)
                      .borderRadius(10)
                      .onClick(() => {
                        this.confirmAdvance(order)
                      })
                  }
                  .width('100%')
                  .justifyContent(FlexAlign.End)
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
          }, (order: ChefOrder) => order.id)
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
