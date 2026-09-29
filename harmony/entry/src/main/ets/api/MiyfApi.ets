import { ApiClient } from './ApiClient'
import {
  Binding,
  Category,
  ChefDish,
  ChefDishAiDraft,
  ChefDishAiExtractResult,
  ChefOrder,
  Dish,
  DraftLine,
  HealthProvider,
  HealthProviderBinding,
  HealthSample,
  HealthSyncRun,
  HealthSubject,
  HealthTrend,
  HealthTrendPoint,
  HuaweiAuthorizeResult,
  HuaweiOAuthStatus,
  Invite,
  Kitchen,
  LoginData,
  Order,
  OrderDraftAiItem,
  OrderDraftAiResult,
  OrderDraftAiUnmatched,
  OrderInsightAiResult,
  OrderInsightDish,
  OrderInsightSuggestion,
  OrderItem,
  Recipe,
  RecipeStep,
  UserProfile,
  asText
} from './Models'

interface RawMap {
  [key: string]: Object | string | number | boolean | null | undefined
}

function asMap(value: Object | null): RawMap {
  if (value === null) {
    return {}
  }
  return value as RawMap
}

function asList(value: Object | null | undefined): Object[] {
  if (value === null || value === undefined) {
    return []
  }
  return value as Object[]
}

function mapLogin(raw: Object | null): LoginData {
  const m = asMap(raw)
  return {
    token: asText(m['token']),
    expireSeconds: typeof m['expireSeconds'] === 'number' ? m['expireSeconds'] as number : undefined,
    userId: asText(m['userId']),
    displayName: asText(m['displayName']),
    username: asText(m['username']),
    avatarUrl: asText(m['avatarUrl'])
  }
}

function mapUser(raw: Object | null): UserProfile {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    nickname: asText(m['nickname']) || asText(m['username']) || '用户',
    username: asText(m['username']),
    phone: asText(m['phone']),
    avatarUrl: asText(m['avatarUrl']),
    activeRole: asText(m['activeRole']),
    chef: m['chef'] === true,
    diner: m['diner'] === true
  }
}

function mapBinding(raw: Object | null): Binding | null {
  if (raw === null) {
    return null
  }
  const m = asMap(raw)
  const id = asText(m['id'])
  if (id.length === 0) {
    return null
  }
  return {
    id: id,
    kitchenName: asText(m['kitchenName']),
    dinerNickname: asText(m['dinerNickname']),
    status: asText(m['status']),
    rejectReason: asText(m['rejectReason']),
    ownerSelf: m['ownerSelf'] === true
  }
}

function mapCategory(raw: Object): Category {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    name: asText(m['name'])
  }
}

function mapDish(raw: Object): Dish {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    name: asText(m['name']),
    subtitle: asText(m['subtitle']),
    description: asText(m['description']),
    coverImage: asText(m['coverImage']) || asText(m['coverUrl']),
    categoryId: asText(m['categoryId']),
    categoryName: asText(m['categoryName']),
    stock: typeof m['stock'] === 'number' ? m['stock'] as number : undefined,
    stockType: asText(m['stockType']),
    unit: asText(m['unit']) || '份',
    recommend: m['recommend'] === true
  }
}

function mapOrderItem(raw: Object): OrderItem {
  const m = asMap(raw)
  return {
    dishId: asText(m['dishId']),
    dishName: asText(m['dishName']),
    quantity: typeof m['quantity'] === 'number' ? m['quantity'] as number : 1,
    remark: asText(m['remark'])
  }
}

function mapOrder(raw: Object): Order {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    orderNo: asText(m['orderNo']),
    status: asText(m['status']),
    remark: asText(m['remark']),
    createTime: asText(m['createTime']),
    items: asList(m['items'] as Object).map(mapOrderItem)
  }
}

function mapRecipe(raw: Object | null): Recipe | null {
  if (raw === null) {
    return null
  }
  const m = asMap(raw)
  const steps: RecipeStep[] = asList(m['steps'] as Object).map((item: Object) => {
    const s = asMap(item)
    return {
      step: typeof s['step'] === 'number' ? s['step'] as number : undefined,
      content: asText(s['content']) || asText(s['description']),
      description: asText(s['description']) || asText(s['content'])
    }
  })
  return {
    description: asText(m['description']),
    steps: steps
  }
}

