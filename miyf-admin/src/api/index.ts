import { get, post, put, del } from './http';
import type {
  AdminUser,
  Category,
  Comment,
  DashboardStats,
  Dish,
  DishRecipe,
  LoginVo,
  OperationLog,
  Order,
  OrderItem,
  OrderStatus,
  PageQuery,
  PageResult,
  RecipeIngredient,
  RecipeStep,
  User,
} from '@/types';

/** Map UI pageSize to backend rows */
export function toPageParams(
  params?: PageQuery | Record<string, unknown>,
): Record<string, unknown> | undefined {
  if (!params) return undefined;
  const src = params as Record<string, unknown>;
  const { pageSize, rows, page, ...rest } = src;
  return {
    ...rest,
    page: (page as number | undefined) ?? 1,
    rows: (rows as number | undefined) ?? (pageSize as number | undefined) ?? 10,
  };
}

function asPage<T>(
  raw: PageResult<T> | T[] | null | undefined,
  fallbackPage = 1,
  fallbackSize = 10,
): PageResult<T> {
  if (!raw) {
    return { records: [], total: 0, page: fallbackPage, pageSize: fallbackSize };
  }
  if (Array.isArray(raw)) {
    return { records: raw, total: raw.length, page: 1, pageSize: raw.length || fallbackSize };
  }
  return {
    records: raw.records ?? [],
    total: Number(raw.total ?? 0),
    page: Number(raw.page ?? fallbackPage),
    pageSize: Number(raw.pageSize ?? fallbackSize),
  };
}

function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

function mapCategory(raw: Record<string, unknown>): Category {
  const status = String(raw.status ?? 'ENABLED').toUpperCase();
  return {
    id: sid(raw.id),
    name: String(raw.name ?? ''),
    icon: raw.icon ? String(raw.icon) : undefined,
    sortOrder: Number(raw.sortOrder ?? 0),
    status,
    enabled: status === 'ENABLED',
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
  };
}

function mapDish(raw: Record<string, unknown>): Dish {
  const ratingCount = Number(raw.ratingCount ?? 0);
  const cover = String(raw.coverImage ?? raw.coverUrl ?? '');
  const status = String(raw.status ?? 'DRAFT').toUpperCase();
  return {
    id: sid(raw.id),
    name: String(raw.name ?? ''),
    subtitle: raw.subtitle ? String(raw.subtitle) : undefined,
    description: raw.description ? String(raw.description) : undefined,
    coverImage: cover || undefined,
    coverUrl: cover || undefined,
    categoryId: sid(raw.categoryId),
    categoryName: raw.categoryName ? String(raw.categoryName) : undefined,
    status,
    stockType: (raw.stockType as Dish['stockType']) || 'UNLIMITED',
    stock: raw.stock != null ? Number(raw.stock) : undefined,
    unit: raw.unit ? String(raw.unit) : undefined,
    sortOrder: raw.sortOrder != null ? Number(raw.sortOrder) : undefined,
    recommend: Boolean(raw.recommend ?? raw.recommended),
    recommended: Boolean(raw.recommend ?? raw.recommended),
    rating: ratingCount > 0 && raw.rating != null ? Number(raw.rating) : undefined,
    ratingCount,
    images: Array.isArray(raw.images) ? (raw.images as string[]) : undefined,
  };
}

function mapMaterial(list: unknown): RecipeIngredient[] {
  if (!Array.isArray(list)) return [];
  return list.map((item) => {
    const o = item as Record<string, unknown>;
    return {
      name: String(o.name ?? ''),
      amount: o.amount != null ? String(o.amount) : undefined,
    };
  });
}

function mapSteps(list: unknown): RecipeStep[] {
  if (!Array.isArray(list)) return [];
  return list.map((item, idx) => {
    const o = item as Record<string, unknown>;
    return {
      step: Number(o.step ?? idx + 1),
      title: o.title ? String(o.title) : undefined,
      content: String(o.content ?? o.description ?? ''),
      description: o.description ? String(o.description) : undefined,
      imageUrl: o.imageUrl ? String(o.imageUrl) : o.image ? String(o.image) : undefined,
    };
  });
}

