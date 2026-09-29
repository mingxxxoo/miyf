export class Theme {
  static readonly cream: string = '#FFFDF8'
  static readonly ink: string = '#2B2622'
  static readonly muted: string = '#8A8178'
  static readonly faint: string = '#B5AC9F'
  static readonly line: string = '#F1E9DD'
  static readonly orange: string = '#F07B1F'
  static readonly orange2: string = '#FFB36B'
  static readonly orangeSoft: string = '#FFF1E2'
  static readonly orangeSofter: string = '#FFF8EF'
  static readonly mint: string = '#18A885'
  static readonly mint2: string = '#5FCFAE'
  static readonly mintSoft: string = '#E7F7F0'
  static readonly mintSofter: string = '#F4FBF8'
  static readonly white: string = '#FFFFFF'
  static readonly red: string = '#E15A4B'
  static readonly redSoft: string = '#FCEEEA'
  static readonly amber: string = '#D99426'
  static readonly amberSoft: string = '#FBF3E0'
  static readonly blue: string = '#4A90D9'
  static readonly blueSoft: string = '#EBF3FB'
}

/** 开发联调打开；正式包改为 false。服务端 mock 关闭时，此按钮也无法登录。 */
export class AppConfig {
  static readonly API_BASE: string = 'https://www.miyf.cn'
  static readonly DEV_LOGIN: boolean = true
}
