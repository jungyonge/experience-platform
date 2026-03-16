export interface LoginRequest {
  email: string
  password: string
}

export interface AuthTokenResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface UserInfo {
  id: number
  email: string
  nickname: string
}

export interface AuthState {
  isAuthenticated: boolean
  user: UserInfo | null
  accessToken: string | null
  isLoading: boolean
}

export type AuthAction =
  | { type: 'LOGIN_SUCCESS'; payload: { user: UserInfo; accessToken: string } }
  | { type: 'LOGOUT' }
  | { type: 'TOKEN_REFRESHED'; payload: { accessToken: string } }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'UPDATE_USER'; payload: Partial<UserInfo> }
