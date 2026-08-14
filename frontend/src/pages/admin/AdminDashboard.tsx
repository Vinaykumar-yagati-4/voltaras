import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  Activity,
  ArrowRight,
  Bell,
  Building2,
  CheckCheck,
  CheckCircle2,
  ChevronRight,
  Clock3,
  MessageSquareWarning,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Card, CardBody } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { useAuth } from '@/hooks/useAuth'
import {
  getAdminComplaints,
  getComplaintStatusCounts,
  type ComplaintStatus,
} from '@/services/complaints'
import { getUnreadCount } from '@/services/notifications'
import { getAdminOrganizations } from '@/services/organizations'
import { formatCategoryLabel, formatDateTime, formatEnumLabel } from '@/utils/format'
import { cn } from '@/utils/cn'

const STATUS_META: {
  status: ComplaintStatus
  icon: LucideIcon
  iconClass: string
  label: string
  description: string
}[] = [
  {
    status: 'OPEN',
    icon: Clock3,
    iconClass: 'bg-amber-50 text-amber-600 ring-amber-100',
    label: 'Open',
    description: 'Awaiting response',
  },
  {
    status: 'IN_PROGRESS',
    icon: Activity,
    iconClass: 'bg-volt-50 text-volt-600 ring-volt-100',
    label: 'In progress',
    description: 'Being worked on',
  },
  {
    status: 'RESOLVED',
    icon: CheckCircle2,
    iconClass: 'bg-emerald-50 text-emerald-600 ring-emerald-100',
    label: 'Resolved',
    description: 'Completed',
  },
  {
    status: 'CLOSED',
    icon: CheckCheck,
    iconClass: 'bg-slate-100 text-slate-600 ring-slate-200',
    label: 'Closed',
    description: 'Closed cases',
  },
]

function MetricCard({
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
    <Card
      className={cn(
        'h-full transition-all duration-200 motion-reduce:transition-none',
        to && 'hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-lg motion-reduce:hover:translate-y-0',
      )}
    >
      <CardBody className="flex items-start gap-4">
        <div
          className={cn(
            'flex h-12 w-12 shrink-0 items-center justify-center rounded-card ring-1 ring-inset',
            iconClass,
          )}
        >
          <Icon className="h-6 w-6" aria-hidden="true" />
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-1 text-3xl font-bold tracking-tight text-navy-900">{value}</p>
          <p className="mt-0.5 truncate text-xs text-slate-500">{hint}</p>
        </div>
      </CardBody>
    </Card>
  )
  return to ? (
    <Link
      to={to}
      className="block h-full rounded-card focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-volt-500"
    >
      {content}
    </Link>
  ) : (
    content
  )
}

