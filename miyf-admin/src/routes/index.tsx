import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import PermissionGuard from '@/components/PermissionGuard';
import AdminLayout from '@/layouts/AdminLayout';
import LoginPage from '@/pages/login/LoginPage';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import UsersPage from '@/modules/kitchen/pages/users/UsersPage';
import UserDetailPage from '@/modules/kitchen/pages/users/UserDetailPage';
import CategoriesPage from '@/modules/kitchen/pages/categories/CategoriesPage';
import DishesPage from '@/modules/kitchen/pages/dishes/DishesPage';
import DishCreatePage from '@/modules/kitchen/pages/dishes/DishCreatePage';
import DishEditPage from '@/modules/kitchen/pages/dishes/DishEditPage';
import RecipesPage from '@/modules/kitchen/pages/recipes/RecipesPage';
import OrdersPage from '@/modules/kitchen/pages/orders/OrdersPage';
import OrderDetailPage from '@/modules/kitchen/pages/orders/OrderDetailPage';
import CommentsPage from '@/modules/kitchen/pages/comments/CommentsPage';
import OperationLogsPage from '@/modules/kitchen/pages/operation-logs/OperationLogsPage';
import AdminsPage from '@/modules/iam/pages/admins/AdminsPage';
import RolesPage from '@/modules/iam/pages/roles/RolesPage';
import PermissionsPage from '@/modules/iam/pages/permissions/PermissionsPage';
import OrgUnitsPage from '@/modules/iam/pages/org-units/OrgUnitsPage';
import MenusPage from '@/modules/iam/pages/menus/MenusPage';
import PermGroupsPage from '@/modules/iam/pages/perm-groups/PermGroupsPage';
import SystemHomePage from '@/pages/system/SystemHomePage';
import SystemConfigPage from '@/pages/system/SystemConfigPage';
import SystemDictPage from '@/pages/system/SystemDictPage';
import SystemNotifyPage from '@/pages/system/SystemNotifyPage';
import SystemJobsPage from '@/pages/system/SystemJobsPage';
import SystemMonitorPage from '@/pages/system/SystemMonitorPage';
import HealthOverviewPage from '@/modules/health/pages/HealthOverviewPage';
import HealthSubjectsPage from '@/modules/health/pages/HealthSubjectsPage';
import HealthSamplesPage from '@/modules/health/pages/HealthSamplesPage';
import HealthProvidersPage from '@/modules/health/pages/HealthProvidersPage';
import HealthTrendsPage from '@/modules/health/pages/HealthTrendsPage';

/** 旧路径 /dishes/:id/edit → /kitchen/dishes/:id/edit */
function LegacyDishEditRedirect() {
  const { id } = useParams();
  return <Navigate to={`/kitchen/dishes/${id}/edit`} replace />;
}

/** 旧路径 /orders/:id → /kitchen/orders/:id */
function LegacyOrderDetailRedirect() {
  const { id } = useParams();
  return <Navigate to={`/kitchen/orders/${id}`} replace />;
}

/** 旧路径 /users/:id → /kitchen/users/:id */
function LegacyUserDetailRedirect() {
  const { id } = useParams();
  return <Navigate to={`/kitchen/users/${id}`} replace />;
}

