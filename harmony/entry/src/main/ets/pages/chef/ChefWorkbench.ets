import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { LucideIcon } from '../../common/LucideIcon'
import { AppNav } from '../../session/AppNav'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import {
  ChefOrder,
  UserProfile,
  greetByHour,
  itemSummary,
  nextChefAction,
  nextChefStatus,
  nickInitial,
  orderStatusText
} from '../../api/Models'
import { FormCardUpdater } from '../../platform/FormCardUpdater'
import { common } from '@kit.AbilityKit'

interface GridItem {
  key: string
  label: string
  url: string
  bg: string
  icon: string
}

@Entry
@Component
struct ChefWorkbench {
  @State nickname: string = '厨师'
  @State kitchenName: string = '我的厨房'
  @State pending: number = 0
  @State preparing: number = 0
  @State onSale: number = 0
  @State diners: number = 0
  @State applyCount: number = 0
  @State recent: ChefOrder[] = []
  @State loading: boolean = true
  @State error: string = ''
  @State actingId: string = ''
  private menus: GridItem[] = [
    { key: 'dish', label: '菜品', url: 'pages/chef/ChefDishes', bg: '#FFF1E2', icon: 'utensils' },
    { key: 'recipe', label: '菜谱', url: 'pages/chef/ChefDishes', bg: '#E7F7F0', icon: 'book-open' },
    { key: 'order', label: '预约', url: 'pages/chef/ChefOrders', bg: '#EBF3FB', icon: 'clipboard-list' },
    { key: 'apply', label: '申请', url: 'pages/chef/ChefBindings', bg: '#FCEEEA', icon: 'users' },
    { key: 'invite', label: '邀请码', url: 'pages/chef/ChefKitchen', bg: '#FFF1E2', icon: 'ticket' },
    { key: 'kitchen', label: '资料', url: 'pages/chef/ChefKitchen', bg: '#E7F7F0', icon: 'home' },
    { key: 'ai', label: 'AI 建菜', url: 'pages/chef/ChefAiExtract', bg: '#FFE8C8', icon: 'sparkles' }
  ]

