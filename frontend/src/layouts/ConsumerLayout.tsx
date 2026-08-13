import { Outlet } from 'react-router-dom'
import { LayoutDashboard } from 'lucide-react'
import { AppShell, type AppNavItem } from '@/layouts/AppShell'

const navItems: AppNavItem[] = [{ to: '/consumer', label: 'Dashboard', icon: LayoutDashboard }]

export function ConsumerLayout() {
  return (
    <AppShell navLabel="Consumer navigation" navItems={navItems} sectionLabel="My account">
      <Outlet />
    </AppShell>
  )
}
