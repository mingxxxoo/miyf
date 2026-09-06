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
  createdAt?: string;
}

export interface IamRole {
  id: string;
  code: string;
  name: string;
  description?: string;
  createdAt?: string;
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
}

export interface IamOrgUnit {
  id: string;
  parentId?: string;
  code: string;
  name: string;
  sortOrder?: number;
  status: string;
  createdAt?: string;
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
  createdAt?: string;
}

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

function mapUser(raw: Record<string, unknown>): IamUser {
  return {
    id: sid(raw.id),
    orgUnitId: raw.orgUnitId != null ? sid(raw.orgUnitId) : undefined,
    username: String(raw.username ?? ''),
    nickname: raw.nickname ? String(raw.nickname) : undefined,
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
  };
}

function mapRole(raw: Record<string, unknown>): IamRole {
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
  };
}

function mapPermission(raw: Record<string, unknown>): IamPermission {
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
  list: async (): Promise<IamRole[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/roles');
    return (list || []).map(mapRole);
  },
  create: async (data: { code: string; name: string; description?: string; groupIds?: string[] }) => {
    const raw = await post<Record<string, unknown>>('/iam/roles', {
      code: data.code,
      name: data.name,
      description: data.description,
      groupIds: data.groupIds,
    });
    return mapRole(raw);
  },
  update: async (
    id: string,
    data: { code: string; name: string; description?: string; groupIds?: string[] },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/roles/${id}`, {
      code: data.code,
      name: data.name,
      description: data.description,
      groupIds: data.groupIds,
    });
    return mapRole(raw);
  },
  remove: (id: string) => del<void>(`/iam/roles/${id}`),
};

/** IAM 权限（只读，启动扫描写入） */
export const iamPermissionApi = {
  list: async (): Promise<IamPermission[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/permissions');
    return (list || []).map(mapPermission);
  },
};

function mapOrgUnit(raw: Record<string, unknown>): IamOrgUnit {
  return {
    id: sid(raw.id),
    parentId: raw.parentId != null ? sid(raw.parentId) : undefined,
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
  };
}

function mapMenu(raw: Record<string, unknown>): IamMenu {
  return {
    id: sid(raw.id),
    parentId: raw.parentId != null ? sid(raw.parentId) : undefined,
    name: String(raw.name ?? ''),
    path: raw.path ? String(raw.path) : undefined,
    component: raw.component ? String(raw.component) : undefined,
    icon: raw.icon ? String(raw.icon) : undefined,
    menuType: String(raw.menuType ?? 'MENU').toUpperCase(),
    permissionCode: raw.permissionCode ? String(raw.permissionCode) : undefined,
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    visible: raw.visible == null ? true : Boolean(raw.visible),
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
  };
}

function mapPermGroup(raw: Record<string, unknown>): IamPermGroup {
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
  };
}

/** 将扁平菜单列表转为树 */
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

/** IAM 组织单位 */
export const iamOrgUnitApi = {
  list: async (): Promise<IamOrgUnit[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/org-units');
    return (list || []).map(mapOrgUnit);
  },
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

/** IAM 菜单 */
export const iamMenuApi = {
  list: async (): Promise<IamMenu[]> => {
    const list = await get<Record<string, unknown>[]>('/iam/menus');
    return (list || []).map(mapMenu);
  },
  create: async (data: {
    name: string;
    menuType: string;
    parentId?: string;
    path?: string;
    component?: string;
    icon?: string;
    permissionCode?: string;
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
    permissionIds?: string[];
  }) => {
    const raw = await post<Record<string, unknown>>('/iam/perm-groups', {
      code: data.code,
      name: data.name,
      description: data.description,
      sortOrder: data.sortOrder ?? 0,
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
      permissionIds?: string[];
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/iam/perm-groups/${id}`, {
      code: data.code,
      name: data.name,
      description: data.description,
      sortOrder: data.sortOrder ?? 0,
      permissionIds: data.permissionIds,
    });
    return mapPermGroup(raw);
  },
  remove: (id: string) => del<void>(`/iam/perm-groups/${id}`),
};
