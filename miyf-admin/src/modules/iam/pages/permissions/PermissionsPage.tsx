import { useCallback, useEffect, useMemo, useState, type Key } from 'react';
import {
  Button,
  Descriptions,
  Drawer,
  Input,
  Space,
  Spin,
  Tabs,
  Tag,
  Tree,
  Typography,
  message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  iamPermGroupApi,
  iamPermissionApi,
  iamRoleApi,
  type IamPermGroup,
  type IamRole,
  type IamPermission,
} from '@/modules/iam/api';
import {
  EmptyState,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
} from '@/ui';
import { notifyError } from '@/api/errors';

type PermNode = IamPermission;

/** 页面 Tab：厨房 / 健康 / 框架（iam+system） */
type WorkbenchTab = 'kitchen' | 'health' | 'framework';

const TAB_ITEMS: { key: WorkbenchTab; label: string; products: string[] }[] = [
  { key: 'kitchen', label: '厨房', products: ['kitchen'] },
  { key: 'health', label: '健康', products: ['health'] },
  { key: 'framework', label: '框架', products: ['iam', 'system', 'basic'] },
];

const PRODUCT_LABEL: Record<string, string> = {
  kitchen: '厨房',
  health: '健康',
  iam: '权限',
  system: '系统设置',
  basic: '基础',
};

const NODE_TYPE_META: Record<string, { label: string; color: string }> = {
  ROOT: { label: '工作台', color: 'gold' },
  PRODUCT: { label: '产品域', color: 'blue' },
  BIZ: { label: '业务', color: 'cyan' },
  API: { label: '接口', color: 'green' },
};

/**
 * 权限定义：按业务 Tab 切换，目录按 工作台 → 业务 → 接口 展示（只读）。
 */
