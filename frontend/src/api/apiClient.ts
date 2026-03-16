import type { ErrorResponse } from '@/types/member'

type TokenGetter = () => string | null
type TokenRefresher = () => Promise<string | null>
type LogoutHandler = () => void

let getAccessToken: TokenGetter = () => null
let refreshAccessToken: TokenRefresher = async () => null
let onLogout: LogoutHandler = () => {}

export function configureApiClient(
  tokenGetter: TokenGetter,
  tokenRefresher: TokenRefresher,
  logoutHandler: LogoutHandler
) {
  getAccessToken = tokenGetter
  refreshAccessToken = tokenRefresher
  onLogout = logoutHandler
}

// 동시 갱신 요청 방지를 위한 큐
let isRefreshing = false
let refreshQueue: Array<{
  resolve: (token: string | null) => void
  reject: (error: unknown) => void
}> = []

function processQueue(token: string | null, error?: unknown) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  refreshQueue = []
}

export async function apiRequest<T>(
  url: string,
  options: RequestInit = {},
  skipAuth = false,
  isRetry = false
): Promise<T> {
  const headers = new Headers(options.headers)

  if (!skipAuth) {
    const token = getAccessToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(url, { ...options, headers })

  if (response.ok) {
    // 204 No Content 등
    if (response.status === 204) return undefined as T
    return response.json()
  }

  const errorData: ErrorResponse = await response.json()

  // TOKEN_EXPIRED이고 재시도가 아닌 경우 자동 갱신
  if (errorData.code === 'TOKEN_EXPIRED' && !isRetry && !skipAuth) {
    if (isRefreshing) {
      // 이미 갱신 중이면 큐에 대기
      const newToken = await new Promise<string | null>((resolve, reject) => {
        refreshQueue.push({ resolve, reject })
      })
      if (newToken) {
        return apiRequest<T>(url, options, skipAuth, true)
      }
      throw errorData
    }

    isRefreshing = true
    try {
      const newToken = await refreshAccessToken()
      isRefreshing = false

      if (newToken) {
        processQueue(newToken)
        return apiRequest<T>(url, options, skipAuth, true)
      } else {
        processQueue(null, errorData)
        onLogout()
        throw errorData
      }
    } catch (err) {
      isRefreshing = false
      processQueue(null, err)
      onLogout()
      throw err
    }
  }

  throw errorData
}
