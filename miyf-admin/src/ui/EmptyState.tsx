import { Button, Empty, Space } from 'antd';
import type { ReactNode } from 'react';

type EmptyStateProps = {
  description?: ReactNode;
  actionText?: string;
  onAction?: () => void;
  extra?: ReactNode;
};

/** 空数据态，可带下一步操作。 */
export default function EmptyState({
  description = '暂无数据',
  actionText,
  onAction,
  extra,
}: EmptyStateProps) {
  return (
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description={description}
      style={{ padding: '32px 0' }}
    >
      {(actionText && onAction) || extra ? (
        <Space>
          {actionText && onAction ? (
            <Button type="primary" onClick={onAction}>
              {actionText}
            </Button>
          ) : null}
          {extra}
        </Space>
      ) : null}
    </Empty>
  );
}
