import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  Building2,
  CheckCircle2,
  CircleDashed,
  Clock3,
  UserRound,
  Zap,
} from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { LoadingState } from '@/components/ui/LoadingState'
import { Select } from '@/components/ui/Select'
import { getMyBills } from '@/services/bills'
import { getMyMeters } from '@/services/meters'
import {
  createJoinRequest,
  getAvailableOrganizations,
  getMyJoinRequests,
  getMyOrganizations,
} from '@/services/organizations'
import { getMyProfile } from '@/services/profile'
import { getMyReadings } from '@/services/readings'
import { getMyWallet } from '@/services/wallet'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/types/api'
import { cn } from '@/utils/cn'
import { formatDateTime } from '@/utils/format'

type StepState = 'done' | 'waiting' | 'action'

function StepRow({
  state,
  title,
  description,
  action,
}: {
  state: StepState
  title: string
  description: string
  action?: { label: string; to: string }
}) {
  return (
    <li className="flex items-start gap-3 px-5 py-4">
      <span className="mt-0.5 shrink-0">
        {state === 'done' ? (
          <CheckCircle2 className="h-5 w-5 text-emerald-600" aria-hidden="true" />
        ) : state === 'waiting' ? (
          <Clock3 className="h-5 w-5 text-amber-500" aria-hidden="true" />
        ) : (
          <CircleDashed className="h-5 w-5 text-volt-600" aria-hidden="true" />
        )}
      </span>
      <div className="min-w-0 flex-1">
        <p
          className={cn(
            'text-sm font-semibold',
            state === 'done' ? 'text-navy-900' : 'text-navy-900',
          )}
        >
          {title}
        </p>
        <p className="mt-0.5 text-xs leading-relaxed text-slate-500">{description}</p>
        {action && (
          <Link
            to={action.to}
            className="mt-2 inline-flex h-10 items-center gap-1.5 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-900 transition-colors hover:bg-slate-50"
          >
            {action.label}
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        )}
      </div>
    </li>
  )
}

/** Inline "request organization access" form shown when the user has no membership yet. */
function JoinRequestForm({ onRequested }: { onRequested: () => void }) {
  const queryClient = useQueryClient()
  const [organizationId, setOrganizationId] = useState('')
  const [message, setMessage] = useState('')

  const availableQuery = useQuery({
    queryKey: ['available-organizations'],
    queryFn: getAvailableOrganizations,
  })

  const joinMutation = useMutation({
    mutationFn: () =>
      createJoinRequest(Number(organizationId), {
        requestedRole: 'MEMBER',
        requestMessage: message || undefined,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['join-requests'] })
      void queryClient.invalidateQueries({ queryKey: ['available-organizations'] })
      setOrganizationId('')
      setMessage('')
      onRequested()
    },
  })

  if (availableQuery.isLoading) {
    return (
      <div className="border-t border-slate-100">
        <LoadingState label="Loading organizations…" className="py-6" />
      </div>
    )
  }

  if (availableQuery.isError) {
    return (
      <div className="border-t border-slate-100 px-5 py-4">
        <Alert tone="error">Could not load organizations. Please try again.</Alert>
      </div>
    )
  }

  const organizations = availableQuery.data ?? []

  if (organizations.length === 0) {
    return (
      <div className="border-t border-slate-100 px-5 py-4">
        <p className="text-sm text-slate-600">
          No organizations are open for requests at the moment. VOLTARAS support can link your
          account directly — reach out to your administrator.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-3 border-t border-slate-100 bg-slate-50/60 px-5 py-4">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
        Request access to an organization
      </p>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Select
          label="Organization"
          options={[
            { value: '', label: 'Select an organization…' },
            ...organizations.map((org) => ({
              value: String(org.id),
              label: org.name,
            })),
          ]}
          value={organizationId}
          onChange={(e) => setOrganizationId(e.target.value)}
        />
        <label className="block w-full">
          <span className="mb-1.5 block text-sm font-medium text-navy-800">Message (optional)</span>
          <input
            type="text"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            maxLength={500}
            placeholder="e.g. I live in this society"
            className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-navy-900 shadow-sm placeholder:text-slate-400 focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
          />
        </label>
      </div>
      {joinMutation.isError && (
        <Alert tone="error">
          {joinMutation.error instanceof ApiError
            ? joinMutation.error.message
            : 'Your request could not be submitted.'}
        </Alert>
      )}
      <Button
        loading={joinMutation.isPending}
        disabled={!organizationId}
        onClick={() => joinMutation.mutate()}
      >
        <Building2 className="h-4 w-4" aria-hidden="true" />
        Submit request
      </Button>
      <p className="text-xs text-slate-500">
        Your request stays pending until the organization approves it. You can track it here and in
        your profile.
      </p>
    </div>
  )
}