function mapRecipe(raw: Record<string, unknown>): DishRecipe {
  return {
    id: sid(raw.id) || undefined,
    dishId: sid(raw.dishId),
    dishName: raw.dishName ? String(raw.dishName) : undefined,
    description: raw.description ? String(raw.description) : undefined,
    difficulty: raw.difficulty ? String(raw.difficulty) : undefined,
    prepareMinutes: raw.prepareMinutes != null ? Number(raw.prepareMinutes) : undefined,
    cookMinutes: raw.cookMinutes != null ? Number(raw.cookMinutes) : undefined,
    servings: raw.servings != null ? Number(raw.servings) : undefined,
    ingredients: mapMaterial(raw.ingredients),
    seasonings: mapMaterial(raw.seasonings),
    steps: mapSteps(raw.steps),
    tips: raw.tips ? String(raw.tips) : undefined,
    nutrition: raw.nutrition as DishRecipe['nutrition'],
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
  };
}

function mapOrderItem(raw: Record<string, unknown>): OrderItem {
  const cover = raw.coverImage ?? raw.coverUrl;
  return {
    id: raw.id ? sid(raw.id) : undefined,
    dishId: sid(raw.dishId),
    dishName: String(raw.dishName ?? ''),
    quantity: Number(raw.quantity ?? 0),
    unit: raw.unit ? String(raw.unit) : undefined,
    coverUrl: cover ? String(cover) : undefined,
    note: raw.remark ? String(raw.remark) : raw.note ? String(raw.note) : undefined,
    remark: raw.remark ? String(raw.remark) : undefined,
  };
}

function mapOrder(raw: Record<string, unknown>): Order {
  const items = Array.isArray(raw.items)
    ? (raw.items as Record<string, unknown>[]).map(mapOrderItem)
    : [];
  const remark = raw.remark ? String(raw.remark) : undefined;
  return {
    id: sid(raw.id),
    orderNo: String(raw.orderNo ?? ''),
    userId: sid(raw.userId),
    userNickname: raw.userNickname ? String(raw.userNickname) : undefined,
    status: String(raw.status ?? 'PENDING').toUpperCase() as OrderStatus,
    items,
    note: remark,
    remark,
    createdAt: String(raw.createdAt ?? ''),
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined,
    displayTip: raw.displayTip ? String(raw.displayTip) : undefined,
  };
}

function mapComment(raw: Record<string, unknown>): Comment {
  const status = String(raw.status ?? 'NORMAL').toUpperCase();
  return {
    id: sid(raw.id),
    orderId: sid(raw.orderId),
    dishId: sid(raw.dishId),
    dishName: raw.dishName ? String(raw.dishName) : undefined,
    userId: sid(raw.userId),
    userNickname: raw.userNickname ? String(raw.userNickname) : undefined,
    userAvatar: raw.userAvatar ? String(raw.userAvatar) : undefined,
    rating: Number(raw.rating ?? 0),
    content: raw.content ? String(raw.content) : undefined,
    images: Array.isArray(raw.images) ? (raw.images as string[]) : undefined,
    status,
    hidden: status === 'HIDDEN',
    createdAt: String(raw.createdAt ?? ''),
  };
}

export interface LoginResult {
  token: string;
  user: AdminUser;
  permissions: string[];
}

export interface LoginRisk {
  failCount: number;
  captchaRequired: boolean;
  locked: boolean;
  lockedUntil?: number | null;
  lockRemainSeconds: number;
}

export interface CaptchaPayload {
  captchaId: string;
  imageBase64: string;
  expireSeconds: number;
}

export const authApi = {
  login: async (
    username: string,
    password: string,
    captcha?: { captchaId?: string; captchaCode?: string },
  ): Promise<LoginResult> => {
    const vo = await post<LoginVo>('/admin/auth/login', {
      username,
      password,
      captchaId: captcha?.captchaId,
      captchaCode: captcha?.captchaCode,
    });
    const permissions = vo.permissions?.length ? vo.permissions : [];
    const roles = vo.roles ?? [];
    return {
      token: vo.token,
      permissions,
      user: {
        id: sid(vo.userId),
        username: vo.displayName || username,
        nickname: vo.displayName || username,
        roles,
      },
    };
  },
  loginStatus: (username: string) =>
    get<LoginRisk>('/admin/auth/login-status', { username }),
  captcha: () => get<CaptchaPayload>('/admin/auth/captcha'),
  me: async (): Promise<LoginResult> => {
    const vo = await get<LoginVo>('/admin/auth/me');
    const permissions = vo.permissions?.length ? vo.permissions : [];
    return {
      token: vo.token || '',
      permissions,
      user: {
        id: sid(vo.userId),
        username: vo.displayName || 'admin',
        nickname: vo.displayName || 'admin',
        roles: vo.roles ?? [],
      },
    };
  },
};

