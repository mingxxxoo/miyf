import { useMemo, useState } from 'react';
import {
  Avatar,
  Badge,
  Dropdown,
  Input,
  Layout,
  Popover,
  Space,
  Typography,
  List,
  Empty,
} from 'antd';
import type { MenuProps } from 'antd';
import {
  BellOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SearchOutlined,
  UserOutlined,
} from '@ant-design/icons';
import Breadcrumbs from '@/ui/Breadcrumbs';

const { Header } = Layout;

export type HeaderSearchItem = {
  path: string;
  label: string;
  group: string;
};

export type AppHeaderProps = {
  mode: 'business' | 'system';
  collapsed: boolean;
  onToggleCollapse?: () => void;
  headerLeft?: React.ReactNode;
  userName?: string;
  avatar?: string;
  userMenu: MenuProps;
  breadcrumbCurrent?: string;
  searchItems?: HeaderSearchItem[];
  onSearchNavigate?: (path: string) => void;
  unreadCount?: number;
  notifications?: { id: string; title: string; time?: string }[];
  onOpenNotifications?: () => void;
};

/** 顶栏：折叠 + 面包屑 + 搜索/通知 + 用户菜单。 */
export default function AppHeader({
  mode,
  collapsed,
  onToggleCollapse,
  headerLeft,
  userName,
  avatar,
  userMenu,
  breadcrumbCurrent,
  searchItems = [],
  onSearchNavigate,
  unreadCount = 0,
  notifications = [],
  onOpenNotifications,
}: AppHeaderProps) {
  const [keyword, setKeyword] = useState('');
  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return searchItems.slice(0, 8);
    return searchItems
      .filter(
        (i) =>
          i.label.toLowerCase().includes(q) ||
          i.group.toLowerCase().includes(q) ||
          i.path.toLowerCase().includes(q),
      )
      .slice(0, 12);
  }, [keyword, searchItems]);

  const searchPanel = (
    <div className="app-header-search-panel">
      {filtered.length ? (
        <List
          size="small"
          dataSource={filtered}
          renderItem={(item) => (
            <List.Item
              className="app-header-search-item"
              onClick={() => {
                onSearchNavigate?.(item.path);
                setKeyword('');
              }}
            >
              <List.Item.Meta
                title={item.label}
                description={<Typography.Text type="secondary">{item.group}</Typography.Text>}
              />
            </List.Item>
          )}
        />
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="无匹配页面" />
      )}
    </div>
  );

  const notifyPanel = (
    <div className="app-header-notify-panel">
      {notifications.length ? (
        <List
          size="small"
          dataSource={notifications}
          renderItem={(n) => (
            <List.Item>
              <List.Item.Meta title={n.title} description={n.time || '—'} />
            </List.Item>
          )}
          footer={
            onOpenNotifications ? (
              <Typography.Link onClick={onOpenNotifications}>查看全部</Typography.Link>
            ) : null
          }
        />
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无通知" />
      )}
    </div>
  );

  return (
    <Header className="admin-header app-header">
      <div className="app-header-left">
        <Space size={16}>
          {collapsed ? (
            <MenuUnfoldOutlined className="app-header-trigger" onClick={onToggleCollapse} />
          ) : (
            <MenuFoldOutlined className="app-header-trigger" onClick={onToggleCollapse} />
          )}
          {headerLeft}
          <Breadcrumbs currentLabel={breadcrumbCurrent} />
        </Space>
      </div>
      <div className="app-header-right">
        {mode === 'system' && searchItems.length > 0 ? (
          <Popover
            trigger="click"
            placement="bottomRight"
            content={searchPanel}
            overlayClassName="app-header-popover"
          >
            <Input
              allowClear
              prefix={<SearchOutlined />}
              placeholder="搜索系统页面"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="app-header-search"
            />
          </Popover>
        ) : null}
        {mode === 'system' ? (
          <Popover
            trigger="click"
            placement="bottomRight"
            content={notifyPanel}
            overlayClassName="app-header-popover"
          >
            <Badge count={unreadCount} size="small" offset={[-2, 2]}>
              <BellOutlined className="app-header-trigger" />
            </Badge>
          </Popover>
        ) : null}
        <Dropdown menu={userMenu} placement="bottomRight">
          <Space className="app-header-user" style={{ cursor: 'pointer' }}>
            <Avatar icon={<UserOutlined />} src={avatar} />
            <Typography.Text className="app-header-username">
              {userName || '管理员'}
            </Typography.Text>
          </Space>
        </Dropdown>
      </div>
    </Header>
  );
}
