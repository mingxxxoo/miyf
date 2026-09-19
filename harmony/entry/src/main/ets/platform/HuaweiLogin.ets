import { authentication } from '@kit.AccountKit'
import { common } from '@kit.AbilityKit'
import { util } from '@kit.ArkTS'

/**
 * 向 Account Kit 申请授权码，交给服务端换票。
 * 真机/模拟器需登录华为账号；联调可走登录页「开发登录」。
 */
export async function requestHuaweiAuthorizationCode(context: common.UIAbilityContext): Promise<string> {
  const provider = new authentication.HuaweiIDProvider()
  const authRequest = provider.createAuthorizationWithHuaweiIDRequest()
  authRequest.scopes = ['openid', 'profile']
  authRequest.forceAuthorization = true
  authRequest.state = util.generateRandomUUID()
  const controller = new authentication.AuthenticationController(context)
  const data = await controller.executeRequest(authRequest)
  const response = data as authentication.AuthorizationWithHuaweiIDResponse
  const code = response.data?.authorizationCode
  if (code === undefined || code.length === 0) {
    throw new Error('未取得华为授权码')
  }
  return code
}
