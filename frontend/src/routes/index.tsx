import { lazy, Suspense } from 'react'
import { createBrowserRouter } from 'react-router-dom'
import { AdminLayout } from '@/layouts/AdminLayout'
import { AuthLayout } from '@/layouts/AuthLayout'
import { ConsumerLayout } from '@/layouts/ConsumerLayout'
import { PublicLayout } from '@/layouts/PublicLayout'
import { LoadingState } from '@/components/ui/LoadingState'
import { AdminDashboard } from '@/pages/admin/AdminDashboard'
import { ForgotPasswordPage } from '@/pages/ForgotPasswordPage'
import { LandingPage } from '@/pages/LandingPage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ResetPasswordPage } from '@/pages/ResetPasswordPage'
import { UnauthorizedPage } from '@/pages/UnauthorizedPage'
import { ProtectedRoute } from './ProtectedRoute'
import { RoleGuard } from './RoleGuard'

// Consumer portal pages are code-split so the auth/landing bundle stays lean.
const ConsumerDashboard = lazy(() =>
  import('@/pages/consumer/ConsumerDashboard').then((m) => ({ default: m.ConsumerDashboard })),
)
const ProfilePage = lazy(() =>
  import('@/pages/consumer/ProfilePage').then((m) => ({ default: m.ProfilePage })),
)
const BillsPage = lazy(() =>
  import('@/pages/consumer/BillsPage').then((m) => ({ default: m.BillsPage })),
)
const BillDetailPage = lazy(() =>
  import('@/pages/consumer/BillDetailPage').then((m) => ({ default: m.BillDetailPage })),
)
const WalletPage = lazy(() =>
  import('@/pages/consumer/WalletPage').then((m) => ({ default: m.WalletPage })),
)
const PaymentsPage = lazy(() =>
  import('@/pages/consumer/PaymentsPage').then((m) => ({ default: m.PaymentsPage })),
)
const PaymentDetailPage = lazy(() =>
  import('@/pages/consumer/PaymentDetailPage').then((m) => ({ default: m.PaymentDetailPage })),
)
const ReadingsPage = lazy(() =>
  import('@/pages/consumer/ReadingsPage').then((m) => ({ default: m.ReadingsPage })),
)
const ComplaintsPage = lazy(() =>
  import('@/pages/consumer/ComplaintsPage').then((m) => ({ default: m.ComplaintsPage })),
)
const NewComplaintPage = lazy(() =>
  import('@/pages/consumer/NewComplaintPage').then((m) => ({ default: m.NewComplaintPage })),
)
const ComplaintDetailPage = lazy(() =>
  import('@/pages/consumer/ComplaintDetailPage').then((m) => ({ default: m.ComplaintDetailPage })),
)
const NotificationsPage = lazy(() =>
  import('@/pages/consumer/NotificationsPage').then((m) => ({ default: m.NotificationsPage })),
)

// Admin portal pages are code-split so the auth/landing bundle stays lean.
const AdminComplaintsPage = lazy(() =>
  import('@/pages/admin/AdminComplaintsPage').then((m) => ({ default: m.AdminComplaintsPage })),
)
const AdminComplaintDetailPage = lazy(() =>
  import('@/pages/admin/AdminComplaintDetailPage').then((m) => ({
    default: m.AdminComplaintDetailPage,
  })),
)
const AdminOrganizationsPage = lazy(() =>
  import('@/pages/admin/AdminOrganizationsPage').then((m) => ({
    default: m.AdminOrganizationsPage,
  })),
)
const AdminOrganizationDetailPage = lazy(() =>
  import('@/pages/admin/AdminOrganizationDetailPage').then((m) => ({
    default: m.AdminOrganizationDetailPage,
  })),
)
const AdminNotificationsPage = lazy(() =>
  import('@/pages/admin/AdminNotificationsPage').then((m) => ({
    default: m.AdminNotificationsPage,
  })),
)

const consumerFallback = (
  <div className="py-10">
    <LoadingState label="Loading…" />
  </div>
)

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
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={consumerFallback}>
            <ConsumerDashboard />
          </Suspense>
        ),
      },
      {
        path: 'profile',
        element: (
          <Suspense fallback={consumerFallback}>
            <ProfilePage />
          </Suspense>
        ),
      },
      {
        path: 'bills',
        element: (
          <Suspense fallback={consumerFallback}>
            <BillsPage />
          </Suspense>
        ),
      },
      {
        path: 'bills/:billId',
        element: (
          <Suspense fallback={consumerFallback}>
            <BillDetailPage />
          </Suspense>
        ),
      },
      {
        path: 'wallet',
        element: (
          <Suspense fallback={consumerFallback}>
            <WalletPage />
          </Suspense>
        ),
      },
      {
        path: 'payments',
        element: (
          <Suspense fallback={consumerFallback}>
            <PaymentsPage />
          </Suspense>
        ),
      },
      {
        path: 'payments/:paymentId',
        element: (
          <Suspense fallback={consumerFallback}>
            <PaymentDetailPage />
          </Suspense>
        ),
      },
      {
        path: 'readings',
        element: (
          <Suspense fallback={consumerFallback}>
            <ReadingsPage />
          </Suspense>
        ),
      },
      {
        path: 'complaints',
        element: (
          <Suspense fallback={consumerFallback}>
            <ComplaintsPage />
          </Suspense>
        ),
      },
      {
        path: 'complaints/new',
        element: (
          <Suspense fallback={consumerFallback}>
            <NewComplaintPage />
          </Suspense>
        ),
      },
      {
        path: 'complaints/:complaintId',
        element: (
          <Suspense fallback={consumerFallback}>
            <ComplaintDetailPage />
          </Suspense>
        ),
      },
      {
        path: 'notifications',
        element: (
          <Suspense fallback={consumerFallback}>
            <NotificationsPage />
          </Suspense>
        ),
      },
    ],
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
    children: [
      { index: true, element: <AdminDashboard /> },
      {
        path: 'complaints',
        element: (
          <Suspense fallback={consumerFallback}>
            <AdminComplaintsPage />
          </Suspense>
        ),
      },
      {
        path: 'complaints/:complaintId',
        element: (
          <Suspense fallback={consumerFallback}>
            <AdminComplaintDetailPage />
          </Suspense>
        ),
      },
      {
        path: 'organizations',
        element: (
          <Suspense fallback={consumerFallback}>
            <AdminOrganizationsPage />
          </Suspense>
        ),
      },
      {
        path: 'organizations/:organizationId',
        element: (
          <Suspense fallback={consumerFallback}>
            <AdminOrganizationDetailPage />
          </Suspense>
        ),
      },
      {
        path: 'notifications',
        element: (
          <Suspense fallback={consumerFallback}>
            <AdminNotificationsPage />
          </Suspense>
        ),
      },
    ],
  },
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  { path: '*', element: <NotFoundPage /> },
])
