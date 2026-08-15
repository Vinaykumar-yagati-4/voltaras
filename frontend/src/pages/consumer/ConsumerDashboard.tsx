import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Bell,
  CheckCircle2,
  Clock3,
  FileText,
  Loader2,
  MessageSquareWarning,
  ReceiptText,
  Wallet as WalletIcon,
  Zap,
  type LucideIcon,
} from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { AccountSetupCard } from '@/components/consumer/AccountSetupCard'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { useAuth } from '@/hooks/useAuth'
import { getMyBills, billPeriodLabel } from '@/services/bills'
import { getMyComplaints, type ComplaintStatus, type ComplaintSummary } from '@/services/complaints'
import { getUnreadCount } from '@/services/notifications'
import { getDailyUsage } from '@/services/readings'
import { getMyWallet } from '@/services/wallet'
import { formatCurrency, formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

function StatCard({
  icon: Icon,
  label,
  value,
  hint,
  iconClass,
  to,
}: {
  icon: LucideIcon
  label: string
  value: string | number
  hint: string
  iconClass: string
  to?: string
}) {
  const content = (
    <Card className="h-full">
      <CardBody className="flex items-center gap-3">
        <div
          className={cn('flex h-11 w-11 shrink-0 items-center justify-center rounded-md', iconClass)}
        >
          <Icon className="h-5 w-5" aria-hidden="true" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-xs font-medium uppercase tracking-wide text-slate-500">
            {label}
          </p>
          <p className="mt-0.5 truncate text-2xl font-bold text-navy-900">{value}</p>
          <p className="truncate text-xs text-slate-500">{hint}</p>
        </div>
      </CardBody>
    </Card>
  )
  return to ? (
    <Link to={to} className="block focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-volt-500">
      {content}
    </Link>
  ) : (
    content
  )
}

function UsageStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-slate-50 px-4 py-3">
      <p className="truncate text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 truncate text-lg font-bold text-navy-900">{value}</p>
    </div>
  )
}

function countByStatus(items: ComplaintSummary[], statuses: ComplaintStatus[]): number {
  return items.filter((c) => statuses.includes(c.status)).length
}

