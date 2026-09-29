import { Theme } from '../common/AppConfig'
import { AppNav } from '../session/AppNav'
import { HomeTab } from './tabs/HomeTab'
import { DishTab } from './tabs/DishTab'
import { OrderTab } from './tabs/OrderTab'
import { MineTab } from './tabs/MineTab'

@Entry
@Component
struct Main {
  @State current: number = 0

  aboutToAppear(): void {
    this.current = AppNav.tabIndex
    AppNav.watch(() => {
      this.current = AppNav.tabIndex
    })
  }

  @Builder
  tabBuilder(title: string, target: number) {
    Column({ space: 4 }) {
      Column()
        .width(22)
        .height(4)
        .borderRadius(99)
        .backgroundColor(this.current === target ? Theme.orange : Color.Transparent)
      Text(title)
        .fontSize(10)
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
      AppNav.tabIndex = index
    })
  }
}
