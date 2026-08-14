import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { MessageSquareWarning, Plus } from 'lucide-react'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Pagination } from '@/components/ui/Pagination'
import { getMyComplaints, type ComplaintStatus } from '@/services/complaints'
import { formatCategoryLabel, formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

const PAGE_SIZE = 10

const STATUS_FILTERS: Array<{ value: 'ALL' | ComplaintStatus; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
]

export function ComplaintsPage() {
  const [status, setStatus] = useState<'ALL' | ComplaintStatus>('ALL')
  const [page, setPage] = useState(0)

  const complaintsQuery = useQuery({
    queryKey: ['my-complaints', { page, size: PAGE_SIZE, status }],
    queryFn: () =>
      getMyComplaints(page, PAGE_SIZE, status === 'ALL' ? undefined : { status }),
  })

  const complaints = complaintsQuery.data?.content ?? []
  const totalPages = complaintsQuery.data?.totalPages ?? 1

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-lg font-bold text-navy-900">My complaints</h1>
            <p className="text-sm text-slate-500">
              {complaintsQuery.data?.totalElements ?? 0} complaints
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <div
              role="tablist"
              aria-label="Filter complaints by status"
              className="flex flex-wrap gap-1.5"
            >
              {STATUS_FILTERS.map((filter) => (
                <button
                  key={filter.value}
                  type="button"
                  role="tab"
                  aria-selected={status === filter.value}
                  onClick={() => {
                    setStatus(filter.value)
                    setPage(0)
                  }}
                  className={cn(
                    'inline-flex h-11 items-center rounded-md border px-3 text-sm font-medium transition-colors',
                    status === filter.value
                      ? 'border-volt-600 bg-volt-600 text-white'
                      : 'border-slate-300 bg-white text-navy-800 hover:bg-slate-50',
                  )}
                >
                  {filter.label}
                </button>
              ))}
            </div>
            <Link
              to="/consumer/complaints/new"
              className="inline-flex h-11 items-center gap-1.5 rounded-md bg-volt-600 px-4 text-sm font-medium text-white transition-colors hover:bg-volt-700"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
              New complaint
            </Link>
          </div>
        </CardHeader>
        <CardBody className="p-0">
          {complaintsQuery.isLoading ? (
            <LoadingState label="Loading complaints…" />
          ) : complaintsQuery.isError ? (
            <ErrorState
              title="Could not load complaints"
              message={complaintsQuery.error.message}
              onRetry={() => complaintsQuery.refetch()}
            />
          ) : complaints.length === 0 ? (
            <EmptyState
              icon={MessageSquareWarning}
              title={status === 'ALL' ? 'No complaints yet' : `No ${status.replace('_', ' ').toLowerCase()} complaints`}
              description={
                status === 'ALL'
                  ? 'If you run into an issue, raise a complaint and track it here.'
                  : 'Try a different status filter.'
              }
            />
          ) : (
            <>
              <ul className="divide-y divide-slate-100">
                {complaints.map((complaint) => (
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
                          {complaint.categoryName && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>{formatCategoryLabel(complaint.categoryName)}</span>
                            </>
                          )}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <Badge tone={priorityTone(complaint.priority)}>{complaint.priority}</Badge>
                        <Badge tone={statusTone(complaint.status)}>{complaint.status}</Badge>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="border-t border-slate-100 px-5 py-4">
                <Pagination page={page} totalPages={totalPages} onChange={setPage} />
              </div>
            </>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
