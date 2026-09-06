import { useEffect } from 'react';

import { Navigate, useLocation } from 'react-router-dom';

import { Result, Button } from 'antd';

import { useAuthStore } from '@/stores/authStore';

import { usePermissionStore } from '@/stores/permissionStore';



interface PermissionGuardProps {

  children: React.ReactNode;

  permission?: string;

  permissions?: string[];

  requireAll?: boolean;

}



export default function PermissionGuard({

  children,

  permission,

  permissions = [],

  requireAll = false,

}: PermissionGuardProps) {

  const location = useLocation();

  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const authPermissions = useAuthStore((s) => s.permissions);

  const setPermissions = usePermissionStore((s) => s.setPermissions);

  const hasPermission = usePermissionStore((s) => s.hasPermission);

  const hasAnyPermission = usePermissionStore((s) => s.hasAnyPermission);



  useEffect(() => {

    if (authPermissions?.length) {

      setPermissions(authPermissions);

    }

  }, [authPermissions, setPermissions]);



  if (!isAuthenticated) {

    return <Navigate to="/login" state={{ from: location }} replace />;

  }



  const codes = permission ? [permission, ...permissions] : permissions;



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


