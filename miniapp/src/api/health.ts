import { del, get, post, put } from '@/api/request'
import { asId, asOptionalId } from '@/utils/id'

export interface HealthSubject {
  id: string
  displayName: string
  gender?: string
  birthDate?: string
  heightCm?: number
  status?: string
  remark?: string
}

export interface HealthSample {
  id: string
  subjectId: string
  metricCode: string
  valueNum: number
  unit?: string
  measuredTime: string
  providerCode?: string
  quality?: string
}

export interface HealthTrendPoint {
  measuredTime: string
  value?: number
  providerCode?: string
  quality?: string
}

export interface HealthTrend {
  subjectId: string
  metricCode: string
  unit?: string
  pointCount: number
  min?: number
  max?: number
  avg?: number
  latest?: number
  latestTime?: string
  points: HealthTrendPoint[]
}

interface SubjectRaw {
  id: string
  displayName?: string
  gender?: string
  birthDate?: string
  heightCm?: number
  status?: string
  remark?: string
}

interface SampleRaw {
  id: string
  subjectId: string
  metricCode: string
  valueNum: number
  unit?: string
  measuredTime: string
  providerCode?: string
  quality?: string
}

interface TrendRaw {
  subjectId: string
  metricCode: string
  unit?: string
  pointCount?: number
  min?: number
  max?: number
  avg?: number
  latest?: number
  latestTime?: string
  points?: HealthTrendPoint[]
}

function mapSubject(raw: SubjectRaw): HealthSubject {
  return {
    id: asId(raw.id),
    displayName: raw.displayName || '我',
    gender: raw.gender,
    birthDate: raw.birthDate,
    heightCm: raw.heightCm,
    status: raw.status,
    remark: raw.remark
  }
}

function mapSample(raw: SampleRaw): HealthSample {
  return {
    id: asId(raw.id),
    subjectId: asId(raw.subjectId),
    metricCode: raw.metricCode,
    valueNum: Number(raw.valueNum),
    unit: raw.unit,
    measuredTime: raw.measuredTime,
    providerCode: raw.providerCode,
    quality: raw.quality
  }
}

export async function fetchMyHealth(): Promise<HealthSubject> {
  const raw = await get<SubjectRaw>('/api/health/me')
  return mapSubject(raw)
}

export async function updateMyHealth(payload: {
  displayName: string
  gender?: string
  birthDate?: string
  heightCm?: number
  remark?: string
}): Promise<HealthSubject> {
  const raw = await put<SubjectRaw>('/api/health/me', payload, { showLoading: true })
  return mapSubject(raw)
}

export async function fetchMySamples(params?: {
  metricCode?: string
  limit?: number
}): Promise<HealthSample[]> {
  const list = await get<SampleRaw[]>('/api/health/me/samples', params as Record<string, unknown>)
  return (list || []).map(mapSample)
}

export async function createMySample(payload: {
  metricCode: string
  valueNum: number
  unit?: string
  measuredTime: string
}): Promise<HealthSample> {
  const raw = await post<SampleRaw>('/api/health/me/samples', payload, { showLoading: true })
  return mapSample(raw)
}

export async function removeMySample(id: string): Promise<void> {
  await del<void>(`/api/health/me/samples/${asId(id)}`, undefined, { showLoading: true })
}

export async function fetchMyTrend(metricCode: string, limit = 30): Promise<HealthTrend> {
  const raw = await get<TrendRaw>('/api/health/me/trends', { metricCode, limit })
  return {
    subjectId: asId(raw.subjectId),
    metricCode: raw.metricCode,
    unit: raw.unit,
    pointCount: raw.pointCount || 0,
    min: raw.min,
    max: raw.max,
    avg: raw.avg,
    latest: raw.latest,
    latestTime: raw.latestTime,
    points: raw.points || []
  }
}

