import { Button, Result } from 'antd';
import type { ReactNode } from 'react';
import { getErrorMessage } from '@/api/errors';

type ErrorStateProps = {
  error?: unknown;
  title?: string;
  description?: ReactNode;
  onRetry?: () => void;
};

/** 错误态，可重试。 */
export default function ErrorState({
  error,
  title = '加载失败',
  description,
  onRetry,
}: ErrorStateProps) {
  const detail = description ?? getErrorMessage(error, '请稍后重试');
  return (
    <Result
      status="error"
      title={title}
      subTitle={detail}
      extra={
        onRetry ? (
          <Button type="primary" onClick={onRetry}>
            重试
          </Button>
        ) : null
      }
    />
  );
}
