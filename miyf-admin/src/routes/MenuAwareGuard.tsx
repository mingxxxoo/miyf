import { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Result, Button } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { usePermissionStore } from '@/stores/permissionStore';
import { useMenuStore } from '@/stores/menuStore';

interface MenuAwareGuardProps {
  children: React.ReactNode;
  /** 菜单未命中时的兜底权限（任一即可） */
  fallbackPermissions?: string[];
  requireAll?: boolean;
}

/**
 * 路由守卫：优先使用后端菜单 path → permissionCode；
 * 无匹配时回退到路由注册表中的 fallbackPermissions。
 */
export default function MenuAwareGuard({
  children,
  fallbackPermissions = [],
  requireAll = false,
}: MenuAwareGuardProps) {
  const location = useLocation();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const authPermissions = useAuthStore((s) => s.permissions);
  const setPermissions = usePermissionStore((s) => s.setPermissions);
  const hasPermission = usePermissionStore((s) => s.hasPermission);
  const hasAnyPermission = usePermissionStore((s) => s.hasAnyPermission);
  const permissionForPath = useMenuStore((s) => s.permissionForPath);

  useEffect(() => {
    if (authPermissions?.length) {
      setPermissions(authPermissions);
    }
  }, [authPermissions, setPermissions]);

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const menuCode = permissionForPath(location.pathname);
  // 菜单命中则以其 permissionCode 为准；否则回退路由注册表兜底码
  const codes = menuCode ? [menuCode] : fallbackPermissions;

  if (codes.length > 0) {
    const allowed = requireAll
      ? codes.every((code) => hasPermission(code))
      : hasAnyPermission(codes);

    if (!allowed) {
      return (
        <Result
          status="403"
          title="无权访问"
          subTitle="您没有访问此页面的权限，请联系管理员。"
          extra={
            <Button type="primary" href="/dashboard">
              返回首页
            </Button>
          }
        />
      );
    }
  }

  return <>{children}</>;
}