function Guard({
  children,
  permission,
  permissions,
}: {
  children: React.ReactNode;
  permission?: string;
  permissions?: string[];
}) {
  return (
    <PermissionGuard permission={permission} permissions={permissions}>
      {children}
    </PermissionGuard>
  );
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <PermissionGuard>
            <AdminLayout />
          </PermissionGuard>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />

        {/* kitchen 业务（路径对齐 sys_menu 种子） */}
        <Route
          path="/kitchen/categories"
          element={
            <Guard permission="kitchen:category:list">
              <CategoriesPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/dishes"
          element={
            <Guard permission="kitchen:dish:list">
              <DishesPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/dishes/create"
          element={
            <Guard permission="kitchen:dish:create">
              <DishCreatePage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/dishes/:id/edit"
          element={
            <Guard permissions={['kitchen:dish:update', 'kitchen:dish:list']}>
              <DishEditPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/recipes"
          element={
            <Guard permission="kitchen:recipe:list">
              <RecipesPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/orders"
          element={
            <Guard permission="kitchen:order:list">
              <OrdersPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/orders/:id"
          element={
            <Guard permissions={['kitchen:order:detail', 'kitchen:order:list']}>
              <OrderDetailPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/comments"
          element={
            <Guard permission="kitchen:comment:list">
              <CommentsPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/users"
          element={
            <Guard permission="user:list">
              <UsersPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/users/:id"
          element={
            <Guard permission="user:list">
              <UserDetailPage />
            </Guard>
          }
        />
        <Route
          path="/kitchen/operation-logs"
          element={
            <Guard permission="operation-log:list">
              <OperationLogsPage />
            </Guard>
          }
        />

        {/* IAM */}
        <Route
          path="/iam/users"
          element={
            <Guard permission="iam:user:list">
              <AdminsPage />
            </Guard>
          }
        />
        <Route
          path="/iam/roles"
          element={
            <Guard permission="iam:role:list">
              <RolesPage />
            </Guard>
          }
        />
        <Route
          path="/iam/permissions"
          element={
            <Guard permission="iam:permission:list">
              <PermissionsPage />
            </Guard>
          }
        />
        <Route
          path="/iam/org-units"
          element={
            <Guard permission="iam:org:list">
              <OrgUnitsPage />
            </Guard>
          }
        />
        <Route
          path="/iam/perm-groups"
          element={
            <Guard permission="iam:perm-group:list">
              <PermGroupsPage />
            </Guard>
          }
        />
        <Route
          path="/iam/menus"
          element={
            <Guard permission="iam:menu:list">
              <MenusPage />
            </Guard>
          }
        />

        {/* 系统设置（系统管理员） */}
        <Route path="/system" element={<SystemHomePage />} />
        <Route
          path="/system/permissions"
          element={
            <Guard permission="iam:permission:list">
              <PermissionsPage />
            </Guard>
          }
        />
        <Route
          path="/system/config"
          element={
            <Guard permission="sys:config:list">
              <SystemConfigPage />
            </Guard>
          }
        />
        <Route
          path="/system/dicts"
          element={
            <Guard permission="sys:dict:list">
              <SystemDictPage />
            </Guard>
          }
        />
        <Route
          path="/system/notifications"
          element={
            <Guard permissions={['sys:notify:list', 'sys:notify:send']}>
              <SystemNotifyPage />
            </Guard>
          }
        />
        <Route
          path="/system/jobs"
          element={
            <Guard permission="sys:job:list">
              <SystemJobsPage />
            </Guard>
          }
        />
        <Route
          path="/system/monitor"
          element={
            <Guard permission="sys:monitor:view">
              <SystemMonitorPage />
            </Guard>
          }
        />

        {/* 健康管理 */}
        <Route
          path="/health/overview"
          element={
            <Guard permission="health:overview:view">
              <HealthOverviewPage />
            </Guard>
          }
        />
        <Route
          path="/health/subjects"
          element={
            <Guard permission="health:subject:list">
              <HealthSubjectsPage />
            </Guard>
          }
        />
        <Route
          path="/health/samples"
          element={
            <Guard permission="health:sample:list">
              <HealthSamplesPage />
            </Guard>
          }
        />
        <Route
          path="/health/providers"
          element={
            <Guard permissions={['health:provider:list', 'health:sync:trigger']}>
              <HealthProvidersPage />
            </Guard>
          }
        />
        <Route
          path="/health/trends"
          element={
            <Guard permission="health:trend:view">
              <HealthTrendsPage />
            </Guard>
          }
        />

        {/* 兼容旧路径 */}
        <Route path="/categories" element={<Navigate to="/kitchen/categories" replace />} />
        <Route path="/dishes" element={<Navigate to="/kitchen/dishes" replace />} />
        <Route path="/dishes/create" element={<Navigate to="/kitchen/dishes/create" replace />} />
        <Route path="/dishes/:id/edit" element={<LegacyDishEditRedirect />} />
        <Route path="/recipes" element={<Navigate to="/kitchen/recipes" replace />} />
        <Route path="/orders" element={<Navigate to="/kitchen/orders" replace />} />
        <Route path="/orders/:id" element={<LegacyOrderDetailRedirect />} />
        <Route path="/comments" element={<Navigate to="/kitchen/comments" replace />} />
        <Route path="/users" element={<Navigate to="/kitchen/users" replace />} />
        <Route path="/users/:id" element={<LegacyUserDetailRedirect />} />
        <Route path="/operation-logs" element={<Navigate to="/kitchen/operation-logs" replace />} />
        <Route path="/admins" element={<Navigate to="/iam/users" replace />} />
        <Route path="/roles" element={<Navigate to="/iam/roles" replace />} />
        <Route path="/permissions" element={<Navigate to="/iam/permissions" replace />} />
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
