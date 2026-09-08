import { create } from 'zustand';
import type { MenuTreeNode } from '@/modules/iam/api';

interface MenuState {
  tree: MenuTreeNode[];
  /** path → permissionCode（来自后端菜单，可驱动路由守卫） */
  pathPermissions: Record<string, string>;
  setMenus: (tree: MenuTreeNode[]) => void;
  clear: () => void;
  permissionForPath: (pathname: string) => string | undefined;
}

function flattenPathPermissions(
  nodes: MenuTreeNode[],
  acc: Record<string, string> = {},
): Record<string, string> {
  for (const node of nodes) {
    if (node.path && node.permissionCode) {
      acc[node.path] = node.permissionCode;
    }
    if (node.children?.length) {
      flattenPathPermissions(node.children, acc);
    }
  }
  return acc;
}

/** 精确匹配 path → permissionCode（详情/创建等子路由走路由表兜底权限） */
function matchPermission(
  map: Record<string, string>,
  pathname: string,
): string | undefined {
  return map[pathname];
}

export const useMenuStore = create<MenuState>((set, get) => ({
  tree: [],
  pathPermissions: {},
  setMenus: (tree) =>
    set({
      tree,
      pathPermissions: flattenPathPermissions(tree),
    }),
  clear: () => set({ tree: [], pathPermissions: {} }),
  permissionForPath: (pathname) => matchPermission(get().pathPermissions, pathname),
}));
