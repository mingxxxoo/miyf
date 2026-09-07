import { useCallback, useEffect, useMemo, useState, type Key } from 'react';
import {
  Button,
  Col,
  Descriptions,
  Drawer,
  Input,
  Row,
  Select,
  Space,
  Spin,
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
  type IamPermission,
  type IamRole,
} from '@/modules/iam/api';
import { sysAppApi } from '@/modules/system/api';
import {
  EmptyState,
  MetricCard,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
} from '@/ui';
import { notifyError } from '@/api/errors';

type PermNode = IamPermission;

const NODE_TYPE_META: Record<string, { label: string; color: string }> = {
  ROOT: { label: '根域', color: 'gold' },
  PRODUCT: { label: '产品域', color: 'blue' },
  BIZ: { label: '业务模块', color: 'cyan' },
  API: { label: '接口', color: 'green' },
};

const PRODUCT_FALLBACK: Record<string, string> = {
  system: '系统',
  kitchen: '厨房业务',
  health: '健康数据',
  iam: '系统',
};

/**
 * 权限定义：只读排查台 — 指标、筛选、目录树 + 详情、授权引用。
 */
export default function PermissionsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<PermNode[]>([]);
  const [groups, setGroups] = useState<IamPermGroup[]>([]);
  const [roles, setRoles] = useState<IamRole[]>([]);
  const [productLabels, setProductLabels] = useState<Record<string, string>>({ ...PRODUCT_FALLBACK });

  const [keyword, setKeyword] = useState('');
  const [productFilter, setProductFilter] = useState<string | undefined>();
  const [nodeTypeFilter, setNodeTypeFilter] = useState<string | undefined>();
  const [groupFilter, setGroupFilter] = useState<string | undefined>();

  const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [refsOpen, setRefsOpen] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [perms, groupList, roleList] = await Promise.all([
        iamPermissionApi.list(),
        iamPermGroupApi.list().catch(() => {
          message.warning('权限组加载失败');
          return [] as IamPermGroup[];
        }),
        iamRoleApi.list().catch(() => {
          message.warning('角色加载失败');
          return [] as IamRole[];
        }),
      ]);
      setAll(perms);
      setGroups(groupList);
      setRoles(roleList);
      setSelectedId((prev) => {
        if (prev && perms.some((p) => p.id === prev)) return prev;
        return perms.find((p) => isApiNode(p))?.id ?? perms[0]?.id ?? null;
      });
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

  useEffect(() => {
    sysAppApi
      .list({ status: 'ENABLED' })
      .then((apps) => {
        const map = { ...PRODUCT_FALLBACK };
        for (const a of apps) {
          map[a.code] = a.name;
        }
        setProductLabels(map);
      })
      .catch(() => {
        message.warning('应用列表加载失败，已使用默认产品名');
      });
  }, []);

  const groupByCode = useMemo(() => {
    const map = new Map<string, IamPermGroup>();
    for (const g of groups) map.set(g.code, g);
    return map;
  }, [groups]);

  const byId = useMemo(() => new Map(all.map((p) => [p.id, p])), [all]);

  const stats = useMemo(() => {
    const api = all.filter((p) => isApiNode(p));
    const biz = all.filter((p) => p.nodeType === 'BIZ');
    const boundIds = new Set(groups.flatMap((g) => g.permissionIds ?? []));
    const ungrouped = api.filter((p) => !boundIds.has(p.id));
    return {
      total: all.length,
      api: api.length,
      biz: biz.length,
      ungrouped: ungrouped.length,
    };
  }, [all, groups]);

  const productOptions = useMemo(() => {
    const set = new Set<string>();
    for (const p of all) {
      const prod = resolveProduct(p, byId);
      if (prod) set.add(prod);
    }
    return [...set]
      .sort()
      .map((code) => ({ value: code, label: productLabels[code] || code }));
  }, [all, byId, productLabels]);

  const groupOptions = useMemo(
    () =>
      groups
        .slice()
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name, 'zh'))
        .map((g) => ({ value: g.code, label: `${g.name} (${g.code})` })),
    [groups],
  );

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    const matched = new Set<string>();

    for (const p of all) {
      if (productFilter) {
        const prod = resolveProduct(p, byId);
        if (prod !== productFilter) continue;
      }
      if (nodeTypeFilter) {
        const nt = normalizeNodeType(p);
        if (nt !== nodeTypeFilter) continue;
      }
      if (groupFilter === '__ungrouped__') {
        const boundIds = new Set(groups.flatMap((g) => g.permissionIds ?? []));
        if (!isApiNode(p) || boundIds.has(p.id)) continue;
      } else if (groupFilter) {
        const g = groupByCode.get(groupFilter);
        if (!g?.permissionIds?.includes(p.id)) continue;
      }
      if (q && !matches(p, q)) continue;
      matched.add(p.id);
      if (q || productFilter || nodeTypeFilter || groupFilter) {
        let pid = p.parentId;
        while (pid && byId.has(pid)) {
          matched.add(pid);
          pid = byId.get(pid)!.parentId;
        }
      }
    }

    if (!q && !productFilter && !nodeTypeFilter && !groupFilter) return all;
    return all.filter((p) => matched.has(p.id));
  }, [all, byId, groups, groupByCode, keyword, productFilter, nodeTypeFilter, groupFilter]);

  const treeData = useMemo(() => {
    const hasTree = filtered.some((p) => p.parentId || p.nodeType === 'ROOT');
    return hasTree
      ? buildParentTree(filtered, selectedId)
      : buildLegacyTree(filtered, selectedId, productLabels);
  }, [filtered, selectedId, productLabels]);

  const defaultExpandKeys = useMemo(() => {
    // 默认只展开到产品域一层，避免整树铺开
    return filtered
      .filter((p) => p.nodeType === 'PRODUCT' || p.nodeType === 'ROOT')
      .map((p) => p.id as Key);
  }, [filtered]);

  const allExpandableKeys = useMemo(() => {
    const keys: Key[] = [];
    const walk = (nodes: DataNode[]) => {
      for (const n of nodes) {
        if (n.children?.length) {
          keys.push(n.key);
          walk(n.children);
        }
      }
    };
    walk(treeData);
    return keys;
  }, [treeData]);

  const hasActiveFilter = !!(
    keyword.trim() ||
    productFilter ||
    nodeTypeFilter ||
    groupFilter
  );

  // 仅在筛选条件或数据首次就绪时调整展开，避免与手动展开/收起打架
  useEffect(() => {
    if (!all.length) return;
    if (hasActiveFilter) {
      setExpandedKeys(allExpandableKeys);
    } else {
      setExpandedKeys(defaultExpandKeys);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 故意只跟筛选与数据量
  }, [keyword, productFilter, nodeTypeFilter, groupFilter, all.length]);

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
      if (n.nodeType === 'ROOT') continue;
      if (n.nodeType === 'PRODUCT') {
        parts.push(productLabels[n.product || ''] || shortName(n));
      } else if (n.nodeType === 'BIZ') {
        parts.push(shortName(n));
      }
    }
    if (!parts.length) {
      const prod = resolveProduct(selected, byId);
      if (prod) parts.push(productLabels[prod] || prod);
    }
    return parts.length ? parts.join(' / ') : '—';
  }, [selected, byId, productLabels]);

  const ownedGroups = useMemo(() => {
    if (!selected) return [] as IamPermGroup[];
    return groups.filter((g) => (g.permissionIds ?? []).includes(selected.id));
  }, [selected, groups]);

  const authRefs = useMemo(() => {
    if (!selected) return { groups: [] as IamPermGroup[], roles: [] as IamRole[] };
    const relatedGroups = groups.filter((g) => (g.permissionIds ?? []).includes(selected.id));
    const groupIds = new Set(relatedGroups.map((g) => g.id));
    const relatedRoles = roles.filter((r) => (r.groupIds ?? []).some((id) => groupIds.has(id)));
    return { groups: relatedGroups, roles: relatedRoles };
  }, [selected, groups, roles]);

  const childSummary = useMemo(() => {
    if (!selected || isApiNode(selected)) return null;
    const kids = all.filter((p) => p.parentId === selected.id);
    const apiKids = countDescendants(selected.id, all, (p) => isApiNode(p));
    const bizKids = kids.filter((p) => p.nodeType === 'BIZ').length;
    return { direct: kids.length, api: apiKids, biz: bizKids };
  }, [selected, all]);

  const copyCode = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      message.success('已复制权限码');
    } catch {
      message.error('复制失败');
    }
  };

  const clearFilters = () => {
    setKeyword('');
    setProductFilter(undefined);
    setNodeTypeFilter(undefined);
    setGroupFilter(undefined);
    // 立即还原展开态；effect 也会同步，这里避免一帧延迟
    setExpandedKeys(defaultExpandKeys);
  };

  const isFullyExpanded =
    allExpandableKeys.length > 0 &&
    allExpandableKeys.every((k) => expandedKeys.includes(k));

  return (
    <div className="ck-page">
      <PageHeader
        title="权限定义"
        description="管理系统中的接口权限、业务模块和授权引用关系。权限由后端启动扫描生成，此处主要用于查看和排查。"
        extra={
          <Button onClick={() => void fetchData()} loading={loading}>
            刷新
          </Button>
        }
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <MetricCard
            label="权限总数"
            value={stats.total}
            hint="清空筛选并还原目录展开"
            active={!hasActiveFilter}
            onClick={clearFilters}
          />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard
            label="接口权限"
            value={stats.api}
            active={nodeTypeFilter === 'API' && groupFilter !== '__ungrouped__'}
            onClick={() => {
              setKeyword('');
              setProductFilter(undefined);
              setGroupFilter(undefined);
              setNodeTypeFilter('API');
            }}
          />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard
            label="业务模块"
            value={stats.biz}
            active={nodeTypeFilter === 'BIZ'}
            onClick={() => {
              setKeyword('');
              setProductFilter(undefined);
              setGroupFilter(undefined);
              setNodeTypeFilter('BIZ');
            }}
          />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard
            label="未归组"
            value={stats.ungrouped}
            hint="接口缺少匹配的权限组"
            active={groupFilter === '__ungrouped__'}
            onClick={() => {
              setKeyword('');
              setProductFilter(undefined);
              setNodeTypeFilter('API');
              setGroupFilter('__ungrouped__');
            }}
          />
        </Col>
      </Row>

      <PageToolbar
        left={
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="搜索权限名称、权限码、编号"
              style={{ width: 260 }}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={setKeyword}
            />
            <Select
              allowClear
              placeholder="产品域"
              style={{ width: 140 }}
              options={productOptions}
              value={productFilter}
              onChange={setProductFilter}
            />
            <Select
              allowClear
              placeholder="节点类型"
              style={{ width: 130 }}
              options={[
                { value: 'PRODUCT', label: '产品域' },
                { value: 'BIZ', label: '业务模块' },
                { value: 'API', label: '接口' },
                { value: 'ROOT', label: '根域' },
              ]}
              value={nodeTypeFilter}
              onChange={setNodeTypeFilter}
            />
            <Select
              allowClear
              placeholder="权限组"
              style={{ width: 200 }}
              options={[
                ...groupOptions,
                { value: '__ungrouped__', label: '未归组' },
              ]}
              value={groupFilter}
              onChange={setGroupFilter}
              showSearch
              optionFilterProp="label"
            />
            {hasActiveFilter ? (
              <Button type="link" onClick={clearFilters}>
                清空筛选
              </Button>
            ) : null}
          </Space>
        }
        right={
          <Space>
            <Button
              onClick={() =>
                setExpandedKeys(isFullyExpanded ? defaultExpandKeys : allExpandableKeys)
              }
            >
              {isFullyExpanded ? '还原展开' : '展开全部'}
            </Button>
          </Space>
        }
      />

      <Spin spinning={loading}>
        {filtered.length === 0 ? (
          <SettingSection title="权限目录">
            <EmptyState
              description="没有匹配的权限，试试调整筛选条件"
              actionText="清空筛选"
              onAction={clearFilters}
            />
          </SettingSection>
        ) : (
          <SplitWorkspace
            leftWidth={340}
            leftTitle="权限目录"
            leftExtra={
              <Typography.Link
                onClick={() =>
                  setExpandedKeys(isFullyExpanded ? defaultExpandKeys : allExpandableKeys)
                }
              >
                {isFullyExpanded ? '还原' : '展开'}
              </Typography.Link>
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
            rightTitle="当前权限详情"
            rightExtra={
              selected && isApiNode(selected) ? (
                <Space>
                  <Button size="small" onClick={() => void copyCode(selected.code)}>
                    复制权限码
                  </Button>
                  <Button size="small" type="primary" ghost onClick={() => setRefsOpen(true)}>
                    查看授权引用
                  </Button>
                </Space>
              ) : null
            }
            right={
              selected ? (
                <div className="perm-detail">
                  <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 4 }}>
                    {shortName(selected)}
                  </Typography.Title>
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
                      {selected.code?.startsWith('tree:') ? ` · ${selected.code}` : null}
                    </Typography.Paragraph>
                  )}

                  <Descriptions column={1} size="small" bordered>
                    <Descriptions.Item label="所属模块">{modulePath}</Descriptions.Item>
                    <Descriptions.Item label="节点类型">
                      <Tag color={NODE_TYPE_META[normalizeNodeType(selected)]?.color || 'default'}>
                        {NODE_TYPE_META[normalizeNodeType(selected)]?.label ||
                          selected.nodeType ||
                          '接口'}
                      </Tag>
                    </Descriptions.Item>
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
                    {selected.permNo ? (
                      <Descriptions.Item label="权限编号">
                        <Typography.Text style={{ fontVariantNumeric: 'tabular-nums' }}>
                          {selected.permNo}
                        </Typography.Text>
                      </Descriptions.Item>
                    ) : null}
                    {selected.product ? (
                      <Descriptions.Item label="产品域">
                        {productLabels[selected.product] || selected.product}
                      </Descriptions.Item>
                    ) : null}
                    {childSummary ? (
                      <Descriptions.Item label="下级概览">
                        直接子节点 {childSummary.direct} · 业务模块 {childSummary.biz} · 接口{' '}
                        {childSummary.api}
                      </Descriptions.Item>
                    ) : null}
                  </Descriptions>

                  <SettingSection
                    title="接口说明"
                    description={isApiNode(selected) ? undefined : '目录节点无接口说明'}
                  >
                    {isApiNode(selected) ? (
                      <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                        {selected.description || selected.name || '暂无说明'}
                      </Typography.Paragraph>
                    ) : (
                      <Typography.Text type="secondary">
                        选择左侧接口权限可查看扫描写入的说明文案。
                      </Typography.Text>
                    )}
                  </SettingSection>

                  {isApiNode(selected) ? (
                    <SettingSection title="授权引用摘要">
                      <Space direction="vertical" size={4}>
                        <Typography.Text>
                          权限组：{authRefs.groups.length ? authRefs.groups.map((g) => g.name).join('、') : '无'}
                        </Typography.Text>
                        <Typography.Text>
                          关联角色：{authRefs.roles.length ? authRefs.roles.map((r) => r.name).join('、') : '无'}
                        </Typography.Text>
                        <Button type="link" style={{ padding: 0 }} onClick={() => setRefsOpen(true)}>
                          查看完整引用
                        </Button>
                      </Space>
                    </SettingSection>
                  ) : null}
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
            <SettingSection title="通过权限组关联的角色">
              {authRefs.roles.length ? (
                authRefs.roles.map((r) => (
                  <div key={r.id} style={{ marginBottom: 8 }}>
                    <Typography.Text strong>{r.name}</Typography.Text>
                    <div>
                      <Typography.Text type="secondary">
                        {r.code}
                        {r.product ? ` · ${productLabels[r.product] || r.product}` : ''}
                        {r.userCount != null ? ` · ${r.userCount} 人` : ''}
                      </Typography.Text>
                    </div>
                  </div>
                ))
              ) : (
                <EmptyState description="暂无角色通过权限组引用该权限" />
              )}
            </SettingSection>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, fontSize: 12 }}>
              引用关系根据权限组编码与角色绑定的权限组推导，不修改后端接口。
            </Typography.Paragraph>
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

