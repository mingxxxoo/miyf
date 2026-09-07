import HealthProvidersPage from '@/modules/health/pages/HealthProvidersPage';
import { PageHeader } from '@/ui';

/**
 * 系统管理中心 · 健康同步：复用健康数据源控制台（同一后端接口）。
 */
export default function SystemHealthSyncPage() {
  return (
    <div className="ck-page">
      <PageHeader
        title="健康同步"
        description="数据源绑定、OAuth 授权与同步运行记录"
      />
      <HealthProvidersPage embedded />
    </div>
  );
}