export class MiyfApi {
  static async huaweiLogin(authorizationCode: string): Promise<LoginData> {
    const body = JSON.stringify({ authorizationCode: authorizationCode })
    return mapLogin(await ApiClient.post('/api/auth/huawei-login', body))
  }

  static async fetchMe(): Promise<UserProfile> {
    return mapUser(await ApiClient.get('/api/user/me'))
  }

  static async chooseRole(role: string): Promise<UserProfile> {
    return mapUser(await ApiClient.post('/api/user/me/role', JSON.stringify({ role: role })))
  }

  static async switchRole(role: string): Promise<UserProfile> {
    return mapUser(await ApiClient.put('/api/user/me/role', JSON.stringify({ role: role })))
  }

  static async fetchMyBinding(): Promise<Binding | null> {
    try {
      return mapBinding(await ApiClient.get('/api/bindings/mine'))
    } catch (_err) {
      return null
    }
  }

  static async applyBinding(code: string): Promise<Binding | null> {
    return mapBinding(await ApiClient.post('/api/bindings/apply', JSON.stringify({ code: code })))
  }

  static async fetchCategories(): Promise<Category[]> {
    const data = await ApiClient.get('/api/categories')
    return asList(data).map(mapCategory)
  }

  static async fetchDishes(categoryId: string = '', keyword: string = ''): Promise<Dish[]> {
    const parts: string[] = ['page=1', 'rows=50']
    if (categoryId.length > 0 && categoryId !== 'all') {
      parts.push('categoryId=' + encodeURIComponent(categoryId))
    }
    if (keyword.length > 0) {
      parts.push('keyword=' + encodeURIComponent(keyword))
    }
    const data = await ApiClient.get('/api/dishes', parts.join('&'))
    const page = asMap(data)
    return asList(page['records'] as Object).map(mapDish)
  }

  static async fetchRecommendDishes(limit: number = 6): Promise<Dish[]> {
    const data = await ApiClient.get('/api/dishes/recommend', 'limit=' + limit)
    return asList(data).map(mapDish)
  }

  static async fetchDishDetail(id: string): Promise<Dish> {
    return mapDish(await ApiClient.get('/api/dishes/' + id) as Object)
  }

  static async fetchRecipe(id: string): Promise<Recipe | null> {
    try {
      return mapRecipe(await ApiClient.get('/api/dishes/' + id + '/recipe'))
    } catch (_err) {
      return null
    }
  }

  static async fetchOrders(): Promise<Order[]> {
    const data = await ApiClient.get('/api/orders', 'page=1&rows=50')
    const page = asMap(data)
    return asList(page['records'] as Object).map(mapOrder)
  }

  static async fetchOrderDetail(id: string): Promise<Order> {
    return mapOrder(await ApiClient.get('/api/orders/' + id) as Object)
  }

  static async createOrder(lines: DraftLine[], remark: string = ''): Promise<Order> {
    const items = lines.map((line: DraftLine) => {
      return {
        dishId: line.dishId,
        quantity: line.quantity
      }
    })
    const body = JSON.stringify({ remark: remark, items: items })
    return mapOrder(await ApiClient.post('/api/orders', body) as Object)
  }

  static async cancelOrder(id: string): Promise<Order> {
    return mapOrder(await ApiClient.post('/api/orders/' + id + '/cancel') as Object)
  }

  static async fetchMyKitchen(): Promise<Kitchen | null> {
    try {
      const raw = await ApiClient.get('/api/my-kitchen')
      if (raw === null) {
        return null
      }
      const m = asMap(raw)
      const id = asText(m['id'])
      if (id.length === 0) {
        return null
      }
      return {
        id: id,
        name: asText(m['name']),
        intro: asText(m['intro']),
        coverImage: asText(m['coverImage']),
        status: asText(m['status'])
      }
    } catch (_err) {
      return null
    }
  }

  static async saveMyKitchen(name: string, intro: string): Promise<Kitchen | null> {
    const raw = await ApiClient.put('/api/my-kitchen', JSON.stringify({ name: name, intro: intro }))
    if (raw === null) {
      return null
    }
    const m = asMap(raw)
    return {
      id: asText(m['id']),
      name: asText(m['name']),
      intro: asText(m['intro']),
      coverImage: asText(m['coverImage']),
      status: asText(m['status'])
    }
  }

