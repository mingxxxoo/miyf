import { ApiClient } from './ApiClient'
import {
  Binding,
  Category,
  Dish,
  DraftLine,
  LoginData,
  Order,
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
    unit: asText(m['unit']) || '份'
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
}
