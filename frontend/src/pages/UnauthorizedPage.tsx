import { Link } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="max-w-md text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-card bg-red-50">
          <ShieldAlert className="h-7 w-7 text-red-500" aria-hidden="true" />
        </div>
        <p className="mt-4 text-sm font-semibold uppercase tracking-wide text-red-600">403 · Unauthorized</p>
        <h1 className="mt-2 text-2xl font-bold text-navy-900">You don&apos;t have access</h1>
        <p className="mt-2 text-sm text-slate-500">
          Your account does not have permission to view this page. If you believe this is a
          mistake, please contact your administrator.
        </p>
        <div className="mt-6 flex justify-center gap-3">
          <Link to="/">
            <Button variant="secondary">Back to home</Button>
          </Link>
          <Link to="/login">
            <Button>Sign in</Button>
          </Link>
        </div>
      </div>
    </div>
  )
}
