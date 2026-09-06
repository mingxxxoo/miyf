import { Modal } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

interface ConfirmDialogOptions {
  title?: string;
  content?: React.ReactNode;
  okText?: string;
  cancelText?: string;
  onOk?: () => void | Promise<void>;
  onCancel?: () => void;
  danger?: boolean;
}

export function confirmDialog({
  title = '确认操作',
  content = '确定要执行此操作吗？',
  okText = '确定',
  cancelText = '取消',
  onOk,
  onCancel,
  danger = false,
}: ConfirmDialogOptions) {
  Modal.confirm({
    title,
    icon: <ExclamationCircleOutlined />,
    content,
    okText,
    cancelText,
    okButtonProps: danger ? { danger: true } : undefined,
    onOk,
    onCancel,
  });
}

interface ConfirmDialogProps {
  open: boolean;
  title?: string;
  content?: React.ReactNode;
  okText?: string;
  cancelText?: string;
  loading?: boolean;
  danger?: boolean;
  onOk: () => void;
  onCancel: () => void;
}

export default function ConfirmDialog({
  open,
  title = '确认操作',
  content = '确定要执行此操作吗？',
  okText = '确定',
  cancelText = '取消',
  loading,
  danger,
  onOk,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      title={title}
      okText={okText}
      cancelText={cancelText}
      confirmLoading={loading}
      okButtonProps={danger ? { danger: true } : undefined}
      onOk={onOk}
      onCancel={onCancel}
    >
      {content}
    </Modal>
  );
}
