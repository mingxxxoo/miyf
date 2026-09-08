import { del, get, post, put } from '@/api/http';

export interface MenuTreeNode {
  id: string;
  parentId?: string | null;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  menuType?: string;
  permissionCode?: string;
  sortOrder?: number;
  children?: MenuTreeNode[];
}

export interface IamUser {
  id: string;
  orgUnitId?: string;
  username: string;
  nickname?: string;
  status: string;
  roleIds?: string[];
  createTime?: string;
}

export interface IamRole {
  id: string;
  code: string;
  name: string;
  description?: string;
  isDefault?: boolean;
  product?: string;
  dataScope?: string;
  userCount?: number;
  groupIds?: string[];
  createTime?: string;
}

export interface IamRoleUser {
  id: string;
  username: string;
  nickname?: string;
  status: string;
}

export interface RoleAuthSummary {
  roleCount: number;
  userCount: number;
}

export interface IamPermission {
  id: string;
  code: string;
  name: string;
  description?: string;
  groupCode?: string;
  permNo?: string;
  parentId?: string;
  product?: string;
  treeName?: string;
  nodeType?: string;
  sortOrder?: number;
  children?: IamPermission[];
}

export interface IamOrgUnit {
  id: string;
  parentId?: string;
  code: string;
  name: string;
  sortOrder?: number;
  status: string;
  createTime?: string;
  children?: IamOrgUnit[];
}

export interface IamMenu {
  id: string;
  parentId?: string;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  menuType: string;
  permissionCode?: string;
  product?: string;
  sortOrder?: number;
  visible?: boolean;
  status: string;
  children?: IamMenu[];
}

export interface IamPermGroup {
  id: string;
  code: string;
  name: string;
  description?: string;
  sortOrder?: number;
  product?: string;
  permissionIds?: string[];
  createTime?: string;
}

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

function mapUser(raw: Record<string, unknown>): IamUser {
  const roleIdsRaw = Array.isArray(raw.roleIds) ? raw.roleIds : [];
  return {
    id: sid(raw.id),
    orgUnitId: raw.orgUnitId != null ? sid(raw.orgUnitId) : undefined,
    username: String(raw.username ?? ''),
    nickname: raw.nickname ? String(raw.nickname) : undefined,
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    roleIds: roleIdsRaw.map((g) => sid(g)),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
  };
}

function mapRole(raw: Record<string, unknown>): IamRole {
  const groupIdsRaw = Array.isArray(raw.groupIds) ? raw.groupIds : [];
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    isDefault: Boolean(raw.isDefault),
    product: raw.product ? String(raw.product) : 'system',
    dataScope: raw.dataScope ? String(raw.dataScope).toUpperCase() : 'ALL',
    userCount: raw.userCount != null ? Number(raw.userCount) : 0,
    groupIds: groupIdsRaw.map((g) => sid(g)),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
  };
}

function mapRoleUser(raw: Record<string, unknown>): IamRoleUser {
  return {
    id: sid(raw.id),
    username: String(raw.username ?? ''),
    nickname: raw.nickname ? String(raw.nickname) : undefined,
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
  };
}

function mapPermission(raw: Record<string, unknown>): IamPermission {
  const childrenRaw = Array.isArray(raw.children) ? raw.children : [];
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    groupCode: raw.groupCode ? String(raw.groupCode) : undefined,
    permNo: raw.permNo ? String(raw.permNo) : undefined,
    parentId: raw.parentId != null ? sid(raw.parentId) : undefined,
    product: raw.product ? String(raw.product) : undefined,
    treeName: raw.treeName ? String(raw.treeName) : undefined,
    nodeType: raw.nodeType ? String(raw.nodeType) : undefined,
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    children: childrenRaw.map((c) => mapPermission(c as Record<string, unknown>)),
  };
}

function mapNode(raw: Record<string, unknown>): MenuTreeNode {
  const childrenRaw = Array.isArray(raw.children) ? raw.children : [];
  return {
    id: String(raw.id ?? ''),
    parentId: raw.parentId == null ? null : String(raw.parentId),
    name: String(raw.name ?? ''),
    path: raw.path ? String(raw.path) : undefined,
    component: raw.component ? String(raw.component) : undefined,
    icon: raw.icon ? String(raw.icon) : undefined,
    menuType: raw.menuType ? String(raw.menuType) : undefined,
    permissionCode: raw.permissionCode ? String(raw.permissionCode) : undefined,
    sortOrder: Number(raw.sortOrder ?? 0),
    children: childrenRaw.map((c) => mapNode(c as Record<string, unknown>)),
  };
}

/** 当前用户菜单树 */
export async function fetchMyMenus(): Promise<MenuTreeNode[]> {
  const data = await get<unknown[]>('/iam/me/menus');
  if (!Array.isArray(data)) return [];
  return data.map((item) => mapNode(item as Record<string, unknown>));
}

