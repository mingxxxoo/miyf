import type { MenuProps } from 'antd';
import {
  AppstoreOutlined,
  ApartmentOutlined,
  BellOutlined,
  BookOutlined,
  ClusterOutlined,
  ControlOutlined,
  DashboardOutlined,
  FileSearchOutlined,
  HeartOutlined,
  MenuOutlined,
  MonitorOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';

export type SysMenuItem = {
  key: string;
  label: string;
  icon?: React.ReactNode;
  permission?: string;
  permissions?: string[];
};

export type SysMenuGroup = {
  key: string;
  label: string;
  children: SysMenuItem[];
};

/**
 * 系统管理中心信息架构（静态菜单，按权限裁剪）。
 */
export const SYSTEM_MENU_GROUPS: SysMenuGroup[] = [
  {
    key: 'overview-group',
    label: '系统概览',
    children: [
      {
        key: '/system/overview',
        label: '系统概览',
        icon: <DashboardOutlined />,
        permissions: ['sys:monitor:view', 'sys:settings:view', 'iam:role:list'],
      },
    ],
  },
  {
    key: 'iam',
    label: '权限与组织',
    children: [
      { key: '/system/role-auth', label: '用户授权', icon: <UserOutlined />, permission: 'iam:role:list' },
      { key: '/system/roles', label: '角色管理', icon: <TeamOutlined />, permission: 'iam:role:list' },
      { key: '/system/org-units', label: '组织架构', icon: <ApartmentOutlined />, permission: 'iam:org:list' },
      { key: '/system/menus', label: '菜单管理', icon: <MenuOutlined />, permission: 'iam:menu:list' },
      {
        key: '/system/permissions',
        label: '权限定义',
        icon: <SafetyOutlined />,
        permission: 'iam:permission:list',
      },
      {
        key: '/system/perm-groups',
        label: '权限组',
        icon: <ClusterOutlined />,
        permission: 'iam:perm-group:list',
      },
    ],
  },
  {
    key: 'config',
    label: '系统配置',
    children: [
      { key: '/system/config', label: '基础设置', icon: <ControlOutlined />, permission: 'sys:config:list' },
      { key: '/system/apps', label: '应用管理', icon: <AppstoreOutlined />, permission: 'sys:app:list' },
      { key: '/system/dicts', label: '数据字典', icon: <BookOutlined />, permission: 'sys:dict:list' },
      {
        key: '/system/notifications',
        label: '通知中心',
        icon: <BellOutlined />,
        permissions: ['sys:notify:list', 'sys:notify:send'],
      },
      { key: '/system/jobs', label: '定时任务', icon: <ScheduleOutlined />, permission: 'sys:job:list' },
    ],
  },
  {
    key: 'ops',
    label: '运维监控',
    children: [
      { key: '/system/monitor', label: '系统监控', icon: <MonitorOutlined />, permission: 'sys:monitor:view' },
      {
        key: '/system/operation-logs',
        label: '操作日志',
        icon: <FileSearchOutlined />,
        permission: 'operation-log:list',
      },
      {
        key: '/system/health-sync',
        label: '健康同步',
        icon: <HeartOutlined />,
        permissions: ['health:provider:list', 'health:sync:trigger'],
      },
    ],
  },
];

export function canSeeSystemItem(
  item: SysMenuItem,
  hasPermission: (c: string) => boolean,
  hasAny: (c: string[]) => boolean,
) {
  if (item.permission) return hasPermission(item.permission);
  if (item.permissions?.length) return hasAny(item.permissions);
  return true;
}

export function firstSystemPath(
  hasPermission: (c: string) => boolean,
  hasAny: (c: string[]) => boolean,
): string {
  for (const g of SYSTEM_MENU_GROUPS) {
    for (const c of g.children) {
      if (canSeeSystemItem(c, hasPermission, hasAny)) return c.key;
    }
  }
  return '/dashboard';
}

export function buildSystemMenuItems(
  hasPermission: (c: string) => boolean,
  hasAny: (c: string[]) => boolean,
): MenuProps['items'] {
  return SYSTEM_MENU_GROUPS.map((g) => {
    const children = g.children
      .filter((c) => canSeeSystemItem(c, hasPermission, hasAny))
      .map((c) => ({
        key: c.key,
        icon: c.icon,
        label: c.label,
      }));
    if (!children.length) return null;
    // 单叶子分组（概览）直接提升为一级项，避免无巢菜单
    if (g.key === 'overview-group' && children.length === 1) {
      return children[0];
    }
    return {
      key: g.key,
      label: g.label,
      children,
    };
  }).filter(Boolean);
}

export function systemLeafKeys(
  hasPermission: (c: string) => boolean,
  hasAny: (c: string[]) => boolean,
): string[] {
  return SYSTEM_MENU_GROUPS.flatMap((g) => g.children)
    .filter((c) => canSeeSystemItem(c, hasPermission, hasAny))
    .map((c) => c.key);
}

/** 全局搜索候选（系统中心）。 */
export function systemSearchCandidates(
  hasPermission: (c: string) => boolean,
  hasAny: (c: string[]) => boolean,
): { path: string; label: string; group: string }[] {
  return SYSTEM_MENU_GROUPS.flatMap((g) =>
    g.children
      .filter((c) => canSeeSystemItem(c, hasPermission, hasAny))
      .map((c) => ({ path: c.key, label: c.label, group: g.label })),
  );
}