export default function PermissionsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<PermNode[]>([]);
  const [groups, setGroups] = useState<IamPermGroup[]>([]);
  const [roles, setRoles] = useState<IamRole[]>([]);

  const [activeTab, setActiveTab] = useState<WorkbenchTab>('kitchen');
  const [keyword, setKeyword] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [refsOpen, setRefsOpen] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [perms, groupList, roleList] = await Promise.all([
        iamPermissionApi.list(),
        iamPermGroupApi.list().catch(() => [] as IamPermGroup[]),
        iamRoleApi.list().catch(() => [] as IamRole[]),
      ]);
      setAll(perms);
      setGroups(groupList);
      setRoles(roleList);
    } catch (err) {
      setAll([]);
      setGroups([]);
      setRoles([]);
      notifyError(err, '加载权限失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const byId = useMemo(() => new Map(all.map((p) => [p.id, p])), [all]);

  const tabProducts = useMemo(
    () => TAB_ITEMS.find((t) => t.key === activeTab)?.products ?? [],
    [activeTab],
  );

  /** 当前 Tab 下的节点（含祖先，保证树完整） */
  const scoped = useMemo(() => {
    const keep = new Set<string>();
    for (const p of all) {
      const prod = resolveProduct(p, byId);
      if (!prod || !tabProducts.includes(normalizeProduct(prod))) continue;
      keep.add(p.id);
      let pid = p.parentId;
      while (pid && byId.has(pid)) {
        keep.add(pid);
        pid = byId.get(pid)!.parentId;
      }
    }
    return all.filter((p) => keep.has(p.id));
  }, [all, byId, tabProducts]);

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return scoped;
    const keep = new Set<string>();
    for (const p of scoped) {
      if (!matches(p, q)) continue;
      keep.add(p.id);
      let pid = p.parentId;
      while (pid && byId.has(pid)) {
        keep.add(pid);
        pid = byId.get(pid)!.parentId;
      }
    }
    return scoped.filter((p) => keep.has(p.id));
  }, [scoped, keyword, byId]);

  const treeData = useMemo(
    () => buildWorkbenchTree(filtered, selectedId, activeTab),
    [filtered, selectedId, activeTab],
  );

  const defaultExpandKeys = useMemo(() => {
    // 默认展开工作台层，露出业务模块
    return filtered.filter((p) => p.nodeType === 'ROOT').map((p) => p.id as Key);
  }, [filtered]);

  const allExpandableKeys = useMemo(() => collectExpandableKeys(treeData), [treeData]);

  // Tab / 搜索变化时调整展开与选中
  useEffect(() => {
    if (!scoped.length) {
      setSelectedId(null);
      setExpandedKeys([]);
      return;
    }
    if (keyword.trim()) {
      setExpandedKeys(allExpandableKeys);
    } else {
      setExpandedKeys(defaultExpandKeys);
    }
    setSelectedId((prev) => {
      if (prev && scoped.some((p) => p.id === prev)) return prev;
      return (
        scoped.find((p) => isApiNode(p))?.id ??
        scoped.find((p) => p.nodeType === 'BIZ')?.id ??
        scoped[0]?.id ??
        null
      );
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 仅随 Tab/关键词/数据量同步
  }, [activeTab, keyword, scoped.length, all.length]);

  const selected = selectedId ? byId.get(selectedId) ?? null : null;

  const modulePath = useMemo(() => {
    if (!selected) return '—';
    const parts: string[] = [];
    let cur: PermNode | undefined = selected;
    const stack: PermNode[] = [];
    while (cur) {
      stack.unshift(cur);
      cur = cur.parentId ? byId.get(cur.parentId) : undefined;
    }
    for (const n of stack) {
      if (n.nodeType === 'ROOT') {
        parts.push(workbenchLabel(n, activeTab));
      } else if (n.nodeType === 'BIZ') {
        parts.push(shortName(n));
      } else if (n.nodeType === 'PRODUCT') {
        parts.push(PRODUCT_LABEL[normalizeProduct(n.product)] || shortName(n));
      }
    }
    return parts.length ? parts.join(' / ') : '—';
  }, [selected, byId, activeTab]);

  const ownedGroups = useMemo(() => {
    if (!selected || !isApiNode(selected)) return [] as IamPermGroup[];
    return groups.filter((g) => (g.permissionIds ?? []).includes(selected.id));
  }, [selected, groups]);

  const authRefs = useMemo(() => {
    if (!selected) return { groups: [] as IamPermGroup[], roles: [] as IamRole[] };
    const relatedGroups = groups.filter((g) => (g.permissionIds ?? []).includes(selected.id));
    const groupIds = new Set(relatedGroups.map((g) => g.id));
    const relatedRoles = roles.filter((r) => (r.groupIds ?? []).some((id) => groupIds.has(id)));
    return { groups: relatedGroups, roles: relatedRoles };
  }, [selected, groups, roles]);

  const isFullyExpanded =
    allExpandableKeys.length > 0 && allExpandableKeys.every((k) => expandedKeys.includes(k));

  const copyCode = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      message.success('已复制权限码');
    } catch {
      message.error('复制失败');
    }
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="权限定义"
        description="按业务查看权限树：工作台 → 业务 → 接口。权限由后端启动扫描生成，此处只读排查。"
        extra={
          <Button onClick={() => void fetchData()} loading={loading}>
            刷新
          </Button>
        }
      />

      <Tabs
        activeKey={activeTab}
        onChange={(k) => {
          setKeyword('');
          setActiveTab(k as WorkbenchTab);
        }}
        items={TAB_ITEMS.map((t) => ({ key: t.key, label: t.label }))}
        style={{ marginBottom: 8 }}
      />

      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索权限名称、权限码"
            style={{ width: 280 }}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={setKeyword}
          />
        }
        right={
          <Button
            onClick={() =>
              setExpandedKeys(isFullyExpanded ? defaultExpandKeys : allExpandableKeys)
            }
          >
            {isFullyExpanded ? '收起业务' : '展开全部'}
          </Button>
        }
      />

      <Spin spinning={loading}>
        {filtered.length === 0 ? (
          <SettingSection title="权限目录">
            <EmptyState
              description={
                keyword
                  ? '没有匹配的权限'
                  : `当前「${TAB_ITEMS.find((t) => t.key === activeTab)?.label}」下暂无权限`
              }
              actionText={keyword ? '清空搜索' : undefined}
              onAction={keyword ? () => setKeyword('') : undefined}
            />
          </SettingSection>
        ) : (
          <SplitWorkspace
            leftWidth={360}
            leftTitle="权限目录"
            leftExtra={
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                工作台 → 业务 → 接口
              </Typography.Text>
            }
            left={
              <Tree
                className="perm-tree"
                showLine={{ showLeafIcon: false }}
                treeData={treeData}
                selectedKeys={selectedId ? [selectedId] : []}
                expandedKeys={expandedKeys}
                onExpand={(keys) => setExpandedKeys(keys)}
                onSelect={(keys) => {
                  const key = keys[0];
                  if (key != null) setSelectedId(String(key));
                }}
                style={{ background: 'transparent', padding: '4px 12px 12px' }}
              />
            }
            rightTitle={selected ? shortName(selected) : '权限详情'}
            rightExtra={
              selected && isApiNode(selected) ? (
                <Space>
                  <Button size="small" onClick={() => void copyCode(selected.code)}>
                    复制权限码
                  </Button>
                  <Button size="small" type="primary" ghost onClick={() => setRefsOpen(true)}>
                    授权引用
                  </Button>
                </Space>
              ) : null
            }
            right={
              selected ? (
                <div className="perm-detail">
                  {isApiNode(selected) ? (
                    <Typography.Paragraph
                      code
                      copyable={{ text: selected.code }}
                      style={{ marginBottom: 16 }}
                    >
                      {selected.code}
                    </Typography.Paragraph>
                  ) : (
                    <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
                      {NODE_TYPE_META[selected.nodeType || '']?.label || '目录节点'}
                    </Typography.Paragraph>
                  )}

                  <Descriptions column={1} size="small" bordered>
                    <Descriptions.Item label="层级路径">{modulePath}</Descriptions.Item>
                    <Descriptions.Item label="节点类型">
                      <Tag color={NODE_TYPE_META[normalizeNodeType(selected)]?.color || 'default'}>
                        {NODE_TYPE_META[normalizeNodeType(selected)]?.label ||
                          selected.nodeType ||
                          '接口'}
                      </Tag>
                    </Descriptions.Item>
                    {isApiNode(selected) ? (
                      <Descriptions.Item label="所属权限组">
                        {ownedGroups.length ? (
                          <Space direction="vertical" size={0}>
                            {ownedGroups.map((g) => (
                              <span key={g.id}>
                                {g.name}{' '}
                                <Typography.Text type="secondary">({g.code})</Typography.Text>
                              </span>
                            ))}
                          </Space>
                        ) : (
                          <Tag>未归组</Tag>
                        )}
                      </Descriptions.Item>
                    ) : null}
                    {selected.permNo ? (
                      <Descriptions.Item label="权限编号">
                        <Typography.Text style={{ fontVariantNumeric: 'tabular-nums' }}>
                          {selected.permNo}
                        </Typography.Text>
                      </Descriptions.Item>
                    ) : null}
                  </Descriptions>

                  {isApiNode(selected) ? (
                    <SettingSection title="接口说明">
                      <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                        {selected.description || selected.name || '暂无说明'}
                      </Typography.Paragraph>
                    </SettingSection>
                  ) : (
                    <SettingSection title="说明">
                      <Typography.Text type="secondary">
                        选择左侧接口节点可查看权限码与授权引用。
                      </Typography.Text>
                    </SettingSection>
                  )}
                </div>
              ) : (
                <EmptyState description="请从左侧选择一个权限节点" />
              )
            }
          />
        )}
      </Spin>

      <Drawer
        title="授权引用"
        open={refsOpen}
        onClose={() => setRefsOpen(false)}
        width={420}
        destroyOnClose
      >
        {selected ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <div>
              <Typography.Text type="secondary">权限</Typography.Text>
              <div>
                <Typography.Text strong>{shortName(selected)}</Typography.Text>
              </div>
              <Typography.Text code>{selected.code}</Typography.Text>
            </div>
            <SettingSection title="所属权限组">
              {authRefs.groups.length ? (
                authRefs.groups.map((g) => (
                  <div key={g.id} style={{ marginBottom: 8 }}>
                    <Typography.Text strong>{g.name}</Typography.Text>
                    <div>
                      <Typography.Text type="secondary">{g.code}</Typography.Text>
                    </div>
                  </div>
                ))
              ) : (
                <EmptyState description="未归入任何权限组" />
              )}
            </SettingSection>
            <SettingSection title="关联角色">
              {authRefs.roles.length ? (
                authRefs.roles.map((r) => (
                  <div key={r.id} style={{ marginBottom: 8 }}>
                    <Typography.Text strong>{r.name}</Typography.Text>
                    <div>
                      <Typography.Text type="secondary">
                        {r.code}
                        {r.userCount != null ? ` · ${r.userCount} 人` : ''}
                      </Typography.Text>
                    </div>
                  </div>
                ))
              ) : (
                <EmptyState description="暂无角色通过权限组引用该权限" />
              )}
            </SettingSection>
          </Space>
        ) : null}
      </Drawer>
    </div>
  );
}