/** IAM 系统用户 */
export const iamUserApi = {
  list: async (): Promise<IamUser[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/users');
    return (list || []).map(mapUser);
  },
  create: async (data: {
    username: string;
    password: string;
    nickname?: string;
    status?: string;
    orgUnitId?: string;
    roleIds?: string[];
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/users', {
      username: data.username,
      password: data.password,
      nickname: data.nickname,
      status: data.status ?? 'ENABLED',
      orgUnitId: data.orgUnitId || undefined,
      roleIds: data.roleIds,
    });
    return mapUser(raw);
  },
  update: async (
    id: string,
    data: {
      username: string;
      password?: string;
      nickname?: string;
      status?: string;
      orgUnitId?: string;
      roleIds?: string[];
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/users/${id}`, {
      username: data.username,
      password: data.password || undefined,
      nickname: data.nickname,
      status: data.status,
      orgUnitId: data.orgUnitId || undefined,
      roleIds: data.roleIds,
    });
    return mapUser(raw);
  },
  remove: (id: string) => del<void>(`/iam/users/${id}`),
};

/** IAM 角色 */
export const iamRoleApi = {
  list: async (product?: string): Promise<IamRole[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/roles', product ? { product } : undefined);
    return (list || []).map(mapRole);
  },
  authSummary: async (product?: string): Promise<RoleAuthSummary> => {
    const raw = await get<Record<string, unknown>>('/iam/roles/auth-summary', product ? { product } : undefined);
    return {
      roleCount: Number(raw?.roleCount ?? 0),
      userCount: Number(raw?.userCount ?? 0),
    };
  },
  create: async (data: {
    code: string;
    name: string;
    description?: string;
    product?: string;
    dataScope?: string;
    groupIds?: string[];
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/roles', {
      code: data.code,
      name: data.name,
      description: data.description,
      product: data.product,
      dataScope: data.dataScope,
      groupIds: data.groupIds,
    });
    return mapRole(raw);
  },
  update: async (
    id: string,
    data: {
      code: string;
      name: string;
      description?: string;
      product?: string;
      dataScope?: string;
      groupIds?: string[];
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/roles/${id}`, {
      code: data.code,
      name: data.name,
      description: data.description,
      product: data.product,
      dataScope: data.dataScope,
      groupIds: data.groupIds,
    });
    return mapRole(raw);
  },
  setDefault: async (id: string, isDefault: boolean) => {
    const raw = await put<Record<string, unknown>>(`/iam/roles/${id}/default`, { isDefault });
    return mapRole(raw);
  },
  remove: (id: string) => del<void>(`/iam/roles/${id}`),
  listUsers: async (id: string): Promise<IamRoleUser[]> => {
    const list = await get<Record<string, unknown>[]>(`/iam/roles/${id}/users`);
    return (list || []).map(mapRoleUser);
  },
  addUsers: (id: string, userIds: string[]) => post<void>(`/iam/roles/${id}/users`, { userIds }),
  removeUser: (id: string, userId: string) => del<void>(`/iam/roles/${id}/users/${userId}`),
  exportUsers: async (id: string) => {
    const token =
      localStorage.getItem('miyf_admin_token') || localStorage.getItem('ck_admin_token');
    const res = await fetch(`/api/iam/roles/${id}/users/export`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) throw new Error('导出失败');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `role-${id}-users.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

/** IAM 权限（只读，服务端 TreeUtils 组无限极树） */
export const iamPermissionApi = {
  /** 权限树根列表 */
  listTree: async (): Promise<IamPermission[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/permissions');
    return (list || []).map(mapPermission);
  },
  /** 平铺列表（由树展平，供筛选/勾选） */
  list: async (): Promise<IamPermission[]> => flattenTree(await iamPermissionApi.listTree()),
};

function mapOrgUnit(raw: Record<string, unknown>): IamOrgUnit {
  const childrenRaw = Array.isArray(raw.children) ? raw.children : [];
  return {
    id: sid(raw.id),
    parentId: raw.parentId != null ? sid(raw.parentId) : undefined,
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
    children: childrenRaw.map((c) => mapOrgUnit(c as Record<string, unknown>)),
  };
}

function mapMenu(raw: Record<string, unknown>): IamMenu {
  const childrenRaw = Array.isArray(raw.children) ? raw.children : [];
  return {
    id: sid(raw.id),
    parentId: raw.parentId != null ? sid(raw.parentId) : undefined,
    name: String(raw.name ?? ''),
    path: raw.path ? String(raw.path) : undefined,
    component: raw.component ? String(raw.component) : undefined,
    icon: raw.icon ? String(raw.icon) : undefined,
    menuType: String(raw.menuType ?? 'MENU').toUpperCase(),
    permissionCode: raw.permissionCode ? String(raw.permissionCode) : undefined,
    product: raw.product ? String(raw.product) : 'system',
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    visible: raw.visible == null ? true : Boolean(raw.visible),
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    children: childrenRaw.map((c) => mapMenu(c as Record<string, unknown>)),
  };
}

function mapPermGroup(raw: Record<string, unknown>): IamPermGroup {
  const permissionIdsRaw = Array.isArray(raw.permissionIds) ? raw.permissionIds : [];
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    product: raw.product ? String(raw.product) : 'system',
    permissionIds: permissionIdsRaw.map((g) => sid(g)),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
  };
}

/** 树前序展平（对齐服务端 TreeUtils.flatten） */
export function flattenTree<T extends { children?: T[] }>(roots: T[]): T[] {
  const out: T[] = [];
  const walk = (nodes: T[]) => {
    for (const n of nodes) {
      out.push(n);
      if (n.children?.length) walk(n.children);
    }
  };
  walk(roots || []);
  return out;
}

/**
 * 将扁平菜单列表转为树（仅用于客户端筛选后再组树；列表数据请优先用服务端树）。
 * @deprecated 优先使用 iamMenuApi.listTree()
 */
export function buildMenuTree(list: IamMenu[]): IamMenu[] {
  const nodes = list.map((m) => ({ ...m, children: [] as IamMenu[] }));
  const map = new Map(nodes.map((n) => [n.id, n]));
  const roots: IamMenu[] = [];
  for (const n of nodes) {
    if (n.parentId && map.has(n.parentId)) {
      map.get(n.parentId)!.children!.push(n);
    } else {
      roots.push(n);
    }
  }
  const sortRec = (arr: IamMenu[]) => {
    arr.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
    arr.forEach((c) => c.children && sortRec(c.children));
  };
  sortRec(roots);
  return roots;
}

/** IAM 组织单位（列表为无限极树） */
export const iamOrgUnitApi = {
  listTree: async (): Promise<IamOrgUnit[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/org-units');
    return (list || []).map(mapOrgUnit);
  },
  list: async (): Promise<IamOrgUnit[]> => flattenTree(await iamOrgUnitApi.listTree()),
  create: async (data: {
    code: string;
    name: string;
    parentId?: string;
    sortOrder?: number;
    status?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/org-units', {
      code: data.code,
      name: data.name,
      parentId: data.parentId || undefined,
      sortOrder: data.sortOrder ?? 0,
      status: data.status ?? 'ENABLED',
    });
    return mapOrgUnit(raw);
  },
  update: async (
    id: string,
    data: {
      code: string;
      name: string;
      parentId?: string;
      sortOrder?: number;
      status?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/org-units/${id}`, {
      code: data.code,
      name: data.name,
      parentId: data.parentId || undefined,
      sortOrder: data.sortOrder ?? 0,
      status: data.status ?? 'ENABLED',
    });
    return mapOrgUnit(raw);
  },
  remove: (id: string) => del<void>(`/iam/org-units/${id}`),
};

