import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { FileText } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Pagination } from '@/components/ui/Pagination'
import { getMyBills, type BillStatus } from '@/services/bills'
import { formatBillingMonth, formatCurrency, formatDate } from '@/utils/format'
import { cn } from '@/utils/cn'

const PAGE_SIZE = 8

const STATUS_FILTERS: Array<{ value: 'ALL' | BillStatus; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'PAID', label: 'Paid' },
  { value: 'OVERDUE', label: 'Overdue' },
  { value: 'GENERATED', label: 'Generated' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export function BillsPage() {
  const [status, setStatus] = useState<'ALL' | BillStatus>('ALL')
  const [page, setPage] = useState(0)

  const billsQuery = useQuery({
    queryKey: ['bills'],
    queryFn: getMyBills,
  })

  const filtered = useMemo(() => {
    const all = billsQuery.data ?? []
    return status === 'ALL' ? all : all.filter((bill) => bill.billStatus === status)
  }, [billsQuery.data, status])

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const currentPage = Math.min(page, totalPages - 1)
  const visible = filtered.slice(currentPage * PAGE_SIZE, currentPage * PAGE_SIZE + PAGE_SIZE)

  const selectStatus = (next: 'ALL' | BillStatus) => {
    setStatus(next)
    setPage(0)
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-lg font-bold text-navy-900">My bills</h1>
            <p className="text-sm text-slate-500">
              {filtered.length} bill{filtered.length === 1 ? '' : 's'}
            </p>
          </div>
          <div
            role="tablist"
            aria-label="Filter bills by status"
            className="flex flex-wrap gap-1.5"
          >
            {STATUS_FILTERS.map((filter) => (
              <button
                key={filter.value}
                type="button"
                role="tab"
                aria-selected={status === filter.value}
                onClick={() => selectStatus(filter.value)}
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
        </CardHeader>
        <CardBody className="p-0">
          {billsQuery.isLoading ? (
            <LoadingState label="Loading your bills…" />
          ) : billsQuery.isError ? (
            <ErrorState
              title="Could not load bills"
              message={billsQuery.error.message}
              onRetry={() => billsQuery.refetch()}
            />
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={FileText}
              title={status === 'ALL' ? 'No bills yet' : `No ${status.toLowerCase()} bills`}
              description={
                status === 'ALL'
                  ? 'Your first bill is generated once your meter is active and your first reading is verified. Complete the account setup steps on your dashboard to get started.'
                  : 'Try a different status filter.'
              }
            />
          ) : (
            <>
              <ul className="divide-y divide-slate-100">
                {visible.map((bill) => (
                  <li key={bill.id}>
                    <Link
                      to={`/consumer/bills/${bill.id}`}
                      className="flex flex-col gap-2 px-5 py-4 transition-colors hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-navy-900">
                          {formatBillingMonth(bill.billingMonth, bill.billingYear)}
                        </p>
                        <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                          <span className="font-mono">{bill.meterNumber}</span>
                          <span aria-hidden="true">·</span>
                          <span>Due {formatDate(bill.dueDate)}</span>
                          <span aria-hidden="true">·</span>
                          <span>{bill.unitsConsumed} units</span>
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-3">
                        <div className="text-right">
                          <p className="text-sm font-bold text-navy-900">
                            {formatCurrency(bill.totalAmount)}
                          </p>
                          {bill.outstandingAmount > 0 && (
                            <p className="text-xs font-medium text-red-600">
                              {formatCurrency(bill.outstandingAmount)} outstanding
                            </p>
                          )}
                        </div>
                        <Badge tone={statusTone(bill.billStatus)}>{bill.billStatus}</Badge>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="border-t border-slate-100 px-5 py-4">
                <Pagination page={currentPage} totalPages={totalPages} onChange={setPage} />
              </div>
            </>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
