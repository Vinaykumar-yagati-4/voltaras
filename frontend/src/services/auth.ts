import api from '@/services/api'
import type {
  AuthUser,
  ForgotPasswordResult,
  LoginResponse,
  RefreshTokenResponse,
  RegisterPayload,
  ResetPasswordResult,
} from '@/types/auth'

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/auth/login', { email, password })
  return data
}

export async function register(payload: RegisterPayload): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/auth/register', payload)
  return data
}

export async function logout(): Promise<void> {
  try {
    await api.post('/api/auth/logout')
  } catch {
    // Logout is best-effort; the client session is cleared regardless.
  }
}

export async function forgotPassword(email: string): Promise<ForgotPasswordResult> {
  const { data } = await api.post<ForgotPasswordResult>('/api/auth/forgot-password', { email })
  return data
}

export async function resetPassword(payload: {
  token: string
  newPassword: string
  confirmNewPassword: string
}): Promise<ResetPasswordResult> {
  const { data } = await api.post<ResetPasswordResult>('/api/auth/reset-password', payload)
  return data
}

export async function refreshTokens(refreshToken: string): Promise<RefreshTokenResponse> {
  const { data } = await api.post<RefreshTokenResponse>('/api/auth/refresh-token', {
    refreshToken,
  })
  return data
}

/** Build the cached AuthUser from a login/register response. */
export function userFromLogin(response: LoginResponse): AuthUser {
  return {
    userId: response.userId,
    email: response.email,
    fullName: response.fullName,
    role: response.role,
  }
}
