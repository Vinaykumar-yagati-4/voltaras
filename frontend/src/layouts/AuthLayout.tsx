import { Link, Outlet } from 'react-router-dom'
import { VoltarasMark } from '@/components/branding/VoltarasLogo'
import { OfflineBanner } from '@/components/ui/OfflineBanner'

export function AuthLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <OfflineBanner />
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" aria-label="VOLTARAS home" className="inline-flex items-center gap-2">
            <VoltarasMark />
            <span className="text-lg font-bold tracking-wide text-navy-900">VOLTARAS</span>
          </Link>
          <Link
            to="/"
            className="text-sm font-medium text-navy-700 transition-colors hover:text-volt-600"
          >
            Back to home
          </Link>
        </div>
      </header>

      <main className="flex flex-1 items-start justify-center px-4 py-10 sm:py-16">
        <Outlet />
      </main>

      <footer className="py-6 text-center text-xs text-slate-500">
        VOLTARAS — internal demonstration and testing platform. All data is fictional.
      </footer>
    </div>
  )
}
