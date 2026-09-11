/** Unified API envelope */

export interface ApiResult<T = unknown> {

  code: number;

  message: string;

  data: T;

}



/** Paginated list (records / total / page / pageSize) */

export interface PageResult<T> {

  records: T[];

  total: number;

  page: number;

  pageSize: number;

}



export interface PageQuery {

  page?: number;

  /** UI 用 pageSize；请求时映射为后端 rows */

  pageSize?: number;

  rows?: number;

  keyword?: string;

}



export interface Category {

  id: string;

  name: string;

  icon?: string;

  sortOrder: number;

  /** ENABLED / DISABLED */

  status: string;

  /** 兼容表单开关 */

  enabled?: boolean;

  dishCount?: number;

  createTime?: string;

  lastModifyTime?: string;

}



/** 后端：DRAFT / ON_SALE / OFF_SALE */

export type DishStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SALE' | string;

export type StockType = 'LIMITED' | 'UNLIMITED';



export interface DishImage {

  id?: string;

  url: string;

  sortOrder?: number;

}



export interface RecipeIngredient {

  name: string;

  amount?: string;

}



export interface RecipeStep {

  step: number;

  title?: string;

  content?: string;

  description?: string;

  imageUrl?: string;

  image?: string;

}



export interface DishRecipe {

  id?: string;

  dishId: string;

  dishName?: string;

  description?: string;

  difficulty?: string;

  prepareMinutes?: number;

  cookMinutes?: number;

  servings?: number;

  ingredients: RecipeIngredient[];

  seasonings: RecipeIngredient[];

  steps: RecipeStep[];

  nutrition?: Record<string, unknown> | string;

  tips?: string;

  lastModifyTime?: string;

}



export interface Dish {

  id: string;

  name: string;

  subtitle?: string;

  description?: string;

  coverImage?: string;

  coverUrl?: string;

  categoryId: string;

  categoryName?: string;

  status: DishStatus;

  stockType: StockType;

  stock?: number;

  unit?: string;

  sortOrder?: number;

  recommend?: boolean;

  recommended?: boolean;

  rating?: number;

  ratingCount?: number;

  images?: string[] | DishImage[];

  recipe?: DishRecipe;

  createTime?: string;

  lastModifyTime?: string;

}



export type OrderStatus =

  | 'PENDING'

  | 'CONFIRMED'

  | 'PREPARING'

  | 'READY'

  | 'COMPLETED'

  | 'CANCELLED';



export interface OrderItem {

  id?: string;

  dishId: string;

  dishName: string;

  coverUrl?: string;

  quantity: number;

  unit?: string;

  note?: string;

  remark?: string;

}



export interface Order {

  id: string;

  orderNo: string;

  userId: string;

  userNickname?: string;

  userAvatar?: string;

  status: OrderStatus;

  items: OrderItem[];

  scheduledTime?: string;

  note?: string;

  remark?: string;

  guestCount?: number;

  createTime: string;

  lastModifyTime?: string;

  displayTip?: string;

}



export interface Comment {

  id: string;

  orderId: string;

  dishId: string;

  dishName?: string;

  userId: string;

  userNickname?: string;

  userAvatar?: string;

  rating: number;

  content?: string;

  images?: string[];

  /** NORMAL / HIDDEN */

  status?: string;

  hidden?: boolean;

  createTime: string;

}



export interface User {

  id: string;

  username?: string;

  nickname: string;

  avatarUrl?: string;

  phone?: string;

  wechatId?: string;

  openId?: string;

  status?: 'ENABLED' | 'DISABLED' | 'ACTIVE' | string;

  createTime?: string;

  lastLoginTime?: string;

}



export interface AdminUser {

  id: string;

  username: string;

  nickname: string;

  avatar?: string;

  roles: string[];

  roleIds?: string[];

  enabled?: boolean;

  createTime?: string;

}



export interface Role {

  id: string;

  code: string;

  name: string;

  description?: string;

  permissionIds?: string[];

  createTime?: string;

}



export interface Permission {

  id: string;

  code: string;

  name: string;

  description?: string;

  module?: string;

}



export interface OperationLog {

  id: string;

  operatorId?: string;

  operatorName?: string;

  action: string;

  module?: string;

  detail?: string;

  ip?: string;

  createTime: string;

  /** 以下为可选映射字段（后端 VO 可能未返回） */
  operationType?: string;
  requestPath?: string;
  requestUri?: string;
  requestMethod?: string;
  result?: string;
  before?: string;
  after?: string;

}



export interface DashboardStats {

  userCount: number;

  todayOrders: number;

  dishCount: number;

  pendingComments: number;

  reservationTrend: { date: string; count: number }[];

  hotDishes: { name: string; count: number }[];

  ratingDistribution: { rating: number; count: number }[];

  userActivity: { date: string; activeUsers: number }[];

}



export interface LoginVo {

  token: string;

  expireSeconds?: number;

  userId: string;

  displayName: string;

  /** 登录名（通知 userKey 等稳定标识） */
  username?: string;

  principalType?: string;

  permissions?: string[];

  roles?: string[];

}


