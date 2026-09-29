export interface LoginData {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  username?: string
  avatarUrl?: string
}

export interface UserProfile {
  id: string
  nickname: string
  username?: string
  phone?: string
  avatarUrl?: string
  activeRole?: string
  chef?: boolean
  diner?: boolean
}

export interface Binding {
  id: string
  kitchenName?: string
  dinerNickname?: string
  status: string
  rejectReason?: string
  ownerSelf?: boolean
}

export interface Category {
  id: string
  name: string
}

export interface Dish {
  id: string
  name: string
  subtitle?: string
  description?: string
  coverImage?: string
  categoryId?: string
  categoryName?: string
  stock?: number
  stockType?: string
  unit?: string
  recommend?: boolean
}

export interface Kitchen {
  id: string
  name: string
  intro?: string
  coverImage?: string
  status?: string
}

export interface Invite {
  code: string
}

export interface ChefDish {
  id: string
  name: string
  subtitle?: string
  status: string
  auditStatus?: string
  rejectReason?: string
  stock?: number
  stockType?: string
  unit?: string
  categoryName?: string
}

export interface RecipeStep {
  step?: number
  content?: string
  description?: string
}

export interface Recipe {
  description?: string
  steps?: RecipeStep[]
}

export interface OrderItem {
  dishId: string
  dishName: string
  quantity: number
  remark?: string
}

export interface Order {
  id: string
  orderNo: string
  status: string
  remark?: string
  createTime?: string
  items?: OrderItem[]
}

export interface ChefOrder {
  id: string
  orderNo: string
  userNickname?: string
  status: string
  remark?: string
  createTime?: string
  items?: OrderItem[]
}

export interface DraftLine {
  dishId: string
  dishName: string
  quantity: number
}

export function asText(value: Object | string | number | undefined | null): string {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
}

export function orderStatusText(status: string): string {
  if (status === 'PENDING') {
    return '待确认'
  }
  if (status === 'CONFIRMED') {
    return '已确认'
  }
  if (status === 'PREPARING') {
    return '备餐中'
  }
  if (status === 'READY') {
    return '可取餐'
  }
  if (status === 'COMPLETED') {
    return '已完成'
  }
  if (status === 'CANCELLED') {
    return '已取消'
  }
  return status
}

export function dishStatusText(status: string, audit?: string): string {
  if (audit === 'PENDING_REVIEW') {
    return '审核中'
  }
  if (audit === 'REJECTED') {
    return '已驳回'
  }
  if (status === 'ON_SALE') {
    return '在售'
  }
  if (status === 'OFF_SALE') {
    return '已下架'
  }
  if (status === 'DRAFT') {
    return '草稿'
  }
  return status
}

export function nextChefStatus(status: string): string {
  if (status === 'PENDING') {
    return 'CONFIRMED'
  }
  if (status === 'CONFIRMED') {
    return 'PREPARING'
  }
  if (status === 'PREPARING') {
    return 'READY'
  }
  if (status === 'READY') {
    return 'COMPLETED'
  }
  return ''
}

export function nextChefAction(status: string): string {
  if (status === 'PENDING') {
    return '确认'
  }
  if (status === 'CONFIRMED') {
    return '开始备餐'
  }
  if (status === 'PREPARING') {
    return '标记可取'
  }
  if (status === 'READY') {
    return '完成'
  }
  return ''
}

export function greetByHour(): string {
  const h = new Date().getHours()
  if (h < 11) {
    return '上午好'
  }
  if (h < 14) {
    return '中午好'
  }
  if (h < 18) {
    return '下午好'
  }
  return '晚上好'
}

export function stockLabel(stockType: string | undefined, stock: number | undefined, unit: string | undefined): string {
  if (stockType === 'UNLIMITED') {
    return '不限量'
  }
  return '今日还剩 ' + (stock ?? 0) + ' ' + (unit && unit.length > 0 ? unit : '份')
}

export function nickInitial(name: string | undefined): string {
  const n = (name || '客').trim()
  if (n.length === 0) {
    return '客'
  }
  return n.substring(0, 1)
}

