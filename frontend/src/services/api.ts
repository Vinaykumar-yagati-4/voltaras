import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ApiError, type ApiErrorPayload } from '@/types/api'
import type { RefreshTokenResponse } from '@/types/auth'
import { tokenStore } from '@/utils/token'

/**
 * All frontend traffic goes through the API Gateway. The base URL is
 * configured via VITE_API_BASE_URL (default: the local Docker Gateway).
 */
export const API_BASE_URL: string =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const UNAUTHORIZED_EVENT = 'voltaras:unauthorized'

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

// Attach the bearer token to every request.
api.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Public auth endpoints that must never be retried with a refreshed token
 * (login/register/refresh would otherwise loop or leak a stale token).
 */
const PUBLIC_AUTH_PATHS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh-token',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
]

function isPublicAuthUrl(url?: string): boolean {
  if (!url) return false
  return PUBLIC_AUTH_PATHS.some((p) => url.includes(p))
}

/** Normalize any thrown error into our ApiError shape. */
export function normalizeError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorPayload>
    const status = axiosError.response?.status ?? 0
    const data = axiosError.response?.data
    const fieldErrors = data?.error?.details ?? []
    const message =
      data?.error?.message ?? data?.message ?? axiosError.message ?? 'Something went wrong'
    return new ApiError(message, status, data?.error?.code, fieldErrors, data)
  }
  const message = error instanceof Error ? error.message : 'Something went wrong'
  return new ApiError(message, 0)
}

/** Refresh the access token once; on failure clears the session. */
async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.getRefreshToken()
  if (!refreshToken) return null
  try {
    const { data } = await axios.post<RefreshTokenResponse>(
      `${API_BASE_URL}/api/auth/refresh-token`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' }, timeout: 15_000 },
    )
    tokenStore.setTokens(data.accessToken, data.refreshToken)
    return data.accessToken
  } catch {
    tokenStore.clear()
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    return null
  }
}

let refreshPromise: Promise<string | null> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const original = (
      (error as AxiosError).config ?? {}
    ) as InternalAxiosRequestConfig & { _retry?: boolean }

    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !isPublicAuthUrl(original.url)
    ) {
      original._retry = true
      refreshPromise = refreshPromise ?? refreshAccessToken()
      const newToken = await refreshPromise
      refreshPromise = null
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      }
    }
    return Promise.reject(normalizeError(error))
  },
)

export default api
