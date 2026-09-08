import { Layout, Menu } from 'antd';
import type { MenuProps } from 'antd';
import { SettingOutlined } from '@ant-design/icons';
import kiwiIcon from '@/assets/icons/kiwi-admin.svg';

const { Sider } = Layout;

export type AppSidebarProps = {
  mode: 'business' | 'system';
  collapsed: boolean;
  onCollapse?: (v: boolean) => void;
  selectedKey?: string;
  openKeys?: string[];
  onOpenKeysChange?: (keys: string[]) => void;
  items: MenuProps['items'];
  onNavigate: (path: string) => void;
};

/** 侧栏：品牌 + 分组导航，业务/系统均支持收缩。 */
export default function AppSidebar({
  mode,
  collapsed,
  onCollapse,
  selectedKey,
  openKeys,
  onOpenKeysChange,
  items,
  onNavigate,
}: AppSidebarProps) {
  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={onCollapse}
      trigger={null}
      width={232}
      collapsedWidth={72}
      className="admin-sider app-sidebar"
      breakpoint="lg"
      onBreakpoint={(broken) => onCollapse?.(broken)}
    >
      <div className="admin-logo">
        {mode === 'system' ? (
          <>
            <div className="admin-logo-icon">
              <SettingOutlined />
            </div>
            {!collapsed && <span>系统管理中心</span>}
          </>
        ) : (
          <>
            <div className="admin-logo-icon admin-logo-icon--kiwi">
              <img src={kiwiIcon} alt="" width={28} height={28} />
            </div>
            {!collapsed && <span>miyf</span>}
          </>
        )}
      </div>
      <Menu
        mode="inline"
        selectedKeys={selectedKey ? [selectedKey] : []}
        openKeys={collapsed ? [] : openKeys}
        onOpenChange={(keys) => onOpenKeysChange?.(keys as string[])}
        items={items}
        onClick={({ key }) => {
          if (typeof key === 'string' && key.startsWith('/')) onNavigate(key);
        }}
        style={{ border: 'none' }}
      />
    </Sider>
  );
}
