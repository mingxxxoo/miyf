import type { ReactNode } from 'react';
import { Space, Typography } from 'antd';

export type PageHeaderProps = {
  title: ReactNode;
  description?: ReactNode;
  /** @deprecated 使用 description */
  subtitle?: ReactNode;
  extra?: ReactNode;
};

/** 页面标题区：标题 + 描述 + 右侧操作。 */
export default function PageHeader({ title, description, subtitle, extra }: PageHeaderProps) {
  const desc = description ?? subtitle;
  return (
    <div className="page-header">
      <div className="page-header-main">
        <h2 className="ck-page-title" style={{ margin: 0 }}>
          {title}
        </h2>
        {desc ? (
          <Typography.Text type="secondary" className="page-header-desc">
            {desc}
          </Typography.Text>
        ) : null}
      </div>
      {extra ? (
        <div className="page-header-extra">
          <Space wrap>{extra}</Space>
        </div>
      ) : null}
    </div>
  );
}
