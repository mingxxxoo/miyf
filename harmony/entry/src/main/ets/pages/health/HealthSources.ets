import { AlertDialog, promptAction, router } from '@kit.ArkUI'
import { common } from '@kit.AbilityKit'
import { Theme } from '../../common/AppConfig'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import { HealthProvider, HealthProviderBinding, formatMeasuredTime } from '../../api/Models'
import { openSystemBrowser } from '../../platform/OpenBrowser'
import { pasteboard } from '@kit.BasicServicesKit'

@Entry
@Component
struct HealthSources {
  @State loading: boolean = true
  @State error: string = ''
  @State authorized: boolean = false
  @State busy: boolean = false
  @State polling: boolean = false
  @State authorizeUrl: string = ''
  @State lastSync: string = ''
  @State providers: HealthProvider[] = []
  @State bindings: HealthProviderBinding[] = []
  private pollTimer: number = -1
  private pollDeadline: number = 0

  aboutToAppear(): void {
    void this.reload()
  }

  aboutToDisappear(): void {
    this.stopPoll()
  }

  private context(): common.UIAbilityContext {
    return getContext(this) as common.UIAbilityContext
  }

  private stopPoll(): void {
    if (this.pollTimer >= 0) {
      clearInterval(this.pollTimer)
      this.pollTimer = -1
    }
    this.polling = false
  }

  private startPoll(): void {
    this.stopPoll()
    this.pollDeadline = Date.now() + 3 * 60 * 1000
    this.polling = true
    this.pollTimer = setInterval(() => {
      if (Date.now() > this.pollDeadline) {
        this.stopPoll()
        promptAction.showToast({ message: '仍未检测到授权，可稍后下拉刷新' })
        return
      }
      void this.refreshStatus(true)
    }, 3000)
  }

