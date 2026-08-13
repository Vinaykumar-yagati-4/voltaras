import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { LogOut, Menu, X, type LucideIcon } from 'lucide-react'
import { VoltarasMark } from '@/components/branding/VoltarasLogo'
import { Badge } from '@/components/ui/Badge'
import { OfflineBanner } from '@/components/ui/OfflineBanner'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/utils/cn'

export interface AppNavItem {
  to: string
  label: string
  icon: LucideIcon
}

interface AppShellProps {
  navLabel: string
  navItems: AppNavItem[]
  sectionLabel: string
  children: ReactNode
}

function NavList({
  label,
  items,
  onNavigate,
}: {
  label: string
  items: AppNavItem[]
  onNavigate?: () => void
}) {
  return (
    <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label={label}>
      <ul className="space-y-1">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  'flex min-h-11 items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-white/10 text-white'
                    : 'text-slate-300 hover:bg-white/5 hover:text-white',
                )
              }
            >
              <item.icon className="h-5 w-5 shrink-0" aria-hidden="true" />
              <span className="min-w-0 truncate">{item.label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}

function SidebarBody({
  navLabel,
  items,
  onNavigate,
  onSignOut,
}: {
  navLabel: string
  items: AppNavItem[]
  onNavigate?: () => void
  onSignOut: () => void
}) {
  const { user } = useAuth()
  const initial = user?.fullName?.trim().charAt(0).toUpperCase() ?? 'U'

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-16 shrink-0 items-center gap-2.5 border-b border-white/10 px-5">
        <VoltarasMark className="bg-white/10" />
        <span className="text-lg font-bold tracking-wide text-white">VOLTARAS</span>
      </div>

      <NavList label={navLabel} items={items} onNavigate={onNavigate} />

      <div className="border-t border-white/10 p-4">
        <div className="flex items-center gap-3">
          <span
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-volt-600 text-sm font-bold text-white"
            aria-hidden="true"
          >
            {initial}
          </span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold text-white">{user?.fullName}</p>
            <p className="truncate text-xs text-slate-400">{user?.email}</p>
          </div>
          <Badge tone="blue" className="bg-volt-500/20 text-volt-100 ring-volt-400/30">
            {user?.role}
          </Badge>
        </div>
      </div>

      <div className="border-t border-white/10 p-3">
        <button
          type="button"
          onClick={onSignOut}
          className="flex min-h-11 w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-slate-300 transition-colors hover:bg-white/5 hover:text-white"
        >
          <LogOut className="h-5 w-5" aria-hidden="true" />
          Sign out
        </button>
      </div>
    </div>
  )
}

export function AppShell({ navLabel, navItems, sectionLabel, children }: AppShellProps) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const initial = user?.fullName?.trim().charAt(0).toUpperCase() ?? 'U'

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  // Close the drawer with Escape (keyboard accessibility).
  useEffect(() => {
    if (!drawerOpen) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setDrawerOpen(false)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [drawerOpen])

  // Lock page scroll and move focus into the drawer while it is open.
  useEffect(() => {
    if (!drawerOpen) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    closeButtonRef.current?.focus()
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [drawerOpen])

  return (
    <div className="min-h-screen bg-slate-50">
      <OfflineBanner />

      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 bg-navy-900 lg:block">
        <SidebarBody navLabel={navLabel} items={navItems} onSignOut={handleLogout} />
      </aside>

      {/* Content column (right of the sidebar on desktop) */}
      <div className="flex min-h-screen flex-col lg:pl-64">
        {/* Mobile app bar */}
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-slate-200 bg-white px-4 lg:hidden">
          <Link to="/" aria-label="VOLTARAS home" className="flex items-center gap-2">
            <VoltarasMark className="h-7 w-7" />
            <span className="text-base font-bold tracking-wide text-navy-900">VOLTARAS</span>
          </Link>
          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            className="inline-flex h-11 w-11 items-center justify-center rounded-md text-navy-800 transition-colors hover:bg-slate-100"
            aria-label="Open navigation"
            title="Open navigation"
          >
            <Menu className="h-5 w-5" aria-hidden="true" />
          </button>
        </header>

        {/* Desktop top context bar */}
        <header className="sticky top-0 z-20 hidden h-14 items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 lg:flex">
          <div className="min-w-0">
            <h1 className="truncate text-sm font-semibold text-navy-900">{sectionLabel}</h1>
          </div>
          <div className="flex shrink-0 items-center gap-3">
            <div className="hidden text-right md:block">
              <p className="text-xs font-medium text-navy-900">{user?.fullName}</p>
              <p className="max-w-64 truncate text-xs text-slate-500">{user?.email}</p>
            </div>
            <span
              className="flex h-9 w-9 items-center justify-center rounded-md bg-volt-600 text-sm font-bold text-white"
              aria-hidden="true"
            >
              {initial}
            </span>
          </div>
        </header>

        <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-5xl">{children}</div>
        </main>
      </div>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div
          className="fixed inset-0 z-50 lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Navigation"
        >
          <div
            className="absolute inset-0 bg-navy-950/60"
            onClick={() => setDrawerOpen(false)}
            aria-hidden="true"
          />
          <div className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-navy-900 shadow-xl">
            <button
              ref={closeButtonRef}
              type="button"
              onClick={() => setDrawerOpen(false)}
              className="absolute right-3 top-3 inline-flex h-11 w-11 items-center justify-center rounded-md text-slate-300 transition-colors hover:bg-white/10 hover:text-white"
              aria-label="Close navigation"
              title="Close navigation"
            >
              <X className="h-5 w-5" aria-hidden="true" />
            </button>
            <SidebarBody
              navLabel={navLabel}
              items={navItems}
              onNavigate={() => setDrawerOpen(false)}
              onSignOut={handleLogout}
            />
          </div>
        </div>
      )}
    </div>
  )
}
