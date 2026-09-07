import { Space } from 'antd';
import type { ReactNode } from 'react';

type PageToolbarProps = {
  left?: ReactNode;
  right?: ReactNode;
};

/** 表格上方操作条：左侧筛选摘要/Tab，右侧主操作。 */
export default function PageToolbar({ left, right }: PageToolbarProps) {
  return (
    <div className="page-toolbar">
      <div className="page-toolbar-left">{left}</div>
      <div className="page-toolbar-right">
        <Space wrap>{right}</Space>
      </div>
    </div>
  );
}