function isApiNode(p: PermNode): boolean {
  if (p.nodeType === 'API') return true;
  if (p.nodeType === 'ROOT' || p.nodeType === 'PRODUCT' || p.nodeType === 'BIZ') return false;
  return !String(p.code || '').startsWith('tree:');
}

function normalizeNodeType(p: PermNode): string {
  if (p.nodeType) return p.nodeType;
  return isApiNode(p) ? 'API' : 'BIZ';
}

function normalizeProduct(product?: string) {
  if (!product) return '';
  return product.toLowerCase();
}

function matches(p: PermNode, q: string): boolean {
  return (
    p.code.toLowerCase().includes(q) ||
    (p.name || '').toLowerCase().includes(q) ||
    (p.description || '').toLowerCase().includes(q) ||
    (p.treeName || '').toLowerCase().includes(q) ||
    (p.permNo || '').includes(q)
  );
}

function shortName(p: PermNode): string {
  return (p.name || '').trim() || (p.treeName || '').trim() || p.code || p.permNo || p.id;
}

/** 工作台节点展示名：个人 / 单位 / 超管；框架域追加产品区分 */
function workbenchLabel(p: PermNode, tab: WorkbenchTab): string {
  const scope = shortName(p);
  if (tab !== 'framework') return scope;
  const prod = PRODUCT_LABEL[normalizeProduct(p.product)] || p.product;
  return prod ? `${scope} · ${prod}` : scope;
}