export interface HuaweiOAuthStatus {
  subjectId: string
  providerCode: string
  authorized: boolean
  source?: string
  hasRefreshToken?: boolean
  openId?: string
  expiresTime?: string
}

export interface HuaweiAuthorizeResult {
  authorizeUrl: string
  subjectId: string
  providerCode: string
}

export interface HealthSyncRun {
  id: string
  providerCode: string
  subjectId?: string
  status: string
  fetchedCount?: number
  ingestedCount?: number
  errorMessage?: string
}

/** 查询「我的」华为授权状态（轮询时建议 showError: false） */
export async function fetchHuaweiOAuthStatus(options?: {
  showError?: boolean
}): Promise<HuaweiOAuthStatus> {
  const raw = await get<Record<string, unknown>>(
    '/api/health/providers/huawei/oauth/status',
    undefined,
    { showError: options?.showError !== false }
  )
  return {
    subjectId: asId(String(raw.subjectId ?? '')),
    providerCode: String(raw.providerCode ?? 'huawei'),
    authorized: Boolean(raw.authorized),
    source: raw.source ? String(raw.source) : undefined,
    hasRefreshToken: raw.hasRefreshToken == null ? undefined : Boolean(raw.hasRefreshToken),
    openId: raw.openId ? String(raw.openId) : undefined,
    expiresTime: raw.expiresTime ? String(raw.expiresTime) : undefined
  }
}

/** 获取个人端华为 OAuth 授权 URL */
export async function fetchHuaweiAuthorizeUrl(): Promise<HuaweiAuthorizeResult> {
  const raw = await get<Record<string, unknown>>(
    '/api/health/providers/huawei/authorize-url',
    undefined,
    { showLoading: true }
  )
  return {
    authorizeUrl: String(raw.authorizeUrl ?? ''),
    subjectId: asId(String(raw.subjectId ?? '')),
    providerCode: String(raw.providerCode ?? 'huawei')
  }
}

/** 撤销「我的」华为授权 */
export async function revokeHuaweiOAuth(): Promise<void> {
  await del<void>('/api/health/providers/huawei/oauth', undefined, { showLoading: true })
}

/** 同步「我的」指定数据源（如 huawei） */
export async function syncMyProvider(providerCode: string): Promise<HealthSyncRun> {
  const raw = await post<Record<string, unknown>>(
    `/api/health/providers/${encodeURIComponent(providerCode)}/sync`,
    {},
    { showLoading: true }
  )
  return {
    id: asId(String(raw.id ?? '')),
    providerCode: String(raw.providerCode ?? providerCode),
    subjectId: raw.subjectId == null ? undefined : asId(String(raw.subjectId)),
    status: String(raw.status ?? ''),
    fetchedCount: raw.fetchedCount == null ? undefined : Number(raw.fetchedCount),
    ingestedCount: raw.ingestedCount == null ? undefined : Number(raw.ingestedCount),
    errorMessage: raw.errorMessage ? String(raw.errorMessage) : undefined
  }
}

export const METRIC_OPTIONS: { code: string; label: string; unit: string }[] = [
  { code: 'WEIGHT', label: '体重', unit: 'kg' },
  { code: 'HEART_RATE', label: '心率', unit: 'bpm' },
  { code: 'STEPS', label: '步数', unit: '步' },
  { code: 'BLOOD_PRESSURE_SYS', label: '收缩压', unit: 'mmHg' },
  { code: 'BLOOD_PRESSURE_DIA', label: '舒张压', unit: 'mmHg' },
  { code: 'BLOOD_GLUCOSE', label: '血糖', unit: 'mmol/L' },
  { code: 'BODY_FAT', label: '体脂', unit: '%' },
  { code: 'BMI', label: 'BMI', unit: '' },
  { code: 'HEIGHT', label: '身高', unit: 'cm' },
  /** 入库为分钟；小程序录入/展示按小时 */
  { code: 'SLEEP_MINUTES', label: '睡眠', unit: 'h' },
  { code: 'STRESS', label: '压力', unit: '分' }
]

