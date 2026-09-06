import { useEffect, useMemo, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Dropdown, Avatar, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import * as Icons from '@ant-design/icons';
import {
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { fetchMyMenus, type MenuTreeNode } from '@/modules/iam/api';
import { useAuthStore } from '@/stores/authStore';
import { usePermissionStore } from '@/stores/permissionStore';

const { Header, Sider, Content } = Layout;

function resolveIcon(name?: string) {
  if (!name) return undefined;
  const Comp = (Icons as Record<string, unknown>)[name] as React.ComponentType | undefined;
  return Comp ? <Comp /> : undefined;
}

function toMenuItems(nodes: MenuTreeNode[]): MenuProps['items'] {
  return nodes.map((node) => {
    const children = node.children?.length ? toMenuItems(node.children) : undefined;
    const hasChildren = !!children?.length;
    return {
      key: node.path || String(node.id),
      icon: resolveIcon(node.icon),
      label: node.name,
      children: hasChildren ? children : undefined,
    };
  });
}

function collectLeafPaths(nodes: MenuTreeNode[], acc: string[] = []): string[] {
  for (const node of nodes) {
    if (node.children?.length) {
      collectLeafPaths(node.children, acc);
    } else if (node.path) {
      acc.push(node.path);
    }
  }
  return acc;
}

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [menuTree, setMenuTree] = useState<MenuTreeNode[]>([]);
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const clearPermissions = usePermissionStore((s) => s.clearPermissions);

  useEffect(() => {
    let cancelled = false;
    fetchMyMenus()
      .then((tree) => {
        if (!cancelled) setMenuTree(tree);
      })
      .catch(() => {
        if (!cancelled) setMenuTree([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const items = useMemo(() => toMenuItems(menuTree), [menuTree]);
  const leafPaths = useMemo(() => collectLeafPaths(menuTree), [menuTree]);

  const selectedKey =
    leafPaths
      .filter((p) => location.pathname === p || location.pathname.startsWith(`${p}/`))
      .sort((a, b) => b.length - a.length)[0] ?? '/dashboard';

  const handleLogout = () => {
    logout();
    clearPermissions();
    navigate('/login');
  };

  const userMenu: MenuProps = {
    items: [
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: handleLogout,
      },
    ],
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        width={220}
        className="admin-sider"
      >
        <div className="admin-logo">
          <div className="admin-logo-icon">M</div>
          {!collapsed && <span>miyf</span>}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={items}
          onClick={({ key }) => {
            if (typeof key === 'string' && key.startsWith('/')) {
              navigate(key);
            }
          }}
          style={{ border: 'none' }}
        />
      </Sider>
      <Layout>
        <Header className="admin-header">
          <Space>
            {collapsed ? (
              <MenuUnfoldOutlined
                style={{ fontSize: 18, cursor: 'pointer' }}
                onClick={() => setCollapsed(false)}
              />
            ) : (
              <MenuFoldOutlined
                style={{ fontSize: 18, cursor: 'pointer' }}
                onClick={() => setCollapsed(true)}
              />
            )}
          </Space>
          <Dropdown menu={userMenu} placement="bottomRight">
            <Space style={{ cursor: 'pointer' }}>
              <Avatar icon={<UserOutlined />} src={user?.avatar} />
              <Typography.Text>{user?.nickname ?? user?.username ?? '管理员'}</Typography.Text>
            </Space>
          </Dropdown>
        </Header>
        <Content className="admin-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
