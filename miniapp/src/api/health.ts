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

export const METRIC_OPTIONS: { code: string; label: string; unit: string }[] = [
  { code: 'WEIGHT', label: '体重', unit: 'kg' },
  { code: 'HEART_RATE', label: '心率', unit: 'bpm' },
  { code: 'STEPS', label: '步数', unit: '步' },
  { code: 'BLOOD_PRESSURE_SYS', label: '收缩压', unit: 'mmHg' },
  { code: 'BLOOD_GLUCOSE', label: '血糖', unit: 'mmol/L' }
]

export function metricLabel(code: string): string {
  return METRIC_OPTIONS.find((m) => m.code === code)?.label || code
}

export function asOptionalSampleId(id?: string): string | undefined {
  return asOptionalId(id)
}