  static async fetchInvite(): Promise<Invite | null> {
    try {
      const m = asMap(await ApiClient.get('/api/my-kitchen/invite'))
      const code = asText(m['code'])
      if (code.length === 0) {
        return null
      }
      return { code: code }
    } catch (_err) {
      return null
    }
  }

  static async fetchChefBindings(status: string = ''): Promise<Binding[]> {
    const query = status.length > 0
      ? 'page=1&rows=50&status=' + encodeURIComponent(status)
      : 'page=1&rows=50'
    const data = await ApiClient.get('/api/bindings', query)
    const out: Binding[] = []
    asPageRecords(data).forEach((item: Object) => {
      const b = mapBinding(item)
      if (b !== null) {
        out.push(b)
      }
    })
    return out
  }

  static async approveBinding(id: string): Promise<void> {
    await ApiClient.post('/api/bindings/' + id + '/approve')
  }

  static async rejectBinding(id: string, reason: string = ''): Promise<void> {
    await ApiClient.post('/api/bindings/' + id + '/reject', JSON.stringify({ reason: reason }))
  }

  static async fetchChefDishes(): Promise<ChefDish[]> {
    const data = await ApiClient.get('/api/chef/dishes', 'page=1&rows=50')
    return asPageRecords(data).map(mapChefDish)
  }

  static async createChefDish(name: string, stock: number, subtitle: string = '',
    description: string = ''): Promise<ChefDish> {
    const payload: Record<string, string | number> = {
      name: name,
      stockType: stock > 0 ? 'LIMITED' : 'UNLIMITED',
      unit: '份'
    }
    if (stock > 0) {
      payload['stock'] = stock
    }
    if (subtitle.length > 0) {
      payload['subtitle'] = subtitle
    }
    if (description.length > 0) {
      payload['description'] = description
    }
    return mapChefDish(await ApiClient.post('/api/chef/dishes', JSON.stringify(payload)) as Object)
  }

  static async submitChefDish(id: string): Promise<void> {
    await ApiClient.post('/api/chef/dishes/' + id + '/submit-audit')
  }

  static async publishChefDish(id: string): Promise<void> {
    await ApiClient.post('/api/chef/dishes/' + id + '/publish')
  }

  static async unpublishChefDish(id: string): Promise<void> {
    await ApiClient.post('/api/chef/dishes/' + id + '/unpublish')
  }

  static async fetchChefOrders(status: string = ''): Promise<ChefOrder[]> {
    const query = status.length > 0
      ? 'page=1&rows=50&status=' + encodeURIComponent(status)
      : 'page=1&rows=50'
    const data = await ApiClient.get('/api/chef/orders', query)
    return asPageRecords(data).map(mapChefOrder)
  }

  static async updateChefOrderStatus(id: string, status: string): Promise<void> {
    await ApiClient.put('/api/chef/orders/' + id + '/status', JSON.stringify({ status: status }))
  }

  static async fetchMyHealth(): Promise<HealthSubject> {
    const m = asMap(await ApiClient.get('/api/health/me'))
    return {
      id: asText(m['id']),
      displayName: asText(m['displayName']) || '我'
    }
  }

  static async fetchMyTrend(metricCode: string, limit: number = 30): Promise<HealthTrend | null> {
    try {
      const m = asMap(await ApiClient.get(
        '/api/health/me/trends',
        'metricCode=' + encodeURIComponent(metricCode) + '&limit=' + limit
      ))
      const points: HealthTrendPoint[] = asList(m['points'] as Object).map((item: Object) => {
        const p = asMap(item)
        return {
          measuredTime: asText(p['measuredTime']),
          value: typeof p['value'] === 'number' ? p['value'] as number : undefined
        }
      })
      return {
        metricCode: asText(m['metricCode']) || metricCode,
        unit: asText(m['unit']),
        pointCount: typeof m['pointCount'] === 'number' ? m['pointCount'] as number : points.length,
        min: typeof m['min'] === 'number' ? m['min'] as number : undefined,
        max: typeof m['max'] === 'number' ? m['max'] as number : undefined,
        avg: typeof m['avg'] === 'number' ? m['avg'] as number : undefined,
        latest: typeof m['latest'] === 'number' ? m['latest'] as number : undefined,
        latestTime: asText(m['latestTime']),
        points: points
      }
    } catch (_err) {
      return null
    }
  }

