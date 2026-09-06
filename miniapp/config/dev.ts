import type { UserConfigExport } from '@tarojs/cli'

export default {
  logger: {
    quiet: false,
    stats: true
  },
  defineConstants: {
    'process.env.TARO_APP_API_BASE': JSON.stringify(
      process.env.TARO_APP_API_BASE || 'http://www.miyf.cn'
    )
  },
  mini: {},
  h5: {}
} satisfies UserConfigExport<'webpack5'>
