import { Theme } from '../common/AppConfig'
import { HomeTab } from './tabs/HomeTab'
import { DishTab } from './tabs/DishTab'
import { OrderTab } from './tabs/OrderTab'
import { MineTab } from './tabs/MineTab'

@Entry
@Component
struct Main {
  @State current: number = 0

  @Builder
  tabBuilder(title: string, target: number) {
    Column() {
      Text(title)
        .fontSize(12)
        .fontColor(this.current === target ? Theme.orange : Theme.faint)
        .fontWeight(this.current === target ? FontWeight.Medium : FontWeight.Regular)
    }
  }

  build() {
    Tabs({ barPosition: BarPosition.End, index: this.current }) {
      TabContent() {
        HomeTab()
      }.tabBar(this.tabBuilder('首页', 0))

      TabContent() {
        DishTab()
      }.tabBar(this.tabBuilder('菜品', 1))

      TabContent() {
        OrderTab()
      }.tabBar(this.tabBuilder('预约', 2))

      TabContent() {
        MineTab()
      }.tabBar(this.tabBuilder('我的', 3))
    }
    .width('100%')
    .height('100%')
    .barBackgroundColor(Theme.white)
    .onChange((index: number) => {
      this.current = index
    })
  }
}