function resolveProduct(p: PermNode, byId: Map<string, PermNode>): string | undefined {
  if (p.product) return p.product;
  let cur: PermNode | undefined = p;
  while (cur) {
    if (cur.product) return cur.product;
    cur = cur.parentId ? byId.get(cur.parentId) : undefined;
  }
  if (p.code && !p.code.startsWith('tree:')) {
    const head = p.code.split(':')[0];
    if (head) return head;
  }
  return undefined;
}

function titleOf(p: PermNode, selectedId: string | null, tab: WorkbenchTab): DataNode['title'] {
  const nt = p.nodeType;
  const active = p.id === selectedId;
  const isApi = isApiNode(p);
  let text = shortName(p);
  if (nt === 'ROOT') text = workbenchLabel(p, tab);

  return (
    <span
      className={
        nt === 'ROOT'
          ? 'perm-tree__root'
          : nt === 'BIZ'
            ? 'perm-tree__biz'
            : isApi
              ? 'perm-tree__api'
              : undefined
      }
      style={{
        color: 'var(--ck-text)',
        fontWeight: active ? 600 : nt === 'ROOT' || nt === 'BIZ' ? 500 : 400,
        fontSize: isApi ? 13 : 14,
      }}
    >
      {text}
      {isApi ? (
        <Typography.Text type="secondary" style={{ marginLeft: 8, fontSize: 12, fontWeight: 400 }}>
          {p.code}
        </Typography.Text>
      ) : null}
    </span>
  );
}

/**
 * 构建 工作台(ROOT) → 业务(BIZ) → 接口(API) 树。
 * 若无 parent 关系则按权限码 kitchen:biz:action 兜底组树。
 */
