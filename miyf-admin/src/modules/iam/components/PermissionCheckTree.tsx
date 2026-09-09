import { useEffect, useMemo, useState, type Key, type ReactNode } from 'react';
import { Button, Input, Space, Tree, Typography } from 'antd';
import type { DataNode } from 'antd/es/tree';
import type { IamPermission } from '@/modules/iam/api';

const PRODUCT_LABEL: Record<string, string> = {
  kitchen: '厨房业务',
  health: '健康管理',
  basic: '基础',
  system: '基础',
  iam: '基础',
  platform: '平台',
};

export type PermissionCheckTreeProps = {
  value?: string[];
  onChange?: (ids: string[]) => void;
  permissions: IamPermission[];
  /** 仅展示该产品域；不传则展示全部 */
  productFilter?: string;
  height?: number;
};

/**
 * 权限勾选树：产品 → 业务 → 接口，仅叶子（API）写入表单值。
 */
export default function PermissionCheckTree({
  value,
  onChange,
  permissions,
  productFilter,
  height = 360,
}: PermissionCheckTreeProps) {
  const [keyword, setKeyword] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);

  const apiIdSet = useMemo(() => {
    const set = new Set<string>();
    for (const p of permissions) {
      if (isApiNode(p)) set.add(p.id);
    }
    return set;
  }, [permissions]);

  const scoped = useMemo(() => {
    if (!productFilter) return permissions;
    const byId = new Map(permissions.map((p) => [p.id, p]));
    const keep = new Set<string>();
    for (const p of permissions) {
      const prod = resolveProduct(p, byId);
      if (!sameProductDomain(prod, productFilter)) continue;
      keep.add(p.id);
      let pid = p.parentId;
      while (pid && byId.has(pid)) {
        keep.add(pid);
        pid = byId.get(pid)?.parentId;
      }
    }
    return permissions.filter((p) => keep.has(p.id));
  }, [permissions, productFilter]);

  const visible = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return scoped;
    const byId = new Map(scoped.map((p) => [p.id, p]));
    const keep = new Set<string>();
    for (const p of scoped) {
      if (!matches(p, q)) continue;
      keep.add(p.id);
      let pid = p.parentId;
      while (pid && byId.has(pid)) {
        keep.add(pid);
        pid = byId.get(pid)?.parentId;
      }
    }
    return scoped.filter((p) => keep.has(p.id));
  }, [scoped, keyword]);

  const treeData = useMemo(() => buildTree(visible), [visible]);
  const allKeys = useMemo(() => collectKeys(treeData), [treeData]);

  useEffect(() => {
    if (keyword.trim()) {
      setExpandedKeys(allKeys);
    } else {
      // 默认展开到业务模块层
      const keys: Key[] = [];
      for (const p of visible) {
        if (p.nodeType === 'ROOT' || p.nodeType === 'PRODUCT' || p.nodeType === 'BIZ') {
          keys.push(p.id);
        }
      }
      setExpandedKeys(keys.length ? keys : allKeys.slice(0, Math.min(allKeys.length, 24)));
    }
  }, [keyword, visible, allKeys]);

  const checkedKeys = useMemo(
    () => (value ?? []).filter((id) => apiIdSet.has(id)),
    [value, apiIdSet],
  );

  const handleCheck = (
    checked:
      | Key[]
      | {
          checked: Key[];
          halfChecked: Key[];
        },
  ) => {
    const raw = Array.isArray(checked) ? checked : checked.checked;
    const next = raw.map(String).filter((id) => apiIdSet.has(id));
    onChange?.(next);
  };

  return (
    <div className="perm-check-tree">
      <div className="perm-check-tree__toolbar">
        <Input.Search
          allowClear
          placeholder="搜索权限名称或编码"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onSearch={setKeyword}
          style={{ flex: 1, minWidth: 160 }}
        />
        <Space size={4}>
          <Button type="link" size="small" onClick={() => setExpandedKeys(allKeys)}>
            展开
          </Button>
          <Button type="link" size="small" onClick={() => setExpandedKeys([])}>
            折叠
          </Button>
        </Space>
      </div>
      <div className="perm-check-tree__meta">
        <Typography.Text type="secondary">
          已选 {checkedKeys.length} 项接口权限
        </Typography.Text>
        {checkedKeys.length > 0 ? (
          <Button type="link" size="small" onClick={() => onChange?.([])}>
            清空
          </Button>
        ) : null}
      </div>
      <div className="perm-check-tree__body" style={{ maxHeight: height }}>
        {treeData.length === 0 ? (
          <Typography.Text type="secondary">暂无可选权限</Typography.Text>
        ) : (
          <Tree
            checkable
            showLine={{ showLeafIcon: false }}
            treeData={treeData}
            checkedKeys={checkedKeys}
            expandedKeys={expandedKeys}
            onExpand={(keys) => setExpandedKeys(keys)}
            onCheck={handleCheck}
            selectable={false}
            height={height - 8}
          />
        )}
      </div>
    </div>
  );
}

