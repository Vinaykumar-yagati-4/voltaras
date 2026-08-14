import { Outlet } from 'react-router-dom'
import {
  Bell,
  CreditCard,
  Gauge,
  LayoutDashboard,
  MessageSquareWarning,
  ReceiptText,
  UserRound,
  Wallet,
} from 'lucide-react'
import { AppShell, type AppNavItem } from '@/layouts/AppShell'

const navItems: AppNavItem[] = [
  { to: '/consumer', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/consumer/profile', label: 'My profile', icon: UserRound },
  { to: '/consumer/bills', label: 'Bills', icon: ReceiptText },
  { to: '/consumer/wallet', label: 'Wallet', icon: Wallet },
  { to: '/consumer/payments', label: 'Payments', icon: CreditCard },
  { to: '/consumer/readings', label: 'Readings', icon: Gauge },
  { to: '/consumer/complaints', label: 'Complaints', icon: MessageSquareWarning },
  { to: '/consumer/notifications', label: 'Notifications', icon: Bell },
]

export function ConsumerLayout() {
  return (
    <AppShell navLabel="Consumer navigation" navItems={navItems} sectionLabel="Consumer portal">
      <Outlet />
    </AppShell>
  )
}
