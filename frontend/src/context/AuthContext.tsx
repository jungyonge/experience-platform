import { createContext } from 'react'
import type { AuthState, UserInfo } from '@/types/auth'

export interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  setUser: (user: UserInfo, accessToken: string) => void
  updateUser: (partial: Partial<UserInfo>) => void
}

export const initialState: AuthState = {
  isAuthenticated: false,
  user: null,
  accessToken: null,
  isLoading: true,
}

export const AuthContext = createContext<AuthContextType | null>(null)
