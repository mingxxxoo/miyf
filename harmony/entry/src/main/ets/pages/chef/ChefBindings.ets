import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { Binding, bindingStatusText, nickInitial } from '../../api/Models'

@Entry
@Component
struct ChefBindings {
  @State list: Binding[] = []
  @State loading: boolean = true
  @State error: string = ''

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      this.list = await MiyfApi.fetchChefBindings()
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private async approve(id: string): Promise<void> {
    try {
      await MiyfApi.approveBinding(id)
      promptAction.showToast({ message: '已通过' })
      await this.reload()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '操作失败' })
    }
  }

  private reject(item: Binding): void {
    AlertDialog.show({
      title: '拒绝申请',
      message: '确认拒绝「' + (item.dinerNickname || '食客') + '」？',
      primaryButton: {
        value: '拒绝',
        fontColor: Theme.red,
        action: () => {
          void this.doReject(item.id)
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private async doReject(id: string): Promise<void> {
    try {
      await MiyfApi.rejectBinding(id, '厨房暂不接待')
      promptAction.showToast({ message: '已拒绝' })
      await this.reload()
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
        Text('食客申请')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text(' ').width(22)
      }
      .width('100%')
      .padding({ left: 12, right: 12, top: 10, bottom: 6 })

      Text('通过后对方即可浏览你的菜单并预约')
        .fontSize(12)
        .fontColor(Theme.muted)
        .width('100%')
        .padding({ left: 16, bottom: 8 })

      if (this.loading) {
        Text('加载中…').fontColor(Theme.muted).margin({ top: 24 })
      } else if (this.error.length > 0) {
        Text(this.error).fontColor(Theme.red)
      } else if (this.list.length === 0) {
        Text('还没有申请，把邀请码发给食客')
          .fontColor(Theme.faint)
          .margin({ top: 24 })
      } else {
        List({ space: 12 }) {
          ForEach(this.list, (item: Binding) => {
            ListItem() {
              Column({ space: 8 }) {
                Row({ space: 10 }) {
                  Text(nickInitial(item.dinerNickname))
                    .width(36).height(36).textAlign(TextAlign.Center).borderRadius(18)
                    .fontColor(Theme.white).backgroundColor(Theme.orange)
                  Column() {
                    Text(item.dinerNickname || '食客')
                      .fontSize(14)
                      .fontWeight(FontWeight.Medium)
                    Text(bindingStatusText(item.status))
                      .fontSize(11)
                      .fontColor(item.status === 'PENDING' ? Theme.amber : Theme.muted)
                      .margin({ top: 2 })
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)
                }
                if (item.rejectReason) {
                  Text(item.rejectReason).fontSize(11).fontColor(Theme.red)
                }
                if (item.status === 'PENDING' && !item.ownerSelf) {
                  Row({ space: 8 }) {
                    Button('拒绝')
                      .layoutWeight(1)
                      .height(36)
                      .backgroundColor(Theme.white)
                      .fontColor(Theme.red)
                      .border({ width: 1, color: '#F3D3CE' })
                      .borderRadius(12)
                      .onClick(() => {
                        this.reject(item)
                      })
                    Button('通过')
                      .layoutWeight(1)
                      .height(36)
                      .linearGradient({ angle: 135, colors: [[Theme.orange2, 0], [Theme.orange, 1]] })
                      .fontColor(Theme.white)
                      .borderRadius(12)
                      .onClick(() => {
                        void this.approve(item.id)
                      })
                  }
                  .width('100%')
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
          }, (item: Binding) => item.id)
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
