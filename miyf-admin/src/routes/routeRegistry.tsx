import type { ReactNode } from 'react';
import CategoriesPage from '@/modules/kitchen/pages/categories/CategoriesPage';
import DishesPage from '@/modules/kitchen/pages/dishes/DishesPage';
import DishCreatePage from '@/modules/kitchen/pages/dishes/DishCreatePage';
import DishEditPage from '@/modules/kitchen/pages/dishes/DishEditPage';
import RecipesPage from '@/modules/kitchen/pages/recipes/RecipesPage';
import OrdersPage from '@/modules/kitchen/pages/orders/OrdersPage';
import OrderDetailPage from '@/modules/kitchen/pages/orders/OrderDetailPage';
import CommentsPage from '@/modules/kitchen/pages/comments/CommentsPage';
import UsersPage from '@/modules/kitchen/pages/users/UsersPage';
import UserDetailPage from '@/modules/kitchen/pages/users/UserDetailPage';
import OperationLogsPage from '@/modules/kitchen/pages/operation-logs/OperationLogsPage';
import HealthOverviewPage from '@/modules/health/pages/HealthOverviewPage';
import HealthSubjectsPage from '@/modules/health/pages/HealthSubjectsPage';
import HealthSamplesPage from '@/modules/health/pages/HealthSamplesPage';
import HealthProvidersPage from '@/modules/health/pages/HealthProvidersPage';
import HealthTrendsPage from '@/modules/health/pages/HealthTrendsPage';
import RolesPage from '@/modules/iam/pages/roles/RolesPage';
import RoleAuthPage from '@/modules/iam/pages/roles/RoleAuthPage';
import IamUsersPage from '@/modules/iam/pages/users/IamUsersPage';
import PermissionsPage from '@/modules/iam/pages/permissions/PermissionsPage';
import OrgUnitsPage from '@/modules/iam/pages/org-units/OrgUnitsPage';
import MenusPage from '@/modules/iam/pages/menus/MenusPage';
import PermGroupsPage from '@/modules/iam/pages/perm-groups/PermGroupsPage';
import SystemConfigPage from '@/pages/system/SystemConfigPage';
import SystemAppsPage from '@/pages/system/SystemAppsPage';
import SystemDictPage from '@/pages/system/SystemDictPage';
import SystemNotifyPage from '@/pages/system/SystemNotifyPage';
import SystemJobsPage from '@/pages/system/SystemJobsPage';
import SystemMonitorPage from '@/pages/system/SystemMonitorPage';
import SystemOverviewPage from '@/pages/system/SystemOverviewPage';
import SystemHealthSyncPage from '@/pages/system/SystemHealthSyncPage';

/**
 * 路由注册表：path → 页面组件 + 兜底权限码。
 * 实际准入优先使用后端菜单 path/permissionCode（见 MenuAwareGuard），
 * 无菜单匹配时回退到此处的 fallbackPermission(s)。
 */
export type RouteDef = {
  path: string;
  element: ReactNode;
  /** 菜单未配置时的兜底权限（任一即可） */
  fallbackPermissions?: string[];
  /** 是否系统区（挂在 /system 下） */
  system?: boolean;
};

