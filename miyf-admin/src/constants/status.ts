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

export const DISH_AUDIT_STATUS: Record<string, StatusMeta> = {
  DRAFT: { color: 'default', text: '待提交' },
  PENDING_REVIEW: { color: 'processing', text: '待审核' },
  APPROVED: { color: 'success', text: '已通过' },
  REJECTED: { color: 'error', text: '已驳回' },
};

export const BINDING_STATUS: Record<string, StatusMeta> = {
  PENDING: { color: 'gold', text: '待确认' },
  BOUND: { color: 'success', text: '已绑定' },
  REJECTED: { color: 'error', text: '已拒绝' },
  UNBOUND: { color: 'default', text: '已解除' },
};

export const KITCHEN_STATUS: Record<string, StatusMeta> = {
  OPEN: { color: 'success', text: '营业' },
  CLOSED: { color: 'default', text: '停业' },
  BANNED: { color: 'error', text: '封禁' },
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