export const dashboardApi = {
  stats: async (): Promise<DashboardStats> => get<DashboardStats>('/admin/dashboard/stats'),
};

export const userApi = {
  page: (params: PageQuery) =>
    get<PageResult<User>>('/admin/users', toPageParams(params)).then((r) =>
      asPage(r, params.page, params.pageSize),
    ),
  detail: (id: string) => get<User>(`/admin/users/${id}`),
};

export const categoryApi = {
  list: async (): Promise<Category[]> => {
    const list = await get<Record<string, unknown>[]>('/admin/categories');
    return (list || []).map(mapCategory);
  },
  page: async (params?: PageQuery): Promise<PageResult<Category>> => {
    const list = await categoryApi.list();
    const keyword = params?.keyword?.trim().toLowerCase();
    const filtered = keyword
      ? list.filter((c) => c.name.toLowerCase().includes(keyword))
      : list;
    const page = params?.page ?? 1;
    const pageSize = params?.pageSize ?? 10;
    const start = (page - 1) * pageSize;
    return {
      records: filtered.slice(start, start + pageSize),
      total: filtered.length,
      page,
      pageSize,
    };
  },
  create: async (data: Partial<Category>) => {
    const raw = await post<Record<string, unknown>>('/admin/categories', {
      name: data.name,
      icon: data.icon,
      sortOrder: data.sortOrder ?? 0,
      status: data.status ?? (data.enabled === false ? 'DISABLED' : 'ENABLED'),
    });
    return mapCategory(raw);
  },
  update: async (id: string, data: Partial<Category>) => {
    const raw = await put<Record<string, unknown>>(`/admin/categories/${id}`, {
      name: data.name,
      icon: data.icon,
      sortOrder: data.sortOrder ?? 0,
      status: data.status ?? (data.enabled === false ? 'DISABLED' : 'ENABLED'),
    });
    return mapCategory(raw);
  },
  remove: (id: string) => del<void>(`/admin/categories/${id}`),
};

function toDishSaveBody(data: Partial<Dish>) {
  const images = Array.isArray(data.images)
    ? data.images
        .map((img) => (typeof img === 'string' ? img : img.url))
        .filter((url): url is string => Boolean(url))
    : undefined;
  return {
    categoryId: data.categoryId,
    name: data.name,
    subtitle: data.subtitle,
    description: data.description,
    coverImage: data.coverImage || data.coverUrl,
    sortOrder: data.sortOrder,
    recommend: data.recommend ?? data.recommended,
    stockType: data.stockType,
    stock: data.stock,
    unit: data.unit,
    images,
  };
}

export const dishApi = {
  page: async (params: PageQuery & { categoryId?: string; status?: string }) => {
    const raw = await get<PageResult<Record<string, unknown>>>(
      '/admin/dishes',
      toPageParams(params),
    );
    const page = asPage(raw, params.page, params.pageSize);
    return { ...page, records: page.records.map(mapDish) };
  },
  detail: async (id: string) => mapDish(await get<Record<string, unknown>>(`/admin/dishes/${id}`)),
  create: async (data: Partial<Dish>) => {
    const raw = await post<Record<string, unknown>>('/admin/dishes', toDishSaveBody(data));
    return mapDish(raw);
  },
  update: async (id: string, data: Partial<Dish>) => {
    const raw = await put<Record<string, unknown>>(`/admin/dishes/${id}`, toDishSaveBody(data));
    return mapDish(raw);
  },
  remove: (id: string) => del<void>(`/admin/dishes/${id}`),
  publish: async (id: string) =>
    mapDish(await post<Record<string, unknown>>(`/admin/dishes/${id}/publish`)),
  unpublish: async (id: string) =>
    mapDish(await post<Record<string, unknown>>(`/admin/dishes/${id}/unpublish`)),
};

