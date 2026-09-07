import type { CSSProperties, ReactNode } from 'react';
import { Row } from 'antd';
import {
  DEFAULT_FORM_LAYOUT,
  FormLayoutContext,
  type FormColumns,
  type FormLayoutConfig,
} from './layout';

export type FormGridProps = {
  /** 一行几列，默认 3。 */
  columns?: FormColumns;
  gutter?: FormLayoutConfig['gutter'];
  children?: ReactNode;
  className?: string;
  style?: CSSProperties;
};

/**
 * 表单响应式栅格容器。子项使用 {@link FormItem}，默认一行三列。
 */
export default function FormGrid({
  columns = DEFAULT_FORM_LAYOUT.columns,
  gutter = DEFAULT_FORM_LAYOUT.gutter,
  children,
  className,
  style,
}: FormGridProps) {
  return (
    <FormLayoutContext.Provider value={{ columns, gutter }}>
      <Row gutter={gutter} className={className} style={style}>
        {children}
      </Row>
    </FormLayoutContext.Provider>
  );
}
