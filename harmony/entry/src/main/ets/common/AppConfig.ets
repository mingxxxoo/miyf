export class Theme {
  static readonly cream: string = '#FFFDF8'
  static readonly ink: string = '#2B2622'
  static readonly muted: string = '#8A8178'
  static readonly faint: string = '#B5AC9F'
  static readonly line: string = '#F1E9DD'
  static readonly orange: string = '#F07B1F'
  static readonly orangeSoft: string = '#FFF1E2'
  static readonly white: string = '#FFFFFF'
  static readonly red: string = '#E15A4B'
}

/** 开发联调打开；正式包改为 false。服务端 mock 关闭时，此按钮也无法登录。 */
export class AppConfig {
  static readonly API_BASE: string = 'https://www.miyf.cn'
  static readonly DEV_LOGIN: boolean = true
}
