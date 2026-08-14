import { useMemo, useState, type Dispatch, type SetStateAction } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Building2, ChevronRight, Search, SearchX } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Pagination } from '@/components/ui/Pagination'
import { Select } from '@/components/ui/Select'
import { AdminPageHeader } from '@/pages/admin/AdminPageHeader'
import {
  getAdminOrganizations,
  type OrganizationStatus,
  type OrganizationType,
} from '@/services/organizations'
import { formatDate, formatEnumLabel } from '@/utils/format'

const PAGE_SIZE = 10

const STATUS_FILTERS: Array<{ value: 'ALL' | OrganizationStatus; label: string }> = [
  { value: 'ALL', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'SUSPENDED', label: 'Suspended' },
]

const TYPE_OPTIONS: Array<{ value: 'ALL' | OrganizationType; label: string }> = [
  { value: 'ALL', label: 'All types' },
  { value: 'HOSTEL', label: 'Hostel' },
  { value: 'INSTITUTION', label: 'Institution' },
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'COMMERCIAL', label: 'Commercial' },
]

export function AdminOrganizationsPage() {
  const [status, setStatus] = useState<'ALL' | OrganizationStatus>('ALL')
  const [type, setType] = useState<'ALL' | OrganizationType>('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const organizationsQuery = useQuery({
    queryKey: ['admin-organizations', { page, size: PAGE_SIZE, status, type }],
    queryFn: () =>
      getAdminOrganizations(page, PAGE_SIZE, {
        ...(status === 'ALL' ? {} : { status }),
        ...(type === 'ALL' ? {} : { type }),
      }),
  })

  const organizations = organizationsQuery.data?.content ?? []
  const totalPages = organizationsQuery.data?.totalPages ?? 1
  const totalElements = organizationsQuery.data?.totalElements ?? 0

  // The list API has no text-search parameter, so search filters the loaded page.
  const filteredOrganizations = useMemo(() => {
    const query = search.trim().toLowerCase()
    const items = organizationsQuery.data?.content ?? []
    if (query === '') return items
    return items.filter(
      (organization) =>
        organization.name.toLowerCase().includes(query) ||
        organization.organizationCode.toLowerCase().includes(query) ||
        (organization.city ?? '').toLowerCase().includes(query),
    )
  }, [organizationsQuery.data, search])

  const hasActiveFilters = status !== 'ALL' || type !== 'ALL' || search.trim() !== ''

  const handleFilterChange =
    <T extends string>(setter: Dispatch<SetStateAction<T>>) =>
    (value: string) => {
      setter(value as T)
      setPage(0)
    }

  return (
    <div className="space-y-5">
      <AdminPageHeader
        title="Organizations"
        description="Manage the organizations registered on the platform and their premises."
        actions={
          <span className="inline-flex h-9 items-center rounded-md bg-navy-50 px-3 text-sm font-semibold text-navy-800 ring-1 ring-inset ring-navy-100">
            {totalElements} total
          </span>
        }
      />

      <Card>
        <CardHeader className="space-y-3">
          {/* Search + status + type: labelled fields, one aligned row on desktop */}
          <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
            <div className="w-full">
              <label
                htmlFor="organizations-search"
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
                  id="organizations-search"
                  type="search"
                  value={search}
                  onChange={(event) => {
                    setSearch(event.target.value)
                    setPage(0)
                  }}
                  placeholder="Search name, code, or city"
                  aria-label="Search organizations by name, code or city"
                  className="h-11 w-full rounded-md border border-slate-300 bg-white pl-10 pr-3 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                />
              </div>
            </div>

            <Select
              label="Status"
              options={STATUS_FILTERS.map((f) => ({ value: f.value, label: f.label }))}
              value={status}
              onChange={(event) => handleFilterChange(setStatus)(event.target.value)}
            />
            <Select
              label="Type"
              options={TYPE_OPTIONS.map((f) => ({ value: f.value, label: f.label }))}
              value={type}
              onChange={(event) => handleFilterChange(setType)(event.target.value)}
            />
          </div>
        </CardHeader>

        <CardBody className="p-0">
          {organizationsQuery.isLoading ? (
            <LoadingState label="Loading organizations…" />
          ) : organizationsQuery.isError ? (
            <ErrorState
              title="Could not load organizations"
              message={organizationsQuery.error?.message}
              onRetry={() => organizationsQuery.refetch()}
            />
          ) : filteredOrganizations.length === 0 ? (
            <EmptyState
              icon={hasActiveFilters ? SearchX : Building2}
              title={
                organizations.length === 0
                  ? 'No organizations'
                  : 'No matching organizations'
              }
              description={
                organizations.length === 0
                  ? 'Organizations registered on the platform will appear here.'
                  : 'No records match the selected search and filters. Try adjusting them.'
              }
            />
          ) : (
            <>
              <div className="flex items-center justify-between gap-2 border-b border-slate-100 px-5 py-3">
                <p className="text-sm text-slate-600">
                  Showing{' '}
                  <span className="font-semibold text-navy-900">
                    {filteredOrganizations.length}
                  </span>{' '}
                  of <span className="font-semibold text-navy-900">{totalElements}</span>{' '}
                  organizations
                </p>
              </div>
              <ul className="divide-y divide-slate-100">
                {filteredOrganizations.map((organization) => (
                  <li key={organization.id}>
                    <Link
                      to={`/admin/organizations/${organization.id}`}
                      className="group flex min-h-14 items-center gap-3 px-5 py-3.5 transition-colors hover:bg-slate-50 focus-visible:bg-slate-50"
                    >
                      <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-navy-50 text-navy-600 ring-1 ring-inset ring-navy-100 transition-colors group-hover:bg-volt-50 group-hover:text-volt-600 group-hover:ring-volt-100">
                        <Building2 className="h-5 w-5" aria-hidden="true" />
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="break-words text-sm font-semibold text-navy-900 transition-colors group-hover:text-volt-700">
                          {organization.name}
                        </p>
                        <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                          <span className="font-mono">{organization.organizationCode}</span>
                          <span aria-hidden="true">·</span>
                          <span>{formatEnumLabel(organization.organizationType)}</span>
                          {organization.city && (
                            <>
                              <span aria-hidden="true">·</span>
                              <span>{organization.city}</span>
                            </>
                          )}
                          <span aria-hidden="true">·</span>
                          <span>Since {formatDate(organization.createdAt)}</span>
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <Badge tone={statusTone(organization.status)}>
                          {formatEnumLabel(organization.status)}
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
