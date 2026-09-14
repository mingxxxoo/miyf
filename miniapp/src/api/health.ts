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

export async function fetchMyTrend(
  metricCode: string,
  limit = 30,
  options?: { showError?: boolean }
): Promise<HealthTrend> {
  const raw = await get<TrendRaw>(
    '/api/health/me/trends',
    { metricCode, limit },
    { showError: options?.showError !== false }
  )
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
  /** 入库为分钟；展示按小时 */
  { code: 'SLEEP_MINUTES', label: '睡眠', unit: 'h' },
  { code: 'STRESS', label: '压力', unit: '分' }
]

/** 展示值：睡眠分钟 → 小时 */
export function displayMetricValue(metricCode: string, valueNum: number): number {
  if (metricCode === 'SLEEP_MINUTES') {
    return Math.round((valueNum / 60) * 100) / 100
  }
  return valueNum
}

/** 入库单位（兼容旧录入逻辑；个人端已不再手动录入） */
export function storedMetricUnit(metricCode: string, displayUnit: string): string {
  if (metricCode === 'SLEEP_MINUTES') return 'min'
  if (metricCode === 'STRESS') return 'score'
  return displayUnit
}

export function metricLabel(code: string): string {
  return METRIC_OPTIONS.find((m) => m.code === code)?.label || code
}

export function metricUnit(code: string): string {
  return METRIC_OPTIONS.find((m) => m.code === code)?.unit || ''
}

export function formatMeasuredTime(raw?: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw.replace('T', ' ').slice(0, 16)
  const pad = (n: number) => String(n).padStart(2, '0')
  const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const startOfDay = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const now = new Date()
  const diffDays = Math.round((startOfDay(now) - startOfDay(d)) / 86400000)
  if (diffDays === 0) return `今天 ${hm}`
  if (diffDays === 1) return `昨天 ${hm}`
  if (diffDays < 7 && diffDays > 1) return `${diffDays} 天前 ${hm}`
  return `${d.getMonth() + 1}/${d.getDate()} ${hm}`
}

export interface MetricAlert {
  level: 'low' | 'high'
  message: string
}

/** 成人参考区间（按入库单位；睡眠为分钟），超出则在明细中预警 */
const METRIC_ALERT_RULES: Record<
  string,
  { low?: number; high?: number; lowMessage: string; highMessage: string }
> = {
  WEIGHT: { low: 35, high: 150, lowMessage: '体重偏低', highMessage: '体重偏高' },
  HEART_RATE: { low: 50, high: 100, lowMessage: '心率偏低', highMessage: '心率偏高' },
  BLOOD_PRESSURE_SYS: { low: 90, high: 139, lowMessage: '收缩压偏低', highMessage: '收缩压偏高' },
  BLOOD_PRESSURE_DIA: { low: 60, high: 89, lowMessage: '舒张压偏低', highMessage: '舒张压偏高' },
  BLOOD_GLUCOSE: { low: 3.9, high: 7.8, lowMessage: '血糖偏低', highMessage: '血糖偏高' },
  BODY_FAT: { low: 8, high: 35, lowMessage: '体脂偏低', highMessage: '体脂偏高' },
  BMI: { low: 18.5, high: 27.9, lowMessage: 'BMI 偏低', highMessage: 'BMI 偏高' },
  SLEEP_MINUTES: { low: 300, high: 600, lowMessage: '睡眠偏少', highMessage: '睡眠偏长' },
  STRESS: { high: 79, lowMessage: '压力偏低', highMessage: '压力偏高' }
}

export function assessMetricValue(code: string, value: number): MetricAlert | null {
  if (!Number.isFinite(value)) return null
  const rule = METRIC_ALERT_RULES[code]
  if (!rule) return null
  if (rule.low != null && value < rule.low) {
    return { level: 'low', message: rule.lowMessage }
  }
  if (rule.high != null && value > rule.high) {
    return { level: 'high', message: rule.highMessage }
  }
  return null
}

function mean(nums: number[]): number {
  return nums.reduce((a, b) => a + b, 0) / nums.length
}

function stddev(nums: number[]): number {
  if (nums.length < 2) return 0
  const avg = mean(nums)
  const varSum = nums.reduce((acc, n) => acc + (n - avg) ** 2, 0)
  return Math.sqrt(varSum / (nums.length - 1))
}

/** 参考区间优先；样本足够时再标出偏离近期均值的点（value/series 均为入库单位） */
export function assessMetricPoint(
  code: string,
  value: number,
  series?: number[]
): MetricAlert | null {
  const clinical = assessMetricValue(code, value)
  if (clinical) return clinical
  if (!series || series.length < 6) return null
  const valid = series.filter((n) => Number.isFinite(n))
  if (valid.length < 6) return null
  const sd = stddev(valid)
  if (sd <= 0) return null
  const avg = mean(valid)
  if (Math.abs(value - avg) < 2.5 * sd) return null
  return {
    level: value > avg ? 'high' : 'low',
    message: '明显偏离近期均值'
  }
}

export function formatStatValue(metricCode: string, value?: number): string {
  if (value == null || Number.isNaN(value)) return '—'
  const shown = displayMetricValue(metricCode, value)
  return Number.isInteger(shown) ? String(shown) : String(Number(shown.toFixed(2)))
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
