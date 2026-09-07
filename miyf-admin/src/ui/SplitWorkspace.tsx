import type { ReactNode } from 'react';
import { Card } from 'antd';

export type SplitWorkspaceProps = {
  left: ReactNode;
  right: ReactNode;
  leftWidth?: number | string;
  leftTitle?: ReactNode;
  rightTitle?: ReactNode;
  leftExtra?: ReactNode;
  rightExtra?: ReactNode;
};

/**
 * 左右分栏工作区（用户授权 / 菜单 / 字典等）。
 */
export default function SplitWorkspace({
  left,
  right,
  leftWidth = 320,
  leftTitle,
  rightTitle,
  leftExtra,
  rightExtra,
}: SplitWorkspaceProps) {
  return (
    <div className="split-workspace">
      <Card
        className="split-workspace-pane"
        title={leftTitle}
        extra={leftExtra}
        styles={{ body: { padding: 0, height: '100%' } }}
        style={{ width: leftWidth, flexShrink: 0 }}
      >
        <div className="split-workspace-scroll">{left}</div>
      </Card>
      <Card
        className="split-workspace-pane split-workspace-main"
        title={rightTitle}
        extra={rightExtra}
        styles={{ body: { padding: 16, minHeight: 420 } }}
      >
        {right}
      </Card>
    </div>
  );
}