export function AdminDashboard() {
  const { user } = useAuth()

  const countsQuery = useQuery({
    queryKey: ['complaint-status-counts'],
    queryFn: getComplaintStatusCounts,
    enabled: user?.role === 'ADMIN',
  })

  const recentQuery = useQuery({
    queryKey: ['admin-complaints', { page: 0, size: 5 }],
    queryFn: () => getAdminComplaints(0, 5),
    enabled: user?.role === 'ADMIN',
  })

  const orgsQuery = useQuery({
    queryKey: ['admin-organizations', { page: 0, size: 1 }],
    queryFn: () => getAdminOrganizations(0, 1),
    enabled: user?.role === 'ADMIN',
  })

  const unreadQuery = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
    enabled: user?.role === 'ADMIN',
  })

  const counts = countsQuery.data ?? ({} as Record<ComplaintStatus, number>)
  const totalComplaints = Object.values(counts).reduce((sum, value) => sum + (value ?? 0), 0)
  const recentComplaints = recentQuery.data?.content ?? []
  const organizationTotal = orgsQuery.data?.totalElements ?? 0
  const unreadCount = unreadQuery.data?.unreadCount ?? 0
  const firstName = user?.fullName?.split(' ')[0]

  const summarySentence = countsQuery.isLoading
    ? 'Loading live operational totals…'
    : `${totalComplaints} complaint${totalComplaints === 1 ? '' : 's'} tracked across ${organizationTotal} organization${organizationTotal === 1 ? '' : 's'} · ${unreadCount} unread alert${unreadCount === 1 ? '' : 's'}`

  return (
    <div className="space-y-6">
      {/* Operations hero */}
      <section className="relative overflow-hidden rounded-card bg-gradient-to-br from-navy-800 via-navy-900 to-navy-950 p-6 text-white shadow-card sm:p-8">
        {/* Subtle CSS-only grid / power motif */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-[0.12]"
          style={{
            backgroundImage:
              'linear-gradient(rgba(255,255,255,0.35) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.35) 1px, transparent 1px)',
            backgroundSize: '26px 26px',
          }}
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-20 -top-24 h-64 w-64 rounded-full bg-volt-500/25 blur-3xl"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -bottom-24 right-24 h-40 w-40 rounded-full bg-volt-400/10 blur-3xl"
        />

        <div className="relative flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-white/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-widest text-volt-200 ring-1 ring-inset ring-white/10">
                <ShieldCheck className="h-3.5 w-3.5" aria-hidden="true" />
                Operations overview
              </span>
              <Badge tone="blue" className="bg-volt-500/20 text-volt-100 ring-volt-400/30">
                ADMIN
              </Badge>
            </div>
            <h1 className="mt-3 break-words text-2xl font-bold tracking-tight sm:text-3xl">
              {firstName ? `Good to see you, ${firstName}` : 'Operations overview'}
            </h1>
            <p className="mt-2 max-w-xl text-sm text-slate-300">{summarySentence}</p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            <Link
              to="/admin/complaints"
              className="inline-flex h-11 items-center gap-2 rounded-md bg-white px-4 text-sm font-semibold text-navy-900 shadow-sm transition-colors hover:bg-slate-100"
            >
              Review complaints
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
            <Link
              to="/admin/organizations"
              className="inline-flex h-11 items-center gap-2 rounded-md border border-white/25 bg-white/5 px-4 text-sm font-semibold text-white transition-colors hover:bg-white/10"
            >
              Manage organizations
            </Link>
          </div>
        </div>
      </section>

      {/* Real account metrics */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-3" aria-label="Overview totals">
        <MetricCard
          icon={MessageSquareWarning}
          label="Total complaints"
          value={countsQuery.isLoading ? '…' : totalComplaints}
          hint="All tickets across statuses"
          iconClass="bg-navy-50 text-navy-600 ring-navy-100"
          to="/admin/complaints"
        />
        <MetricCard
          icon={Building2}
          label="Organizations"
          value={orgsQuery.isLoading ? '…' : organizationTotal}
          hint="Active and suspended"
          iconClass="bg-volt-50 text-volt-600 ring-volt-100"
          to="/admin/organizations"
        />
        <MetricCard
          icon={Bell}
          label="Unread alerts"
          value={unreadQuery.isLoading ? '…' : unreadCount}
          hint="Notifications for you"
          iconClass="bg-emerald-50 text-emerald-600 ring-emerald-100"
          to="/admin/notifications"
        />
      </section>

      {/* Status totals */}
      <section
        className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
        aria-label="Complaint status totals"
      >
        {STATUS_META.map(({ status, icon, iconClass, label, description }) => (
          <MetricCard
            key={status}
            icon={icon}
            label={label}
            value={countsQuery.isLoading ? '…' : (counts[status] ?? 0)}
            hint={description}
            iconClass={iconClass}
            to="/admin/complaints"
          />
        ))}
      </section>

      {/* Latest complaints */}
      <Card>
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-5 py-4">
          <div>
            <h2 className="text-base font-semibold text-navy-900">Latest complaints</h2>
            <p className="mt-0.5 text-xs text-slate-500">
              The most recently raised tickets, ready for review.
            </p>
          </div>
          <Link
            to="/admin/complaints"
            className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 transition-colors hover:text-volt-700"
          >
            View all
            <ChevronRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </div>
        <CardBody className="p-0">
          {recentQuery.isLoading ? (
            <p className="px-5 py-8 text-sm text-slate-500">Loading complaints…</p>
          ) : recentQuery.isError ? (
            <ErrorState
              title="Could not load complaints"
              message={recentQuery.error?.message}
              onRetry={() => recentQuery.refetch()}
            />
          ) : recentComplaints.length === 0 ? (
            <EmptyState
              icon={MessageSquareWarning}
              title="No complaints"
              description="Complaints raised by consumers will appear here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentComplaints.map((complaint) => (
                <li key={complaint.id}>
                  <Link
                    to={`/admin/complaints/${complaint.id}`}
                    className="group flex min-h-14 items-center gap-3 px-5 py-3.5 transition-colors hover:bg-slate-50 focus-visible:bg-slate-50"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="break-words text-sm font-semibold text-navy-900 transition-colors group-hover:text-volt-700">
                        {complaint.subject}
                      </p>
                      <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                        <span className="font-mono">{complaint.ticketNumber}</span>
                        <span aria-hidden="true">·</span>
                        <span>{formatDateTime(complaint.createdAt)}</span>
                        {complaint.categoryName && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span>{formatCategoryLabel(complaint.categoryName)}</span>
                          </>
                        )}
                      </p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <Badge tone={priorityTone(complaint.priority)}>
                        {formatEnumLabel(complaint.priority)}
                      </Badge>
                      <Badge tone={statusTone(complaint.status)}>
                        {formatEnumLabel(complaint.status)}
                      </Badge>
                      <ChevronRight
                        className="h-5 w-5 text-slate-300 transition-transform motion-reduce:transition-none group-hover:translate-x-0.5 group-hover:text-volt-500"
                        aria-hidden="true"
                      />
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
