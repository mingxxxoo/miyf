import { Rate } from 'antd';

interface StarRatingProps {
  value?: number;
  onChange?: (value: number) => void;
  disabled?: boolean;
  allowHalf?: boolean;
  count?: number;
}

export default function StarRating({
  value = 0,
  onChange,
  disabled = false,
  allowHalf = true,
  count = 5,
}: StarRatingProps) {
  return (
    <Rate
      value={value}
      onChange={onChange}
      disabled={disabled}
      allowHalf={allowHalf}
      count={count}
      style={{ color: 'var(--ck-primary)' }}
    />
  );
}