function isApiNode(p: IamPermission): boolean {
  return !p.nodeType || p.nodeType === 'API';
}

function normalizeProduct(product?: string) {
  if (!product) return 'system';
  const p = product.toLowerCase();
  if (p === 'iam') return 'basic';
  return p;
}

/** basic / system / iam 同属基础框架域 */
function sameProductDomain(a?: string, b?: string) {
  const left = normalizeProduct(a);
  const right = normalizeProduct(b);
  if (left === right) return true;
  const basicish = new Set(['basic', 'system']);
  return basicish.has(left) && basicish.has(right);
}

function resolveProduct(p: IamPermission, byId: Map<string, IamPermission>): string {
  if (p.product) return p.product;
  let cur: IamPermission | undefined = p;
  while (cur) {
    if (cur.product) return cur.product;
    cur = cur.parentId ? byId.get(cur.parentId) : undefined;
  }
  return 'system';
}

function matches(p: IamPermission, q: string): boolean {
  return (
    p.code.toLowerCase().includes(q) ||
    (p.name || '').toLowerCase().includes(q) ||
    (p.treeName || '').toLowerCase().includes(q) ||
    (p.groupCode || '').toLowerCase().includes(q)
  );
}

function nodeTitle(p: IamPermission): ReactNode {
  const name = (p.treeName || p.name || p.code).trim();
  if (p.nodeType === 'PRODUCT') {
    const label = PRODUCT_LABEL[normalizeProduct(p.product)] || name;
    return <span className="perm-check-tree__product">{label}</span>;
  }
  if (p.nodeType === 'BIZ') {
    return <span className="perm-check-tree__biz">{name}</span>;
  }
  if (p.nodeType === 'ROOT') {
    return <span className="perm-check-tree__root">{name}</span>;
  }
  return (
    <span className="perm-check-tree__api">
      <span className="perm-check-tree__api-name">{p.name || p.code}</span>
      <Typography.Text type="secondary" className="perm-check-tree__api-code">
        {p.code}
      </Typography.Text>
    </span>
  );
}

function buildTree(list: IamPermission[]): DataNode[] {
  if (!list.length) return [];
  const hasParent = list.some((p) => p.parentId || p.nodeType === 'ROOT');
  if (!hasParent) {
    const byProduct = new Map<string, IamPermission[]>();
    for (const p of list) {
      if (!isApiNode(p)) continue;
      const prod = normalizeProduct(p.product || p.groupCode || 'system');
      if (!byProduct.has(prod)) byProduct.set(prod, []);
      byProduct.get(prod)!.push(p);
    }
    return [...byProduct.entries()]
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([prod, apis]) => ({
        key: `product:${prod}`,
        title: <span className="perm-check-tree__product">{PRODUCT_LABEL[prod] || prod}</span>,
        children: apis
          .slice()
          .sort((a, b) => (a.name || a.code).localeCompare(b.name || b.code, 'zh'))
          .map((p) => ({
            key: p.id,
            title: nodeTitle(p),
            isLeaf: true,
          })),
      }));
  }

  const byId = new Map(list.map((p) => [p.id, p]));
  const childrenMap = new Map<string, IamPermission[]>();
  for (const p of list) {
    const pid = p.parentId && byId.has(p.parentId) ? p.parentId : '';
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid)!.push(p);
  }
  for (const arr of childrenMap.values()) {
    arr.sort(
      (a, b) =>
        (a.sortOrder ?? 0) - (b.sortOrder ?? 0) ||
        (a.name || a.code).localeCompare(b.name || b.code, 'zh'),
    );
  }
  const walk = (parentKey: string): DataNode[] =>
    (childrenMap.get(parentKey) || []).map((p) => {
      const kids = walk(p.id);
      const isLeaf = isApiNode(p) && kids.length === 0;
      return {
        key: p.id,
        title: nodeTitle(p),
        checkable: isApiNode(p) || kids.length > 0,
        children: kids.length ? kids : undefined,
        isLeaf,
      };
    });
  return walk('');
}

function collectKeys(nodes: DataNode[]): Key[] {
  const keys: Key[] = [];
  const walk = (arr: DataNode[]) => {
    for (const n of arr) {
      keys.push(n.key);
      if (n.children?.length) walk(n.children as DataNode[]);
    }
  };
  walk(nodes);
  return keys;
}
