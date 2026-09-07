import { Breadcrumb } from 'antd';
import { Link, useLocation } from 'react-router-dom';

const LABEL_MAP: Record<string, string> = {
  dashboard: '数据概览',
  kitchen: '厨房',
  categories: '分类管理',
  dishes: '菜品管理',
  create: '新建',
  edit: '编辑',
  recipes: '菜谱管理',
  orders: '预约管理',
  comments: '评论管理',
  users: '用户管理',
  'operation-logs': '操作日志',
  health: '健康',
  overview: '概览',
  subjects: '主体',
  samples: '采样',
  providers: '数据源',
  trends: '趋势',
  iam: '组织',
  'org-units': '组织架构',
  system: '系统管理中心',
  roles: '角色管理',
  'role-auth': '用户授权',
  menus: '菜单管理',
  permissions: '权限定义',
  'perm-groups': '权限组',
  apps: '应用管理',
  config: '基础设置',
  dicts: '数据字典',
  notifications: '通知中心',
  jobs: '定时任务',
  monitor: '系统监控',
  'health-sync': '健康同步',
};

type BreadcrumbsProps = {
  /** 覆盖最后一段标题。 */
  currentLabel?: string;
};

/** 根据路由自动生成面包屑。 */
export default function Breadcrumbs({ currentLabel }: BreadcrumbsProps) {
  const location = useLocation();
  const parts = location.pathname.split('/').filter(Boolean);

  if (parts.length === 0) {
    return null;
  }

  const items = parts.map((part, index) => {
    const path = `/${parts.slice(0, index + 1).join('/')}`;
    const isLast = index === parts.length - 1;
    const isId = /^\d+$/.test(part) || part.length > 16;
    let label = LABEL_MAP[part] || (isId ? '详情' : part);
    if (isLast && currentLabel) label = currentLabel;

    return {
      key: path,
      title: isLast || isId ? <span>{label}</span> : <Link to={path}>{label}</Link>,
    };
  });

  return <Breadcrumb items={items} className="app-breadcrumbs" />;
}
