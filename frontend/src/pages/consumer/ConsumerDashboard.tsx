import { useQuery } from '@tanstack/react-query'
import {
  CheckCircle2,
  Clock3,
  Loader2,
  MessageSquareWarning,
  type LucideIcon,
} from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { useAuth } from '@/hooks/useAuth'
import { getMyComplaints, type ComplaintStatus, type ComplaintSummary } from '@/services/complaints'
import { formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

function StatCard({
  icon: Icon,
  label,
  value,
  hint,
  iconClass,
}: {
  icon: LucideIcon
  label: string
  value: string | number
  hint: string
  iconClass: string
}) {
  return (
    <Card>
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
          <p className="mt-0.5 text-2xl font-bold text-navy-900">{value}</p>
          <p className="truncate text-xs text-slate-500">{hint}</p>
        </div>
      </CardBody>
    </Card>
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

  const allComplaints = complaintsQuery.data?.content ?? []
  const recentComplaints = allComplaints.slice(0, 5)
  const isLoading = complaintsQuery.isLoading
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
          </div>
        </div>
        <Badge tone="blue" className="shrink-0 self-start sm:self-auto">
          CONSUMER
        </Badge>
      </section>

      {/* Complaint summary metrics */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Complaint summary">
        <StatCard
          icon={MessageSquareWarning}
          label="My complaints"
          value={complaintsQuery.data?.totalElements ?? '—'}
          hint={isLoading ? 'Loading…' : 'Total raised'}
          iconClass="bg-volt-50 text-volt-600"
        />
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
            <p className="text-xs text-slate-500">
              {complaintsQuery.data.totalElements} total
            </p>
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
              description="When you raise a complaint it will show up here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {recentComplaints.map((complaint: ComplaintSummary) => (
                <li
                  key={complaint.id}
                  className="flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
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
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
