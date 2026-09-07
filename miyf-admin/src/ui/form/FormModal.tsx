import { Form, Modal } from 'antd';
import type { FormInstance, ModalProps } from 'antd';
import type { ReactNode } from 'react';
import FormGrid from './FormGrid';
import { DEFAULT_FORM_LAYOUT, type FormColumns } from './layout';

export type FormModalProps = Omit<ModalProps, 'onOk'> & {
  form: FormInstance;
  /** 确认时校验并回调表单值；也可自行在外部 validate。 */
  onOk?: () => void | Promise<void>;
  columns?: FormColumns;
  /** 表单初始值。 */
  initialValues?: Record<string, unknown>;
  children?: ReactNode;
};

/**
 * 弹窗表单：统一 vertical + 响应式栅格（默认一行三列）。
 * 默认宽度 800，适配三列布局。
 */
export default function FormModal({
  form,
  onOk,
  columns = DEFAULT_FORM_LAYOUT.columns,
  initialValues,
  children,
  width = 800,
  destroyOnClose = true,
  ...modalProps
}: FormModalProps) {
  const handleOk = async () => {
    if (onOk) {
      await onOk();
      return;
    }
    await form.validateFields();
  };

  return (
    <Modal width={width} destroyOnClose={destroyOnClose} onOk={() => void handleOk()} {...modalProps}>
      <Form form={form} layout="vertical" initialValues={initialValues} style={{ marginTop: 8 }}>
        <FormGrid columns={columns}>{children}</FormGrid>
      </Form>
    </Modal>
  );
}
