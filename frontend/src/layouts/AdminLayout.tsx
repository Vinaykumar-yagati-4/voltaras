import { Outlet } from 'react-router-dom'
import { LayoutDashboard } from 'lucide-react'
import { AppShell, type AppNavItem } from '@/layouts/AppShell'

const navItems: AppNavItem[] = [{ to: '/admin', label: 'Dashboard', icon: LayoutDashboard }]

export function AdminLayout() {
  return (
    <AppShell navLabel="Admin navigation" navItems={navItems} sectionLabel="Complaints dashboard">
      <Outlet />
    </AppShell>
  )
}
