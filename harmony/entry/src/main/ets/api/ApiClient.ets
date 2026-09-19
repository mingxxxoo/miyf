import { http } from '@kit.NetworkKit'
import { AppConfig } from '../common/AppConfig'
import { asText } from './Models'

export class ApiError extends Error {
  code: number = 0

  constructor(message: string, code: number) {
    super(message)
    this.code = code
  }
}

interface Envelope {
  code: number
  message: string
  data: Object | null
}

export class ApiClient {
  private static token: string = ''

  static setToken(token: string): void {
    ApiClient.token = token
  }

  static getToken(): string {
    return ApiClient.token
  }

  static async get(path: string, query: string = ''): Promise<Object | null> {
    return ApiClient.request(http.RequestMethod.GET, path, '', query)
  }

  static async post(path: string, body: string = ''): Promise<Object | null> {
    return ApiClient.request(http.RequestMethod.POST, path, body, '')
  }

  static async put(path: string, body: string = ''): Promise<Object | null> {
    return ApiClient.request(http.RequestMethod.PUT, path, body, '')
  }

  private static async request(method: http.RequestMethod, path: string, body: string,
    query: string): Promise<Object | null> {
    const client = http.createHttp()
    const header: Record<string, string> = { 'Content-Type': 'application/json' }
    if (ApiClient.token.length > 0) {
      header['Authorization'] = 'Bearer ' + ApiClient.token
    }
    let url = AppConfig.API_BASE + path
    if (query.length > 0) {
      url = url + '?' + query
    }
    try {
      const response = await client.request(url, {
        method: method,
        header: header,
        extraData: body.length > 0 ? body : undefined,
        expectDataType: http.HttpDataType.STRING,
        connectTimeout: 10000,
        readTimeout: 20000
      })
      const text = typeof response.result === 'string' ? response.result as string : ''
      if (response.responseCode === 401) {
        throw new ApiError('请先登录', 40100)
      }
      if (text.length === 0) {
        throw new ApiError('请求失败', response.responseCode)
      }
      const envelope = JSON.parse(text) as Envelope
      if (envelope.code === 40100) {
        throw new ApiError('请先登录', 40100)
      }
      if (envelope.code !== 0) {
        const message = asText(envelope.message)
        throw new ApiError(message.length > 0 ? message : '请求失败', envelope.code)
      }
      return envelope.data
    } catch (err) {
      if (err instanceof ApiError) {
        throw err
      }
      throw new ApiError('网络不可用，请稍后重试', -1)
    } finally {
      client.destroy()
    }
  }
}
