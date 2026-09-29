import { router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { LucideIcon } from '../../common/LucideIcon'
import { ApiError } from '../../api/ApiClient'
import { MiyfApi } from '../../api/MiyfApi'
import {
  HealthTrend,
  METRIC_DEFS,
  MetricDef,
  assessMetricAlert,
  formatMeasuredTime,
  formatStatValue,
  healthScoreOf
} from '../../api/Models'

interface MetricCard {
  def: MetricDef
  trend: HealthTrend | null
  alert: string
}

@Entry
@Component
struct HealthHome {
  @State loading: boolean = true
  @State error: string = ''
  @State cards: MetricCard[] = []
  @State score: number = 0
  @State scoreTitle: string = '暂无数据'
  @State alertBanner: string = ''
  @State syncHint: string = ''
  @State huaweiAuthorized: boolean = false

  aboutToAppear(): void {
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      await MiyfApi.fetchMyHealth()
      const oauth = await MiyfApi.fetchHuaweiOAuthStatus()
      this.huaweiAuthorized = oauth.authorized
      const next: MetricCard[] = []
      let withData = 0
      let alerts = 0
      let firstAlert = ''
      let latestTs = 0
      for (let i = 0; i < METRIC_DEFS.length; i++) {
        const def = METRIC_DEFS[i]
        const trend = await MiyfApi.fetchMyTrend(def.code, 30)
        const alert = trend !== null && trend.latest !== undefined
          ? assessMetricAlert(def.code, trend.latest)
          : ''
        if (trend !== null && trend.pointCount > 0) {
          withData += 1
        }
        if (alert.length > 0) {
          alerts += 1
          if (firstAlert.length === 0) {
            firstAlert = alert
          }
        }
        if (trend !== null && trend.latestTime !== undefined && trend.latestTime.length > 0) {
          const t = new Date(trend.latestTime.replace(' ', 'T')).getTime()
          if (!Number.isNaN(t) && t > latestTs) {
            latestTs = t
          }
        }
        next.push({ def: def, trend: trend, alert: alert })
      }
      this.cards = next
      this.score = healthScoreOf(withData, alerts)
      if (withData === 0) {
        this.scoreTitle = '暂无数据'
      } else if (alerts === 0) {
        this.scoreTitle = '状态不错，继续保持'
      } else if (alerts <= 2) {
        this.scoreTitle = '需要留意一下'
      } else {
        this.scoreTitle = '建议多关注身体'
      }
      this.alertBanner = firstAlert
      this.syncHint = latestTs > 0
        ? '数据更新至 ' + formatMeasuredTime(new Date(latestTs).toISOString())
        : (this.huaweiAuthorized ? '已连接华为运动健康' : '尚未连接数据源')
    } catch (err) {
      this.error = err instanceof ApiError ? err.message : '加载失败'
    } finally {
      this.loading = false
    }
  }

  build() {
    Column() {
      Row() {
        Text('‹')
          .fontSize(22)
          .fontColor(Theme.ink)
          .onClick(() => {
            router.back()
          })
        Text('健康')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor(Theme.ink)
          .layoutWeight(1)
          .margin({ left: 8 })
        Row({ space: 5 }) {
          Column()
            .width(7)
            .height(7)
            .borderRadius(99)
            .backgroundColor(Theme.mint)
          Text('胡闹健康')
            .fontSize(11)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.ink)
        }
        .padding({ left: 12, right: 12, top: 5, bottom: 5 })
        .borderRadius(99)
        .backgroundColor(Theme.white)
        .border({ width: 1, color: Theme.line })
        .onClick(() => {
          router.pushUrl({ url: 'pages/health/HealthSources' })
        })
      }
      .width('100%')
      .padding({ left: 12, right: 16, top: 12, bottom: 6 })

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
            Row({ space: 14 }) {
              Stack() {
                Progress({ value: this.score, total: 100, type: ProgressType.Ring })
                  .width(92)
                  .height(92)
                  .style({ strokeWidth: 8 })
                  .color(Theme.white)
                  .backgroundColor('rgba(255,255,255,0.22)')
                Column() {
                  Text(String(this.score))
                    .fontSize(24)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(Theme.white)
                  Text('综合')
                    .fontSize(9)
                    .fontColor('rgba(255,255,255,0.85)')
                }
              }
              Column() {
                Text(this.scoreTitle)
                  .fontSize(15)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Theme.white)
                Text(this.huaweiAuthorized
                  ? '数据来自华为运动健康'
                  : '连接华为运动健康后可自动同步')
                  .fontSize(11)
                  .fontColor('rgba(255,255,255,0.88)')
                  .margin({ top: 6 })
                Text(this.syncHint)
                  .fontSize(10)
                  .fontColor('rgba(255,255,255,0.75)')
                  .margin({ top: 8 })
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)
            }
            .width('100%')
            .padding(18)
            .margin({ left: 16, right: 16, top: 8 })
            .borderRadius(22)
            .linearGradient({ angle: 135, colors: [['#1FBF92', 0], ['#0E9E77', 1]] })

            if (this.alertBanner.length > 0) {
              Row({ space: 10 }) {
                Text('!')
                  .width(30)
                  .height(30)
                  .textAlign(TextAlign.Center)
                  .borderRadius(10)
                  .backgroundColor('#F5DFAF')
                  .fontColor(Theme.amber)
                Text(this.alertBanner)
                  .fontSize(12)
                  .fontColor('#8A6A1F')
                  .layoutWeight(1)
                Text('查看')
                  .fontSize(11)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(Theme.amber)
              }
              .width('100%')
              .padding(12)
              .margin({ left: 16, right: 16, top: 12 })
              .borderRadius(14)
              .backgroundColor(Theme.amberSoft)
              .border({ width: 1, color: '#F0DDB4' })
            }

            Row() {
              Text('今日指标')
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor(Theme.ink)
              Blank()
              Text('数据源 ›')
                .fontSize(11)
                .fontColor(Theme.faint)
                .onClick(() => {
                  router.pushUrl({ url: 'pages/health/HealthSources' })
                })
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 18, bottom: 8 })

            if (this.cards.every((c: MetricCard) => c.trend === null || c.trend.pointCount === 0)) {
              Column({ space: 10 }) {
                Text('还没有健康数据')
                  .fontSize(14)
                  .fontColor(Theme.muted)
                Text('去连接华为运动健康')
                  .fontSize(13)
                  .fontWeight(FontWeight.Medium)
                  .fontColor(Theme.mint)
                  .onClick(() => {
                    router.pushUrl({ url: 'pages/health/HealthSources' })
                  })
              }
              .width('100%')
              .padding(24)
              .margin({ left: 16, right: 16 })
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
            } else {
              Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
                ForEach(this.cards, (card: MetricCard) => {
                  Column() {
                    Row() {
                      Stack({ alignContent: Alignment.Center }) {
                        LucideIcon(card.def.icon, 16, '#5A524A')
                      }
                        .width(28)
                        .height(28)
                        .borderRadius(9)
                        .backgroundColor(card.def.bg)
                      Text(card.def.label)
                        .fontSize(11)
                        .fontColor(Theme.muted)
                        .layoutWeight(1)
                        .margin({ left: 6 })
                      if (card.alert.length > 0) {
                        Text('需关注')
                          .fontSize(9)
                          .fontColor(Theme.amber)
                          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                          .borderRadius(99)
                          .backgroundColor(Theme.amberSoft)
                      } else if (card.trend !== null && card.trend.latest !== undefined) {
                        Text('正常')
                          .fontSize(9)
                          .fontColor(Theme.mint)
                          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                          .borderRadius(99)
                          .backgroundColor(Theme.mintSoft)
                      }
                    }
                    .width('100%')
                    Row() {
                      Text(card.trend !== null ? formatStatValue(card.def.code, card.trend.latest) : '—')
                        .fontSize(20)
                        .fontWeight(FontWeight.Bold)
                        .fontColor(Theme.ink)
                      Text(card.def.unit)
                        .fontSize(10)
                        .fontColor(Theme.faint)
                        .margin({ left: 3 })
                    }
                    .margin({ top: 8 })
                    .alignItems(VerticalAlign.Bottom)
                    Text(card.trend !== null ? formatMeasuredTime(card.trend.latestTime) : '暂无记录')
                      .fontSize(9)
                      .fontColor(Theme.faint)
                      .margin({ top: 6 })
                  }
                  .width('48%')
                  .padding(12)
                  .margin({ bottom: 10 })
                  .borderRadius(18)
                  .backgroundColor(Theme.white)
                  .border({ width: 1, color: Theme.line })
                  .alignItems(HorizontalAlign.Start)
                  .onClick(() => {
                    router.pushUrl({
                      url: 'pages/health/HealthDetail',
                      params: { metric: card.def.code }
                    })
                  })
                }, (card: MetricCard) => card.def.code)
              }
              .width('100%')
              .padding({ left: 16, right: 16 })
            }

            Text('以上为一般性生活方式参考，不构成医疗建议')
              .fontSize(10)
              .fontColor(Theme.faint)
              .width('100%')
              .padding({ left: 16, right: 16, top: 8, bottom: 24 })
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
      if (visible && !this.loading) {
        void MiyfApi.fetchHuaweiOAuthStatus().then((st) => {
          this.huaweiAuthorized = st.authorized
        })
      }
    })
  }
}
