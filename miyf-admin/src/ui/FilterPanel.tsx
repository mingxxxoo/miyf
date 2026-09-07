import { Card } from 'antd';
import type { ReactNode } from 'react';

type FilterPanelProps = {
  children: ReactNode;
  extra?: ReactNode;
};

/** 列表筛选区统一容器。 */
export default function FilterPanel({ children, extra }: FilterPanelProps) {
  return (
    <Card
      bordered={false}
      className="filter-panel"
      styles={{ body: { padding: '16px 20px' } }}
      style={{ marginBottom: 16 }}
    >
      <div className="filter-panel-inner">
        <div className="filter-panel-fields">{children}</div>
        {extra ? <div className="filter-panel-extra">{extra}</div> : null}
      </div>
    </Card>
  );
}