export function ConsumerDashboard() {
  const { user } = useAuth()

  const complaintsQuery = useQuery({
    queryKey: ['my-complaints', 'dashboard'],
    queryFn: () => getMyComplaints(0, 20),
    enabled: user?.role === 'CONSUMER',
  })

  const billsQuery = useQuery({
    queryKey: ['bills'],
    queryFn: getMyBills,
    enabled: user?.role === 'CONSUMER',
  })

  const walletQuery = useQuery({
    queryKey: ['wallet'],
    queryFn: getMyWallet,
    enabled: user?.role === 'CONSUMER',
  })

  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
    enabled: user?.role === 'CONSUMER',
  })

  const dailyUsageQuery = useQuery({
    queryKey: ['daily-usage'],
    queryFn: getDailyUsage,
    enabled: user?.role === 'CONSUMER',
  })

  const usage = dailyUsageQuery.data

  const allComplaints = complaintsQuery.data?.content ?? []
  const recentComplaints = allComplaints.slice(0, 5)
  const bills = billsQuery.data ?? []
  const latestBill = bills[0] // newest first
  const unpaidCount = bills.filter((b) => b.billStatus === 'PENDING' || b.billStatus === 'OVERDUE').length
  const isLoading = complaintsQuery.isLoading || billsQuery.isLoading
  const initial = user?.fullName?.trim().charAt(0).toUpperCase() ?? 'U'
  const firstName = user?.fullName?.split(' ')[0]

  return (
    <div className="space-y-6">
      {/* Account identity */}
      <section className="flex flex-col gap-4 rounded-card border border-slate-200 bg-white p-5 shadow-card sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          <span
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-md bg-navy-900 text-lg font-bold text-white"
            aria-hidden="true"
          >
            {initial}
          </span>
          <div className="min-w-0">
            <h1 className="truncate text-lg font-bold text-navy-900 sm:text-xl">
              Welcome back, {firstName ?? 'there'}
            </h1>
            <p className="truncate text-sm text-slate-500">{user?.email}</p>
            <p className="mt-1 flex flex-wrap items-center gap-1.5">
              <Badge tone="green">User ID #{user?.userId}</Badge>
              <span className="text-xs text-slate-500">
                Share this User ID with admin for account preparation.
              </span>
            </p>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Link
            to="/consumer/notifications"
            className="relative inline-flex h-11 items-center gap-1.5 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-900 transition-colors hover:bg-slate-50"
            aria-label={`Notifications${unreadQuery.data?.unreadCount ? ` (${unreadQuery.data.unreadCount} unread)` : ''}`}
            title="Notifications"
          >
            <Bell className="h-4 w-4" aria-hidden="true" />
            {unreadQuery.data && unreadQuery.data.unreadCount > 0 && (
              <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-red-600 px-1.5 text-xs font-bold text-white">
                {unreadQuery.data.unreadCount}
              </span>
            )}
          </Link>
          <Badge tone="blue" className="self-start sm:self-auto">
            CONSUMER
          </Badge>
        </div>
      </section>

      {/* Account setup guidance for brand-new consumers */}
      <AccountSetupCard />

      {/* Today's electricity usage (backend-calculated from real readings) */}
      <section aria-label="Today's electricity usage">
        <Card>
          <CardHeader className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
                <Zap className="h-5 w-5" aria-hidden="true" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-navy-900">Today's electricity usage</h2>
                {usage?.meterNumber && (
                  <p className="font-mono text-xs text-slate-500">{usage.meterNumber}</p>
                )}
              </div>
            </div>
            <Link
              to="/consumer/readings"
              className="inline-flex h-11 items-center text-sm font-medium text-volt-600 hover:text-volt-700"
            >
              View 7-day usage
            </Link>
          </CardHeader>
          <CardBody>
            {dailyUsageQuery.isLoading ? (
              <LoadingState label="Loading your daily usage…" />
            ) : dailyUsageQuery.isError ? (
              <ErrorState
                title="Could not load daily usage"
                message={dailyUsageQuery.error?.message}
                onRetry={() => dailyUsageQuery.refetch()}
              />
            ) : !usage?.hasReadings ? (
              <EmptyState
                icon={Zap}
                title="No meter readings recorded yet"
                description="Submit your first daily meter reading and your daily usage will appear here."
              />
            ) : (
              <>
                <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-5">
                  <UsageStat label="Today's units" value={`${usage.unitsConsumedToday} units`} />
                  <UsageStat
                    label="Today's estimated cost"
                    value={formatCurrency(usage.estimatedTodayCost)}
                  />
                  <UsageStat
                    label="Per unit (est.)"
                    value={`${formatCurrency(usage.estimatedPerUnitCost)}/unit`}
                  />
                  <UsageStat label="This month" value={`${usage.monthUnitsSoFar} units`} />
                  <UsageStat
                    label="Month estimate"
                    value={formatCurrency(usage.estimatedMonthCost)}
                  />
                </div>
                <div className="mt-4 space-y-1 border-t border-slate-100 pt-3">
                  {!usage.hasReadingToday && (
                    <p className="text-sm font-medium text-amber-600">
                      No reading recorded today yet — today's units will update after you submit
                      today's reading.
                    </p>
                  )}
                  <p className="text-xs text-slate-500">
                    Last reading recorded {formatDateTime(usage.latestReadingAt)}. Costs are
                    estimates based on the current tariff slabs; your bill is the source of truth.
                  </p>
                </div>
              </>
            )}
          </CardBody>
        </Card>
      </section>

      {/* Real account metrics */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Account summary">
        <StatCard
          icon={WalletIcon}
          label="Wallet balance"
          value={walletQuery.isLoading ? '…' : formatCurrency(walletQuery.data?.balance)}
          hint="Available in your wallet"
          iconClass="bg-emerald-50 text-emerald-600"
          to="/consumer/wallet"
        />
        <StatCard
          icon={ReceiptText}
          label={latestBill ? billPeriodLabel(latestBill.billingMonth, latestBill.billingYear) : 'Latest bill'}
          value={latestBill ? formatCurrency(latestBill.totalAmount) : '—'}
          hint={latestBill ? `Due ${latestBill.dueDate}` : 'No bills yet'}
          iconClass="bg-volt-50 text-volt-600"
          to="/consumer/bills"
        />
        <StatCard
          icon={FileText}
          label="Unpaid bills"
          value={isLoading ? '…' : unpaidCount}
          hint="Pending or overdue"
          iconClass="bg-amber-50 text-amber-600"
          to="/consumer/bills"
        />
        <StatCard
          icon={MessageSquareWarning}
          label="My complaints"
          value={complaintsQuery.data?.totalElements ?? '—'}
          hint={isLoading ? 'Loading…' : 'Total raised'}
          iconClass="bg-navy-50 text-navy-600"
          to="/consumer/complaints"
        />
      </section>

      {/* Complaint summary metrics */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3" aria-label="Complaint summary">
        <StatCard
          icon={Clock3}
          label="Open"
          value={isLoading ? '…' : countByStatus(allComplaints, ['OPEN'])}
          hint="Awaiting response"
          iconClass="bg-amber-50 text-amber-600"
        />
        <StatCard
          icon={Loader2}
          label="In progress"
          value={isLoading ? '…' : countByStatus(allComplaints, ['IN_PROGRESS'])}
          hint="Being worked on"
          iconClass="bg-volt-50 text-volt-600"
        />
        <StatCard
          icon={CheckCircle2}
          label="Resolved"
          value={isLoading ? '…' : countByStatus(allComplaints, ['RESOLVED', 'CLOSED'])}
          hint="Closed cases"
          iconClass="bg-emerald-50 text-emerald-600"
        />
      </section>

      {/* Recent complaints activity */}
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-navy-900">Recent complaints</h2>
          {complaintsQuery.data && (
            <Link
              to="/consumer/complaints"
              className="inline-flex h-11 items-center text-sm font-medium text-volt-600 hover:text-volt-700"
            >
              View all
            </Link>
          )}
        </CardHeader>
        <CardBody className="p-0">
          {complaintsQuery.isLoading ? (
            <LoadingState label="Loading your complaints…" />
          ) : complaintsQuery.isError ? (
            <ErrorState
              title="Could not load complaints"
              message={complaintsQuery.error?.message}
              onRetry={() => complaintsQuery.refetch()}
            />
          ) : recentComplaints.length === 0 ? (
            <EmptyState
              icon={MessageSquareWarning}
              title="No complaints yet"
              description="If you run into an issue, raise a complaint and track it here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentComplaints.map((complaint: ComplaintSummary) => (
                <li key={complaint.id}>
                  <Link
                    to={`/consumer/complaints/${complaint.id}`}
                    className="flex flex-col gap-2 px-5 py-4 transition-colors hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                  >
                    <div className="min-w-0">
                      <p className="break-words text-sm font-medium text-navy-900">
                        {complaint.subject}
                      </p>
                      <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                        <span className="font-mono">{complaint.ticketNumber}</span>
                        <span aria-hidden="true">·</span>
                        <span>{formatDateTime(complaint.createdAt)}</span>
                      </p>
                    </div>
                    <div className="flex shrink-0 items-center">
                      <Badge tone={statusTone(complaint.status)}>{complaint.status}</Badge>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
