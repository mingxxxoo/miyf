import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Layout, Button } from 'antd';
import type { MenuProps } from 'antd';
import * as Icons from '@ant-design/icons';
import { ArrowLeftOutlined, LogoutOutlined, SettingOutlined } from '@ant-design/icons';
import { fetchMyMenus, type MenuTreeNode } from '@/modules/iam/api';
import { sysNotificationApi } from '@/modules/system/api';
import { useAuthStore } from '@/stores/authStore';
import { usePermissionStore } from '@/stores/permissionStore';
import { useMenuStore } from '@/stores/menuStore';
import AppHeader from '@/layouts/AppHeader';
import AppSidebar from '@/layouts/AppSidebar';
import {
  buildSystemMenuItems,
  systemLeafKeys,
  systemSearchCandidates,
} from '@/layouts/systemMenus';

const { Content } = Layout;

function isSystemAdmin(roles?: string[], permissions?: string[]) {
  if (roles?.includes('SUPER_ADMIN') || roles?.includes('SYSTEM_ADMIN')) return true;
  if (permissions?.includes('*') || permissions?.includes('sys:settings:view')) return true;
  return false;
}

function resolveIcon(name?: string) {
  if (!name) return undefined;
  const Comp = (Icons as Record<string, unknown>)[name] as React.ComponentType | undefined;
  return Comp ? <Comp /> : undefined;
}

function toMenuItems(nodes: MenuTreeNode[]): MenuProps['items'] {
  return nodes.map((node) => {
    const children = node.children?.length ? toMenuItems(node.children) : undefined;
    return {
      key: node.path || String(node.id),
      icon: resolveIcon(node.icon),
      label: node.name,
      children: children?.length ? children : undefined,
    };
  });
}

function collectLeafPaths(nodes: MenuTreeNode[], acc: string[] = []): string[] {
  for (const node of nodes) {
    if (node.children?.length) collectLeafPaths(node.children, acc);
    else if (node.path) acc.push(node.path);
  }
  return acc;
}

export type AppShellProps = {
  mode: 'business' | 'system';
};

/**
 * 统一后台壳：业务动态菜单 / 系统管理中心静态分组菜单。
 */
export default function AppShell({ mode }: AppShellProps) {
  const [collapsed, setCollapsed] = useState(false);
  const [menuTree, setMenuTree] = useState<MenuTreeNode[]>([]);
  const [openKeys, setOpenKeys] = useState<string[]>(['iam', 'config', 'ops']);
  const [inboxPreview, setInboxPreview] = useState<{ id: string; title: string; time?: string }[]>(
    [],
  );
  const [unreadCount, setUnreadCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const permissions = usePermissionStore((s) => s.permissions);
  const authPermissions = useAuthStore((s) => s.permissions);
  const hasPermission = usePermissionStore((s) => s.hasPermission);
  const hasAnyPermission = usePermissionStore((s) => s.hasAnyPermission);
  const clearPermissions = usePermissionStore((s) => s.clearPermissions);
  const setMenus = useMenuStore((s) => s.setMenus);
  const clearMenus = useMenuStore((s) => s.clear);
  const sysAdmin = isSystemAdmin(user?.roles, permissions.length ? permissions : authPermissions);

  useEffect(() => {
    if (mode !== 'business') return;
    let cancelled = false;
    fetchMyMenus()
      .then((tree) => {
        if (!cancelled) {
          setMenuTree(tree);
          setMenus(tree);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setMenuTree([]);
          setMenus([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [mode, setMenus]);

  useEffect(() => {
    if (mode !== 'system') return;
    const userKey = user?.username || user?.id;
    if (!userKey) return;
    let cancelled = false;
    sysNotificationApi
      .listInbox(userKey, 8)
      .then((list) => {
        if (cancelled) return;
        setInboxPreview(
          list.map((i) => ({
            id: i.id,
            title: i.title,
            time: i.createTime,
          })),
        );
        setUnreadCount(list.filter((i) => !i.read).length);
      })
      .catch(() => {
        if (!cancelled) {
          setInboxPreview([]);
          setUnreadCount(0);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [mode, user?.username, user?.id]);

  const businessItems = useMemo(() => toMenuItems(menuTree), [menuTree]);
  const businessLeaves = useMemo(() => collectLeafPaths(menuTree), [menuTree]);
  const systemItems = useMemo(
    () => buildSystemMenuItems(hasPermission, hasAnyPermission),
    [hasPermission, hasAnyPermission],
  );
  const systemLeaves = useMemo(
    () => systemLeafKeys(hasPermission, hasAnyPermission),
    [hasPermission, hasAnyPermission],
  );
  const searchItems = useMemo(
    () => (mode === 'system' ? systemSearchCandidates(hasPermission, hasAnyPermission) : []),
    [mode, hasPermission, hasAnyPermission],
  );

  const leafPaths = mode === 'business' ? businessLeaves : systemLeaves;
  const items = mode === 'business' ? businessItems : systemItems;
  const selectedKey =
    leafPaths
      .filter((p) => location.pathname === p || location.pathname.startsWith(`${p}/`))
      .sort((a, b) => b.length - a.length)[0] ??
    (mode === 'business' ? '/dashboard' : leafPaths[0]);

  const handleLogout = () => {
    logout();
    clearPermissions();
    clearMenus();
    navigate('/login');
  };

  const userMenu: MenuProps = {
    items: [
      ...(mode === 'business' && sysAdmin
        ? [
            {
              key: 'system',
              icon: <SettingOutlined />,
              label: '系统管理中心',
              onClick: () => navigate('/system/overview'),
            },
            { type: 'divider' as const },
          ]
        : []),
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: handleLogout,
      },
    ],
  };

  const headerLeft: ReactNode =
    mode === 'system' ? (
      <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/dashboard')}>
        返回业务
      </Button>
    ) : null;

  return (
    <Layout className="app-shell" style={{ minHeight: '100vh' }}>
      <AppSidebar
        mode={mode}
        collapsed={collapsed}
        onCollapse={setCollapsed}
        selectedKey={selectedKey}
        openKeys={openKeys}
        onOpenKeysChange={setOpenKeys}
        items={items}
        onNavigate={navigate}
      />
      <Layout>
        <AppHeader
          mode={mode}
          collapsed={collapsed}
          onToggleCollapse={() => setCollapsed((v) => !v)}
          headerLeft={headerLeft}
          userName={user?.nickname ?? user?.username}
          avatar={user?.avatar}
          userMenu={userMenu}
          searchItems={searchItems}
          onSearchNavigate={navigate}
          unreadCount={unreadCount}
          notifications={inboxPreview}
          onOpenNotifications={() => navigate('/system/notifications')}
        />
        <Content className="admin-content app-content">
          <div className="app-content-inner">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
