import { create } from 'zustand';

import { useAuthStore } from './authStore';



interface PermissionState {

  permissions: string[];

  setPermissions: (permissions: string[]) => void;

  hasPermission: (code: string) => boolean;

  hasAnyPermission: (codes: string[]) => boolean;

  clearPermissions: () => void;

}



function resolvePermissions(local: string[]): string[] {

  if (local.length) return local;

  return useAuthStore.getState().permissions || [];

}



export const usePermissionStore = create<PermissionState>((set, get) => ({

  permissions: [],

  setPermissions: (permissions) => {

    useAuthStore.getState().setPermissions(permissions);

    set({ permissions });

  },

  hasPermission: (code) => {

    const permissions = resolvePermissions(get().permissions);

    return permissions.includes('*') || permissions.includes(code);

  },

  hasAnyPermission: (codes) => {

    const permissions = resolvePermissions(get().permissions);

    if (permissions.includes('*')) return true;

    return codes.some((code) => permissions.includes(code));

  },

  clearPermissions: () => {

    useAuthStore.getState().setPermissions([]);

    set({ permissions: [] });

  },

}));


