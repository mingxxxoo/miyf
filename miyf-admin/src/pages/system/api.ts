import { del, get, post, put } from '@/api/http';

export interface SysConfig {
  id: string;
  configKey: string;
  configValue?: string;
  valueType: string;
  groupCode: string;
  name: string;
  description?: string;
  status: string;
  sortOrder?: number;
  updatedAt?: string;
}

export interface SysDictType {
  id: string;
  code: string;
  name: string;
  description?: string;
  status: string;
  sortOrder?: number;
  updatedAt?: string;
}

export interface SysDictItem {
  id: string;
  typeId: string;
  itemValue: string;
  itemLabel: string;
  sortOrder?: number;
  status: string;
  remark?: string;
  updatedAt?: string;
}

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

function mapConfig(raw: Record<string, unknown>): SysConfig {
  return {
    id: sid(raw.id),
    configKey: String(raw.configKey ?? ''),
    configValue: raw.configValue == null ? undefined : String(raw.configValue),
    valueType: String(raw.valueType ?? 'STRING'),
    groupCode: String(raw.groupCode ?? 'default'),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    status: String(raw.status ?? 'ENABLED'),
    sortOrder: Number(raw.sortOrder ?? 0),
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
  };
}

function mapDictType(raw: Record<string, unknown>): SysDictType {
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    status: String(raw.status ?? 'ENABLED'),
    sortOrder: Number(raw.sortOrder ?? 0),
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
  };
}

function mapDictItem(raw: Record<string, unknown>): SysDictItem {
  return {
    id: sid(raw.id),
    typeId: sid(raw.typeId),
    itemValue: String(raw.itemValue ?? ''),
    itemLabel: String(raw.itemLabel ?? ''),
    sortOrder: Number(raw.sortOrder ?? 0),
    status: String(raw.status ?? 'ENABLED'),
    remark: raw.remark ? String(raw.remark) : undefined,
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
  };
}

