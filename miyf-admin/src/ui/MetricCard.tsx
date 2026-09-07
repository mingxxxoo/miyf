import type { ReactNode } from 'react';
import { Card, Tooltip } from 'antd';

type MetricCardProps = {
  label: string;
  value: ReactNode;
  hint?: ReactNode;
  onClick?: () => void;
  active?: boolean;
};

/** 仪表盘/概览指标卡：等高，说明用 Tooltip，避免 hint 撑高错位。 */
export default function MetricCard({ label, value, hint, onClick, active }: MetricCardProps) {
  const card = (
    <Card
      bordered={false}
      className={`metric-card${active ? ' metric-card--active' : ''}${onClick ? ' metric-card--clickable' : ''}`}
      hoverable={!!onClick}
      onClick={onClick}
      styles={{ body: { padding: 20, minHeight: 96 } }}
    >
      <div className="metric-card-value">{value}</div>
      <div className="metric-card-label">{label}</div>
    </Card>
  );

  if (hint) {
    return (
      <Tooltip title={hint} placement="bottom">
        {card}
      </Tooltip>
    );
  }
  return card;
}
