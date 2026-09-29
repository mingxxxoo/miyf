const storage = new LocalStorage()

@Entry(storage)
@Component
struct OrderStatusCard {
  @LocalStorageProp('kitchenName') kitchenName: string = '胡闹厨房'
  @LocalStorageProp('statusText') statusText: string = ''
  @LocalStorageProp('statusHint') statusHint: string = ''
  @LocalStorageProp('dishSummary') dishSummary: string = ''
  @LocalStorageProp('orderId') orderId: string = ''
  @LocalStorageProp('hasActive') hasActive: string = '0'
  @LocalStorageProp('emptyTip') emptyTip: string = '暂无进行中的预约'

  build() {
    Column() {
      Text('MIYF')
        .fontSize(10)
        .fontWeight(FontWeight.Bold)
        .fontColor('#F07B1F')
        .letterSpacing(0.5)
        .width('100%')

      Text(this.kitchenName)
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor('#2B2622')
        .margin({ top: 8 })
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')

      if (this.hasActive === '1') {
        Row({ space: 6 }) {
          Column()
            .width(7)
            .height(7)
            .borderRadius(99)
            .backgroundColor('#F07B1F')
          Text(this.statusText + (this.statusHint.length > 0 ? ' · ' + this.statusHint : ''))
            .fontSize(12)
            .fontWeight(FontWeight.Medium)
            .fontColor('#F07B1F')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ top: 10 })

        Text(this.dishSummary)
          .fontSize(11)
          .fontColor('#8A8178')
          .margin({ top: 6 })
          .maxLines(2)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .width('100%')
      } else {
        Text(this.emptyTip)
          .fontSize(12)
          .fontColor('#B5AC9F')
          .margin({ top: 14 })
          .width('100%')
      }

      Blank()
      Text(this.hasActive === '1' ? '查看预约 ›' : '打开 miyf ›')
        .fontSize(10)
        .fontColor('#B5AC9F')
        .width('100%')
    }
    .width('100%')
    .height('100%')
    .padding(14)
    .linearGradient({
      angle: 145,
      colors: [['#FFFDF8', 0], ['#FFF1E2', 1]]
    })
    .borderRadius(20)
    .onClick(() => {
      postCardAction(this, {
        action: 'router',
        bundleName: 'cn.miyf.harmony',
        abilityName: 'EntryAbility',
        params: {
          targetPage: this.hasActive === '1' && this.orderId.length > 0 ? 'orderDetail' : 'main',
          orderId: this.orderId
        }
      })
    })
  }
}
