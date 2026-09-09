import { del, get, post, put } from '@/api/http';

export interface SysConfig {
  id: string;
  configKey: string;
  configValue?: string;
  sensitive?: boolean;
  configured?: boolean;
  valueType: string;
  groupCode: string;
  name: string;
  description?: string;
  status: string;
  sortOrder?: number;
  lastModifyTime?: string;
}

export interface SysApp {
  id: string;
  code: string;
  name: string;
  description?: string;
  icon?: string;
  homePath?: string;
  sortOrder?: number;
  status: string;
  builtIn?: boolean;
  permissionCount?: number;
  menuCount?: number;
  lastModifyTime?: string;
}

export interface SysDictType {
  id: string;
  code: string;
  name: string;
  description?: string;
  status: string;
  sortOrder?: number;
  lastModifyTime?: string;
}

export interface SysDictItem {
  id: string;
  typeId: string;
  itemValue: string;
  itemLabel: string;
  sortOrder?: number;
  status: string;
  remark?: string;
  lastModifyTime?: string;
}

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

function mapConfig(raw: Record<string, unknown>): SysConfig {
  return {
    id: sid(raw.id),
    configKey: String(raw.configKey ?? ''),
    configValue: raw.configValue == null ? undefined : String(raw.configValue),
    sensitive: Boolean(raw.sensitive),
    configured: Boolean(raw.configured),
    valueType: String(raw.valueType ?? 'STRING'),
    groupCode: String(raw.groupCode ?? 'default'),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    status: String(raw.status ?? 'ENABLED'),
    sortOrder: raw.sortOrder == null ? undefined : Number(raw.sortOrder),
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
  };
}

function mapApp(raw: Record<string, unknown>): SysApp {
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    description: raw.description ? String(raw.description) : undefined,
    icon: raw.icon ? String(raw.icon) : undefined,
    homePath: raw.homePath ? String(raw.homePath) : undefined,
    sortOrder: raw.sortOrder == null ? undefined : Number(raw.sortOrder),
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    builtIn: Boolean(raw.builtIn),
    permissionCount: raw.permissionCount == null ? undefined : Number(raw.permissionCount),
    menuCount: raw.menuCount == null ? undefined : Number(raw.menuCount),
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
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
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
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
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
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
    sensitive?: boolean;
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
      sensitive?: boolean;
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

export const sysAppApi = {
  list: async (params?: { keyword?: string; status?: string }) => {
    const list = await get<Record<string, unknown>[]>('/admin/system/apps', params);
    return (list ?? []).map(mapApp);
  },
  create: async (payload: {
    code: string;
    name: string;
    description?: string;
    icon?: string;
    homePath?: string;
    sortOrder?: number;
    status?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/system/apps', payload);
    return mapApp(raw);
  },
  update: async (
    id: string,
    payload: {
      code: string;
      name: string;
      description?: string;
      icon?: string;
      homePath?: string;
      sortOrder?: number;
      status?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/admin/system/apps/${id}`, payload);
    return mapApp(raw);
  },
  remove: (id: string) => del<void>(`/admin/system/apps/${id}`),
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
  collectedTime?: number;
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
  recentErrors?: unknown;
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
      collectedTime: raw.collectedTime == null ? undefined : Number(raw.collectedTime),
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
      recentErrors: raw.recentErrors,
    } satisfies MonitorOverview;
  },
};

export interface InboxItem {
  id: string;
  userKey: string;
  title: string;
  content?: string;
  createTime?: string;
  read: boolean;
}

export interface NotifyTemplate {
  id: string;
  code: string;
  name: string;
  channel: string;
  titleTemplate: string;
  contentTemplate: string;
  status: string;
  createTime?: string;
  lastModifyTime?: string;
}

export interface NotifySendLog {
  id: string;
  templateId?: string;
  channel: string;
  toKey: string;
  title: string;
  content?: string;
  status: string;
  error?: string;
  createTime?: string;
}

export interface SysJob {
  code: string;
  name: string;
  description?: string;
  beanName?: string;
  methodName?: string;
  cron?: string;
  enabled: boolean;
  lastStartedTime?: string;
  lastFinishedTime?: string;
  lastStatus?: string;
  lastError?: string;
  nextRunTime?: string;
}

function mapNotifyTemplate(raw: Record<string, unknown>): NotifyTemplate {
  return {
    id: sid(raw.id),
    code: String(raw.code ?? ''),
    name: String(raw.name ?? ''),
    channel: String(raw.channel ?? 'INBOX').toUpperCase(),
    titleTemplate: String(raw.titleTemplate ?? ''),
    contentTemplate: String(raw.contentTemplate ?? ''),
    status: String(raw.status ?? 'ENABLED').toUpperCase(),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
  };
}

function mapNotifySendLog(raw: Record<string, unknown>): NotifySendLog {
  return {
    id: sid(raw.id),
    templateId: raw.templateId != null ? sid(raw.templateId) : undefined,
    channel: String(raw.channel ?? '').toUpperCase(),
    toKey: String(raw.toKey ?? ''),
    title: String(raw.title ?? ''),
    content: raw.content == null ? undefined : String(raw.content),
    status: String(raw.status ?? '').toUpperCase(),
    error: raw.error == null ? undefined : String(raw.error),
    createTime: raw.createTime ? String(raw.createTime) : undefined,
  };
}

export const sysNotificationApi = {
  send: (payload: {
    channel?: string;
    to: string;
    title?: string;
    content?: string;
    templateCode?: string;
    vars?: Record<string, string>;
  }) => post<void>('/admin/system/notifications/send', payload),
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
        createTime: raw.createTime ? String(raw.createTime) : undefined,
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
      createTime: raw.createTime ? String(raw.createTime) : undefined,
      read: Boolean(raw.read),
    } satisfies InboxItem;
  },
  listTemplates: async (params?: { channel?: string; status?: string }) => {
    const list = await get<Record<string, unknown>[]>('/admin/system/notifications/templates', params);
    return (list ?? []).map(mapNotifyTemplate);
  },
  createTemplate: async (payload: {
    code: string;
    name: string;
    channel: string;
    titleTemplate: string;
    contentTemplate: string;
    status?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/system/notifications/templates', payload);
    return mapNotifyTemplate(raw);
  },
  updateTemplate: async (
    id: string,
    payload: {
      code: string;
      name: string;
      channel: string;
      titleTemplate: string;
      contentTemplate: string;
      status?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(
      `/admin/system/notifications/templates/${id}`,
      payload,
    );
    return mapNotifyTemplate(raw);
  },
  removeTemplate: (id: string) => del<void>(`/admin/system/notifications/templates/${id}`),
  listSendLogs: async (params?: { channel?: string; status?: string; limit?: number }) => {
    const list = await get<Record<string, unknown>[]>(
      '/admin/system/notifications/send-logs',
      params,
    );
    return (list ?? []).map(mapNotifySendLog);
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
        lastStartedTime: raw.lastStartedTime ? String(raw.lastStartedTime) : undefined,
        lastFinishedTime: raw.lastFinishedTime ? String(raw.lastFinishedTime) : undefined,
        lastStatus: raw.lastStatus ? String(raw.lastStatus) : undefined,
        lastError: raw.lastError ? String(raw.lastError) : undefined,
        nextRunTime: raw.nextRunTime ? String(raw.nextRunTime) : undefined,
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
