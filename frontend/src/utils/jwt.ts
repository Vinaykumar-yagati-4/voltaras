export interface JwtPayload {
  sub?: string
  email?: string
  role?: string
  userId?: number
  exp?: number
  iat?: number
  [key: string]: unknown
}

/**
 * Decodes the payload of a JWT without verifying the signature.
 * Used only to inspect the `exp` claim locally; the backend remains
 * the authority for authentication.
 */
export function decodeJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    )
    return JSON.parse(json) as JwtPayload
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeJwt(token)
  if (!payload || typeof payload.exp !== 'number') return true
  return payload.exp * 1000 <= Date.now()
}
