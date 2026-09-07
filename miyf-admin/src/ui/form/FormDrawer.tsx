import { Button, Drawer, Form, Space } from 'antd';
import type { DrawerProps, FormInstance } from 'antd';
import type { ReactNode } from 'react';
import FormGrid from './FormGrid';
import { DEFAULT_FORM_LAYOUT, type FormColumns } from './layout';

export type FormDrawerProps = Omit<DrawerProps, 'extra'> & {
  form: FormInstance;
  onSave?: () => void | Promise<void>;
  columns?: FormColumns;
  initialValues?: Record<string, unknown>;
  saveText?: string;
  children?: ReactNode;
  /** 覆盖右上角操作区；默认提供保存按钮。 */
  extra?: ReactNode;
};

/**
 * 抽屉表单：与 FormModal 同一套栅格约定。
 */
export default function FormDrawer({
  form,
  onSave,
  columns = DEFAULT_FORM_LAYOUT.columns,
  initialValues,
  saveText = '保存',
  children,
  width = 640,
  destroyOnClose = true,
  extra,
  ...drawerProps
}: FormDrawerProps) {
  const actions =
    extra ??
    (onSave ? (
      <Space>
        <Button type="primary" onClick={() => void onSave()}>
          {saveText}
        </Button>
      </Space>
    ) : undefined);

  return (
    <Drawer width={width} destroyOnClose={destroyOnClose} extra={actions} {...drawerProps}>
      <Form form={form} layout="vertical" initialValues={initialValues}>
        <FormGrid columns={columns}>{children}</FormGrid>
      </Form>
    </Drawer>
  );
}
