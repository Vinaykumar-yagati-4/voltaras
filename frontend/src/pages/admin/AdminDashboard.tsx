import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  CheckCheck,
  CheckCircle2,
  Clock3,
  Loader2,
  MessageSquareWarning,
  Search,
  ShieldCheck,
  type LucideIcon,
} from 'lucide-react'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { useAuth } from '@/hooks/useAuth'
import {
  getAdminComplaints,
  getComplaintStatusCounts,
  type ComplaintStatus,
  type ComplaintSummary,
} from '@/services/complaints'
import { formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

const STATUS_META: {
  status: ComplaintStatus
  icon: LucideIcon
  iconClass: string
  description: string
}[] = [
  {
    status: 'OPEN',
    icon: Clock3,
    iconClass: 'bg-amber-50 text-amber-600',
    description: 'Awaiting response',
  },
  {
    status: 'IN_PROGRESS',
    icon: Loader2,
    iconClass: 'bg-volt-50 text-volt-600',
    description: 'Being worked on',
  },
  {
    status: 'RESOLVED',
    icon: CheckCircle2,
    iconClass: 'bg-emerald-50 text-emerald-600',
    description: 'Resolved',
  },
  {
    status: 'CLOSED',
    icon: CheckCheck,
    iconClass: 'bg-slate-100 text-slate-600',
    description: 'Closed cases',
  },
]

const STATUS_FILTERS: Array<ComplaintStatus | 'ALL'> = [
  'ALL',
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
]

function StatusCard({
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

const GRID_COLS = 'lg:grid-cols-[10rem_minmax(0,1fr)_9rem_auto_auto]'

export function AdminDashboard() {
  const { user } = useAuth()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<ComplaintStatus | 'ALL'>('ALL')

  const complaintsQuery = useQuery({
    queryKey: ['admin-complaints', 'dashboard'],
    queryFn: () => getAdminComplaints(0, 100),
    enabled: user?.role === 'ADMIN',
  })

  const countsQuery = useQuery({
    queryKey: ['complaint-status-counts'],
    queryFn: () => getComplaintStatusCounts(),
    enabled: user?.role === 'ADMIN',
  })

  const allComplaints = useMemo(
    () => complaintsQuery.data?.content ?? [],
    [complaintsQuery.data],
  )
  const counts = countsQuery.data ?? ({} as Record<ComplaintStatus, number>)
  const total = complaintsQuery.data?.totalElements ?? 0

  // Client-side search/filter — operates only on the already-fetched list.
  const filteredComplaints = useMemo(() => {
    const query = search.trim().toLowerCase()
    return allComplaints.filter((complaint) => {
      const matchesStatus = statusFilter === 'ALL' || complaint.status === statusFilter
      const matchesSearch =
        query === '' ||
        complaint.subject.toLowerCase().includes(query) ||
        complaint.ticketNumber.toLowerCase().includes(query) ||
        (complaint.categoryName ?? '').toLowerCase().includes(query)
      return matchesStatus && matchesSearch
    })
  }, [allComplaints, search, statusFilter])

  return (
    <div className="space-y-6">
      {/* Overview header */}
      <section className="flex flex-col gap-4 rounded-card border border-slate-200 bg-white p-5 shadow-card sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <h1 className="truncate text-lg font-bold text-navy-900 sm:text-xl">
            Complaints overview
          </h1>
          <p className="mt-0.5 truncate text-sm text-slate-500">
            {user?.fullName} ·{' '}
            {total > 0 ? `${total} ${total === 1 ? 'complaint' : 'complaints'} across your service area` : 'No complaints to review'}
          </p>
        </div>
        <Badge tone="blue" className="shrink-0 self-start gap-1.5 px-3 py-1 text-sm sm:self-auto">
          <ShieldCheck className="h-4 w-4" aria-hidden="true" />
          ADMIN
        </Badge>
      </section>

      {/* Status metrics */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Complaint status totals">
        {STATUS_META.map(({ status, icon, iconClass, description }) => (
          <StatusCard
            key={status}
            icon={icon}
            label={status.replace('_', ' ')}
            value={countsQuery.isLoading ? '…' : (counts[status] ?? 0)}
            hint={description}
            iconClass={iconClass}
          />
        ))}
      </section>

      {/* Complaints list */}
      <Card>
        <CardHeader className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-base font-semibold text-navy-900">All complaints</h2>
            <p className="text-xs text-slate-500">
              {filteredComplaints.length} of {total} shown
            </p>
          </div>
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative w-full lg:max-w-xs">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
                aria-hidden="true"
              />
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search subject or ticket"
                aria-label="Search complaints by subject or ticket number"
                className="h-11 w-full rounded-md border border-slate-300 bg-white pl-9 pr-3 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
              />
            </div>
            <div
              className="flex flex-wrap items-center gap-2"
              role="group"
              aria-label="Filter complaints by status"
            >
              {STATUS_FILTERS.map((status) => (
                <button
                  key={status}
                  type="button"
                  onClick={() => setStatusFilter(status)}
                  aria-pressed={statusFilter === status}
                  className={cn(
                    'inline-flex h-11 items-center rounded-md border px-3 text-sm font-medium transition-colors',
                    statusFilter === status
                      ? 'border-volt-600 bg-volt-600 text-white'
                      : 'border-slate-300 bg-white text-navy-700 hover:bg-slate-50',
                  )}
                >
                  {status === 'ALL' ? 'All' : status.replace('_', ' ')}
                </button>
              ))}
            </div>
          </div>
        </CardHeader>

        {complaintsQuery.isLoading ? (
          <CardBody>
            <LoadingState label="Loading complaints…" />
          </CardBody>
        ) : complaintsQuery.isError ? (
          <CardBody>
            <ErrorState
              title="Could not load complaints"
              message={complaintsQuery.error?.message}
              onRetry={() => complaintsQuery.refetch()}
            />
          </CardBody>
        ) : filteredComplaints.length === 0 ? (
          <CardBody className="p-0">
            <EmptyState
              icon={MessageSquareWarning}
              title={allComplaints.length === 0 ? 'No complaints' : 'No matching complaints'}
              description={
                allComplaints.length === 0
                  ? 'Complaints raised by consumers will appear here.'
                  : 'Try adjusting the search or status filter.'
              }
            />
          </CardBody>
        ) : (
          <CardBody className="p-0">
            {/* Column labels (desktop only) */}
            <div
              className={cn(
                'hidden border-b border-slate-100 bg-slate-50 px-5 py-2.5 lg:grid lg:gap-4',
                GRID_COLS,
              )}
            >
              <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Ticket
              </span>
              <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Subject
              </span>
              <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Raised
              </span>
              <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Priority
              </span>
              <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Status
              </span>
            </div>
            <ul className="divide-y divide-slate-100">
              {filteredComplaints.map((complaint: ComplaintSummary) => (
                <li
                  key={complaint.id}
                  className={cn(
                    'flex flex-col gap-2 px-5 py-4 lg:grid lg:items-center lg:gap-4',
                    GRID_COLS,
                  )}
                >
                  <span className="break-all font-mono text-xs font-medium text-slate-500">
                    {complaint.ticketNumber}
                  </span>
                  <div className="min-w-0">
                    <p className="break-words text-sm font-medium text-navy-900">
                      {complaint.subject}
                    </p>
                    {complaint.categoryName && (
                      <p className="mt-0.5 text-xs text-slate-500">{complaint.categoryName}</p>
                    )}
                  </div>
                  <span className="text-xs text-slate-500">
                    {formatDateTime(complaint.createdAt)}
                  </span>
                  <div className="flex flex-wrap items-center gap-2 lg:contents">
                    <Badge tone={priorityTone(complaint.priority)}>{complaint.priority}</Badge>
                    <Badge tone={statusTone(complaint.status)}>{complaint.status}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          </CardBody>
        )}
      </Card>
    </div>
  )
}
