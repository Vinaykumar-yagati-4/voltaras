import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ArrowDownToLine, ArrowUpFromLine, PlusCircle, Wallet as WalletIcon } from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { getMyOrganizations } from '@/services/organizations'
import { getMyPayments, getMyRecharges, type Payment, type RechargeTransaction } from '@/services/payments'
import { getMyWallet, topUpWallet } from '@/services/wallet'
import { formatCurrency, formatDateTime } from '@/utils/format'

const RECHARGE_PRESETS = [100, 200, 500]

function RechargeRow({ recharge }: { recharge: RechargeTransaction }) {
  return (
    <li className="flex flex-col gap-1.5 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
      <div className="min-w-0">
        <p className="text-sm font-medium text-navy-900">Wallet recharge</p>
        <p className="mt-0.5 text-xs text-slate-500">
          {formatDateTime(recharge.createdAt)} · {recharge.paymentMethod}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-3">
        <span className="text-sm font-bold text-emerald-600">+{formatCurrency(recharge.amount)}</span>
        <Badge tone={statusTone(recharge.status)}>{recharge.status}</Badge>
      </div>
    </li>
  )
}

function PaymentRow({ payment }: { payment: Payment }) {
  return (
    <li>
      <Link
        to={`/consumer/payments/${payment.id}`}
        className="flex flex-col gap-1.5 px-5 py-4 transition-colors hover:bg-slate-50 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
      >
        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-navy-900">
            {payment.transactionType === 'BILL_PAYMENT' ? 'Bill payment' : payment.transactionType}
          </p>
          <p className="mt-0.5 text-xs text-slate-500">{formatDateTime(payment.createdAt)}</p>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          <span className="text-sm font-bold text-navy-900">−{formatCurrency(payment.amount)}</span>
          <Badge tone={statusTone(payment.status)}>{payment.status}</Badge>
        </div>
      </Link>
    </li>
  )
}

function RechargeCard() {
  const queryClient = useQueryClient()
  const [amount, setAmount] = useState(100)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const membershipsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: getMyOrganizations,
  })

  const organizationId = membershipsQuery.data?.find(
    (m) => m.membershipStatus === 'ACTIVE',
  )?.organizationId

  const rechargeMutation = useMutation({
    mutationFn: () => topUpWallet({ amount, organizationId }),
    onSuccess: (wallet) => {
      setSuccessMessage(
        `Added ${formatCurrency(amount)} to your wallet. New balance: ${formatCurrency(wallet.balance)}`,
      )
      void queryClient.invalidateQueries({ queryKey: ['wallet'] })
      void queryClient.invalidateQueries({ queryKey: ['recharges'] })
      void queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })

  const canRecharge = organizationId != null && amount > 0

  return (
    <Card>
      <CardHeader>
        <h2 className="text-base font-semibold text-navy-900">Add money for local testing</h2>
      </CardHeader>
      <CardBody className="space-y-4">
        <Alert tone="info">
          This is a local development recharge — it credits your wallet directly through the
          VOLTARAS backend without any payment gateway, and the transaction is recorded in your
          recharge history.
        </Alert>

        {successMessage && <Alert tone="success">{successMessage}</Alert>}
        {rechargeMutation.isError && (
          <Alert tone="error">
            {rechargeMutation.error instanceof Error
              ? rechargeMutation.error.message
              : 'The recharge could not be completed.'}
          </Alert>
        )}

        <div className="flex flex-wrap items-center gap-2">
          {RECHARGE_PRESETS.map((preset) => (
            <button
              key={preset}
              type="button"
              onClick={() => setAmount(preset)}
              className={`inline-flex h-10 items-center rounded-md border px-4 text-sm font-medium transition-colors ${
                amount === preset
                  ? 'border-volt-600 bg-volt-50 text-volt-700'
                  : 'border-slate-300 bg-white text-navy-800 hover:bg-slate-50'
              }`}
            >
              ₹{preset}
            </button>
          ))}
          <label className="flex h-10 items-center gap-2 rounded-md border border-slate-300 bg-white px-3">
            <span className="text-sm font-medium text-slate-500">₹</span>
            <input
              type="number"
              min={1}
              step={1}
              value={amount}
              onChange={(e) => setAmount(Math.max(1, Number(e.target.value) || 0))}
              className="w-20 text-sm font-medium text-navy-900 outline-none"
              aria-label="Recharge amount in rupees"
            />
          </label>
        </div>

        <div>
          <Button
            loading={rechargeMutation.isPending}
            disabled={!canRecharge || rechargeMutation.isPending}
            onClick={() => {
              setSuccessMessage(null)
              rechargeMutation.mutate()
            }}
          >
            <PlusCircle className="h-4 w-4" aria-hidden="true" />
            Add {formatCurrency(amount)} to wallet
          </Button>
          {organizationId == null && !membershipsQuery.isLoading && (
            <p className="mt-2 text-xs text-slate-500">
              Add money requires an active organization membership on your profile.
            </p>
          )}
        </div>
      </CardBody>
    </Card>
  )
}

