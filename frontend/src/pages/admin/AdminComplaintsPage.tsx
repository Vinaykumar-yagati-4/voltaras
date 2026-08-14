import { useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ChevronRight, MessageSquareWarning, Search } from 'lucide-react'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Pagination } from '@/components/ui/Pagination'
import { Select } from '@/components/ui/Select'
import { AdminPageHeader } from '@/pages/admin/AdminPageHeader'
import {
  getAdminComplaints,
  getComplaintCategories,
  type ComplaintPriority,
  type ComplaintStatus,
} from '@/services/complaints'
import { formatCategoryLabel, formatDateTime, formatEnumLabel } from '@/utils/format'
import { cn } from '@/utils/cn'

const PAGE_SIZE = 10

const STATUS_FILTERS: Array<{ value: 'ALL' | ComplaintStatus; label: string }> = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
]

const PRIORITY_FILTERS: Array<{ value: 'ALL' | ComplaintPriority; label: string }> = [
  { value: 'ALL', label: 'All priorities' },
  { value: 'LOW', label: 'Low' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
]

export function AdminComplaintsPage() {
  const [status, setStatus] = useState<'ALL' | ComplaintStatus>('ALL')
  const [priority, setPriority] = useState<'ALL' | ComplaintPriority>('ALL')
  const [categoryId, setCategoryId] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const categoriesQuery = useQuery({
    queryKey: ['complaint-categories'],
    queryFn: getComplaintCategories,
  })

  const complaintsQuery = useQuery({
    queryKey: ['admin-complaints', { page, size: PAGE_SIZE, status, priority, categoryId }],
    queryFn: () =>
      getAdminComplaints(page, PAGE_SIZE, {
        ...(status === 'ALL' ? {} : { status }),
        ...(priority === 'ALL' ? {} : { priority }),
        ...(categoryId === 'ALL' ? {} : { categoryId: Number(categoryId) }),
      }),
  })

  const complaints = complaintsQuery.data?.content ?? []
  const totalPages = complaintsQuery.data?.totalPages ?? 1
  const totalElements = complaintsQuery.data?.totalElements ?? 0

  // The list API has no text-search parameter, so search filters the loaded page.
  const filteredComplaints = useMemo(() => {
    const query = search.trim().toLowerCase()
    const items = complaintsQuery.data?.content ?? []
    if (query === '') return items
    return items.filter(
      (complaint) =>
        complaint.subject.toLowerCase().includes(query) ||
        complaint.ticketNumber.toLowerCase().includes(query) ||
        (complaint.categoryName ?? '').toLowerCase().includes(query) ||
        formatCategoryLabel(complaint.categoryName).toLowerCase().includes(query),
    )
  }, [complaintsQuery.data, search])

  const categoryOptions = [
    { value: 'ALL', label: 'All categories' },
    ...(categoriesQuery.data ?? []).map((category) => ({
      value: String(category.id),
      label: formatCategoryLabel(category.name),
    })),
  ]

  const handleFilterChange =
    <T extends string>(setter: Dispatch<SetStateAction<T>>) =>
    (value: string) => {
      setter(value as T)
      setPage(0)
    }

  return (
    <div className="space-y-5">
      <AdminPageHeader
        title="Complaints"
        description="Monitor and manage consumer complaints across the platform."
        actions={
          <span className="inline-flex h-9 items-center rounded-md bg-navy-50 px-3 text-sm font-semibold text-navy-800 ring-1 ring-inset ring-navy-100">
            {totalElements} total
          </span>
        }
      />

      <Card>
        <CardHeader className="space-y-4">
          {/* Status controls in a clearly labelled row */}
          <div className="space-y-2">
            <span
              id="status-filter-label"
              className="block text-xs font-semibold uppercase tracking-wide text-slate-500"
            >
              Status
            </span>
            <div role="group" aria-labelledby="status-filter-label" className="flex flex-wrap gap-2">
              {STATUS_FILTERS.map((filter) => (
                <button
                  key={filter.value}
                  type="button"
                  onClick={() => handleFilterChange(setStatus)(filter.value)}
                  aria-pressed={status === filter.value}
                  className={cn(
                    'inline-flex h-11 items-center rounded-md border px-3 text-sm font-medium transition-colors motion-reduce:transition-none',
                    status === filter.value
                      ? 'border-volt-600 bg-volt-600 text-white'
                      : 'border-slate-300 bg-white text-navy-700 hover:bg-slate-50',
                  )}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>

          {/* Search + priority + category: labelled fields, one aligned row on desktop */}
          <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
            <div className="w-full">
              <label
                htmlFor="complaints-search"
                className="mb-1.5 block text-sm font-medium text-navy-800"
              >
                Search
              </label>
              <div className="relative w-full">
                <Search
                  className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
                  aria-hidden="true"
                />
                <input
                  id="complaints-search"
                  type="search"
                  value={search}
                  onChange={(event) => {
                    setSearch(event.target.value)
                    setPage(0)
                  }}
                  placeholder="Search subject, ticket, or category"
                  aria-label="Search complaints by subject, ticket number or category"
                  className="h-11 w-full rounded-md border border-slate-300 bg-white pl-10 pr-3 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                />
              </div>
            </div>

            <Select
              label="Priority"
              options={PRIORITY_FILTERS.map((f) => ({ value: f.value, label: f.label }))}
              value={priority}
              onChange={(event) => handleFilterChange(setPriority)(event.target.value)}
            />
            <Select
              label="Category"
              options={categoryOptions}
              value={categoryId}
              onChange={(event) => handleFilterChange(setCategoryId)(event.target.value)}
            />
          </div>
        </CardHeader>

        <CardBody className="p-0">
          {complaintsQuery.isLoading ? (
            <LoadingState label="Loading complaints…" />
          ) : complaintsQuery.isError ? (
            <ErrorState
              title="Could not load complaints"
              message={complaintsQuery.error?.message}
              onRetry={() => complaintsQuery.refetch()}
            />
          ) : filteredComplaints.length === 0 ? (
            <EmptyState
              icon={MessageSquareWarning}
              title={complaints.length === 0 ? 'No complaints' : 'No matching complaints'}
              description={
                complaints.length === 0
                  ? 'Complaints raised by consumers will appear here.'
                  : 'No records match the current search and filters. Try adjusting them.'
              }
            />
          ) : (
            <>
              <div className="flex items-center justify-between gap-2 border-b border-slate-100 px-5 py-3">
                <p className="text-sm text-slate-600">
                  Showing{' '}
                  <span className="font-semibold text-navy-900">{filteredComplaints.length}</span>{' '}
                  of{' '}
                  <span className="font-semibold text-navy-900">{totalElements}</span> complaints
                </p>
              </div>
              <ul className="divide-y divide-slate-100">
                {filteredComplaints.map((complaint) => (
                  <li key={complaint.id}>
                    <Link
                      to={`/admin/complaints/${complaint.id}`}
                      className="group flex min-h-14 items-center gap-3 px-5 py-3.5 transition-colors hover:bg-slate-50 focus-visible:bg-slate-50"
                    >
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                          <span className="font-mono text-xs font-medium text-slate-500">
                            {complaint.ticketNumber}
                          </span>
                          {typeof complaint.consumerId === 'number' && (
                            <Badge tone="navy">Consumer #{complaint.consumerId}</Badge>
                          )}
                        </div>
                        <p className="mt-1 break-words text-sm font-semibold text-navy-900 transition-colors group-hover:text-volt-700">
                          {complaint.subject}
                        </p>
                        <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                          {complaint.categoryName && (
                            <span>{formatCategoryLabel(complaint.categoryName)}</span>
                          )}
                          <span aria-hidden="true">·</span>
                          <span>{formatDateTime(complaint.createdAt)}</span>
                          {complaint.assignedTo ? (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>Assigned to #{complaint.assignedTo}</span>
                            </>
                          ) : null}
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
