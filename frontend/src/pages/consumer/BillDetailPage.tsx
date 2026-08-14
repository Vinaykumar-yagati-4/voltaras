import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ReceiptText } from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { getMyBillById } from '@/services/bills'
import { getMyOrganizations } from '@/services/organizations'
import { payBillFromWallet } from '@/services/payments'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/types/api'
import { formatBillingMonth, formatCurrency, formatDate, formatDateTime } from '@/utils/format'

function DetailRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 py-2.5">
      <dt className="text-sm text-slate-500">{label}</dt>
      <dd className={highlight ? 'text-sm font-bold text-navy-900' : 'text-sm font-medium text-navy-900'}>
        {value}
      </dd>
    </div>
  )
}

export function BillDetailPage() {
  const { billId } = useParams<{ billId: string }>()
  const id = Number(billId)
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [payError, setPayError] = useState<string | null>(null)

  const billQuery = useQuery({
    queryKey: ['bill', id],
    queryFn: () => getMyBillById(id),
    enabled: Number.isFinite(id),
  })

  const membershipsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: getMyOrganizations,
    enabled: billQuery.data?.outstandingAmount != null && billQuery.data.outstandingAmount > 0,
  })

  const organizationId = membershipsQuery.data?.find(
    (m) => m.membershipStatus === 'ACTIVE',
  )?.organizationId

  const payMutation = useMutation({
    mutationFn: async (outstanding: number) => {
      if (!organizationId) throw new ApiError('No active organization membership found.', 403)
      const idempotencyKey = `pay-${user?.userId ?? 0}-${id}-${Date.now()}`
      return payBillFromWallet(
        id,
        { amount: outstanding, currency: 'INR', organizationId },
        idempotencyKey,
      )
    },
    onSuccess: () => {
      setConfirmOpen(false)
      setPayError(null)
      void queryClient.invalidateQueries({ queryKey: ['bills'] })
      void queryClient.invalidateQueries({ queryKey: ['bill', id] })
      void queryClient.invalidateQueries({ queryKey: ['wallet'] })
      void queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
    onError: (error: unknown) => {
      setPayError(error instanceof ApiError ? error.message : 'Payment could not be completed.')
    },
  })

  if (billQuery.isLoading) return <LoadingState label="Loading bill…" />

  if (billQuery.isError || !Number.isFinite(id) || !billQuery.data) {
    return (
      <ErrorState
        title="Bill not found"
        message={billQuery.error?.message ?? 'This bill does not exist or is not yours.'}
        onRetry={() => billQuery.refetch()}
      />
    )
  }

  const bill = billQuery.data
  const itemized =
    bill.energyCharge !== undefined &&
    bill.fixedCharge !== undefined &&
    bill.taxAmount !== undefined

  return (
    <div className="mx-auto w-full max-w-3xl space-y-6">
      <Link
        to="/consumer/bills"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to bills
      </Link>

      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
              <ReceiptText className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-navy-900">
                {formatBillingMonth(bill.billingMonth, bill.billingYear)}
              </h1>
              <p className="font-mono text-xs text-slate-500">{bill.meterNumber}</p>
            </div>
          </div>
          <Badge tone={statusTone(bill.billStatus)}>{bill.billStatus}</Badge>
        </CardHeader>
        <CardBody>
          {payError && (
            <Alert tone="error" className="mb-4">
              {payError}
            </Alert>
          )}

          <dl className="divide-y divide-slate-100">
            <DetailRow label="Units consumed" value={`${bill.unitsConsumed} units`} />
            <DetailRow label="Billing period" value={formatBillingMonth(bill.billingMonth, bill.billingYear)} />
            <DetailRow label="Generated" value={formatDate(bill.generatedDate)} />
            <DetailRow label="Due date" value={formatDate(bill.dueDate)} />
            {bill.paidAt && <DetailRow label="Paid on" value={formatDateTime(bill.paidAt)} />}
          </dl>

          {itemized && (
            <div className="mt-5">
              <h2 className="text-sm font-semibold text-navy-900">Charge details</h2>
              <dl className="mt-2 divide-y divide-slate-100 border-t border-slate-100">
                <DetailRow label="Energy charge" value={formatCurrency(bill.energyCharge)} />
                <DetailRow label="Fixed charge" value={formatCurrency(bill.fixedCharge)} />
                <DetailRow label="Tax" value={formatCurrency(bill.taxAmount)} />
                {bill.lateFee > 0 && <DetailRow label="Late fee" value={formatCurrency(bill.lateFee)} />}
                <DetailRow label="Total" value={formatCurrency(bill.totalAmount)} highlight />
                {bill.amountPaid > 0 && (
                  <DetailRow label="Paid" value={formatCurrency(bill.amountPaid)} />
                )}
                <DetailRow
                  label="Outstanding"
                  value={formatCurrency(bill.outstandingAmount)}
                  highlight
                />
              </dl>
            </div>
          )}

          {bill.outstandingAmount > 0 && (
            <div className="mt-6 rounded-md border border-amber-200 bg-amber-50 px-4 py-3">
              <p className="text-sm font-semibold text-amber-900">Outstanding balance</p>
              <p className="mt-0.5 text-sm text-amber-800">
                {formatCurrency(bill.outstandingAmount)} is still payable from your wallet.
              </p>
              <Button
                className="mt-3"
                loading={payMutation.isPending}
                disabled={!organizationId || membershipsQuery.isLoading}
                onClick={() => setConfirmOpen(true)}
              >
                Pay from wallet
              </Button>
            </div>
          )}
        </CardBody>
      </Card>

      <ConfirmDialog
        open={confirmOpen}
        title="Confirm payment"
        description={
          <>
            Pay <strong>{formatCurrency(bill.outstandingAmount)}</strong> for your{' '}
            {formatBillingMonth(bill.billingMonth, bill.billingYear)} bill from your wallet balance?
          </>
        }
        confirmLabel="Confirm payment"
        loading={payMutation.isPending}
        onConfirm={() => payMutation.mutate(bill.outstandingAmount)}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )
}
