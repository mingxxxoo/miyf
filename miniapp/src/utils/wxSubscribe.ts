import Taro from '@tarojs/taro'
import { get } from '@/api/request'

export interface WxSubscribeConfig {
  enabled: boolean
  templateIds: string[]
}

/** 拉取厨师端订阅消息配置并尝试授权（须用户手势场景更佳，此处用于进入工作台时补充额度） */
export async function fetchChefWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  const raw = await get<WxSubscribeConfig>('/api/chef/wx-subscribe-config', undefined, {
    showError: false
  }).catch(() => null)
  return {
    enabled: Boolean(raw?.enabled),
    templateIds: Array.isArray(raw?.templateIds) ? raw!.templateIds.filter(Boolean) : []
  }
}

/**
 * 请求微信订阅消息授权。
 * 返回 true 表示至少有一个模板被 accept。
 */
export async function requestChefOrderSubscribe(templateIds?: string[]): Promise<boolean> {
  let ids = templateIds
  if (!ids || !ids.length) {
    const cfg = await fetchChefWxSubscribeConfig()
    if (!cfg.enabled || !cfg.templateIds.length) {
      return false
    }
    ids = cfg.templateIds
  }
  try {
    const res = await Taro.requestSubscribeMessage({ tmplIds: ids })
    return ids.some((id) => res[id] === 'accept')
  } catch {
    return false
  }
}