function matches(p: PermNode, q: string): boolean {
  return (
    p.code.toLowerCase().includes(q) ||
    (p.name || '').toLowerCase().includes(q) ||
    (p.description || '').toLowerCase().includes(q) ||
    (p.groupCode || '').toLowerCase().includes(q) ||
    (p.treeName || '').toLowerCase().includes(q) ||
    (p.permNo || '').includes(q) ||
    (p.product || '').toLowerCase().includes(q)
  );
}

function shortName(p: PermNode): string {
  return (p.name || '').trim() || (p.treeName || '').trim() || p.code || p.permNo || p.id;
}

function resolveProduct(p: PermNode, byId: Map<string, PermNode>): string | undefined {
  if (p.product) return p.product;
  let cur: PermNode | undefined = p;
  while (cur) {
    if (cur.nodeType === 'PRODUCT' && cur.product) return cur.product;
    if (cur.product) return cur.product;
    cur = cur.parentId ? byId.get(cur.parentId) : undefined;
  }
  // 从权限码推断 kitchen:dish:list → kitchen
  if (p.code && !p.code.startsWith('tree:')) {
    const head = p.code.split(':')[0];
    if (head) return head;
  }
  return undefined;
}

function countDescendants(
  rootId: string,
  all: PermNode[],
  pred: (p: PermNode) => boolean,
): number {
  const childrenMap = new Map<string, PermNode[]>();
  for (const p of all) {
    const pid = p.parentId || '';
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid)!.push(p);
  }
  let count = 0;
  const walk = (id: string) => {
    for (const c of childrenMap.get(id) || []) {
      if (pred(c)) count += 1;
      walk(c.id);
    }
  };
  walk(rootId);
  return count;
}

