import { router } from '@kit.ArkUI'
import { Theme } from '../../common/AppConfig'
import { MiyfApi } from '../../api/MiyfApi'
import {
  HealthSample,
  HealthTrend,
  assessMetricAlert,
  formatMeasuredTime,
  formatStatValue,
  metricDefOf
} from '../../api/Models'

@Entry
@Component
struct HealthDetail {
  @State metricCode: string = 'HEART_RATE'
  @State label: string = '指标'
  @State unit: string = ''
  @State loading: boolean = true
  @State error: string = ''
  @State rangeDays: number = 30
  @State trend: HealthTrend | null = null
  @State samples: HealthSample[] = []
  @State alert: string = ''

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>
    const raw = params !== undefined && params['metric'] !== undefined ? String(params['metric']) : 'HEART_RATE'
    const def = metricDefOf(raw)
    this.metricCode = def.code
    this.label = def.label
    this.unit = def.unit
    void this.reload()
  }

  private async reload(): Promise<void> {
    this.loading = true
    this.error = ''
    try {
      const [trend, samples] = await Promise.all([
        MiyfApi.fetchMyTrend(this.metricCode, this.rangeDays),
        MiyfApi.fetchMySamples(this.metricCode, 50)
      ])
      this.trend = trend
      this.samples = samples
      this.alert = trend !== null && trend.latest !== undefined
        ? assessMetricAlert(this.metricCode, trend.latest)
        : ''
    } catch (_err) {
      this.error = '加载失败'
      this.trend = null
      this.samples = []
    } finally {
      this.loading = false
    }
  }

  private setRange(days: number): void {
    if (this.rangeDays === days) {
      return
    }
    this.rangeDays = days
    void this.reload()
  }

  @Builder
  rangeChip(days: number, title: string) {
    Text(title)
      .fontSize(12)
      .layoutWeight(1)
      .textAlign(TextAlign.Center)
      .padding({ top: 7, bottom: 7 })
      .borderRadius(9)
      .fontColor(this.rangeDays === days ? Theme.ink : Theme.muted)
      .fontWeight(this.rangeDays === days ? FontWeight.Medium : FontWeight.Regular)
      .backgroundColor(this.rangeDays === days ? Theme.white : Color.Transparent)
      .onClick(() => {
        this.setRange(days)
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
        Text(this.label)
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
            Row() {
              Column() {
                Row() {
                  Text(this.trend !== null ? formatStatValue(this.metricCode, this.trend.latest) : '—')
                    .fontSize(40)
                    .fontWeight(FontWeight.Bold)
                    .fontColor(Theme.ink)
                  Text(this.unit)
                    .fontSize(13)
                    .fontColor(Theme.faint)
                    .margin({ left: 6 })
                }
                .alignItems(VerticalAlign.Bottom)
                Text(this.trend !== null ? formatMeasuredTime(this.trend.latestTime) : '暂无数据')
                  .fontSize(11)
                  .fontColor(Theme.faint)
                  .margin({ top: 6 })
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)
              Text(this.alert.length > 0 ? '需关注' : (this.trend?.latest !== undefined ? '正常' : '暂无'))
                .fontSize(12)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.alert.length > 0 ? Theme.amber : Theme.mint)
                .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                .borderRadius(99)
                .backgroundColor(this.alert.length > 0 ? Theme.amberSoft : Theme.mintSoft)
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 8 })

            if (this.alert.length > 0) {
              Text(this.alert)
                .fontSize(12)
                .fontColor('#8A6A1F')
                .width('100%')
                .padding(12)
                .margin({ left: 16, right: 16, top: 12 })
                .borderRadius(14)
                .backgroundColor(Theme.amberSoft)
            }

            Row({ space: 0 }) {
              this.rangeChip(7, '7 天')
              this.rangeChip(30, '30 天')
              this.rangeChip(90, '90 天')
            }
            .width('100%')
            .padding(4)
            .margin({ left: 16, right: 16, top: 16 })
            .borderRadius(12)
            .backgroundColor('#F6F1E8')

            Row() {
              this.statCell('平均', this.trend !== null ? formatStatValue(this.metricCode, this.trend.avg) : '—')
              this.statCell('最低', this.trend !== null ? formatStatValue(this.metricCode, this.trend.min) : '—')
              this.statCell('最高', this.trend !== null ? formatStatValue(this.metricCode, this.trend.max) : '—')
              this.statCell('记录', this.trend !== null ? String(this.trend.pointCount) : '0')
            }
            .width('100%')
            .margin({ left: 16, right: 16, top: 14 })
            .padding({ top: 16, bottom: 16 })
            .borderRadius(20)
            .backgroundColor(Theme.white)
            .border({ width: 1, color: Theme.line })

            Text('近期记录')
              .fontSize(15)
              .fontWeight(FontWeight.Bold)
              .fontColor(Theme.ink)
              .width('100%')
              .padding({ left: 16, top: 18, bottom: 8 })

            if (this.samples.length === 0) {
              Text('暂无样本').fontColor(Theme.faint).margin({ left: 16 })
            } else {
              Column() {
                ForEach(this.samples.slice(0, 20), (s: HealthSample) => {
                  Row({ space: 12 }) {
                    Column()
                      .width(8)
                      .height(8)
                      .borderRadius(4)
                      .backgroundColor(assessMetricAlert(this.metricCode, s.valueNum).length > 0
                        ? Theme.amber : Theme.mint)
                    Text(formatStatValue(this.metricCode, s.valueNum) + (this.unit.length > 0 ? ' ' + this.unit : ''))
                      .fontSize(14)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(Theme.ink)
                      .width(86)
                    Text(formatMeasuredTime(s.measuredTime))
                      .fontSize(11)
                      .fontColor(Theme.faint)
                      .layoutWeight(1)
                  }
                  .width('100%')
                  .padding({ top: 12, bottom: 12 })
                  .border({ width: { bottom: 1 }, color: '#F7F2E9' })
                }, (s: HealthSample) => s.id)
              }
              .width('100%')
              .padding({ left: 16, right: 16 })
              .margin({ left: 16, right: 16 })
              .borderRadius(20)
              .backgroundColor(Theme.white)
              .border({ width: 1, color: Theme.line })
            }

            Text('异常仅作提示，不构成诊断或就医建议')
              .fontSize(10)
              .fontColor(Theme.faint)
              .width('100%')
              .padding({ left: 16, right: 16, top: 12, bottom: 24 })
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
  statCell(title: string, value: string) {
    Column() {
      Text(value)
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.ink)
      Text(title)
        .fontSize(11)
        .fontColor(Theme.faint)
        .margin({ top: 4 })
    }
    .layoutWeight(1)
  }
}
