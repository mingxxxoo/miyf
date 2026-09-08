import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import PermissionGuard from '@/components/PermissionGuard';
import MenuAwareGuard from '@/routes/MenuAwareGuard';
import { businessRouteDefs, systemRouteDefs } from '@/routes/routeRegistry';
import AdminLayout from '@/layouts/AdminLayout';
import SystemLayout, { firstSystemPath } from '@/layouts/SystemLayout';
import LoginPage from '@/pages/login/LoginPage';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import { usePermissionStore } from '@/stores/permissionStore';

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

function SystemIndexRedirect() {
  const hasPermission = usePermissionStore((s) => s.hasPermission);
  const hasAnyPermission = usePermissionStore((s) => s.hasAnyPermission);
  return <Navigate to={firstSystemPath(hasPermission, hasAnyPermission)} replace />;
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

        {businessRouteDefs.map((def) => (
          <Route
            key={def.path}
            path={def.path}
            element={
              <MenuAwareGuard fallbackPermissions={def.fallbackPermissions}>
                {def.element}
              </MenuAwareGuard>
            }
          />
        ))}

        <Route path="/iam/org-units" element={<Navigate to="/system/org-units" replace />} />

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
        <Route path="/admins" element={<Navigate to="/system/role-auth" replace />} />
        <Route path="/roles" element={<Navigate to="/system/roles" replace />} />
        <Route path="/permissions" element={<Navigate to="/system/permissions" replace />} />
        <Route path="/iam/users" element={<Navigate to="/system/role-auth" replace />} />
        <Route path="/iam/roles" element={<Navigate to="/system/roles" replace />} />
        <Route path="/iam/permissions" element={<Navigate to="/system/permissions" replace />} />
        <Route path="/iam/perm-groups" element={<Navigate to="/system/perm-groups" replace />} />
        <Route path="/iam/menus" element={<Navigate to="/system/menus" replace />} />
      </Route>

      <Route
        path="/system"
        element={
          <PermissionGuard>
            <SystemLayout />
          </PermissionGuard>
        }
      >
        <Route index element={<SystemIndexRedirect />} />
        {systemRouteDefs.map((def) => (
          <Route
            key={def.path}
            path={def.path}
            element={
              <MenuAwareGuard fallbackPermissions={def.fallbackPermissions}>
                {def.element}
              </MenuAwareGuard>
            }
          />
        ))}
      </Route>

      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
