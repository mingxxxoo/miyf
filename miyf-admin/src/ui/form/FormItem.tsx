import { Col, Form } from 'antd';
import type { FormItemProps as AntFormItemProps } from 'antd';
import type { ReactNode } from 'react';
import { resolveColSpan, useFormLayout } from './layout';

export type FormItemProps = AntFormItemProps & {
  /** 占栅格几列（相对当前 FormGrid.columns），默认 1。 */
  col?: number;
  /** 整行占满（等价 col = columns）。 */
  full?: boolean;
  children?: ReactNode;
};

/**
 * 封装 Form.Item：自动进入响应式栅格。
 * <ul>
 *   <li>xs 单列堆叠</li>
 *   <li>sm 最多两列</li>
 *   <li>md+ 按 FormGrid columns（默认三列）</li>
 * </ul>
 */
export default function FormItem({ col = 1, full, children, hidden, ...rest }: FormItemProps) {
  const { columns } = useFormLayout();

  if (hidden) {
    return (
      <Form.Item {...rest} hidden>
        {children}
      </Form.Item>
    );
  }

  const span = resolveColSpan(columns, col, full);
  return (
    <Col xs={24} sm={span >= 24 ? 24 : span >= 16 ? 24 : 12} md={span} lg={span}>
      <Form.Item {...rest}>{children}</Form.Item>
    </Col>
  );
}
