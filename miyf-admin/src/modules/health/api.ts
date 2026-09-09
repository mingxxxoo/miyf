import { get, post, put, del } from '@/api/http';

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

export interface HealthOverview {
  module: string;
  enabled: boolean;
  subjectCount: number;
  sampleCount: number;
  providers: HealthProvider[];
}

export interface HealthProvider {
  code: string;
  displayName: string;
  enabled: boolean;
  supportsRemoteFetch: boolean;
  supportedMetrics?: string[];
}

export interface HealthSubject {
  id: string;
  displayName: string;
  gender?: string;
  birthDate?: string;
  heightCm?: number;
  externalUserId?: string;
  status: string;
  remark?: string;
  lastModifyTime?: string;
}

export interface HealthSample {
  id: string;
  subjectId: string;
  metricCode: string;
  valueNum?: number;
  unit?: string;
  measuredTime?: string;
  providerCode?: string;
  quality?: string;
}

function mapSubject(raw: Record<string, unknown>): HealthSubject {
  return {
    id: sid(raw.id),
    displayName: String(raw.displayName ?? ''),
    gender: raw.gender ? String(raw.gender) : undefined,
    birthDate: raw.birthDate ? String(raw.birthDate) : undefined,
    heightCm: raw.heightCm == null ? undefined : Number(raw.heightCm),
    externalUserId: raw.externalUserId == null ? undefined : sid(raw.externalUserId),
    status: String(raw.status ?? 'ENABLED'),
    remark: raw.remark ? String(raw.remark) : undefined,
    lastModifyTime: raw.lastModifyTime ? String(raw.lastModifyTime) : undefined,
  };
}

function mapSample(raw: Record<string, unknown>): HealthSample {
  return {
    id: sid(raw.id),
    subjectId: sid(raw.subjectId),
    metricCode: String(raw.metricCode ?? ''),
    valueNum: raw.valueNum == null ? undefined : Number(raw.valueNum),
    unit: raw.unit ? String(raw.unit) : undefined,
    measuredTime: raw.measuredTime ? String(raw.measuredTime) : undefined,
    providerCode: raw.providerCode ? String(raw.providerCode) : undefined,
    quality: raw.quality ? String(raw.quality) : undefined,
  };
}

