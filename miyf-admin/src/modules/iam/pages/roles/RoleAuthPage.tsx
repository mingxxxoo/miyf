import { useCallback, useEffect, useMemo, useState, type Key } from 'react';
import {
  Button,
  Input,
  List,
  Select,
  Space,
  Spin,
  Tree,
  Typography,
  message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  flattenTree,
  iamPermGroupApi,
  iamPermissionApi,
  iamRoleApi,
  iamUserApi,
  type IamPermGroup,
  type IamPermission,
  type IamRole,
  type IamUser,
} from '@/modules/iam/api';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';
import { USER_STATUS } from '@/constants/status';
import {
  ChangeSummaryModal,
  EmptyState,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
  StatusBadge,
  type ChangeItem,
} from '@/ui';

/**
 * 用户授权：左侧选人，右侧分配角色并预览继承权限。
 */
export default function RoleAuthPage() {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<IamUser[]>([]);
  const [roles, setRoles] = useState<IamRole[]>([]);
  const [groups, setGroups] = useState<IamPermGroup[]>([]);
  const [permissions, setPermissions] = useState<IamPermission[]>([]);
  /** userId → roleIds（优先用户 VO；无则空） */
  const [userRoleMap, setUserRoleMap] = useState<Record<string, string[]>>({});
  const [keyword, setKeyword] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [draftRoleIds, setDraftRoleIds] = useState<string[]>([]);
  const [permKeyword, setPermKeyword] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [summaryItems, setSummaryItems] = useState<ChangeItem[]>([]);
  const { submitting, run } = useSubmitting();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [userList, roleList, groupList, permTree] = await Promise.all([
        iamUserApi.list(),
        iamRoleApi.list(),
        iamPermGroupApi.list(),
        iamPermissionApi.listTree(),
      ]);
      setUsers(userList);
      setRoles(roleList);
      setGroups(groupList);
      setPermissions(flattenTree(permTree));
      const map: Record<string, string[]> = {};
      for (const u of userList) {
        map[u.id] = [...(u.roleIds ?? [])];
      }
      setUserRoleMap(map);
      setSelectedUserId((prev) => {
        if (prev && userList.some((u) => u.id === prev)) return prev;
        return userList[0]?.id ?? null;
      });
    } catch (err) {
      setUsers([]);
      setRoles([]);
      setGroups([]);
      setPermissions([]);
      setUserRoleMap({});
      notifyError(err, '加载授权数据失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const filteredUsers = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) =>
        u.username.toLowerCase().includes(q) ||
        (u.nickname || '').toLowerCase().includes(q),
    );
  }, [users, keyword]);

  const selectedUser = useMemo(
    () => users.find((u) => u.id === selectedUserId) ?? null,
    [users, selectedUserId],
  );

  const savedRoleIds = useMemo(
    () => (selectedUserId ? userRoleMap[selectedUserId] ?? [] : []),
    [selectedUserId, userRoleMap],
  );

  useEffect(() => {
    setDraftRoleIds(savedRoleIds);
    setPermKeyword('');
  }, [selectedUserId, savedRoleIds]);

  const dirty = useMemo(() => {
    const a = [...draftRoleIds].sort().join(',');
    const b = [...savedRoleIds].sort().join(',');
    return a !== b;
  }, [draftRoleIds, savedRoleIds]);

  const roleOptions = useMemo(
    () =>
      roles.map((r) => ({
        value: r.id,
        label: `${r.name} (${r.code})${r.product ? ` · ${r.product}` : ''}`,
      })),
    [roles],
  );

  const inheritedPerms = useMemo(() => {
    const assigned = roles.filter((r) => draftRoleIds.includes(r.id));
    const groupIdSet = new Set(assigned.flatMap((r) => r.groupIds ?? []));
    const permIdSet = new Set(
      groups.filter((g) => groupIdSet.has(g.id)).flatMap((g) => g.permissionIds ?? []),
    );
    if (!permIdSet.size) return [] as IamPermission[];
    return permissions.filter((p) => permIdSet.has(p.id));
  }, [draftRoleIds, roles, groups, permissions]);

  const visiblePerms = useMemo(() => {
    const q = permKeyword.trim().toLowerCase();
    if (!q) return inheritedPerms;
    const byId = new Map(permissions.map((p) => [p.id, p]));
    const keep = new Set<string>();
    for (const p of inheritedPerms) {
      if (!permMatches(p, q)) continue;
      keep.add(p.id);
      let pid = p.parentId;
      while (pid && byId.has(pid)) {
        keep.add(pid);
        pid = byId.get(pid)?.parentId;
      }
    }
    return permissions.filter((p) => keep.has(p.id));
  }, [inheritedPerms, permKeyword, permissions]);

  const treeData = useMemo(() => buildPermTree(visiblePerms), [visiblePerms]);

  const allTreeKeys = useMemo(() => collectKeys(treeData), [treeData]);

  useEffect(() => {
    if (permKeyword.trim()) {
      setExpandedKeys(allTreeKeys);
    } else {
      setExpandedKeys(allTreeKeys.slice(0, Math.min(allTreeKeys.length, 40)));
    }
  }, [permKeyword, allTreeKeys]);

  const selectUser = (id: string) => {
    if (id === selectedUserId) return;
    if (dirty) {
      message.warning('请先保存或取消当前用户的角色变更');
      return;
    }
    setSelectedUserId(id);
  };

  const openSaveSummary = () => {
    if (!selectedUser) return;
    if (!dirty) {
      message.info('角色未变更');
      return;
    }
    const saved = new Set(savedRoleIds);
    const draft = new Set(draftRoleIds);
    const added = draftRoleIds.filter((id) => !saved.has(id));
    const removed = savedRoleIds.filter((id) => !draft.has(id));
    const nameOf = (id: string) => roles.find((r) => r.id === id)?.name ?? id;
    const items: ChangeItem[] = [];
    for (const id of added) {
      items.push({ label: `新增角色「${nameOf(id)}」`, from: '未授权', to: '已授权' });
    }
    for (const id of removed) {
      items.push({ label: `移除角色「${nameOf(id)}」`, from: '已授权', to: '未授权' });
    }
    setSummaryItems(items);
    setSummaryOpen(true);
  };

  const confirmSave = () =>
    void run(async () => {
      if (!selectedUser) return;
      try {
        await iamUserApi.update(selectedUser.id, {
          username: selectedUser.username,
          nickname: selectedUser.nickname,
          status: selectedUser.status,
          orgUnitId: selectedUser.orgUnitId,
          roleIds: draftRoleIds,
        });
        message.success('用户角色已更新');
        setSummaryOpen(false);
        setUserRoleMap((prev) => ({ ...prev, [selectedUser.id]: [...draftRoleIds] }));
        void fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  return (
    <div className="ck-page">
      <PageHeader
        title="用户授权"
        description="为系统用户分配角色；权限通过角色权限组继承，不支持直接赋权"
      />
      <PageToolbar
        left={
          <Typography.Text type="secondary">
            共 {users.length} 名用户 · {roles.length} 个角色
          </Typography.Text>
        }
        right={
          <Button onClick={() => void fetchData()} disabled={loading}>
            刷新
          </Button>
        }
      />

      <Spin spinning={loading}>
        <SplitWorkspace
          leftWidth={300}
          leftTitle="用户"
          leftExtra={
            <Input.Search
              allowClear
              placeholder="搜索用户"
              style={{ width: 160 }}
              onSearch={setKeyword}
              onChange={(e) => {
                if (!e.target.value) setKeyword('');
              }}
            />
          }
          left={
            filteredUsers.length ? (
              <List
                size="small"
                dataSource={filteredUsers}
                renderItem={(u) => (
                  <List.Item
                    key={u.id}
                    onClick={() => selectUser(u.id)}
                    style={{
                      cursor: 'pointer',
                      padding: '10px 16px',
                      background: u.id === selectedUserId ? 'rgba(255, 179, 107, 0.12)' : undefined,
                    }}
                  >
                    <List.Item.Meta
                      title={
                        <Space size={8}>
                          <span>{u.nickname || u.username}</span>
                          <StatusBadge code={u.status} map={USER_STATUS} />
                        </Space>
                      }
                      description={
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {u.username}
                          {(userRoleMap[u.id]?.length ?? 0) > 0
                            ? ` · ${userRoleMap[u.id]!.length} 角色`
                            : ' · 未授权'}
                        </Typography.Text>
                      }
                    />
                  </List.Item>
                )}
              />
            ) : (
              <EmptyState description={keyword ? '无匹配用户' : '暂无用户'} />
            )
          }
          rightTitle={
            selectedUser
              ? `${selectedUser.nickname || selectedUser.username} · 授权`
              : '用户详情'
          }
          rightExtra={
            selectedUser ? (
              <Space>
                <Button
                  disabled={!dirty || submitting}
                  onClick={() => setDraftRoleIds(savedRoleIds)}
                >
                  取消
                </Button>
                <Button type="primary" disabled={!dirty} loading={submitting} onClick={openSaveSummary}>
                  保存授权
                </Button>
              </Space>
            ) : null
          }
          right={
            !selectedUser ? (
              <EmptyState description="请选择左侧用户" />
            ) : (
              <Space direction="vertical" size={20} style={{ width: '100%' }}>
                <SettingSection title="基本信息" description="当前选中的系统用户">
                  <Space wrap size={16}>
                    <Typography.Text>
                      登录名：<strong>{selectedUser.username}</strong>
                    </Typography.Text>
                    <Typography.Text>
                      昵称：{selectedUser.nickname || '—'}
                    </Typography.Text>
                    <StatusBadge code={selectedUser.status} map={USER_STATUS} />
                  </Space>
                </SettingSection>

                <SettingSection
                  title="角色权限"
                  description="通过角色获得权限组；保存前可预览继承权限"
                >
                  <Select
                    mode="multiple"
                    allowClear
                    showSearch
                    optionFilterProp="label"
                    style={{ width: '100%', marginBottom: 16 }}
                    placeholder="选择要授予的角色"
                    value={draftRoleIds}
                    options={roleOptions}
                    onChange={(ids) => setDraftRoleIds(ids)}
                  />
                  <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                    继承权限（只读预览）
                  </Typography.Text>
                  <Space style={{ marginBottom: 8 }} wrap>
                    <Input.Search
                      allowClear
                      placeholder="搜索权限"
                      style={{ width: 220 }}
                      onSearch={setPermKeyword}
                      onChange={(e) => {
                        if (!e.target.value) setPermKeyword('');
                      }}
                    />
                    <Button size="small" onClick={() => setExpandedKeys(allTreeKeys)}>
                      全部展开
                    </Button>
                    <Button size="small" onClick={() => setExpandedKeys([])}>
                      全部折叠
                    </Button>
                  </Space>
                  {treeData.length === 0 ? (
                    <EmptyState description="尚未分配角色，或角色未绑定权限组" />
                  ) : (
                    <Tree
                      checkable
                      checkedKeys={allTreeKeys}
                      expandedKeys={expandedKeys}
                      onExpand={(keys) => setExpandedKeys(keys)}
                      treeData={disableTreeChecks(treeData)}
                      selectable={false}
                    />
                  )}
                </SettingSection>

                <SettingSection title="直接权限" description="不经过角色的单独授权">
                  <EmptyState description="当前仅支持通过角色授权" />
                </SettingSection>
              </Space>
            )
          }
        />
      </Spin>

      <ChangeSummaryModal
        open={summaryOpen}
        title="确认角色授权变更"
        items={summaryItems}
        confirmLoading={submitting}
        onOk={() => void confirmSave()}
        onCancel={() => setSummaryOpen(false)}
      />
    </div>
  );
}

function permMatches(p: IamPermission, q: string): boolean {
  return (
    p.code.toLowerCase().includes(q) ||
    (p.name || '').toLowerCase().includes(q) ||
    (p.groupCode || '').toLowerCase().includes(q) ||
    (p.treeName || '').toLowerCase().includes(q)
  );
}

function buildPermTree(list: IamPermission[]): DataNode[] {
  if (!list.length) return [];
  const hasParentLink = list.some((p) => p.parentId);
  if (!hasParentLink) {
    const groupMap = new Map<string, IamPermission[]>();
    for (const p of list) {
      const g = p.groupCode || 'default';
      if (!groupMap.has(g)) groupMap.set(g, []);
      groupMap.get(g)!.push(p);
    }
    return [...groupMap.entries()].map(([code, perms]) => ({
      key: `group:${code}`,
      title: <span style={{ fontWeight: 600 }}>{code}</span>,
      children: perms.map((p) => ({
        key: p.id,
        title: (
          <span style={{ display: 'inline-flex', flexDirection: 'column', lineHeight: 1.35 }}>
            <span>{p.name || p.code}</span>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {p.code}
            </Typography.Text>
          </span>
        ),
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
      const isApi = !p.nodeType || p.nodeType === 'API';
      const title = isApi ? (
        <span style={{ display: 'inline-flex', flexDirection: 'column', lineHeight: 1.35 }}>
          <span>{p.name || p.code}</span>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {p.code}
          </Typography.Text>
        </span>
      ) : (
        <span style={{ fontWeight: p.nodeType === 'PRODUCT' || p.nodeType === 'BIZ' ? 600 : 500 }}>
          {p.treeName || p.name || p.code}
        </span>
      );
      return {
        key: p.id,
        title,
        children: kids.length ? kids : undefined,
      };
    });
  return walk('');
}

function disableTreeChecks(nodes: DataNode[]): DataNode[] {
  return nodes.map((n) => ({
    ...n,
    disableCheckbox: true,
    children: n.children ? disableTreeChecks(n.children as DataNode[]) : undefined,
  }));
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
