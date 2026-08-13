export type Role = 'ADMIN' | 'CONSUMER'

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  refreshTokenExpiresIn: number
}

/** Mirror of auth-service AuthResponse. */
export interface LoginResponse extends AuthTokens {
  userId: number
  role: Role
  email: string
  fullName: string
  message: string | null
}

/** Mirror of auth-service RefreshTokenResponse. */
export type RefreshTokenResponse = AuthTokens

/** Mirror of auth-service RegisterRequest (exact field names). */
export interface RegisterPayload {
  fullName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
  address: string
}

export interface AuthUser {
  userId: number
  email: string
  fullName: string
  role: Role
}

export interface ForgotPasswordResult {
  message: string
}

export interface ResetPasswordResult {
  message: string
}
