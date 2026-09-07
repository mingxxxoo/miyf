import { Tag } from 'antd';
import { statusOf, type StatusMeta } from '@/constants/status';

type StatusBadgeProps = {
  code?: string | null;
  map: Record<string, StatusMeta>;
  fallback?: StatusMeta;
};

/** 统一状态徽章。 */
export default function StatusBadge({ code, map, fallback }: StatusBadgeProps) {
  const meta = statusOf(map, code);
  const resolved = code ? meta : fallback ?? meta;
  return <Tag color={resolved.color}>{resolved.text}</Tag>;
}
