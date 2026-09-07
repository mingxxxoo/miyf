/** 厨房业务状态文案与颜色（集中管理，页面勿再散落定义）。 */

export type StatusMeta = {
  color: string;
  text: string;
};

export const DISH_STATUS: Record<string, StatusMeta> = {
  ON_SALE: { color: 'success', text: '已上架' },
  OFF_SALE: { color: 'default', text: '已下架' },
  DRAFT: { color: 'processing', text: '草稿' },
};

export const ORDER_STATUS: Record<string, StatusMeta> = {
  PENDING: { color: 'gold', text: '待确认' },
  CONFIRMED: { color: 'blue', text: '已确认' },
  PREPARING: { color: 'processing', text: '准备中' },
  READY: { color: 'cyan', text: '待取餐' },
  COMPLETED: { color: 'success', text: '已完成' },
  CANCELLED: { color: 'default', text: '已取消' },
};

/** 预约状态可流转的下一步。 */
export const ORDER_NEXT_STATUS: Partial<Record<string, string[]>> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PREPARING'],
  PREPARING: ['READY'],
  READY: ['COMPLETED'],
};

export const ENABLED_STATUS: Record<string, StatusMeta> = {
  ENABLED: { color: 'success', text: '启用' },
  DISABLED: { color: 'default', text: '停用' },
};

export const COMMENT_STATUS: Record<string, StatusMeta> = {
  NORMAL: { color: 'success', text: '显示' },
  HIDDEN: { color: 'default', text: '已隐藏' },
};

export const USER_STATUS: Record<string, StatusMeta> = {
  ACTIVE: { color: 'success', text: '正常' },
  ENABLED: { color: 'success', text: '正常' },
  DISABLED: { color: 'default', text: '停用' },
};

export function statusOf(
  map: Record<string, StatusMeta>,
  code?: string | null,
): StatusMeta {
  if (!code) return { color: 'default', text: '—' };
  return map[code] ?? { color: 'default', text: code };
}
