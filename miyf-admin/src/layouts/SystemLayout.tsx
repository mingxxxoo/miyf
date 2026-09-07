import AppShell from '@/layouts/AppShell';

export { firstSystemPath } from '@/layouts/systemMenus';

/** 系统模块布局（兼容旧导入）。 */
export default function SystemLayout() {
  return <AppShell mode="system" />;
}
