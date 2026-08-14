import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ArrowUpFromLine } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Pagination } from '@/components/ui/Pagination'
import { getMyPayments, type PaymentStatus } from '@/services/payments'
import { formatCurrency, formatDateTime } from '@/utils/format'
import { cn } from '@/utils/cn'

const PAGE_SIZE = 10

const STATUS_FILTERS: Array<{ value: 'ALL' | PaymentStatus; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'SUCCESS', label: 'Successful' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export function PaymentsPage() {
  const [status, setStatus] = useState<'ALL' | PaymentStatus>('ALL')
  const [page, setPage] = useState(0)

  const paymentsQuery = useQuery({
    queryKey: ['payments', { page, size: PAGE_SIZE, status }],
    queryFn: () => getMyPayments(page, PAGE_SIZE),
  })

  const filtered = useMemo(() => {
    const content = paymentsQuery.data?.content ?? []
    return status === 'ALL' ? content : content.filter((p) => p.status === status)
  }, [paymentsQuery.data, status])

  const totalPages = paymentsQuery.data?.totalPages ?? 1

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-lg font-bold text-navy-900">Payments</h1>
            <p className="text-sm text-slate-500">
              {paymentsQuery.data?.totalElements ?? 0} payments
            </p>
          </div>
          <div role="tablist" aria-label="Filter payments by status" className="flex flex-wrap gap-1.5">
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
        </CardHeader>
        <CardBody className="p-0">
          {paymentsQuery.isLoading ? (
            <LoadingState label="Loading payments…" />
          ) : paymentsQuery.isError ? (
            <ErrorState
              title="Could not load payments"
              message={paymentsQuery.error.message}
              onRetry={() => paymentsQuery.refetch()}
            />
          ) : filtered.length === 0 ? (
            <EmptyState
              icon={ArrowUpFromLine}
              title={status === 'ALL' ? 'No payments yet' : `No ${status.toLowerCase()} payments`}
              description={
                status === 'ALL'
                  ? 'When you pay a bill it will appear here.'
                  : 'Try a different status filter.'
              }
            />
          ) : (
            <>
              <ul className="divide-y divide-slate-100">
                {filtered.map((payment) => (
                  <li key={payment.id}>
                    <Link
                      to={`/consumer/payments/${payment.id}`}
                      className="flex flex-col gap-2 px-5 py-4 transition-colors hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-medium text-navy-900">
                          {payment.transactionType === 'BILL_PAYMENT'
                            ? `Bill payment${payment.billId ? ` #${payment.billId}` : ''}`
                            : payment.transactionType}
                        </p>
                        <p className="mt-0.5 text-xs text-slate-500">
                          {formatDateTime(payment.createdAt)}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-3">
                        <span className="text-sm font-bold text-navy-900">
                          {formatCurrency(payment.amount)}
                        </span>
                        <Badge tone={statusTone(payment.status)}>{payment.status}</Badge>
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