  static async fetchMySamples(metricCode: string, limit: number = 50): Promise<HealthSample[]> {
    try {
      const data = await ApiClient.get(
        '/api/health/me/samples',
        'metricCode=' + encodeURIComponent(metricCode) + '&limit=' + limit
      )
      return asList(data).map((item: Object) => {
        const m = asMap(item)
        return {
          id: asText(m['id']),
          metricCode: asText(m['metricCode']),
          valueNum: typeof m['valueNum'] === 'number' ? m['valueNum'] as number : 0,
          unit: asText(m['unit']),
          measuredTime: asText(m['measuredTime']),
          providerCode: asText(m['providerCode'])
        }
      })
    } catch (_err) {
      return []
    }
  }

  static async fetchHuaweiOAuthStatus(): Promise<HuaweiOAuthStatus> {
    try {
      const m = asMap(await ApiClient.get('/api/health/providers/huawei/oauth/status'))
      return {
        authorized: m['authorized'] === true,
        subjectId: asText(m['subjectId']),
        providerCode: asText(m['providerCode']) || 'huawei'
      }
    } catch (_err) {
      return { authorized: false, providerCode: 'huawei' }
    }
  }

  static async fetchHuaweiAuthorizeUrl(): Promise<HuaweiAuthorizeResult> {
    const m = asMap(await ApiClient.get('/api/health/providers/huawei/authorize-url'))
    return { authorizeUrl: asText(m['authorizeUrl']) }
  }

  static async revokeHuaweiOAuth(): Promise<void> {
    await ApiClient.del('/api/health/providers/huawei/oauth')
  }

  static async syncMyProvider(providerCode: string): Promise<HealthSyncRun> {
    const m = asMap(await ApiClient.post(
      '/api/health/providers/' + encodeURIComponent(providerCode) + '/sync',
      '{}'
    ))
    return {
      status: asText(m['status']),
      ingestedCount: typeof m['ingestedCount'] === 'number' ? m['ingestedCount'] as number : undefined,
      errorMessage: asText(m['errorMessage'])
    }
  }

  static async fetchProviders(): Promise<HealthProvider[]> {
    try {
      return asList(await ApiClient.get('/api/health/providers')).map((item: Object) => {
        const m = asMap(item)
        return {
          code: asText(m['code']),
          displayName: asText(m['displayName']) || asText(m['code']),
          enabled: m['enabled'] !== false
        }
      }).filter((p: HealthProvider) => p.code.length > 0 && p.code !== 'manual')
    } catch (_err) {
      return [{ code: 'huawei', displayName: '华为运动健康', enabled: true }]
    }
  }

  static async fetchHealthBindings(): Promise<HealthProviderBinding[]> {
    try {
      return asList(await ApiClient.get('/api/health/me/bindings')).map((item: Object) => {
        const m = asMap(item)
        return {
          id: asText(m['id']),
          providerCode: asText(m['providerCode']),
          providerDisplayName: asText(m['providerDisplayName']),
          hasCredential: m['hasCredential'] === true,
          status: asText(m['status']),
          lastSyncTime: asText(m['lastSyncTime'])
        }
      })
    } catch (_err) {
      return []
    }
  }

  static async draftOrderFromText(text: string): Promise<OrderDraftAiResult> {
    try {
      const m = asMap(await ApiClient.post('/api/ai/orders/draft', JSON.stringify({ text: text })))
      const items: OrderDraftAiItem[] = asList(m['items'] as Object).map((item: Object) => {
        const row = asMap(item)
        const qty = typeof row['quantity'] === 'number' ? row['quantity'] as number : 1
        return {
          dishId: asText(row['dishId']),
          dishName: asText(row['dishName']),
          quantity: qty > 0 ? qty : 1,
          itemNote: asText(row['itemNote'])
        }
      }).filter((i: OrderDraftAiItem) => i.dishId.length > 0)
      const unmatched: OrderDraftAiUnmatched[] = asList(m['unmatched'] as Object).map((item: Object) => {
        const row = asMap(item)
        return {
          rawText: asText(row['rawText']),
          reason: asText(row['reason'])
        }
      })
      return {
        degraded: m['degraded'] === true,
        items: items,
        unmatched: unmatched,
        mealType: asText(m['mealType']),
        note: asText(m['note']),
        ambiguityNote: asText(m['ambiguityNote'])
      }
    } catch (_err) {
      return { degraded: true, items: [], unmatched: [] }
    }
  }

