import { useReducer, useEffect, useCallback, useRef, type ReactNode } from 'react'
import { AuthContext, initialState } from './AuthContext'
import type { AuthState, AuthAction, UserInfo } from '@/types/auth'
import { loginApi, refreshTokenApi, logoutApi, getMeApi } from '@/api/authApi'
import { configureApiClient } from '@/api/apiClient'

const REFRESH_TOKEN_KEY = 'refreshToken'

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user,
        accessToken: action.payload.accessToken,
        isLoading: false,
      }
    case 'LOGOUT':
      return {
        ...state,
        isAuthenticated: false,
        user: null,
        accessToken: null,
        isLoading: false,
      }
    case 'TOKEN_REFRESHED':
      return {
        ...state,
        accessToken: action.payload.accessToken,
      }
    case 'SET_LOADING':
      return {
        ...state,
        isLoading: action.payload,
      }
    case 'UPDATE_USER':
      return {
        ...state,
        user: state.user ? { ...state.user, ...action.payload } : null,
      }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState)
  const accessTokenRef = useRef<string | null>(null)

  // accessTokenRef를 state와 동기화
  useEffect(() => {
    accessTokenRef.current = state.accessToken
  }, [state.accessToken])

  const handleLogout = useCallback(() => {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    dispatch({ type: 'LOGOUT' })
  }, [])

  const refreshAccessToken = useCallback(async (): Promise<string | null> => {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!storedRefreshToken) return null

    try {
      const tokenResponse = await refreshTokenApi(storedRefreshToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)
      dispatch({ type: 'TOKEN_REFRESHED', payload: { accessToken: tokenResponse.accessToken } })
      return tokenResponse.accessToken
    } catch {
      handleLogout()
      return null
    }
  }, [handleLogout])

  // apiClient 설정
  useEffect(() => {
    configureApiClient(
      () => accessTokenRef.current,
      refreshAccessToken,
      handleLogout
    )
  }, [refreshAccessToken, handleLogout])

  // 앱 초기화: localStorage의 refreshToken으로 자동 갱신 시도
  useEffect(() => {
    const initAuth = async () => {
      const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
      if (!storedRefreshToken) {
        dispatch({ type: 'SET_LOADING', payload: false })
        return
      }

      try {
        const tokenResponse = await refreshTokenApi(storedRefreshToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)
        accessTokenRef.current = tokenResponse.accessToken

        const user = await getMeApi()
        dispatch({
          type: 'LOGIN_SUCCESS',
          payload: { user, accessToken: tokenResponse.accessToken },
        })
      } catch {
        handleLogout()
      }
    }

    initAuth()
  }, [handleLogout])

  const login = useCallback(async (email: string, password: string) => {
    const tokenResponse = await loginApi({ email, password })
    localStorage.setItem(REFRESH_TOKEN_KEY, tokenResponse.refreshToken)
    accessTokenRef.current = tokenResponse.accessToken

    const user = await getMeApi()
    dispatch({
      type: 'LOGIN_SUCCESS',
      payload: { user, accessToken: tokenResponse.accessToken },
    })
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // 서버 로그아웃 실패해도 클라이언트 정리
    }
    handleLogout()
  }, [handleLogout])

  const setUser = useCallback((user: UserInfo, accessToken: string) => {
    dispatch({ type: 'LOGIN_SUCCESS', payload: { user, accessToken } })
  }, [])

  const updateUser = useCallback((partial: Partial<UserInfo>) => {
    dispatch({ type: 'UPDATE_USER', payload: partial })
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, login, logout, setUser, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}