export const healthApi = {
  overview: async () => {
    const raw = await get<Record<string, unknown>>('/admin/health/overview');
    const providers = ((raw.providers as Record<string, unknown>[]) ?? []).map(
      (p): HealthProvider => ({
        code: String(p.code ?? ''),
        displayName: String(p.displayName ?? ''),
        enabled: Boolean(p.enabled),
        supportsRemoteFetch: Boolean(p.supportsRemoteFetch),
        supportedMetrics: Array.isArray(p.supportedMetrics)
          ? (p.supportedMetrics as unknown[]).map(String)
          : undefined,
      }),
    );
    return {
      module: String(raw.module ?? 'health'),
      enabled: Boolean(raw.enabled),
      subjectCount: Number(raw.subjectCount ?? 0),
      sampleCount: Number(raw.sampleCount ?? 0),
      providers,
    } satisfies HealthOverview;
  },
  listSubjects: async (keyword?: string) => {
    const list = await get<Record<string, unknown>[]>('/admin/health/subjects', {
      keyword: keyword || undefined,
    });
    return (list ?? []).map(mapSubject);
  },
  createSubject: async (payload: {
    displayName: string;
    gender?: string;
    birthDate?: string;
    heightCm?: number;
    status?: string;
    remark?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/health/subjects', payload);
    return mapSubject(raw);
  },
  updateSubject: async (
    id: string,
    payload: {
      displayName: string;
      gender?: string;
      birthDate?: string;
      heightCm?: number;
      status?: string;
      remark?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(`/admin/health/subjects/${id}`, payload);
    return mapSubject(raw);
  },
  removeSubject: (id: string) => del<void>(`/admin/health/subjects/${id}`),
  listSamples: async (params?: { subjectId?: string; metricCode?: string; limit?: number }) => {
    const list = await get<Record<string, unknown>[]>('/admin/health/samples', params);
    return (list ?? []).map(mapSample);
  },
  createSample: async (payload: {
    subjectId: string;
    metricCode: string;
    valueNum: number;
    unit?: string;
    measuredTime: string;
    quality?: string;
  }) => {
    const raw = await post<Record<string, unknown>>('/admin/health/samples', payload);
    return mapSample(raw);
  },
  removeSample: (id: string) => del<void>(`/admin/health/samples/${id}`),
  listProviders: async () => {
    const list = await get<Record<string, unknown>[]>('/admin/health/providers');
    return (list ?? []).map(
      (p): HealthProvider => ({
        code: String(p.code ?? ''),
        displayName: String(p.displayName ?? ''),
        enabled: Boolean(p.enabled),
        supportsRemoteFetch: Boolean(p.supportsRemoteFetch),
        supportedMetrics: Array.isArray(p.supportedMetrics)
          ? (p.supportedMetrics as unknown[]).map(String)
          : undefined,
      }),
    );
  },
  listBindings: async (subjectId: string) => {
    const list = await get<Record<string, unknown>[]>(
      `/admin/health/subjects/${subjectId}/bindings`,
    );
    return (list ?? []).map(
      (raw): HealthBinding => ({
        id: sid(raw.id),
        subjectId: sid(raw.subjectId),
        providerCode: String(raw.providerCode ?? ''),
        providerDisplayName: raw.providerDisplayName
          ? String(raw.providerDisplayName)
          : undefined,
        externalAccountMasked: raw.externalAccountMasked
          ? String(raw.externalAccountMasked)
          : undefined,
        hasCredential: Boolean(raw.hasCredential),
        status: String(raw.status ?? 'ACTIVE'),
        lastSyncTime: raw.lastSyncTime ? String(raw.lastSyncTime) : undefined,
      }),
    );
  },
  upsertBinding: async (
    subjectId: string,
    payload: {
      providerCode: string;
      externalAccountId?: string;
      credentialRef?: string;
      status?: string;
    },
  ) => {
    const raw = await put<Record<string, unknown>>(
      `/admin/health/subjects/${subjectId}/bindings`,
      payload,
    );
    return {
      id: sid(raw.id),
      subjectId: sid(raw.subjectId),
      providerCode: String(raw.providerCode ?? ''),
      providerDisplayName: raw.providerDisplayName
        ? String(raw.providerDisplayName)
        : undefined,
      externalAccountMasked: raw.externalAccountMasked
        ? String(raw.externalAccountMasked)
        : undefined,
      hasCredential: Boolean(raw.hasCredential),
      status: String(raw.status ?? 'ACTIVE'),
      lastSyncTime: raw.lastSyncTime ? String(raw.lastSyncTime) : undefined,
    } satisfies HealthBinding;
  },
  sync: async (
    providerCode: string,
    payload: { subjectId: string; from?: string; to?: string },
  ) => {
    const raw = await post<Record<string, unknown>>(
      `/admin/health/providers/${encodeURIComponent(providerCode)}/sync`,
      payload,
    );
    return mapSyncRun(raw);
  },
  listSyncRuns: async (params?: {
    providerCode?: string;
    subjectId?: string;
    limit?: number;
  }) => {
    const list = await get<Record<string, unknown>[]>('/admin/health/sync-runs', params);
    return (list ?? []).map(mapSyncRun);
  },
  huaweiAuthorizeUrl: async (subjectId: string) => {
    const raw = await get<Record<string, unknown>>('/admin/health/providers/huawei/authorize-url', {
      subjectId,
    });
    return {
      authorizeUrl: String(raw.authorizeUrl ?? ''),
      subjectId: sid(raw.subjectId),
    };
  },
  huaweiOAuthCallback: async (payload: { subjectId?: string; code: string; state?: string }) => {
    const raw = await post<Record<string, unknown>>(
      '/admin/health/providers/huawei/oauth/callback',
      payload,
    );
    return {
      subjectId: sid(raw.subjectId),
      authorized: Boolean(raw.authorized),
      openId: raw.openId ? String(raw.openId) : undefined,
      expiresTime: raw.expiresTime ? String(raw.expiresTime) : undefined,
    };
  },
  huaweiOAuthStatus: async (subjectId: string) => {
    const raw = await get<Record<string, unknown>>('/admin/health/providers/huawei/oauth/status', {
      subjectId,
    });
    return {
      subjectId: sid(raw.subjectId),
      authorized: Boolean(raw.authorized),
      source: raw.source ? String(raw.source) : undefined,
      openId: raw.openId ? String(raw.openId) : undefined,
      hasRefreshToken: Boolean(raw.hasRefreshToken),
      expiresTime: raw.expiresTime ? String(raw.expiresTime) : undefined,
    };
  },
  huaweiRevoke: (subjectId: string) =>
    del<void>('/admin/health/providers/huawei/oauth', { subjectId }),
  trend: async (params: {
    subjectId: string;
    metricCode: string;
    from?: string;
    to?: string;
    limit?: number;
  }) => {
    const raw = await get<Record<string, unknown>>('/admin/health/trends', params);
    const points = ((raw.points as Record<string, unknown>[]) ?? []).map(
      (p): HealthTrendPoint => ({
        measuredTime: p.measuredTime ? String(p.measuredTime) : undefined,
        value: p.value == null ? undefined : Number(p.value),
        providerCode: p.providerCode ? String(p.providerCode) : undefined,
        quality: p.quality ? String(p.quality) : undefined,
      }),
    );
    return {
      subjectId: sid(raw.subjectId),
      metricCode: String(raw.metricCode ?? ''),
      unit: raw.unit ? String(raw.unit) : undefined,
      min: raw.min == null ? undefined : Number(raw.min),
      max: raw.max == null ? undefined : Number(raw.max),
      avg: raw.avg == null ? undefined : Number(raw.avg),
      latest: raw.latest == null ? undefined : Number(raw.latest),
      latestTime: raw.latestTime ? String(raw.latestTime) : undefined,
      pointCount: Number(raw.pointCount ?? points.length),
      points,
    } satisfies HealthTrend;
  },
};

export interface HealthTrendPoint {
  measuredTime?: string;
  value?: number;
  providerCode?: string;
  quality?: string;
}

export interface HealthTrend {
  subjectId: string;
  metricCode: string;
  unit?: string;
  min?: number;
  max?: number;
  avg?: number;
  latest?: number;
  latestTime?: string;
  pointCount: number;
  points: HealthTrendPoint[];
}

export interface HealthBinding {
  id: string;
  subjectId: string;
  providerCode: string;
  providerDisplayName?: string;
  externalAccountMasked?: string;
  hasCredential?: boolean;
  status: string;
  lastSyncTime?: string;
}

export interface HealthSyncRun {
  id: string;
  providerCode: string;
  subjectId?: string;
  status: string;
  fetchedCount?: number;
  ingestedCount?: number;
  errorMessage?: string;
  startedTime?: string;
  finishedTime?: string;
}

function mapSyncRun(raw: Record<string, unknown>): HealthSyncRun {
  return {
    id: sid(raw.id),
    providerCode: String(raw.providerCode ?? ''),
    subjectId: raw.subjectId == null ? undefined : sid(raw.subjectId),
    status: String(raw.status ?? ''),
    fetchedCount: raw.fetchedCount == null ? undefined : Number(raw.fetchedCount),
    ingestedCount: raw.ingestedCount == null ? undefined : Number(raw.ingestedCount),
    errorMessage: raw.errorMessage ? String(raw.errorMessage) : undefined,
    startedTime: raw.startedTime ? String(raw.startedTime) : undefined,
    finishedTime: raw.finishedTime ? String(raw.finishedTime) : undefined,
  };
}