/** IAM 菜单（列表为无限极树） */
export const iamMenuApi = {
  listTree: async (): Promise<IamMenu[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/menus');
    return (list || []).map(mapMenu);
  },
  list: async (): Promise<IamMenu[]> => flattenTree(await iamMenuApi.listTree()),
  create: async (data: {
    name: string;
    menuType: string;
    parentId?: string;
    path?: string;
    component?: string;
    icon?: string;
    permissionCode?: string;
    product?: string;
    sortOrder?: number;
    visible?: boolean;
    status?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/menus', {
      name: data.name,
      menuType: data.menuType,
      parentId: data.parentId || undefined,
      path: data.path,
      component: data.component,
      icon: data.icon,
      permissionCode: data.permissionCode,
      product: data.product,
      sortOrder: data.sortOrder ?? 0,
      visible: data.visible ?? true,
      status: data.status ?? 'ENABLED',
    });
    return mapMenu(raw);
  },
  update: async (
    id: string,
    data: {
      name: string;
      menuType: string;
      parentId?: string;
      path?: string;
      component?: string;
      icon?: string;
      permissionCode?: string;
      product?: string;
      sortOrder?: number;
      visible?: boolean;
      status?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/menus/${id}`, {
      name: data.name,
      menuType: data.menuType,
      parentId: data.parentId || undefined,
      path: data.path,
      component: data.component,
      icon: data.icon,
      permissionCode: data.permissionCode,
      product: data.product,
      sortOrder: data.sortOrder ?? 0,
      visible: data.visible ?? true,
      status: data.status ?? 'ENABLED',
    });
    return mapMenu(raw);
  },
  remove: (id: string) => del<void>(`/iam/menus/${id}`),
};

/** IAM 权限组 */
export const iamPermGroupApi = {
  list: async (): Promise<IamPermGroup[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/perm-groups');
    return (list || []).map(mapPermGroup);
  },
  create: async (data: {
    code: string;
    name: string;
    description?: string;
    sortOrder?: number;
    product?: string;
    permissionIds?: string[];
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/perm-groups', {
      code: data.code,
      name: data.name,
      description: data.description,
      sortOrder: data.sortOrder ?? 0,
      product: data.product,
      permissionIds: data.permissionIds,
    });
    return mapPermGroup(raw);
  },
  update: async (
    id: string,
    data: {
      code: string;
      name: string;
      description?: string;
      sortOrder?: number;
      product?: string;
      permissionIds?: string[];
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/perm-groups/${id}`, {
      code: data.code,
      name: data.name,
      description: data.description,
      sortOrder: data.sortOrder ?? 0,
      product: data.product,
      permissionIds: data.permissionIds,
    });
    return mapPermGroup(raw);
  },
  remove: (id: string) => del<void>(`/iam/perm-groups/${id}`),
};
