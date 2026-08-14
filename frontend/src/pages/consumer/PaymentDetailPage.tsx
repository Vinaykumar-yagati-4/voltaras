import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowUpFromLine } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { getPaymentById } from '@/services/payments'
import { formatCurrency, formatDateTime } from '@/utils/format'

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 py-2.5 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className="text-sm font-medium text-navy-900 sm:text-right">{value}</dd>
    </div>
  )
}

export function PaymentDetailPage() {
  const { paymentId } = useParams<{ paymentId: string }>()
  const id = Number(paymentId)

  const paymentQuery = useQuery({
    queryKey: ['payment', id],
    queryFn: () => getPaymentById(id),
    enabled: Number.isFinite(id),
  })

  if (paymentQuery.isLoading) return <LoadingState label="Loading payment…" />

  if (paymentQuery.isError || !Number.isFinite(id) || !paymentQuery.data) {
    return (
      <ErrorState
        title="Payment not found"
        message={paymentQuery.error?.message ?? 'This payment does not exist or is not yours.'}
        onRetry={() => paymentQuery.refetch()}
      />
    )
  }

  const payment = paymentQuery.data

  return (
    <div className="mx-auto w-full max-w-2xl space-y-6">
      <Link
        to="/consumer/payments"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to payments
      </Link>

      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
              <ArrowUpFromLine className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-navy-900">
                {formatCurrency(payment.amount)}
              </h1>
              <p className="text-xs text-slate-500">
                {payment.transactionType === 'BILL_PAYMENT' ? 'Bill payment' : payment.transactionType}
              </p>
            </div>
          </div>
          <Badge tone={statusTone(payment.status)}>{payment.status}</Badge>
        </CardHeader>
        <CardBody>
          <dl className="divide-y divide-slate-100">
            <DetailRow label="Reference" value={payment.paymentReference} />
            <DetailRow label="Method" value={payment.paymentMethod} />
            <DetailRow label="Status" value={payment.status} />
            {payment.billId != null && (
              <DetailRow label="Bill" value={`#${payment.billId}`} />
            )}
            <DetailRow label="Initiated" value={formatDateTime(payment.createdAt)} />
            {payment.paidAt && <DetailRow label="Completed" value={formatDateTime(payment.paidAt)} />}
            {payment.status === 'FAILED' && payment.failureReason && (
              <DetailRow label="Failure reason" value={payment.failureReason} />
            )}
          </dl>
        </CardBody>
      </Card>
    </div>
  )
}
