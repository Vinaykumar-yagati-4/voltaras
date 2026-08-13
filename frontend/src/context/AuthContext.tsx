import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import * as authApi from '@/services/auth'
import { UNAUTHORIZED_EVENT } from '@/services/api'
import { isTokenExpired } from '@/utils/jwt'
import { tokenStore } from '@/utils/token'
import type { AuthUser, RegisterPayload } from '@/types/auth'

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  isInitializing: boolean
  login: (email: string, password: string) => Promise<AuthUser>
  registerAndLogin: (payload: RegisterPayload) => Promise<AuthUser>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => tokenStore.getUser())
  const [isInitializing, setIsInitializing] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function restoreSession() {
      const accessToken = tokenStore.getAccessToken()
      const storedUser = tokenStore.getUser()

      if (!accessToken || !storedUser) {
        tokenStore.clear()
        if (!cancelled) setIsInitializing(false)
        return
      }

      if (!isTokenExpired(accessToken)) {
        if (!cancelled) {
          setUser(storedUser)
          setIsInitializing(false)
        }
        return
      }

      // Access token expired — try one refresh with the stored refresh token.
      const refreshToken = tokenStore.getRefreshToken()
      if (!refreshToken) {
        tokenStore.clear()
        if (!cancelled) setIsInitializing(false)
        return
      }
      try {
        const tokens = await authApi.refreshTokens(refreshToken)
        tokenStore.setTokens(tokens.accessToken, tokens.refreshToken)
        if (!cancelled) {
          setUser(storedUser)
          setIsInitializing(false)
        }
      } catch {
        tokenStore.clear()
        if (!cancelled) setIsInitializing(false)
      }
    }

    void restoreSession()

    const onUnauthorized = () => {
      setUser(null)
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => {
      cancelled = true
      window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    }
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password)
    const nextUser = authApi.userFromLogin(response)
    tokenStore.setTokens(response.accessToken, response.refreshToken)
    tokenStore.setUser(nextUser)
    setUser(nextUser)
    return nextUser
  }, [])

  const registerAndLogin = useCallback(async (payload: RegisterPayload) => {
    const response = await authApi.register(payload)
    const nextUser = authApi.userFromLogin(response)
    tokenStore.setTokens(response.accessToken, response.refreshToken)
    tokenStore.setUser(nextUser)
    setUser(nextUser)
    return nextUser
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
    tokenStore.clear()
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isInitializing,
      login,
      registerAndLogin,
      logout,
    }),
    [user, isInitializing, login, registerAndLogin, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuthContext must be used within an AuthProvider')
  }
  return ctx
}
