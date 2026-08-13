import { Link } from 'react-router-dom'
import { FileQuestion } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="max-w-md text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-card bg-slate-100">
          <FileQuestion className="h-7 w-7 text-slate-500" aria-hidden="true" />
        </div>
        <p className="mt-4 text-sm font-semibold uppercase tracking-wide text-slate-500">404 · Page not found</p>
        <h1 className="mt-2 text-2xl font-bold text-navy-900">This page is not available</h1>
        <p className="mt-2 text-sm text-slate-500">
          The page you are looking for does not exist or may have been moved.
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