function buildWorkbenchTree(
  list: PermNode[],
  selectedId: string | null,
  tab: WorkbenchTab,
): DataNode[] {
  const hasTree = list.some((p) => p.parentId || p.nodeType === 'ROOT');
  if (!hasTree) return buildLegacyTree(list, selectedId);

  const byId = new Map(list.map((p) => [p.id, p]));
  const childrenMap = new Map<string, PermNode[]>();
  for (const p of list) {
    // 跳过 PRODUCT 中间层（若有），把其子节点挂到更上层
    if (p.nodeType === 'PRODUCT') continue;
    let pid = p.parentId || '';
    // 父为 PRODUCT 时挂到 PRODUCT 的父（或作为根）
    const parent = pid ? byId.get(pid) : undefined;
    if (parent?.nodeType === 'PRODUCT') {
      pid = parent.parentId && byId.has(parent.parentId) ? parent.parentId : '';
    }
    // 父不在当前列表中则视为根
    if (pid && !byId.has(pid)) pid = '';
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid)!.push(p);
  }

  for (const arr of childrenMap.values()) {
    arr.sort(compareNodes);
  }

  const walk = (parentKey: string): DataNode[] =>
    (childrenMap.get(parentKey) || []).map((p) => ({
      key: p.id,
      title: titleOf(p, selectedId, tab),
      children: walk(p.id),
      isLeaf: !(childrenMap.get(p.id)?.length),
    }));

  // 优先以 ROOT 为顶层；无 ROOT 时用无父节点
  const roots =
    (childrenMap.get('') || []).length > 0
      ? childrenMap.get('')!
      : list.filter((p) => p.nodeType === 'ROOT' || !p.parentId || !byId.has(p.parentId!));

  const ordered = roots.slice().sort(compareNodes);
  // 工作台排序：个人 → 单位 → 超管
  ordered.sort((a, b) => {
    if (a.nodeType === 'ROOT' && b.nodeType === 'ROOT') {
      return scopeOrder(a.name) - scopeOrder(b.name) || compareNodes(a, b);
    }
    return compareNodes(a, b);
  });

  return ordered.map((p) => ({
    key: p.id,
    title: titleOf(p, selectedId, tab),
    children: walk(p.id),
  }));
}

function buildLegacyTree(list: PermNode[], selectedId: string | null): DataNode[] {
  const workbenchMap = new Map<string, Map<string, PermNode[]>>();
  for (const p of list) {
    if (!isApiNode(p)) continue;
    const parts = p.code.split(':');
    const biz = parts.length >= 2 ? parts[1] : 'default';
    const wb = '超管';
    if (!workbenchMap.has(wb)) workbenchMap.set(wb, new Map());
    const bizMap = workbenchMap.get(wb)!;
    if (!bizMap.has(biz)) bizMap.set(biz, []);
    bizMap.get(biz)!.push(p);
  }
  return [...workbenchMap.entries()].map(([wb, bizMap]) => ({
    key: `wb:${wb}`,
    title: wb,
    selectable: false,
    children: [...bizMap.entries()].map(([biz, items]) => ({
      key: `biz:${wb}:${biz}`,
      title: biz,
      selectable: false,
      children: items
        .slice()
        .sort((a, b) => shortName(a).localeCompare(shortName(b), 'zh'))
        .map((p) => ({
          key: p.id,
          title: titleOf(p, selectedId, 'framework'),
          isLeaf: true,
        })),
    })),
  }));
}

function compareNodes(a: PermNode, b: PermNode) {
  return (
    (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || shortName(a).localeCompare(shortName(b), 'zh')
  );
}

function scopeOrder(name?: string) {
  const n = (name || '').trim();
  if (n === '个人') return 0;
  if (n === '单位') return 1;
  if (n === '超管') return 2;
  return 9;
}

function collectExpandableKeys(nodes: DataNode[]): Key[] {
  const keys: Key[] = [];
  const walk = (arr: DataNode[]) => {
    for (const n of arr) {
      if (n.children?.length) {
        keys.push(n.key);
        walk(n.children);
      }
    }
  };
  walk(nodes);
  return keys;
}