  aboutToAppear(): void {
    void this.reload()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      const me: UserProfile = await MiyfApi.fetchMe()
      this.nickname = me.nickname
      const kitchen = await MiyfApi.fetchMyKitchen()
      if (kitchen !== null && kitchen.name.length > 0) {
        this.kitchenName = kitchen.name
      } else {
        this.kitchenName = '请先完善厨房资料'
      }
      const pendingList = await MiyfApi.fetchChefOrders('PENDING').catch(() => [] as ChefOrder[])
      const preparingList = await MiyfApi.fetchChefOrders('PREPARING').catch(() => [] as ChefOrder[])
      const dishes = await MiyfApi.fetchChefDishes().catch(() => [])
      const bound = await MiyfApi.fetchChefBindings('BOUND').catch(() => [])
      const apply = await MiyfApi.fetchChefBindings('PENDING').catch(() => [])
      const all = await MiyfApi.fetchChefOrders().catch(() => [] as ChefOrder[])
      this.pending = pendingList.length
      this.preparing = preparingList.length
      this.onSale = dishes.filter((d) => d.status === 'ON_SALE').length
      this.diners = bound.length
      this.applyCount = apply.length
      this.recent = all.slice(0, 5)
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private confirmAdvance(order: ChefOrder): void {
    const next = nextChefStatus(order.status)
    const label = nextChefAction(order.status)
    if (next.length === 0) {
      return
    }
    AlertDialog.show({
      title: label,
      message: '确认对「' + (order.userNickname || '食客') + '」执行「' + label + '」？',
      primaryButton: {
        value: label,
        action: () => {
          void this.advance(order, next, label)
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private confirmReject(order: ChefOrder): void {
    AlertDialog.show({
      title: '拒绝预约',
      message: '确认拒绝「' + (order.userNickname || '食客') + '」的预约？',
      primaryButton: {
        value: '拒绝',
        fontColor: Theme.red,
        action: () => {
          void this.advance(order, 'CANCELLED', '已拒绝')
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private async advance(order: ChefOrder, status: string, okTip: string): Promise<void> {
    if (this.actingId.length > 0) {
      return
    }
    this.actingId = order.id
    try {
      await MiyfApi.updateChefOrderStatus(order.id, status)
      promptAction.showToast({ message: okTip })
      await this.reload()
      void FormCardUpdater.refresh(this.context())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '操作失败' })
    } finally {
      this.actingId = ''
    }
  }

  private async backToDiner(): Promise<void> {
    try {
      const me = await MiyfApi.fetchMe()
      if (me.diner) {
        await MiyfApi.switchRole('DINER')
      } else {
        await MiyfApi.chooseRole('DINER')
      }
      await AppNav.afterAuth(await MiyfApi.fetchMe())
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '切换失败' })
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .fontColor(Theme.ink)
          .onClick(() => {
            void this.backToDiner()
          })
        Text('厨房工作台')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.ink)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text('食客')
          .fontSize(13)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.orange)
          .onClick(() => {
            void this.backToDiner()
          })
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 40 })
      } else if (this.error.length > 0) {
        Column({ space: 12 }) {
          Text(this.error).fontColor(Theme.red)
          Button('重试').onClick(() => {
            void this.reload()
          })
        }
        .margin({ top: 40 })
      } else {
        Scroll() {
          Column() {
            Column() {
              Text(greetByHour() + '，' + this.nickname)
                .fontSize(12)
                .fontColor('rgba(255,255,255,0.85)')
              Row({ space: 8 }) {
                Text(this.kitchenName)
                  .fontSize(20)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.white)
                Text('营业中')
                  .fontSize(10)
                  .fontColor(Theme.white)
                  .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                  .borderRadius(99)
                  .backgroundColor('rgba(255,255,255,0.25)')
              }
              .margin({ top: 4 })
              Row() {
                this.statCell(String(this.pending), '待确认')
                this.statCell(String(this.preparing), '备餐中')
                this.statCell(String(this.onSale), '在售')
                this.statCell(String(this.diners), '食客')
              }
              .width('100%')
              .margin({ top: 14 })
              .padding({ top: 12, bottom: 12 })
              .borderRadius(14)
              .backgroundColor('rgba(255,255,255,0.16)')
            }
            .width('100%')
            .padding(18)
            .margin({ left: 16, right: 16, top: 6 })
            .borderRadius(22)
            .linearGradient({ angle: 135, colors: [['#FF9A3E', 0], [Theme.orange, 1]] })
            .alignItems(HorizontalAlign.Start)

            Text('快捷入口')
              .fontSize(15)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.ink)
              .width('100%')
              .padding({ left: 16, top: 18, bottom: 10 })

            Grid() {
              ForEach(this.menus, (item: GridItem) => {
                GridItem() {
                  Stack({ alignContent: Alignment.TopEnd }) {
                    Column({ space: 6 }) {
                      Stack({ alignContent: Alignment.Center }) {
                        LucideIcon(item.icon, 22, '#5A524A')
                      }
                        .width(50)
                        .height(50)
                        .borderRadius(16)
                        .backgroundColor(item.bg)
                      Text(item.label)
                        .fontSize(11)
                        .fontColor(Theme.ink)
                    }
                    if (item.key === 'order' && this.pending > 0) {
                      Text(String(this.pending))
                        .fontSize(9)
                        .fontColor(Theme.white)
                        .constraintSize({ minWidth: 16 })
                        .height(16)
                        .padding({ left: 4, right: 4 })
                        .borderRadius(99)
                        .backgroundColor(Theme.red)
                        .margin({ top: -4, right: 8 })
                    }
                    if (item.key === 'apply' && this.applyCount > 0) {
                      Text(String(this.applyCount))
                        .fontSize(9)
                        .fontColor(Theme.white)
                        .constraintSize({ minWidth: 16 })
                        .height(16)
                        .padding({ left: 4, right: 4 })
                        .borderRadius(99)
                        .backgroundColor(Theme.red)
                        .margin({ top: -4, right: 8 })
                    }
                  }
                  .onClick(() => {
                    router.pushUrl({ url: item.url })
                  })
                }
              }, (item: GridItem) => item.key)
            }
            .columnsTemplate('1fr 1fr 1fr 1fr')
            .rowsGap(14)
            .width('100%')
            .padding({ left: 8, right: 8 })

            Row() {
              Text('待处理')
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
              Blank()
              Text('全部 ›')
                .fontSize(11)
                .fontColor(Theme.faint)
                .onClick(() => {
                  router.pushUrl({ url: 'pages/chef/ChefOrders' })
                })
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 18, bottom: 10 })

            if (this.recent.length === 0) {
              Text('暂时没有预约').fontColor(Theme.faint).margin({ left: 16 })
            } else {
              ForEach(this.recent, (order: ChefOrder) => {
                Column({ space: 10 }) {
                  Row() {
                    Text(nickInitial(order.userNickname))
                      .fontSize(13)
                      .fontColor(Theme.white)
                      .width(34)
                      .height(34)
                      .textAlign(TextAlign.Center)
                      .borderRadius(17)
                      .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                    Column() {
                      Text(order.userNickname || '食客')
                        .fontSize(13)
                        .fontWeight(FontWeight.Medium)
                        .fontColor(Theme.ink)
                      Text((order.createTime || '').replace('T', ' ').substring(0, 16))
                        .fontSize(10)
                        .fontColor(Theme.faint)
                        .margin({ top: 2 })
                    }
                    .alignItems(HorizontalAlign.Start)
                    .layoutWeight(1)
                    .margin({ left: 8 })
                    Text(orderStatusText(order.status))
                      .fontSize(11)
                      .fontColor(order.status === 'PENDING' ? Theme.amber : Theme.mint)
                      .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                      .borderRadius(99)
                      .backgroundColor(order.status === 'PENDING' ? Theme.amberSoft : Theme.mintSoft)
                  }
                  Text(itemSummary(order.items))
                    .fontSize(12)
                    .fontColor(Theme.ink)
                    .width('100%')
                    .padding(10)
                    .borderRadius(12)
                    .backgroundColor(Theme.orangeSofter)
                  Row() {
                    Text(order.remark || '')
                      .fontSize(11)
                      .fontColor(Theme.muted)
                      .layoutWeight(1)
                    if (order.status === 'PENDING') {
                      Button('驳回')
                        .height(30)
                        .fontSize(12)
                        .backgroundColor(Theme.white)
                        .fontColor(Theme.muted)
                        .border({ width: 1, color: Theme.line })
                        .borderRadius(10)
                        .onClick(() => {
                          this.confirmReject(order)
                        })
                    }
                    if (nextChefAction(order.status).length > 0) {
                      Button(nextChefAction(order.status))
                        .height(30)
                        .fontSize(12)
                        .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                        .fontColor(Theme.white)
                        .borderRadius(10)
                        .margin({ left: 8 })
                        .onClick(() => {
                          this.confirmAdvance(order)
                        })
                    }
                  }
                  .width('100%')
                }
                .width('100%')
                .padding(14)
                .margin({ left: 16, right: 16, bottom: 12 })
                .borderRadius(20)
                .backgroundColor(Theme.white)
                .border({ width: 1, color: Theme.line })
                .alignItems(HorizontalAlign.Start)
              }, (order: ChefOrder) => order.id)
            }
            Column().height(20)
          }
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
  statCell(num: string, label: string) {
    Column() {
      Text(num)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.white)
      Text(label)
        .fontSize(10)
        .fontColor('rgba(255,255,255,0.85)')
        .margin({ top: 2 })
    }
    .layoutWeight(1)
  }
}