function toRecipeSaveBody(data: Partial<DishRecipe>) {
  return {
    dishId: data.dishId,
    description: data.description,
    difficulty: data.difficulty,
    prepareMinutes: data.prepareMinutes,
    cookMinutes: data.cookMinutes,
    servings: data.servings,
    ingredients: data.ingredients,
    seasonings: data.seasonings,
    steps: (data.steps || []).map((s, i) => ({
      step: s.step ?? i + 1,
      title: s.title,
      description: s.description || s.content || '',
      image: s.image || s.imageUrl,
    })),
    tips: data.tips,
    nutrition: typeof data.nutrition === 'string' ? undefined : data.nutrition,
  };
}

export const recipeApi = {
  list: async (dishId?: string): Promise<DishRecipe[]> => {
    const raw = await get<Record<string, unknown>[]>(
      '/admin/recipes',
      dishId ? { dishId } : undefined,
    );
    return (raw || []).map(mapRecipe);
  },
  page: async (params?: PageQuery): Promise<PageResult<DishRecipe>> => {
    const raw = await get<PageResult<Record<string, unknown>> | Record<string, unknown>[]>(
      '/admin/recipes',
      toPageParams(params),
    );
    if (Array.isArray(raw)) {
      const page = params?.page ?? 1;
      const pageSize = params?.pageSize ?? 10;
      const start = (page - 1) * pageSize;
      const mapped = raw.map(mapRecipe);
      return {
        records: mapped.slice(start, start + pageSize),
        total: mapped.length,
        page,
        pageSize,
      };
    }
    const page = asPage(raw, params?.page, params?.pageSize);
    return { ...page, records: page.records.map(mapRecipe) };
  },
  detail: async (id: string) =>
    mapRecipe(await get<Record<string, unknown>>(`/admin/recipes/${id}`)),
  create: async (data: Partial<DishRecipe>) => {
    const raw = await post<Record<string, unknown>>('/admin/recipes', toRecipeSaveBody(data));
    return mapRecipe(raw);
  },
  update: async (id: string, data: Partial<DishRecipe>) => {
    const raw = await put<Record<string, unknown>>(`/admin/recipes/${id}`, toRecipeSaveBody(data));
    return mapRecipe(raw);
  },
  remove: (id: string) => del<void>(`/admin/recipes/${id}`),
};

export const orderApi = {
  page: async (params: PageQuery & { status?: string }) => {
    const raw = await get<PageResult<Record<string, unknown>>>(
      '/admin/orders',
      toPageParams(params),
    );
    const page = asPage(raw, params.page, params.pageSize);
    return { ...page, records: page.records.map(mapOrder) };
  },
  detail: async (id: string) => mapOrder(await get<Record<string, unknown>>(`/admin/orders/${id}`)),
  updateStatus: async (id: string, status: OrderStatus) =>
    mapOrder(await put<Record<string, unknown>>(`/admin/orders/${id}/status`, { status })),
};

export const commentApi = {
  page: async (params: PageQuery & { hidden?: boolean; status?: string }) => {
    let status = params.status;
    if (params.hidden === true) status = 'HIDDEN';
    if (params.hidden === false) status = 'NORMAL';
    const raw = await get<PageResult<Record<string, unknown>>>(
      '/admin/comments',
      toPageParams({ ...params, status }),
    );
    const page = asPage(raw, params.page, params.pageSize);
    return { ...page, records: page.records.map(mapComment) };
  },
  hide: async (id: string) =>
    mapComment(await post<Record<string, unknown>>(`/admin/comments/${id}/hide`)),
  restore: async (id: string) =>
    mapComment(await post<Record<string, unknown>>(`/admin/comments/${id}/restore`)),
  remove: (id: string) => del<void>(`/admin/comments/${id}`),
  rebuildRating: (dishId: string) => post(`/admin/comments/rebuild-rating/${dishId}`),
};

export const operationLogApi = {
  page: (params?: PageQuery) =>
    get<PageResult<OperationLog>>('/admin/operation-logs', toPageParams(params)).then((r) =>
      asPage(r, params?.page, params?.pageSize),
    ),
};