/** 手动录入合理区间（宽松校验；睡眠按小时） */
export const METRIC_VALUE_RANGE: Record<string, { min: number; max: number }> = {
  WEIGHT: { min: 20, max: 300 },
  HEART_RATE: { min: 30, max: 220 },
  STEPS: { min: 0, max: 100000 },
  BLOOD_PRESSURE_SYS: { min: 60, max: 250 },
  BLOOD_PRESSURE_DIA: { min: 30, max: 150 },
  BLOOD_GLUCOSE: { min: 1, max: 40 },
  BODY_FAT: { min: 1, max: 70 },
  BMI: { min: 10, max: 60 },
  HEIGHT: { min: 50, max: 250 },
  SLEEP_MINUTES: { min: 0, max: 24 },
  STRESS: { min: 1, max: 99 }
}

/** 展示值：睡眠分钟 → 小时 */
export function displayMetricValue(metricCode: string, valueNum: number): number {
  if (metricCode === 'SLEEP_MINUTES') {
    return Math.round((valueNum / 60) * 100) / 100
  }
  return valueNum
}

/** 录入值入库：睡眠小时 → 分钟 */
export function toStoredMetricValue(metricCode: string, inputValue: number): number {
  if (metricCode === 'SLEEP_MINUTES') {
    return Math.round(inputValue * 60)
  }
  return inputValue
}

/** 入库单位 */
export function storedMetricUnit(metricCode: string, displayUnit: string): string {
  if (metricCode === 'SLEEP_MINUTES') return 'min'
  if (metricCode === 'STRESS') return 'score'
  return displayUnit
}

export function metricLabel(code: string): string {
  return METRIC_OPTIONS.find((m) => m.code === code)?.label || code
}

export function providerLabel(code?: string): string {
  if (!code || code === 'manual') return '手动'
  if (code === 'huawei') return '华为'
  return code
}

export function asOptionalSampleId(id?: string): string | undefined {
  return asOptionalId(id)
}

export interface HealthProvider {
  code: string
  displayName: string
  enabled: boolean
  supportsRemoteFetch: boolean
  supportedMetrics?: string[]
}

export interface HealthProviderBinding {
  id: string
  subjectId: string
  providerCode: string
  providerDisplayName?: string
  externalAccountMasked?: string
  hasCredential: boolean
  status?: string
  lastSyncTime?: string
}

/** 可用数据源列表 */
export async function fetchProviders(): Promise<HealthProvider[]> {
  const raw = await get<Array<Record<string, unknown>>>('/api/health/providers')
  return (raw || []).map((row) => ({
    code: String(row.code ?? ''),
    displayName: String(row.displayName ?? row.code ?? ''),
    enabled: Boolean(row.enabled),
    supportsRemoteFetch: Boolean(row.supportsRemoteFetch),
    supportedMetrics: Array.isArray(row.supportedMetrics)
      ? row.supportedMetrics.map((m) => String(m))
      : undefined
  }))
}

/** 我的数据源绑定 */
export async function fetchMyBindings(): Promise<HealthProviderBinding[]> {
  const raw = await get<Array<Record<string, unknown>>>('/api/health/me/bindings')
  return (raw || []).map((row) => ({
    id: asId(String(row.id ?? '')),
    subjectId: asId(String(row.subjectId ?? '')),
    providerCode: String(row.providerCode ?? ''),
    providerDisplayName: row.providerDisplayName
      ? String(row.providerDisplayName)
      : undefined,
    externalAccountMasked: row.externalAccountMasked
      ? String(row.externalAccountMasked)
      : undefined,
    hasCredential: Boolean(row.hasCredential),
    status: row.status ? String(row.status) : undefined,
    lastSyncTime: row.lastSyncTime ? String(row.lastSyncTime) : undefined
  }))
}