export function WalletPage() {
  const walletQuery = useQuery({ queryKey: ['wallet'], queryFn: getMyWallet })
  const rechargesQuery = useQuery({ queryKey: ['recharges'], queryFn: getMyRecharges })
  const paymentsQuery = useQuery({
    queryKey: ['payments', { page: 0, size: 5 }],
    queryFn: () => getMyPayments(0, 5),
  })

  if (walletQuery.isLoading) return <LoadingState label="Loading your wallet…" />

  if (walletQuery.isError || !walletQuery.data) {
    return (
      <ErrorState
        title="Could not load your wallet"
        message={walletQuery.error?.message ?? 'Unable to load your wallet.'}
        onRetry={() => walletQuery.refetch()}
      />
    )
  }

  const wallet = walletQuery.data
  const recharges = rechargesQuery.data ?? []
  const payments = paymentsQuery.data?.content ?? []

  return (
    <div className="space-y-6">
      {/* Balance */}
      <Card>
        <CardBody className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-md bg-emerald-50 text-emerald-600">
              <WalletIcon className="h-6 w-6" aria-hidden="true" />
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Wallet balance
              </p>
              <p className="text-2xl font-bold text-navy-900">
                {formatCurrency(wallet.balance)}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/consumer/bills"
              className="inline-flex h-11 items-center gap-1.5 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium text-navy-900 transition-colors hover:bg-slate-50"
            >
              <ArrowUpFromLine className="h-4 w-4" aria-hidden="true" />
              View bills
            </Link>
            <Link
              to="/consumer/payments"
              className="inline-flex h-11 items-center gap-1.5 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium text-navy-900 transition-colors hover:bg-slate-50"
            >
              <ArrowDownToLine className="h-4 w-4" aria-hidden="true" />
              Payment history
            </Link>
          </div>
        </CardBody>
      </Card>

      {/* Local test recharge */}
      <RechargeCard />

      {/* Recharges */}
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-navy-900">Recharge history</h2>
        </CardHeader>
        <CardBody className="p-0">
          {rechargesQuery.isLoading ? (
            <LoadingState label="Loading recharge history…" />
          ) : rechargesQuery.isError ? (
            <ErrorState
              title="Could not load recharges"
              message={rechargesQuery.error.message}
              onRetry={() => rechargesQuery.refetch()}
            />
          ) : recharges.length === 0 ? (
            <EmptyState
              icon={ArrowDownToLine}
              title="No recharges yet"
              description="When you add money to your wallet it will appear here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {recharges.map((recharge) => (
                <RechargeRow key={recharge.id} recharge={recharge} />
              ))}
            </ul>
          )}
        </CardBody>
      </Card>

      {/* Recent payments */}
      <Card>
        <CardHeader className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-navy-900">Recent payments</h2>
          {payments.length > 0 && (
            <Link
              to="/consumer/payments"
              className="inline-flex h-11 items-center text-sm font-medium text-volt-600 hover:text-volt-700"
            >
              View all
            </Link>
          )}
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
          ) : payments.length === 0 ? (
            <EmptyState
              icon={ArrowUpFromLine}
              title="No payments yet"
              description="When you pay a bill it will appear here."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {payments.map((payment) => (
                <PaymentRow key={payment.id} payment={payment} />
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
