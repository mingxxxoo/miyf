import { Modal, Typography } from 'antd';

export type ChangeItem = {
  label: string;
  from?: string;
  to?: string;
};

type ChangeSummaryModalProps = {
  open: boolean;
  title?: string;
  items: ChangeItem[];
  confirmLoading?: boolean;
  danger?: boolean;
  onOk: () => void;
  onCancel: () => void;
};

/** 保存前变更摘要确认。 */
export default function ChangeSummaryModal({
  open,
  title = '确认变更',
  items,
  confirmLoading,
  danger,
  onOk,
  onCancel,
}: ChangeSummaryModalProps) {
  return (
    <Modal
      open={open}
      title={title}
      onOk={onOk}
      onCancel={onCancel}
      confirmLoading={confirmLoading}
      okButtonProps={danger ? { danger: true } : undefined}
      destroyOnClose
    >
      <Typography.Paragraph type="secondary">
        请确认以下变更后再提交：
      </Typography.Paragraph>
      <ul className="change-summary-list">
        {items.map((item) => (
          <li key={item.label}>
            <strong>{item.label}</strong>
            {item.from != null || item.to != null ? (
              <span>
                ：{item.from ?? '—'} → {item.to ?? '—'}
              </span>
            ) : null}
          </li>
        ))}
      </ul>
      {!items.length ? <Typography.Text type="secondary">无变更</Typography.Text> : null}
    </Modal>
  );
}
