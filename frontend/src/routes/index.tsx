import { createBrowserRouter } from 'react-router-dom'
import { AdminLayout } from '@/layouts/AdminLayout'
import { AuthLayout } from '@/layouts/AuthLayout'
import { ConsumerLayout } from '@/layouts/ConsumerLayout'
import { PublicLayout } from '@/layouts/PublicLayout'
import { AdminDashboard } from '@/pages/admin/AdminDashboard'
import { ConsumerDashboard } from '@/pages/consumer/ConsumerDashboard'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { LandingPage } from '@/pages/LandingPage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { UnauthorizedPage } from '@/pages/UnauthorizedPage'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleGuard } from './RoleGuard'

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [{ path: '/', element: <LandingPage /> }],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/reset-password', element: <ResetPasswordPage /> },
    ],
  },
  {
    path: '/consumer',
    element: (
      <ProtectedRoute>
        <RoleGuard role="CONSUMER">
          <ConsumerLayout />
        </RoleGuard>
      </ProtectedRoute>
    ),
    children: [{ index: true, element: <ConsumerDashboard /> }],
  },
  {
    path: '/admin',
    element: (
      <ProtectedRoute>
        <RoleGuard role="ADMIN">
          <AdminLayout />
        </RoleGuard>
      </ProtectedRoute>
    ),
    children: [{ index: true, element: <AdminDashboard /> }],
  },
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  { path: '*', element: <NotFoundPage /> },
])
