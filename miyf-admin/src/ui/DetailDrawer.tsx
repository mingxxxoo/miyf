import { Drawer } from 'antd';
import type { DrawerProps } from 'antd';
import type { ReactNode } from 'react';

type DetailDrawerProps = Omit<DrawerProps, 'children'> & {
  children?: ReactNode;
};

/** 详情抽屉：统一宽度与销毁策略。 */
export default function DetailDrawer({
  width = 520,
  destroyOnClose = true,
  children,
  ...rest
}: DetailDrawerProps) {
  return (
    <Drawer width={width} destroyOnClose={destroyOnClose} {...rest}>
      {children}
    </Drawer>
  );
}