  private async refreshStatus(quiet: boolean): Promise<boolean> {
    try {
      const st = await MiyfApi.fetchHuaweiOAuthStatus()
      this.authorized = st.authorized
      if (st.authorized && this.polling) {
        this.stopPoll()
        this.authorizeUrl = ''
        promptAction.showToast({ message: '华为已连接，正在同步' })
        void this.sync(true)
      }
      return st.authorized
    } catch (_err) {
      if (!quiet) {
        this.authorized = false
      }
      return false
    }
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      await MiyfApi.fetchMyHealth()
      await this.refreshStatus(true)
      this.providers = await MiyfApi.fetchProviders()
      this.bindings = await MiyfApi.fetchHealthBindings()
      const hw = this.bindings.find((b: HealthProviderBinding) => b.providerCode === 'huawei')
      this.lastSync = hw !== undefined ? formatMeasuredTime(hw.lastSyncTime) : ''
      if (this.providers.length === 0) {
        this.providers = [{ code: 'huawei', displayName: '华为运动健康', enabled: true }]
      }
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  private async connect(): Promise<void> {
    this.busy = true
    try {
      const result = await MiyfApi.fetchHuaweiAuthorizeUrl()
      if (result.authorizeUrl.length === 0) {
        promptAction.showToast({ message: '未返回授权地址' })
        return
      }
      this.authorizeUrl = result.authorizeUrl
      this.startPoll()
      try {
        await openSystemBrowser(this.context(), result.authorizeUrl)
      } catch (_err) {
        await this.copyUrl(result.authorizeUrl)
        promptAction.showToast({ message: '无法打开浏览器，链接已复制' })
      }
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '获取授权失败' })
    } finally {
      this.busy = false
    }
  }

  private async copyUrl(url: string): Promise<void> {
    try {
      const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, url)
      await pasteboard.getSystemPasteboard().setData(data)
    } catch (_err) {
    }
  }

  private async sync(quiet: boolean = false): Promise<void> {
    this.busy = true
    try {
      const run = await MiyfApi.syncMyProvider('huawei')
      if (!quiet) {
        if (run.status === 'FAILED') {
          promptAction.showToast({
            message: (run.errorMessage || '同步失败').substring(0, 40)
          })
        } else {
          const extra = run.ingestedCount !== undefined ? '，入库 ' + run.ingestedCount + ' 条' : ''
          promptAction.showToast({ message: ('同步成功' + extra).substring(0, 40) })
        }
      }
      this.lastSync = formatMeasuredTime(new Date().toISOString())
      await this.reload()
    } catch (err) {
      if (!quiet) {
        promptAction.showToast({
          message: err instanceof ApiError ? err.message : '暂不可用'
        })
      }
    } finally {
      this.busy = false
    }
  }

  private confirmRevoke(): void {
    AlertDialog.show({
      title: '解除授权',
      message: '解除后将停止从华为运动健康同步数据。',
      primaryButton: {
        value: '解除',
        fontColor: Theme.red,
        action: () => {
          void this.revoke()
        }
      },
      secondaryButton: {
        value: '取消',
        action: () => {
        }
      }
    })
  }

  private async revoke(): Promise<void> {
    this.busy = true
    try {
      await MiyfApi.revokeHuaweiOAuth()
      this.authorized = false
      this.stopPoll()
      promptAction.showToast({ message: '已解除授权' })
      await this.reload()
    } catch (err) {
      promptAction.showToast({ message: err instanceof ApiError ? err.message : '解除失败' })
    } finally {
      this.busy = false
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .onClick(() => {
            router.back()
          })
        Text('数据源')
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
            Text('优先连接华为运动健康。仅读取步数、心率、睡眠、体重等，可随时解除。')
              .fontSize(12)
              .fontColor(Theme.muted)
              .width('100%')
              .padding({ left: 16, right: 16, top: 4, bottom: 8 })

            Column() {
              Row({ space: 12 }) {
                Text('⌚')
                  .width(42)
                  .height(42)
                  .textAlign(TextAlign.Center)
                  .borderRadius(14)
                  .backgroundColor(Theme.mintSoft)
                  .fontSize(20)
                Column() {
                  Row({ space: 6 }) {
                    Text('华为运动健康')
                      .fontSize(14)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(Theme.ink)
                    Text(this.authorized ? '已连接' : (this.polling ? '授权中' : '未连接'))
                      .fontSize(10)
                      .fontColor(this.authorized ? Theme.mint : Theme.amber)
                      .padding({ left: 7, right: 7, top: 2, bottom: 2 })
                      .borderRadius(99)
                      .backgroundColor(this.authorized ? Theme.mintSoft : Theme.amberSoft)
                  }
                  Text('步数 · 心率 · 睡眠 · 体重')
                    .fontSize(11)
                    .fontColor(Theme.faint)
                    .margin({ top: 3 })
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)
              }
              .width('100%')

              if (this.authorized) {
                Row({ space: 8 }) {
                  Column()
                    .width(7)
                    .height(7)
                    .borderRadius(99)
                    .backgroundColor(Theme.mint)
                  Text(this.lastSync.length > 0 ? '上次同步 ' + this.lastSync : '已授权，可立即同步')
                    .fontSize(11)
                    .fontColor(Theme.muted)
                }
                .width('100%')
                .padding(10)
                .margin({ top: 10 })
                .borderRadius(12)
                .backgroundColor(Theme.mintSofter)

                Row({ space: 8 }) {
                  Button(this.busy ? '同步中…' : '立即同步')
                    .layoutWeight(1)
                    .height(36)
                    .linearGradient({ angle: 135, colors: [[Theme.mint2, 0], [Theme.mint, 1]] })
                    .fontColor(Theme.white)
                    .borderRadius(12)
                    .enabled(!this.busy)
                    .onClick(() => {
                      void this.sync(false)
                    })
                  Button('解除授权')
                    .layoutWeight(1)
                    .height(36)
                    .backgroundColor(Theme.white)
                    .fontColor(Theme.red)
                    .border({ width: 1, color: '#F3D3CE' })
                    .borderRadius(12)
                    .enabled(!this.busy)
                    .onClick(() => {
                      this.confirmRevoke()
                    })
                }
                .width('100%')
                .margin({ top: 12 })
              } else {
                Button(this.busy ? '请稍候…' : (this.polling ? '等待授权完成…' : '去授权'))
                  .width('100%')
                  .height(44)
                  .margin({ top: 14 })
                  .linearGradient({ angle: 135, colors: [[Theme.mint2, 0], [Theme.mint, 1]] })
                  .fontColor(Theme.white)
                  .borderRadius(14)
                  .enabled(!this.busy)
                  .onClick(() => {
                    void this.connect()
                  })
                if (this.authorizeUrl.length > 0) {
                  Text('若浏览器未打开，可复制链接在手机浏览器完成授权')
                    .fontSize(11)
                    .fontColor(Theme.faint)
                    .margin({ top: 10 })
                  Button('复制授权链接')
                    .width('100%')
                    .height(36)
                    .margin({ top: 8 })
                    .backgroundColor(Theme.white)
                    .fontColor(Theme.mint)
                    .border({ width: 1, color: Theme.line })
                    .borderRadius(12)
                    .onClick(() => {
                      void this.copyUrl(this.authorizeUrl).then(() => {
                        promptAction.showToast({ message: '已复制' })
                        if (!this.polling) {
                          this.startPoll()
                        }
                      })
                    })
                }
              }
            }
            .width('100%')
            .padding(14)
            .margin({ left: 16, right: 16, top: 8 })
            .borderRadius(20)
            .backgroundColor(Theme.white)
            .border({
              width: this.authorized ? 1.5 : 1,
              color: this.authorized ? '#CBEBDD' : Theme.line
            })
            .alignItems(HorizontalAlign.Start)

            ForEach(this.providers.filter((p: HealthProvider) => p.code !== 'huawei'),
              (p: HealthProvider) => {
                Row({ space: 12 }) {
                  Text('⋯')
                    .width(42)
                    .height(42)
                    .textAlign(TextAlign.Center)
                    .borderRadius(14)
                    .backgroundColor('#F4F0E9')
                  Column() {
                    Row({ space: 6 }) {
                      Text(p.displayName)
                        .fontSize(14)
                        .fontWeight(FontWeight.Medium)
                      Text('即将支持')
                        .fontSize(9)
                        .fontColor(Theme.faint)
                        .padding({ left: 7, right: 7, top: 2, bottom: 2 })
                        .borderRadius(99)
                        .backgroundColor('#F4F0E9')
                    }
                    Text('暂不可用')
                      .fontSize(11)
                      .fontColor(Theme.faint)
                      .margin({ top: 3 })
                  }
                  .alignItems(HorizontalAlign.Start)
                }
                .width('100%')
                .padding(14)
                .margin({ left: 16, right: 16, top: 12 })
                .borderRadius(20)
                .backgroundColor(Theme.white)
                .border({ width: 1, color: Theme.line })
                .opacity(0.55)
              }, (p: HealthProvider) => p.code)

            Text('授权失败或同步失败时会提示「暂不可用」，不会空白。')
              .fontSize(11)
              .fontColor(Theme.faint)
              .width('100%')
              .padding({ left: 16, right: 16, top: 16, bottom: 24 })
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
    .onVisibleAreaChange([0.0, 1.0], (visible: boolean) => {
      if (visible && this.polling) {
        void this.refreshStatus(true)
      }
    })
  }
}
