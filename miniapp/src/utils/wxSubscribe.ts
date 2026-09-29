import Taro from '@tarojs/taro'
import {get} from '@/api/request'

export interface WxSubscribeConfig {
  enabled: boolean
  templateIds: string[]
}

/** 厨师：新预约提醒 */
let chefEnabled = false
let chefTemplateIds: string[] = []

/** 食客：预约状态变更提醒 */
let dinerEnabled = false
let dinerTemplateIds: string[] = []

function normalizeConfig(raw: WxSubscribeConfig | null): WxSubscribeConfig {
  return {
    enabled: Boolean(raw?.enabled),
    templateIds: Array.isArray(raw?.templateIds) ? raw!.templateIds.filter(Boolean) : []
  }
}

async function requestSubscribe(ids: string[]): Promise<boolean> {
  if (!ids.length) return false
  try {
    const res = await Taro.requestSubscribeMessage({ tmplIds: ids })
    return ids.some((id) => res[id] === 'accept')
  } catch {
    return false
  }
}

/** 拉取厨师端订阅消息配置并写入内存缓存（页面展示时预取，点击时复用） */
export async function fetchChefWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  const raw = await get<WxSubscribeConfig>('/api/chef/wx-subscribe-config', undefined, {
    showError: false
  }).catch(() => null)
  const cfg = normalizeConfig(raw)
  chefEnabled = cfg.enabled
  chefTemplateIds = cfg.templateIds
  return cfg
}

/** 进入厨师工作台时预取模板 ID，供点击手势同步调用 */
export async function prefetchChefWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  return fetchChefWxSubscribeConfig()
}

/**
 * 请求微信订阅消息授权（厨师 · 新预约）。
 * 仅使用已缓存的模板 ID，避免 await 网络打断用户手势；未预取时直接返回 false。
 */
export async function requestChefOrderSubscribe(templateIds?: string[]): Promise<boolean> {
  const ids =
    templateIds && templateIds.length
      ? templateIds
      : chefEnabled && chefTemplateIds.length
        ? chefTemplateIds
        : []
  return requestSubscribe(ids)
}

/** 拉取食客端预约状态订阅配置 */
export async function fetchDinerWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  const raw = await get<WxSubscribeConfig>('/api/orders/wx-subscribe-config', undefined, {
    showError: false
  }).catch(() => null)
  const cfg = normalizeConfig(raw)
  dinerEnabled = cfg.enabled
  dinerTemplateIds = cfg.templateIds
  return cfg
}

export async function prefetchDinerWxSubscribeConfig(): Promise<WxSubscribeConfig> {
  return fetchDinerWxSubscribeConfig()
}

/**
 * 请求微信订阅消息授权（食客 · 预约状态）。
 * 须在用户手势同步链路内调用；提交预约按钮点击时先调本方法再 submit。
 */
export async function requestDinerOrderStatusSubscribe(templateIds?: string[]): Promise<boolean> {
  const ids =
    templateIds && templateIds.length
      ? templateIds
      : dinerEnabled && dinerTemplateIds.length
        ? dinerTemplateIds
        : []
  return requestSubscribe(ids)
}

/** 食客端是否已预取到可用模板（用于 W1 提示卡） */
export function isDinerSubscribeReady(): boolean {
  return dinerEnabled && dinerTemplateIds.length > 0
}
