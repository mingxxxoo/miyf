import { Spin } from 'antd';
import type { ReactNode } from 'react';

type LoadingStateProps = {
  spinning?: boolean;
  tip?: string;
  children?: ReactNode;
  minHeight?: number | string;
};

/** 统一加载包裹。 */
export default function LoadingState({
  spinning = true,
  tip,
  children,
  minHeight = 160,
}: LoadingStateProps) {
  if (children) {
    return (
      <Spin spinning={spinning} tip={tip}>
        {children}
      </Spin>
    );
  }
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight }}>
      <Spin tip={tip} />
    </div>
  );
}
