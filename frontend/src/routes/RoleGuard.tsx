import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import type { Role } from '@/types/auth'

export function RoleGuard({ role, children }: { role: Role; children: ReactNode }) {
  const { user } = useAuth()

  if (user?.role !== role) {
    return <Navigate to="/unauthorized" replace />
  }

  return <>{children}</>
}