export const businessRouteDefs: RouteDef[] = [
  {
    path: '/kitchen/categories',
    element: <CategoriesPage />,
    fallbackPermissions: ['kitchen:category:list'],
  },
  {
    path: '/kitchen/dishes',
    element: <DishesPage />,
    fallbackPermissions: ['kitchen:dish:list'],
  },
  {
    path: '/kitchen/dishes/create',
    element: <DishCreatePage />,
    fallbackPermissions: ['kitchen:dish:create'],
  },
  {
    path: '/kitchen/dishes/:id/edit',
    element: <DishEditPage />,
    fallbackPermissions: ['kitchen:dish:update', 'kitchen:dish:list'],
  },
  {
    path: '/kitchen/recipes',
    element: <RecipesPage />,
    fallbackPermissions: ['kitchen:recipe:list'],
  },
  {
    path: '/kitchen/orders',
    element: <OrdersPage />,
    fallbackPermissions: ['kitchen:order:list'],
  },
  {
    path: '/kitchen/orders/:id',
    element: <OrderDetailPage />,
    fallbackPermissions: ['kitchen:order:detail', 'kitchen:order:list'],
  },
  {
    path: '/kitchen/comments',
    element: <CommentsPage />,
    fallbackPermissions: ['kitchen:comment:list'],
  },
  {
    path: '/kitchen/users',
    element: <UsersPage />,
    fallbackPermissions: ['user:list'],
  },
  {
    path: '/kitchen/users/:id',
    element: <UserDetailPage />,
    fallbackPermissions: ['user:list'],
  },
  {
    path: '/kitchen/operation-logs',
    element: <OperationLogsPage />,
    fallbackPermissions: ['operation-log:list'],
  },
  {
    path: '/health/overview',
    element: <HealthOverviewPage />,
    fallbackPermissions: ['health:overview:view'],
  },
  {
    path: '/health/subjects',
    element: <HealthSubjectsPage />,
    fallbackPermissions: ['health:subject:list'],
  },
  {
    path: '/health/samples',
    element: <HealthSamplesPage />,
    fallbackPermissions: ['health:sample:list'],
  },
  {
    path: '/health/providers',
    element: <HealthProvidersPage />,
    fallbackPermissions: ['health:provider:list', 'health:sync:trigger'],
  },
  {
    path: '/health/trends',
    element: <HealthTrendsPage />,
    fallbackPermissions: ['health:trend:view'],
  },
];

export const systemRouteDefs: RouteDef[] = [
  {
    path: 'overview',
    element: <SystemOverviewPage />,
    fallbackPermissions: ['sys:monitor:view', 'sys:settings:view', 'iam:role:list'],
    system: true,
  },
  {
    path: 'users',
    element: <IamUsersPage />,
    fallbackPermissions: ['iam:user:list'],
    system: true,
  },
  {
    path: 'roles',
    element: <RolesPage />,
    fallbackPermissions: ['iam:role:list'],
    system: true,
  },
  {
    path: 'role-auth',
    element: <RoleAuthPage />,
    fallbackPermissions: ['iam:role:list'],
    system: true,
  },
  {
    path: 'org-units',
    element: <OrgUnitsPage />,
    fallbackPermissions: ['iam:org:list'],
    system: true,
  },
  {
    path: 'menus',
    element: <MenusPage />,
    fallbackPermissions: ['iam:menu:list'],
    system: true,
  },
  {
    path: 'permissions',
    element: <PermissionsPage />,
    fallbackPermissions: ['iam:permission:list'],
    system: true,
  },
  {
    path: 'perm-groups',
    element: <PermGroupsPage />,
    fallbackPermissions: ['iam:perm-group:list'],
    system: true,
  },
  {
    path: 'apps',
    element: <SystemAppsPage />,
    fallbackPermissions: ['sys:app:list'],
    system: true,
  },
  {
    path: 'config',
    element: <SystemConfigPage />,
    fallbackPermissions: ['sys:config:list'],
    system: true,
  },
  {
    path: 'dicts',
    element: <SystemDictPage />,
    fallbackPermissions: ['sys:dict:list'],
    system: true,
  },
  {
    path: 'notifications',
    element: <SystemNotifyPage />,
    fallbackPermissions: ['sys:notify:list', 'sys:notify:send'],
    system: true,
  },
  {
    path: 'jobs',
    element: <SystemJobsPage />,
    fallbackPermissions: ['sys:job:list'],
    system: true,
  },
  {
    path: 'monitor',
    element: <SystemMonitorPage />,
    fallbackPermissions: ['sys:monitor:view'],
    system: true,
  },
  {
    path: 'operation-logs',
    element: <OperationLogsPage />,
    fallbackPermissions: ['operation-log:list'],
    system: true,
  },
  {
    path: 'health-sync',
    element: <SystemHealthSyncPage />,
    fallbackPermissions: ['health:provider:list', 'health:sync:trigger'],
    system: true,
  },
];
