import { useCallback, useEffect, useMemo, useState } from 'react';
import { Card, Empty, Input, Spin, Tree, Typography } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { iamPermissionApi, type IamPermission } from '@/modules/iam/api';

type PermNode = IamPermission & {
  parentId?: string;
  permNo?: string;
  treeName?: string;
  nodeType?: string;
  product?: string;
  sortOrder?: number;
};

/**
 * 权限树：优先按 parentId 层级；无树数据时回退按 groupCode/code 分层。
 */
export default function PermissionsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<PermNode[]>([]);
  const [keyword, setKeyword] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setAll((await iamPermissionApi.list()) as PermNode[]);
    } catch {
      setAll([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const treeData = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    const filtered = q
      ? all.filter(
          (p) =>
            p.code.toLowerCase().includes(q) ||
            (p.name || '').toLowerCase().includes(q) ||
            (p.groupCode || '').toLowerCase().includes(q) ||
            (p.treeName || '').toLowerCase().includes(q) ||
            (p.permNo || '').includes(q),
        )
      : all;

    const hasTree = filtered.some((p) => p.parentId || p.nodeType === 'ROOT');
    if (hasTree) {
      return buildParentTree(filtered);
    }
    return buildLegacyTree(filtered);
  }, [all, keyword]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">权限管理</h2>
      <Card
        title="权限树（域 → 产品 → 业务 → 接口）"
        extra={
          <Input.Search
            allowClear
            placeholder="搜索编号 / 编码 / 名称"
            style={{ width: 280 }}
            onSearch={setKeyword}
            onChange={(e) => {
              if (!e.target.value) setKeyword('');
            }}
          />
        }
      >
        <Spin spinning={loading}>
          {treeData.length === 0 ? (
            <Empty description="暂无权限定义" />
          ) : (
            <Tree showLine defaultExpandAll treeData={treeData} />
          )}
        </Spin>
      </Card>
    </div>
  );
}

function titleOf(p: PermNode): DataNode['title'] {
  const label = p.treeName || p.name || p.code;
  return (
    <span>
      <Typography.Text strong>{label}</Typography.Text>
      {p.permNo && (
        <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
          {p.permNo}
        </Typography.Text>
      )}
      {p.nodeType === 'API' && (
        <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
          {p.code}
        </Typography.Text>
      )}
    </span>
  );
}

function buildParentTree(list: PermNode[]): DataNode[] {
  const byId = new Map(list.map((p) => [p.id, p]));
  const childrenMap = new Map<string, PermNode[]>();
  for (const p of list) {
    const pid = p.parentId || '';
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid)!.push(p);
  }
  for (const arr of childrenMap.values()) {
    arr.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.code.localeCompare(b.code));
  }

  const walk = (parentKey: string): DataNode[] => {
    const kids = childrenMap.get(parentKey) || [];
    return kids.map((p) => ({
      key: p.id,
      title: titleOf(p),
      children: walk(p.id),
    }));
  };

  // 根：parent 空或不在当前列表中
  const roots = list.filter((p) => !p.parentId || !byId.has(p.parentId));
  roots.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.code.localeCompare(b.code));
  return roots.map((p) => ({
    key: p.id,
    title: titleOf(p),
    children: walk(p.id),
  }));
}

function buildLegacyTree(list: PermNode[]): DataNode[] {
  const groupMap = new Map<string, PermNode[]>();
  for (const p of list) {
    const g = p.groupCode || 'default';
    if (!groupMap.has(g)) groupMap.set(g, []);
    groupMap.get(g)!.push(p);
  }
  return [...groupMap.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([groupCode, perms]) => {
      const bizMap = new Map<string, PermNode[]>();
      for (const p of perms) {
        const parts = p.code.split(':');
        const biz = parts.length >= 2 ? `${parts[0]}:${parts[1]}` : p.code;
        if (!bizMap.has(biz)) bizMap.set(biz, []);
        bizMap.get(biz)!.push(p);
      }
      return {
        key: `group:${groupCode}`,
        title: `${groupCode}（${perms.length}）`,
        children: [...bizMap.entries()].map(([biz, items]) => ({
          key: `biz:${groupCode}:${biz}`,
          title: biz,
          children: items.map((p) => ({
            key: p.id,
            title: titleOf(p),
            isLeaf: true,
          })),
        })),
      } as DataNode;
    });
}
