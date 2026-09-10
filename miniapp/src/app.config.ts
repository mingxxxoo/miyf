export default defineAppConfig({
  pages: [
    'pages/login/index',
    'pages/index/index',
    'pages/category/index',
    'pages/dish/detail',
    'pages/order/index',
    'pages/order/detail',
    'pages/comment/create',
    'pages/health/index',
    'pages/user/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#FFFDF8',
    navigationBarTitleText: 'miyf',
    navigationBarTextStyle: 'black',
    backgroundColor: '#FFFDF8'
  },
  tabBar: {
    color: '#888888',
    selectedColor: '#FFB36B',
    backgroundColor: '#FFFFFF',
    borderStyle: 'white',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页',
        iconPath: 'assets/tabbar/home.png',
        selectedIconPath: 'assets/tabbar/home-active.png'
      },
      {
        pagePath: 'pages/category/index',
        text: '菜品',
        iconPath: 'assets/tabbar/dish.png',
        selectedIconPath: 'assets/tabbar/dish-active.png'
      },
      {
        pagePath: 'pages/order/index',
        text: '预约',
        iconPath: 'assets/tabbar/order.png',
        selectedIconPath: 'assets/tabbar/order-active.png'
      },
      {
        pagePath: 'pages/user/index',
        text: '我的',
        iconPath: 'assets/tabbar/user.png',
        selectedIconPath: 'assets/tabbar/user-active.png'
      }
    ]
  }
})
