import { common } from '@kit.AbilityKit'
import { preferences } from '@kit.ArkData'
import { ApiClient } from '../api/ApiClient'

const NAME = 'miyf_session'
const TOKEN = 'token'
const EXPIRE_AT = 'expireAt'

export class SessionStore {
  static async save(context: common.UIAbilityContext, token: string, expireSeconds: number): Promise<void> {
    const pref = await preferences.getPreferences(context, NAME)
    await pref.put(TOKEN, token)
    const skew = expireSeconds > 60 ? 60 : 0
    const expireAt = expireSeconds > 0 ? Date.now() + (expireSeconds - skew) * 1000 : 0
    await pref.put(EXPIRE_AT, expireAt)
    await pref.flush()
    ApiClient.setToken(token)
  }

  static async load(context: common.UIAbilityContext): Promise<string> {
    const pref = await preferences.getPreferences(context, NAME)
    const token = await pref.get(TOKEN, '') as string
    const expireAt = await pref.get(EXPIRE_AT, 0) as number
    if (token.length === 0) {
      ApiClient.setToken('')
      return ''
    }
    if (expireAt > 0 && Date.now() >= expireAt) {
      await SessionStore.clear(context)
      return ''
    }
    ApiClient.setToken(token)
    return token
  }

  static async clear(context: common.UIAbilityContext): Promise<void> {
    const pref = await preferences.getPreferences(context, NAME)
    await pref.delete(TOKEN)
    await pref.delete(EXPIRE_AT)
    await pref.flush()
    ApiClient.setToken('')
  }
}
