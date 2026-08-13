import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Menu, X } from 'lucide-react'
import { VoltarasLogo } from '@/components/branding/VoltarasLogo'
import { OfflineBanner } from '@/components/ui/OfflineBanner'
import { useAuth } from '@/hooks/useAuth'

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/register', label: 'Get started' },
]

export function PublicLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { isAuthenticated } = useAuth()

  const primaryCta = isAuthenticated
    ? { to: '/consumer', label: 'Open dashboard' }
    : { to: '/login', label: 'Sign in' }

  return (
    <div className="flex min-h-screen flex-col">
      <OfflineBanner />
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/" aria-label="VOLTARAS home" onClick={() => setMenuOpen(false)}>
            <VoltarasLogo />
          </Link>

          <nav className="hidden items-center gap-6 md:flex" aria-label="Primary">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `text-sm font-medium transition-colors hover:text-volt-600 ${
                    isActive ? 'text-volt-600' : 'text-navy-700'
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
            <Link
              to={primaryCta.to}
              className="inline-flex h-11 items-center rounded-md bg-volt-600 px-4 text-sm font-medium text-white transition-colors hover:bg-volt-700"
            >
              {primaryCta.label}
            </Link>
          </nav>

          <button
            type="button"
            className="inline-flex h-11 w-11 items-center justify-center rounded-md text-navy-800 hover:bg-slate-100 md:hidden"
            onClick={() => setMenuOpen((open) => !open)}
            aria-expanded={menuOpen}
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            title={menuOpen ? 'Close menu' : 'Open menu'}
          >
            {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>

        {menuOpen && (
          <nav
            className="border-t border-slate-100 bg-white px-4 py-3 md:hidden"
            aria-label="Mobile"
          >
            <ul className="flex flex-col gap-1">
              {navLinks.map((link) => (
                <li key={link.to}>
                  <Link
                    to={link.to}
                    onClick={() => setMenuOpen(false)}
                    className="block rounded-md px-3 py-3 text-sm font-medium text-navy-800 hover:bg-slate-50"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
              <li>
                <Link
                  to={primaryCta.to}
                  onClick={() => setMenuOpen(false)}
                  className="mt-1 block rounded-md bg-volt-600 px-3 py-3 text-center text-sm font-medium text-white hover:bg-volt-700"
                >
                  {primaryCta.label}
                </Link>
              </li>
            </ul>
          </nav>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="bg-navy-950 text-slate-300">
        <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
          <div className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
            <div className="max-w-sm">
              <VoltarasLogo light />
              <p className="mt-3 text-sm text-slate-400">
                A modern electricity utility platform for smart metering, transparent
                billing and instant payments.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-8 text-sm sm:grid-cols-3">
              <div>
                <p className="font-semibold text-white">Platform</p>
                <ul className="mt-3 space-y-2">
                  <li><Link to="/login" className="hover:text-white">Sign in</Link></li>
                  <li><Link to="/register" className="hover:text-white">Create account</Link></li>
                </ul>
              </div>
              <div>
                <p className="font-semibold text-white">Company</p>
                <ul className="mt-3 space-y-2">
                  <li><Link to="/" className="hover:text-white">About</Link></li>
                  <li><Link to="/" className="hover:text-white">Contact</Link></li>
                </ul>
              </div>
              <div>
                <p className="font-semibold text-white">Resources</p>
                <ul className="mt-3 space-y-2">
                  <li><Link to="/forgot-password" className="hover:text-white">Forgot password</Link></li>
                  <li><Link to="/reset-password" className="hover:text-white">Reset password</Link></li>
                </ul>
              </div>
            </div>
          </div>
          <div className="mt-10 flex flex-col gap-2 border-t border-navy-800 pt-6 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <p>© {new Date().getFullYear()} VOLTARAS. All rights reserved.</p>
            <p>Internal demonstration and testing platform — all data is fictional.</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
