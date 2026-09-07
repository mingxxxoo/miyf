import type { ReactNode } from 'react';
import { Typography } from 'antd';

export type SettingSectionProps = {
  title: string;
  description?: ReactNode;
  extra?: ReactNode;
  children: ReactNode;
  danger?: boolean;
};

/**
 * 系统设置分组区块：标题 + 说明 + 内容区。
 */
export default function SettingSection({
  title,
  description,
  extra,
  children,
  danger,
}: SettingSectionProps) {
  return (
    <section className={`setting-section${danger ? ' setting-section--danger' : ''}`}>
      <header className="setting-section-header">
        <div>
          <Typography.Title level={5} className="setting-section-title">
            {title}
          </Typography.Title>
          {description ? (
            <Typography.Paragraph type="secondary" className="setting-section-desc">
              {description}
            </Typography.Paragraph>
          ) : null}
        </div>
        {extra ? <div className="setting-section-extra">{extra}</div> : null}
      </header>
      <div className="setting-section-body">{children}</div>
    </section>
  );
}