export const sysConfigApi = {
  list: async (params?: { groupCode?: string; keyword?: string }) => {
    const list = await get<Record<string, unknown>[]>('/admin/system/configs', params);
    return (list ?? []).map(mapConfig);
  },
  create: async (payload: {
    configKey: string;
    configValue?: string;
    valueType?: string;
    groupCode?: string;
    name: string;
    description?: string;
    status?: string;
    sortOrder?: number;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/system/configs', payload);
    return mapConfig(raw);
  },
  update: async (
    id: string,
    payload: {
      configKey: string;
      configValue?: string;
      valueType?: string;
      groupCode?: string;
      name: string;
      description?: string;
      status?: string;
      sortOrder?: number;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/admin/system/configs/${id}`, payload);
    return mapConfig(raw);
  },
  remove: (id: string) => del<void>(`/admin/system/configs/${id}`),
};

export const sysDictApi = {
  listTypes: async (keyword?: string) => {
    const list = await get<Record<string, unknown>[]>('/admin/system/dict-types', {
      keyword: keyword || undefined,
    });
    return (list ?? []).map(mapDictType);
  },
  createType: async (payload: {
    code: string;
    name: string;
    description?: string;
    status?: string;
    sortOrder?: number;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/system/dict-types', payload);
    return mapDictType(raw);
  },
  updateType: async (
    id: string,
    payload: {
      code: string;
      name: string;
      description?: string;
      status?: string;
      sortOrder?: number;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/admin/system/dict-types/${id}`, payload);
    return mapDictType(raw);
  },
  removeType: (id: string) => del<void>(`/admin/system/dict-types/${id}`),
  listItems: async (typeId: string) => {
    const list = await get<Record<string, unknown>[]>(`/admin/system/dict-types/${typeId}/items`);
    return (list ?? []).map(mapDictItem);
  },
  createItem: async (payload: {
    typeId: string;
    itemValue: string;
    itemLabel: string;
    sortOrder?: number;
    status?: string;
    remark?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/system/dict-items', payload);
    return mapDictItem(raw);
  },
  updateItem: async (
    id: string,
    payload: {
      typeId?: string;
      itemValue: string;
      itemLabel: string;
      sortOrder?: number;
      status?: string;
      remark?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/admin/system/dict-items/${id}`, payload);
    return mapDictItem(raw);
  },
  removeItem: (id: string) => del<void>(`/admin/system/dict-items/${id}`),
};

export interface MonitorComponent {
  name: string;
  status: string;
  details?: Record<string, unknown>;
}

export interface MonitorOverview {
  collectedAt?: number;
  jvm?: {
    uptimeMs?: number;
    heapUsed?: number;
    heapMax?: number;
    nonHeapUsed?: number;
    processors?: number;
    systemLoadAverage?: number;
    threadCount?: number;
    daemonThreadCount?: number;
    peakThreadCount?: number;
    javaVersion?: string;
    osName?: string;
    osArch?: string;
    processCpuLoad?: number;
    systemCpuLoad?: number;
  };
  disk?: {
    path?: string;
    total?: number;
    free?: number;
    usable?: number;
  };
  components?: MonitorComponent[];
}

export const sysMonitorApi = {
  overview: async () => {
    const raw = await get<Record<string, unknown>>('/admin/system/monitor/overview');
    const jvm = (raw.jvm as Record<string, unknown>) || {};
    const disk = (raw.disk as Record<string, unknown>) || {};
    const components = ((raw.components as Record<string, unknown>[]) ?? []).map(
      (c): MonitorComponent => ({
        name: String(c.name ?? ''),
        status: String(c.status ?? ''),
        details: (c.details as Record<string, unknown>) || undefined,
      }),
    );
    return {
      collectedAt: raw.collectedAt == null ? undefined : Number(raw.collectedAt),
      jvm: {
        uptimeMs: jvm.uptimeMs == null ? undefined : Number(jvm.uptimeMs),
        heapUsed: jvm.heapUsed == null ? undefined : Number(jvm.heapUsed),
        heapMax: jvm.heapMax == null ? undefined : Number(jvm.heapMax),
        nonHeapUsed: jvm.nonHeapUsed == null ? undefined : Number(jvm.nonHeapUsed),
        processors: jvm.processors == null ? undefined : Number(jvm.processors),
        systemLoadAverage:
          jvm.systemLoadAverage == null ? undefined : Number(jvm.systemLoadAverage),
        threadCount: jvm.threadCount == null ? undefined : Number(jvm.threadCount),
        daemonThreadCount:
          jvm.daemonThreadCount == null ? undefined : Number(jvm.daemonThreadCount),
        peakThreadCount:
          jvm.peakThreadCount == null ? undefined : Number(jvm.peakThreadCount),
        javaVersion: jvm.javaVersion ? String(jvm.javaVersion) : undefined,
        osName: jvm.osName ? String(jvm.osName) : undefined,
        osArch: jvm.osArch ? String(jvm.osArch) : undefined,
        processCpuLoad: jvm.processCpuLoad == null ? undefined : Number(jvm.processCpuLoad),
        systemCpuLoad: jvm.systemCpuLoad == null ? undefined : Number(jvm.systemCpuLoad),
      },
      disk: {
        path: disk.path ? String(disk.path) : undefined,
        total: disk.total == null ? undefined : Number(disk.total),
        free: disk.free == null ? undefined : Number(disk.free),
        usable: disk.usable == null ? undefined : Number(disk.usable),
      },
      components,
    } satisfies MonitorOverview;
  },
};

export interface InboxItem {
  id: string;
  userKey: string;
  title: string;
  content?: string;
  createdAt?: string;
  read: boolean;
}

export interface SysJob {
  code: string;
  name: string;
  description?: string;
  beanName?: string;
  methodName?: string;
  cron?: string;
  enabled: boolean;
  lastStartedAt?: string;
  lastFinishedAt?: string;
  lastStatus?: string;
  lastError?: string;
}

export const sysNotificationApi = {
  send: (payload: { channel: string; to: string; title: string; content: string }) =>
    post<void>('/admin/system/notifications/send', payload),
  listInbox: async (userKey: string, limit?: number) => {
    const list = await get<Record<string, unknown>[]>('/admin/system/notifications/inbox', {
      userKey,
      limit,
    });
    return (list ?? []).map(
      (raw): InboxItem => ({
        id: sid(raw.id),
        userKey: String(raw.userKey ?? ''),
        title: String(raw.title ?? ''),
        content: raw.content == null ? undefined : String(raw.content),
        createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
        read: Boolean(raw.read),
      }),
    );
  },
  markRead: async (id: string, userKey?: string) => {
    const q = userKey ? `?userKey=${encodeURIComponent(userKey)}` : '';
    const raw = await post<Record<string, unknown>>(
      `/admin/system/notifications/inbox/${id}/read${q}`,
    );
    return {
      id: sid(raw.id),
      userKey: String(raw.userKey ?? ''),
      title: String(raw.title ?? ''),
      content: raw.content == null ? undefined : String(raw.content),
      createdAt: raw.createdAt ? String(raw.createdAt) : undefined,
      read: Boolean(raw.read),
    } satisfies InboxItem;
  },
};

export const sysJobApi = {
  list: async () => {
    const list = await get<Record<string, unknown>[]>('/admin/system/jobs');
    return (list ?? []).map(
      (raw): SysJob => ({
        code: String(raw.code ?? ''),
        name: String(raw.name ?? ''),
        description: raw.description ? String(raw.description) : undefined,
        beanName: raw.beanName ? String(raw.beanName) : undefined,
        methodName: raw.methodName ? String(raw.methodName) : undefined,
        cron: raw.cron ? String(raw.cron) : undefined,
        enabled: raw.enabled !== false,
        lastStartedAt: raw.lastStartedAt ? String(raw.lastStartedAt) : undefined,
        lastFinishedAt: raw.lastFinishedAt ? String(raw.lastFinishedAt) : undefined,
        lastStatus: raw.lastStatus ? String(raw.lastStatus) : undefined,
        lastError: raw.lastError ? String(raw.lastError) : undefined,
      }),
    );
  },
  trigger: (code: string) =>
    post<Record<string, unknown>>(`/admin/system/jobs/${encodeURIComponent(code)}/trigger`),
  enable: (code: string) =>
    post<Record<string, unknown>>(`/admin/system/jobs/${encodeURIComponent(code)}/enable`),
  disable: (code: string) =>
    post<Record<string, unknown>>(`/admin/system/jobs/${encodeURIComponent(code)}/disable`),
};