export function itemSummary(items: OrderItem[] | undefined): string {
  if (items === undefined || items.length === 0) {
    return '暂无菜品明细'
  }
  return items.map((it: OrderItem) => it.dishName + ' ×' + it.quantity).join(' · ')
}

export function bindingStatusText(status: string): string {
  if (status === 'BOUND') {
    return '已绑定'
  }
  if (status === 'PENDING') {
    return '待确认'
  }
  if (status === 'REJECTED') {
    return '未通过'
  }
  return '未绑定'
}

export function absoluteUrl(base: string, raw: string | undefined): string {
  if (raw === undefined || raw.length === 0) {
    return ''
  }
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    return raw
  }
  if (raw.startsWith('/')) {
    return base + raw
  }
  return base + '/' + raw
}

export interface HealthSubject {
  id: string
  displayName: string
}

export interface HealthTrendPoint {
  measuredTime?: string
  value?: number
}

export interface HealthTrend {
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

export interface HealthSample {
  id: string
  metricCode: string
  valueNum: number
  unit?: string
  measuredTime: string
  providerCode?: string
}

export interface HuaweiOAuthStatus {
  authorized: boolean
  subjectId?: string
  providerCode?: string
}

export interface HuaweiAuthorizeResult {
  authorizeUrl: string
}

export interface HealthSyncRun {
  status: string
  ingestedCount?: number
  errorMessage?: string
}

export interface HealthProvider {
  code: string
  displayName: string
  enabled: boolean
}

export interface HealthProviderBinding {
  id: string
  providerCode: string
  providerDisplayName?: string
  hasCredential: boolean
  status?: string
  lastSyncTime?: string
}

export interface MetricDef {
  code: string
  label: string
  unit: string
  icon: string
  bg: string
}

export const METRIC_DEFS: MetricDef[] = [
  { code: 'HEART_RATE', label: '心率', unit: 'bpm', icon: 'heart', bg: '#FCEEEA' },
  { code: 'STEPS', label: '步数', unit: '步', icon: 'footprints', bg: '#FBF3E0' },
  { code: 'SLEEP_MINUTES', label: '睡眠', unit: '小时', icon: 'moon', bg: '#E7F7F0' },
  { code: 'WEIGHT', label: '体重', unit: 'kg', icon: 'scale', bg: '#E7F7F0' },
  { code: 'BLOOD_PRESSURE_SYS', label: '收缩压', unit: 'mmHg', icon: 'activity', bg: '#EBF3FB' },
  { code: 'BLOOD_PRESSURE_DIA', label: '舒张压', unit: 'mmHg', icon: 'activity', bg: '#EBF3FB' },
  { code: 'BLOOD_GLUCOSE', label: '血糖', unit: 'mmol/L', icon: 'droplet', bg: '#FFF1E2' },
  { code: 'BODY_FAT', label: '体脂', unit: '%', icon: 'circle', bg: '#F4F0E9' },
  { code: 'BMI', label: 'BMI', unit: '', icon: 'square', bg: '#E7F7F0' },
  { code: 'STRESS', label: '压力', unit: '分', icon: 'wind', bg: '#FBF3E0' }
]

export function metricDefOf(code: string): MetricDef {
  const found = METRIC_DEFS.find((m: MetricDef) => m.code === code)
  if (found !== undefined) {
    return found
  }
  return { code: code, label: code, unit: '', icon: 'circle', bg: '#F4F0E9' }
}

export function displayMetricValue(metricCode: string, valueNum: number): number {
  if (metricCode === 'SLEEP_MINUTES') {
    return Math.round((valueNum / 60) * 100) / 100
  }
  return valueNum
}

export function formatStatValue(metricCode: string, value?: number): string {
  if (value === undefined || Number.isNaN(value)) {
    return '—'
  }
  const shown = displayMetricValue(metricCode, value)
  if (Number.isInteger(shown)) {
    return String(shown)
  }
  return String(Number(shown.toFixed(2)))
}

export function formatMeasuredTime(raw?: string): string {
  if (raw === undefined || raw.length === 0) {
    return ''
  }
  const d = new Date(raw.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) {
    return raw.replace('T', ' ').substring(0, 16)
  }
  const pad = (n: number): string => (n < 10 ? '0' + n : String(n))
  const hm = pad(d.getHours()) + ':' + pad(d.getMinutes())
  const now = new Date()
  const startToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startThat = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const diffDays = Math.round((startToday - startThat) / 86400000)
  if (diffDays === 0) {
    return '今天 ' + hm
  }
  if (diffDays === 1) {
    return '昨天 ' + hm
  }
  return (d.getMonth() + 1) + '/' + d.getDate() + ' ' + hm
}

export function assessMetricAlert(code: string, value: number): string {
  if (!Number.isFinite(value)) {
    return ''
  }
  if (code === 'HEART_RATE' && value > 100) {
    return '心率偏高'
  }
  if (code === 'HEART_RATE' && value < 50) {
    return '心率偏低'
  }
  if (code === 'BLOOD_PRESSURE_SYS' && value > 139) {
    return '收缩压偏高'
  }
  if (code === 'BLOOD_PRESSURE_SYS' && value < 90) {
    return '收缩压偏低'
  }
  if (code === 'BLOOD_PRESSURE_DIA' && value > 89) {
    return '舒张压偏高'
  }
  if (code === 'BLOOD_GLUCOSE' && value > 7.8) {
    return '血糖偏高'
  }
  if (code === 'BLOOD_GLUCOSE' && value < 3.9) {
    return '血糖偏低'
  }
  if (code === 'SLEEP_MINUTES' && value < 300) {
    return '睡眠偏少'
  }
  if (code === 'SLEEP_MINUTES' && value > 600) {
    return '睡眠偏长'
  }
  if (code === 'BMI' && value > 27.9) {
    return 'BMI 偏高'
  }
  if (code === 'BMI' && value < 18.5) {
    return 'BMI 偏低'
  }
  if (code === 'STRESS' && value > 79) {
    return '压力偏高'
  }
  return ''
}

export function healthScoreOf(withData: number, alerts: number): number {
  if (withData <= 0) {
    return 0
  }
  const base = 72
  const bonus = Math.min(28, withData * 4)
  const penalty = alerts * 10
  const score = base + bonus - penalty
  if (score < 35) {
    return 35
  }
  if (score > 98) {
    return 98
  }
  return score
}

export interface OrderDraftAiItem {
  dishId: string
  dishName: string
  quantity: number
  itemNote?: string
}

export interface OrderDraftAiUnmatched {
  rawText: string
  reason: string
}

export interface OrderDraftAiResult {
  degraded: boolean
  items: OrderDraftAiItem[]
  unmatched: OrderDraftAiUnmatched[]
  mealType?: string
  note?: string
  ambiguityNote?: string
}

export interface OrderInsightDish {
  dishId: string
  dishName: string
  verdict: string
  reason?: string
}

export interface OrderInsightSuggestion {
  type: string
  text: string
}

export interface OrderInsightAiResult {
  degraded: boolean
  overallVerdict?: string
  summary?: string
  dishInsights: OrderInsightDish[]
  suggestions: OrderInsightSuggestion[]
  disclaimer?: string
}

export interface ChefDishAiDraft {
  name: string
  subtitle?: string
  categoryName?: string
  description?: string
  stockType?: string
  stock?: number
}

export interface ChefDishAiExtractResult {
  degraded: boolean
  rejectReason?: string
  dishes: ChefDishAiDraft[]
}

export function insightVerdictText(verdict: string): string {
  const v = verdict.toUpperCase()
  if (v === 'OK' || v === 'SUITABLE' || v === '合适') {
    return '合适'
  }
  if (v === 'CAUTION' || v === 'WARN' || v === '需注意' || v === '需关注') {
    return '需注意'
  }
  if (v === 'AVOID' || v === 'NOT_RECOMMENDED' || v === '不建议') {
    return '不建议'
  }
  return verdict.length > 0 ? verdict : '参考'
}

export function insightIsWarn(verdict: string | undefined): boolean {
  if (verdict === undefined) {
    return false
  }
  const t = insightVerdictText(verdict)
  return t === '需注意' || t === '不建议'
}


