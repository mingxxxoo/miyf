import { createContext, useContext } from 'react';

/** 表单栅格列数：默认 3 列一行。 */
export type FormColumns = 1 | 2 | 3 | 4;

export type FormLayoutConfig = {
  columns: FormColumns;
  gutter: number | [number, number];
};

export const DEFAULT_FORM_LAYOUT: FormLayoutConfig = {
  columns: 3,
  gutter: [16, 0],
};

export const FormLayoutContext = createContext<FormLayoutConfig>(DEFAULT_FORM_LAYOUT);

export function useFormLayout(): FormLayoutConfig {
  return useContext(FormLayoutContext);
}

/** 按「占几列」换算 Ant Design Col span（24 栅格）。 */
export function resolveColSpan(columns: FormColumns, col: number, full?: boolean): number {
  if (full || col >= columns) {
    return 24;
  }
  const unit = Math.floor(24 / columns);
  return Math.min(24, unit * Math.max(1, col));
}
