import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { useAuth } from './useAuth'
import { AuthProvider } from '@/context/AuthProvider'

// API mock
vi.mock('@/api/authApi', () => ({
  loginApi: vi.fn(),
  refreshTokenApi: vi.fn(),
  logoutApi: vi.fn(),
  getMeApi: vi.fn(),
}))

vi.mock('@/api/apiClient', () => ({
  configureApiClient: vi.fn(),
}))

import { loginApi, getMeApi, logoutApi } from '@/api/authApi'

const wrapper = ({ children }: { children: ReactNode }) =>
  createElement(AuthProvider, null, children)

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('초기 상태 - 미인증', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper })

    // isLoading이 false가 될 때까지 대기
    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
    expect(result.current.accessToken).toBeNull()
  })

  it('login 후 인증 상태 변경', async () => {
    const mockLoginApi = vi.mocked(loginApi)
    const mockGetMeApi = vi.mocked(getMeApi)

    mockLoginApi.mockResolvedValue({
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 1800,
    })
    mockGetMeApi.mockResolvedValue({
      id: 1,
      email: 'user@example.com',
      nickname: 'testuser',
    })

    const { result } = renderHook(() => useAuth(), { wrapper })

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    await act(async () => {
      await result.current.login('user@example.com', 'password123')
    })

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user).toEqual({
      id: 1,
      email: 'user@example.com',
      nickname: 'testuser',
    })
    expect(result.current.accessToken).toBe('test-access-token')
    expect(localStorage.getItem('refreshToken')).toBe('test-refresh-token')
  })

  it('logout 후 상태 초기화', async () => {
    const mockLoginApi = vi.mocked(loginApi)
    const mockGetMeApi = vi.mocked(getMeApi)
    const mockLogoutApi = vi.mocked(logoutApi)

    mockLoginApi.mockResolvedValue({
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      expiresIn: 1800,
    })
    mockGetMeApi.mockResolvedValue({
      id: 1,
      email: 'user@example.com',
      nickname: 'testuser',
    })
    mockLogoutApi.mockResolvedValue(undefined)

    const { result } = renderHook(() => useAuth(), { wrapper })

    await vi.waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    // 로그인
    await act(async () => {
      await result.current.login('user@example.com', 'password123')
    })

    expect(result.current.isAuthenticated).toBe(true)

    // 로그아웃
    await act(async () => {
      await result.current.logout()
    })

    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
    expect(result.current.accessToken).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('AuthProvider 없이 useAuth 호출 시 에러', () => {
    expect(() => {
      renderHook(() => useAuth())
    }).toThrow('useAuth must be used within an AuthProvider')
  })
})
