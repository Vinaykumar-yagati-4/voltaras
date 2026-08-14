import { Outlet } from 'react-router-dom'
import {
  Bell,
  Building2,
  LayoutDashboard,
  MessageSquareWarning,
} from 'lucide-react'
import { AppShell, type AppNavItem } from '@/layouts/AppShell'

const navItems: AppNavItem[] = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard, section: 'Overview' },
  { to: '/admin/complaints', label: 'Complaints', icon: MessageSquareWarning, section: 'Operations' },
  { to: '/admin/organizations', label: 'Organizations', icon: Building2, section: 'Operations' },
  { to: '/admin/notifications', label: 'Notifications', icon: Bell, section: 'Operations' },
]

export function AdminLayout() {
  return (
    <AppShell
      variant="admin"
      navLabel="Admin navigation"
      navItems={navItems}
      sectionLabel="Admin portal"
    >
      <Outlet />
    </AppShell>
  )
}
