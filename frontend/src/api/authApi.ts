import { apiRequest } from '@/api/apiClient'
import type { LoginRequest, AuthTokenResponse, UserInfo } from '@/types/auth'

export async function loginApi(request: LoginRequest): Promise<AuthTokenResponse> {
  return apiRequest<AuthTokenResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  }, true)
}

export async function refreshTokenApi(refreshToken: string): Promise<AuthTokenResponse> {
  return apiRequest<AuthTokenResponse>('/api/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  }, true)
}

export async function logoutApi(): Promise<void> {
  await apiRequest<{ message: string }>('/api/v1/auth/logout', {
    method: 'POST',
  })
}

export async function getMeApi(): Promise<UserInfo> {
  return apiRequest<UserInfo>('/api/v1/members/me')
}
