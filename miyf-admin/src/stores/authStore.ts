import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { clearToken, setToken, getToken } from '@/api/http';
import type { AdminUser } from '@/types';

try {
  if (typeof localStorage !== 'undefined' && !localStorage.getItem('miyf-auth-storage')) {
    const legacy = localStorage.getItem('ck-auth-storage');
    if (legacy) {
      localStorage.setItem('miyf-auth-storage', legacy);
      localStorage.removeItem('ck-auth-storage');
    }
  }
} catch {
  // ignore
}

interface AuthState {
  token: string | null;
  user: AdminUser | null;
  permissions: string[];
  isAuthenticated: boolean;
  login: (token: string, user: AdminUser, permissions?: string[]) => void;
  logout: () => void;
  setUser: (user: AdminUser) => void;
  setPermissions: (permissions: string[]) => void;
  hydrateToken: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      permissions: [],
      isAuthenticated: false,
      login: (token, user, permissions = []) => {
        setToken(token);
        set({ token, user, permissions, isAuthenticated: true });
      },
      logout: () => {
        clearToken();
        set({ token: null, user: null, permissions: [], isAuthenticated: false });
      },
      setUser: (user) => set({ user }),
      setPermissions: (permissions) => set({ permissions }),
      hydrateToken: () => {
        const token = get().token || getToken();
        if (token) {
          setToken(token);
          set({ token, isAuthenticated: true });
        }
      },
    }),
    {
      name: 'miyf-auth-storage',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        permissions: state.permissions,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);