  static async analyzeOrderInsight(dinerText: string, draftJson: string): Promise<OrderInsightAiResult | null> {
    try {
      const m = asMap(await ApiClient.post('/api/ai/orders/insight',
        JSON.stringify({ dinerText: dinerText, draftJson: draftJson })))
      if (m['degraded'] === true) {
        return null
      }
      const dishInsights: OrderInsightDish[] = asList(m['dishInsights'] as Object).map((item: Object) => {
        const row = asMap(item)
        return {
          dishId: asText(row['dishId']),
          dishName: asText(row['dishName']),
          verdict: asText(row['verdict']) || 'OK',
          reason: asText(row['reason'])
        }
      })
      const suggestions: OrderInsightSuggestion[] = asList(m['suggestions'] as Object).map((item: Object) => {
        const row = asMap(item)
        return {
          type: asText(row['type']) || 'GENERAL',
          text: asText(row['text'])
        }
      })
      return {
        degraded: false,
        overallVerdict: asText(m['overallVerdict']),
        summary: asText(m['summary']),
        dishInsights: dishInsights,
        suggestions: suggestions,
        disclaimer: asText(m['disclaimer']) || '以上为一般性饮食参考，不构成医疗建议'
      }
    } catch (_err) {
      return null
    }
  }

  static async extractChefDishes(text: string, url: string = ''): Promise<ChefDishAiExtractResult> {
    try {
      const payload: Record<string, string> = {}
      if (text.length > 0) {
        payload['text'] = text
      }
      if (url.length > 0) {
        payload['url'] = url
      }
      const m = asMap(await ApiClient.post('/api/chef/ai/dishes/extract', JSON.stringify(payload)))
      const dishes: ChefDishAiDraft[] = asList(m['dishes'] as Object).map((item: Object) => {
        const row = asMap(item)
        return {
          name: asText(row['name']),
          subtitle: asText(row['subtitle']),
          categoryName: asText(row['categoryName']),
          description: asText(row['description']),
          stockType: asText(row['stockType']),
          stock: typeof row['stock'] === 'number' ? row['stock'] as number : undefined
        }
      }).filter((d: ChefDishAiDraft) => d.name.length > 0)
      return {
        degraded: m['degraded'] === true,
        rejectReason: asText(m['rejectReason']),
        dishes: dishes
      }
    } catch (_err) {
      return { degraded: true, dishes: [], rejectReason: 'AI 暂不可用' }
    }
  }
}

function asPageRecords(data: Object | null): Object[] {
  if (data === null) {
    return []
  }
  const m = asMap(data)
  if (m['records'] !== undefined && m['records'] !== null) {
    return asList(m['records'] as Object)
  }
  return asList(data)
}

function mapChefDish(raw: Object): ChefDish {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    name: asText(m['name']),
    subtitle: asText(m['subtitle']),
    status: asText(m['status']),
    auditStatus: asText(m['auditStatus']),
    rejectReason: asText(m['rejectReason']),
    stock: typeof m['stock'] === 'number' ? m['stock'] as number : undefined,
    stockType: asText(m['stockType']),
    unit: asText(m['unit']) || '份',
    categoryName: asText(m['categoryName'])
  }
}

function mapChefOrder(raw: Object): ChefOrder {
  const m = asMap(raw)
  return {
    id: asText(m['id']),
    orderNo: asText(m['orderNo']),
    userNickname: asText(m['userNickname']),
    status: asText(m['status']),
    remark: asText(m['remark']),
    createTime: asText(m['createTime']),
    items: asList(m['items'] as Object).map(mapOrderItem)
  }
}