function titleOf(p: PermNode, selectedId: string | null): DataNode['title'] {
  const isApi = isApiNode(p);
  const active = p.id === selectedId;
  return (
    <span
      style={{
        color: 'var(--ck-text)',
        fontWeight: active ? 600 : isApi ? 400 : 500,
        fontSize: isApi ? 13 : 14,
      }}
    >
      {shortName(p)}
    </span>
  );
}

function buildParentTree(list: PermNode[], selectedId: string | null): DataNode[] {
  const byId = new Map(list.map((p) => [p.id, p]));
  const childrenMap = new Map<string, PermNode[]>();
  for (const p of list) {
    const pid = p.parentId || '';
    if (!childrenMap.has(pid)) childrenMap.set(pid, []);
    childrenMap.get(pid)!.push(p);
  }
  for (const arr of childrenMap.values()) {
    arr.sort(
      (a, b) =>
        (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || shortName(a).localeCompare(shortName(b), 'zh'),
    );
  }

  const walk = (parentKey: string): DataNode[] => {
    const kids = childrenMap.get(parentKey) || [];
    return kids.map((p) => ({
      key: p.id,
      title: titleOf(p, selectedId),
      children: walk(p.id),
      isLeaf: !childrenMap.get(p.id)?.length,
    }));
  };

  const roots = list.filter((p) => !p.parentId || !byId.has(p.parentId));
  roots.sort(
    (a, b) =>
      (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || shortName(a).localeCompare(shortName(b), 'zh'),
  );
  return roots.map((p) => ({
    key: p.id,
    title: titleOf(p, selectedId),
    children: walk(p.id),
  }));
}

function buildLegacyTree(
  list: PermNode[],
  selectedId: string | null,
  productLabels: Record<string, string>,
): DataNode[] {
  const productMap = new Map<string, PermNode[]>();
  for (const p of list) {
    if (!isApiNode(p)) continue;
    const prod = p.code.split(':')[0] || 'other';
    if (!productMap.has(prod)) productMap.set(prod, []);
    productMap.get(prod)!.push(p);
  }
  return [...productMap.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([prod, perms]) => {
      const bizMap = new Map<string, PermNode[]>();
      for (const p of perms) {
        const parts = p.code.split(':');
        const biz = parts.length >= 2 ? parts[1] : 'default';
        if (!bizMap.has(biz)) bizMap.set(biz, []);
        bizMap.get(biz)!.push(p);
      }
      return {
        key: `product:${prod}`,
        title: productLabels[prod] || prod,
        selectable: false,
        children: [...bizMap.entries()].map(([biz, items]) => ({
          key: `biz:${prod}:${biz}`,
          title: biz,
          selectable: false,
          children: items.map((p) => ({
            key: p.id,
            title: titleOf(p, selectedId),
            isLeaf: true,
          })),
        })),
      } as DataNode;
    });
}