/**
 * Guides a newly registered consumer through real account setup instead of
 * showing confusing empty pages. Renders nothing once the account is fully
 * prepared (profile + membership + meter + reading + bill).
 */
export function AccountSetupCard() {
  const { user } = useAuth()
  const [requestSubmitted, setRequestSubmitted] = useState(false)

  const profileQuery = useQuery({ queryKey: ['profile'], queryFn: getMyProfile })
  const membershipsQuery = useQuery({ queryKey: ['organizations'], queryFn: getMyOrganizations })
  const joinRequestsQuery = useQuery({ queryKey: ['join-requests'], queryFn: getMyJoinRequests })
  const metersQuery = useQuery({ queryKey: ['meters'], queryFn: getMyMeters })
  const readingsQuery = useQuery({ queryKey: ['readings'], queryFn: getMyReadings })
  const billsQuery = useQuery({ queryKey: ['bills'], queryFn: getMyBills })
  const walletQuery = useQuery({ queryKey: ['wallet'], queryFn: getMyWallet })

  const stateLoading =
    profileQuery.isLoading ||
    membershipsQuery.isLoading ||
    joinRequestsQuery.isLoading ||
    metersQuery.isLoading ||
    readingsQuery.isLoading ||
    billsQuery.isLoading ||
    walletQuery.isLoading

  if (stateLoading) return null

  const hasProfile =
    !profileQuery.isError ||
    (profileQuery.error instanceof ApiError && profileQuery.error.status !== 404)
  const hasMembership = (membershipsQuery.data ?? []).some(
    (m) => m.membershipStatus === 'ACTIVE',
  )
  const pendingRequest = (joinRequestsQuery.data ?? []).find(
    (r) => r.status === 'PENDING',
  )
  const hasMeter = (metersQuery.data ?? []).length > 0
  // Only VERIFIED readings count towards setup — a submitted reading is not
  // yet billable, so the step stays waiting until an admin verifies it.
  const hasReadings = (readingsQuery.data ?? []).some((r) => r.status === 'VERIFIED')
  const hasBills = (billsQuery.data ?? []).length > 0
  const walletBalance = walletQuery.data?.balance ?? 0

  const setupComplete = hasProfile && hasMembership && hasMeter && hasReadings && hasBills
  if (setupComplete) return null

  const doneCount = [
    hasProfile,
    hasMembership,
    hasMeter,
    hasReadings && hasBills,
    walletBalance > 0,
  ].filter(Boolean).length

  return (
    <Card className="overflow-hidden">
      <CardBody className="space-y-4 border-b border-volt-100 bg-gradient-to-br from-volt-50 via-white to-navy-50 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="min-w-0">
            <h2 className="flex items-center gap-2 text-base font-bold text-navy-900">
              <span className="flex h-8 w-8 items-center justify-center rounded-md bg-volt-600 text-white">
                <Zap className="h-4 w-4" aria-hidden="true" />
              </span>
              Finish setting up your account
            </h2>
            <p className="mt-1 text-sm text-slate-600">
              A few quick steps and your electricity service will be ready. Your progress:{' '}
              {doneCount} of 5.
            </p>
            <p className="mt-1 text-xs text-slate-500">
              You are logged in as{' '}
              <span className="font-medium text-navy-800">{user?.email}</span> · User ID{' '}
              <span className="font-medium text-navy-800">#{user?.userId}</span>
            </p>
          </div>
          <div className="flex items-center gap-1" aria-hidden="true">
            {[0, 1, 2, 3, 4].map((i) => (
              <span
                key={i}
                className={cn(
                  'h-1.5 w-6 rounded-full',
                  i < doneCount ? 'bg-volt-600' : 'bg-slate-200',
                )}
              />
            ))}
          </div>
        </div>
      </CardBody>

      {requestSubmitted && (
        <div className="border-b border-slate-100 px-5 py-3">
          <Alert tone="success">Your organization access request has been submitted.</Alert>
        </div>
      )}

      <ul className="divide-y divide-slate-100">
        <StepRow
          state={hasProfile ? 'done' : 'action'}
          title="Complete your profile"
          description={
            hasProfile
              ? 'Your contact and address details are on file.'
              : 'Add your name, phone and address so your electricity service can be registered to you.'
          }
          action={hasProfile ? undefined : { label: 'Complete profile', to: '/consumer/profile' }}
        />

        <li className="px-5 py-4">
          <div className="flex items-start gap-3">
            <span className="mt-0.5 shrink-0">
              {hasMembership ? (
                <CheckCircle2 className="h-5 w-5 text-emerald-600" aria-hidden="true" />
              ) : pendingRequest ? (
                <Clock3 className="h-5 w-5 text-amber-500" aria-hidden="true" />
              ) : (
                <CircleDashed className="h-5 w-5 text-volt-600" aria-hidden="true" />
              )}
            </span>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-navy-900">Request organization access</p>
              {hasMembership ? (
                <p className="mt-0.5 text-xs text-slate-500">
                  You are linked to{' '}
                  {membershipsQuery.data?.find((m) => m.membershipStatus === 'ACTIVE')
                    ?.organizationName ?? 'your organization'}
                  .
                </p>
              ) : pendingRequest ? (
                <p className="mt-0.5 text-xs leading-relaxed text-slate-500">
                  Your request to join{' '}
                  <span className="font-medium text-navy-800">{pendingRequest.organizationName}</span>{' '}
                  is pending approval (submitted {formatDateTime(pendingRequest.createdAt)}). Once
                  approved, your meter and billing can be linked.
                </p>
              ) : (
                <p className="mt-0.5 text-xs leading-relaxed text-slate-500">
                  Electricity is billed through an organization (society, hostel or apartment).
                  Request access and an administrator will approve your membership.
                </p>
              )}
            </div>
          </div>
          {!hasMembership && !pendingRequest && (
            <JoinRequestForm onRequested={() => setRequestSubmitted(true)} />
          )}
        </li>

        <StepRow
          state={hasMeter ? 'done' : 'waiting'}
          title="Meter activation"
          description={
            hasMeter
              ? `Your meter ${metersQuery.data?.[0]?.meterNumber ?? ''} is active and assigned to you.`
              : 'After your organization access is approved, VOLTARAS support assigns a meter to your account.'
          }
        />

        <StepRow
          state={hasReadings && hasBills ? 'done' : 'waiting'}
          title="First reading and bill"
          description={
            hasReadings && hasBills
              ? 'Your first meter reading is recorded and your bill has been generated.'
              : 'Your first meter reading is recorded and verified, then your first bill is generated for it.'
          }
        />

        <StepRow
          state={walletBalance > 0 ? 'done' : 'action'}
          title="Recharge wallet and pay bill"
          description={
            walletBalance > 0
              ? 'Your wallet is funded — you can pay your bill from it whenever it is due.'
              : 'Add money to your wallet, then pay your bill from your wallet in one place.'
          }
          action={
            walletBalance > 0
              ? undefined
              : { label: 'Go to wallet', to: '/consumer/wallet' }
          }
        />
      </ul>

      <div className="border-t border-slate-100 bg-slate-50/60 px-5 py-3">
        <p className="flex items-center gap-1.5 text-xs text-slate-500">
          <UserRound className="h-3.5 w-3.5" aria-hidden="true" />
          Your progress updates automatically as VOLTARAS prepares your account.
        </p>
        <p className="mt-1 text-xs text-slate-500">
          Share your User ID (#{user?.userId}) with admin for account preparation.
        </p>
      </div>
    </Card>
  )
}
