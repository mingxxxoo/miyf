import { common, Want } from '@kit.AbilityKit'

/** 用系统浏览器打开授权页；失败时抛错，由页面提示复制链接。 */
export async function openSystemBrowser(context: common.UIAbilityContext, url: string): Promise<void> {
  if (url.length === 0) {
    throw new Error('链接为空')
  }
  const want: Want = {
    action: 'ohos.want.action.viewData',
    entities: ['entity.system.browsable'],
    uri: url
  }
  await context.startAbility(want)
}
