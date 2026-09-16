import Taro from '@tarojs/taro'
import { get } from '@/api/request'

export interface WxSubscribeConfig {
  enabled: boolean
  templateIds: string[]
}

/** 预取缓存：授权须在用户手势同步链路内触发，不可先 await 网络再弹窗 */
let cachedEnabled = false
let cachedTemplateIds: string[] = []

/** 拉取厨师端订阅消息配置并写入内存缓存（页面展示时预取，点击时复用） */
export async function fetchChefWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  const raw = await get<WxSubscribeConfig>('/api/chef/wx-subscribe-config', undefined, {
    showError: false
  }).catch(() => null)
  const cfg: WxSubscribeConfig = {
    enabled: Boolean(raw?.enabled),
    templateIds: Array.isArray(raw?.templateIds) ? raw!.templateIds.filter(Boolean) : []
  }
  cachedEnabled = cfg.enabled
  cachedTemplateIds = cfg.templateIds
  return cfg
}

/** 进入厨师工作台时预取模板 ID，供点击手势同步调用 */
export async function prefetchChefWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  return fetchChefWxSubscribeConfig()
}

/**
 * 请求微信订阅消息授权。
 * 仅使用已缓存的模板 ID，避免 await 网络打断用户手势；未预取时直接返回 false。
 * 返回 true 表示至少有一个模板被 accept。
 */
export async function requestChefOrderSubscribe(templateIds?: string[]): Promise<boolean> {
  const ids =
    templateIds && templateIds.length
      ? templateIds
      : cachedEnabled && cachedTemplateIds.length
        ? cachedTemplateIds
        : []
  if (!ids.length) {
    return false
  }
  try {
    const res = await Taro.requestSubscribeMessage({ tmplIds: ids })
    return ids.some((id) => res[id] === 'accept')
  } catch {
    return false
  }
}
