import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Building2,
  CheckCircle2,
  Gauge,
  Hash,
  Link2,
  ReceiptText,
  Search,
  ShieldCheck,
  UserRound,
} from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { generateBill, type BillDetail } from '@/services/bills'
import {
  createMeter,
  assignMeter,
  getAdminMetersByUser,
  type MeterSummary,
} from '@/services/meters'
import {
  approveJoinRequest,
  createOrganizationMembership,
  getAdminOrganizations,
  getOrganizationJoinRequests,
  getOrganizationMembers,
} from '@/services/organizations'
import { createAdminReading, verifyAdminReading, type MeterReading } from '@/services/readings'
import { ApiError } from '@/types/api'
import { formatCurrency, formatDateTime } from '@/utils/format'

function SectionHeader({
  step,
  icon,
  title,
  description,
}: {
  step: string
  icon: React.ReactNode
  title: string
  description: string
}) {
  return (
    <div className="flex items-start gap-3">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-volt-600 text-sm font-bold text-white">
        {step}
      </span>
      <div className="min-w-0">
        <h2 className="flex items-center gap-2 text-base font-semibold text-navy-900">
          <span className="text-volt-600">{icon}</span>
          {title}
        </h2>
        <p className="mt-0.5 text-sm text-slate-500">{description}</p>
      </div>
    </div>
  )
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function daysFromToday(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

export function PrepareConsumerPage() {
  const queryClient = useQueryClient()
  const [userId, setUserId] = useState('')
  const [lookedUpUserId, setLookedUpUserId] = useState<number | null>(null)
  const [selectedOrgId, setSelectedOrgId] = useState('')
  const [pageError, setPageError] = useState<string | null>(null)

  // Step 3 — meter form
  const [meterNumber, setMeterNumber] = useState('')
  const [meterType, setMeterType] = useState<'SMART' | 'ANALOG' | 'PREPAID'>('SMART')
  const [connectionType, setConnectionType] = useState<'RESIDENTIAL' | 'COMMERCIAL'>('RESIDENTIAL')
  const [phaseType, setPhaseType] = useState<'SINGLE_PHASE' | 'THREE_PHASE'>('SINGLE_PHASE')
  const [sanctionedLoad, setSanctionedLoad] = useState('5.0')

  // Step 4 — reading form
  const [readingPrev, setReadingPrev] = useState('0')
  const [readingCurrent, setReadingCurrent] = useState('')
  const [readingDate, setReadingDate] = useState(today())

  // Step 5 — bill form
  const [billingMonth, setBillingMonth] = useState('')
  const [billingYear, setBillingYear] = useState('')
  const [dueDate, setDueDate] = useState(daysFromToday(15))

  const [createdReading, setCreatedReading] = useState<MeterReading | null>(null)
  const [createdBill, setCreatedBill] = useState<BillDetail | null>(null)

  const organizationsQuery = useQuery({
    queryKey: ['admin-organizations', { page: 0, size: 100 }],
    queryFn: () => getAdminOrganizations(0, 100),
  })

  const metersQuery = useQuery({
    queryKey: ['admin-meters-by-user', lookedUpUserId],
    queryFn: () => getAdminMetersByUser(lookedUpUserId as number),
    enabled: lookedUpUserId != null,
  })

  const orgIdNumber = selectedOrgId ? Number(selectedOrgId) : null

  const orgMembersQuery = useQuery({
    queryKey: ['organization-members', orgIdNumber, lookedUpUserId],
    queryFn: () => getOrganizationMembers(orgIdNumber as number, 0, 50),
    enabled: orgIdNumber != null && lookedUpUserId != null,
  })

  const joinRequestsQuery = useQuery({
    queryKey: ['organization-join-requests', orgIdNumber, 'PENDING'],
    queryFn: () => getOrganizationJoinRequests(orgIdNumber as number, 'PENDING'),
    enabled: orgIdNumber != null,
  })

  const membership = useMemo(() => {
    if (lookedUpUserId == null) return null
    return (orgMembersQuery.data?.content ?? []).find(
      (m) => m.authUserId === lookedUpUserId,
    )
  }, [orgMembersQuery.data, lookedUpUserId])

  const pendingRequest = useMemo(() => {
    if (lookedUpUserId == null) return null
    return (joinRequestsQuery.data ?? []).find((r) => r.authUserId === lookedUpUserId)
  }, [joinRequestsQuery.data, lookedUpUserId])

  const userMeters = metersQuery.data ?? []
  const firstMeterNumber = userMeters[0]?.meterNumber ?? ''

  useEffect(() => {
    if (firstMeterNumber && !meterNumber) setMeterNumber(firstMeterNumber)
  }, [firstMeterNumber, meterNumber])

  const invalidateUserState = () => {
    void queryClient.invalidateQueries({ queryKey: ['admin-meters-by-user', lookedUpUserId] })
    void queryClient.invalidateQueries({ queryKey: ['organization-members', orgIdNumber, lookedUpUserId] })
    void queryClient.invalidateQueries({ queryKey: ['organization-join-requests', orgIdNumber] })
  }

  // Step 2 — membership
  const membershipMutation = useMutation({
    mutationFn: () =>
      createOrganizationMembership(orgIdNumber as number, {
        authUserId: lookedUpUserId as number,
      }),
    onSuccess: () => {
      setPageError(null)
      invalidateUserState()
    },
    onError: (error: unknown) => {
      setPageError(error instanceof ApiError ? error.message : 'Membership could not be created.')
    },
  })

  const approveRequestMutation = useMutation({
    mutationFn: () =>
      approveJoinRequest(orgIdNumber as number, pendingRequest?.id as number),
    onSuccess: () => {
      setPageError(null)
      invalidateUserState()
    },
    onError: (error: unknown) => {
      setPageError(
        error instanceof ApiError
          ? `${error.message} Note: approving requires an owner or admin role within the organization.`
          : 'The request could not be approved.',
      )
    },
  })

  // Step 3 — meter create + assign
  const meterMutation = useMutation({
    mutationFn: async () => {
      const meter = await createMeter({
        meterNumber,
        meterType,
        connectionType,
        phaseType,
        sanctionedLoadKw: Number(sanctionedLoad) || 0,
        city: undefined,
        remarks: 'Created during consumer account preparation',
      })
      return assignMeter(meter.id, {
        authUserId: lookedUpUserId as number,
        ...(orgIdNumber != null ? { organizationId: orgIdNumber } : {}),
      })
    },
    onSuccess: () => {
      setPageError(null)
      setMeterNumber('')
      invalidateUserState()
    },
    onError: (error: unknown) => {
      setPageError(
        error instanceof ApiError
          ? `Meter could not be created or assigned: ${error.message}`
          : 'Meter could not be created or assigned.',
      )
    },
  })

  // Step 4 — reading create + verify
  const readingMutation = useMutation({
    mutationFn: async () => {
      const reading = await createAdminReading({
        authUserId: lookedUpUserId as number,
        meterNumber: readingMeterNumber,
        previousReading: Number(readingPrev) || 0,
        currentReading: Number(readingCurrent) || 0,
        readingDate,
        remarks: 'Recorded during consumer account preparation',
      })
      const verified = await verifyAdminReading(reading.id)
      setCreatedReading(verified)
      if (!billingMonth) setBillingMonth(String(verified.billingMonth))
      if (!billingYear) setBillingYear(String(verified.billingYear))
      return verified
    },
    onSuccess: () => {
      setPageError(null)
      invalidateUserState()
    },
    onError: (error: unknown) => {
      setPageError(
        error instanceof ApiError
          ? `Reading could not be recorded: ${error.message}`
          : 'Reading could not be recorded.',
      )
    },
  })

  const [readingMeterNumberInput, setReadingMeterNumberInput] = useState('')
  const readingMeterNumber = readingMeterNumberInput || firstMeterNumber || meterNumber

  // Step 5 — bill generation
  const billMutation = useMutation({
    mutationFn: () => {
      if (!createdReading) throw new ApiError('Verify a reading before generating a bill.', 400)
      return generateBill({
        authUserId: lookedUpUserId as number,
        meterReadingId: createdReading.id,
        meterNumber: createdReading.meterNumber,
        previousReading: Number(createdReading.previousReading),
        currentReading: Number(createdReading.currentReading),
        billingMonth: Number(billingMonth) || createdReading.billingMonth,
        billingYear: Number(billingYear) || createdReading.billingYear,
        dueDate,
        remarks: 'Generated during consumer account preparation',
      })
    },
    onSuccess: (bill) => {
      setCreatedBill(bill)
      setPageError(null)
    },
    onError: (error: unknown) => {
      setPageError(
        error instanceof ApiError
          ? `Bill could not be generated: ${error.message}`
          : 'Bill could not be generated.',
      )
    },
  })

  const userIdNumber = lookedUpUserId != null ? Number(lookedUpUserId) : null
  const organizations = organizationsQuery.data?.content ?? []

  return (
    <div className="mx-auto w-full max-w-4xl space-y-6">
      <Card>
        <CardHeader>
          <div className="flex items-start gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-navy-900 text-white">
              <UserRound className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-navy-900">Prepare consumer account</h1>
              <p className="mt-0.5 text-sm text-slate-500">
                Link a brand-new consumer to an organization, meter, reading and bill — all driven
                by the userId from their registration. No personal details are shown or guessed.
              </p>
            </div>
          </div>
        </CardHeader>
        <CardBody className="space-y-4">
          {pageError && <Alert tone="error">{pageError}</Alert>}

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
            <Input
              label="User ID (from registration)"
              hint="The userId returned after the consumer registers or logs in."
              inputMode="numeric"
              placeholder="e.g. 31"
              value={userId}
              onChange={(e) => setUserId(e.target.value.replace(/\D/g, ''))}
            />
            <Button
              className="sm:mb-0.5"
              disabled={!userId}
              onClick={() => {
                setPageError(null)
                setCreatedReading(null)
                setCreatedBill(null)
                setLookedUpUserId(Number(userId))
              }}
            >
              <Search className="h-4 w-4" aria-hidden="true" />
              Look up user
            </Button>
          </div>

          {userIdNumber != null && (
            <div className="flex flex-wrap items-center gap-2">
              <Badge tone="blue">User #{userIdNumber}</Badge>
              {userMeters.length > 0 && (
                <Badge tone="green">{userMeters.length} meter(s) assigned</Badge>
              )}
              {membership && <Badge tone="green">Member of this organization</Badge>}
              {pendingRequest && <Badge tone="amber">Pending join request</Badge>}
            </div>
          )}
        </CardBody>
      </Card>

      {userIdNumber == null ? (
        <Card>
          <CardBody>
            <EmptyState
              icon={Hash}
              title="Start with the userId"
              description="Enter the userId of the newly registered consumer to begin preparing their account."
            />
          </CardBody>
        </Card>
      ) : (
        <>
          {/* Step 2: membership */}
          <Card>
            <CardHeader>
              <SectionHeader
                step="2"
                icon={<Building2 className="h-4 w-4" aria-hidden="true" />}
                title="Link the organization membership"
                description="Consumers are billed through an organization. Create an ACTIVE membership for the user, or approve their pending request."
              />
            </CardHeader>
            <CardBody className="space-y-4">
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
                  value={selectedOrgId}
                  onChange={(e) => setSelectedOrgId(e.target.value)}
                />
              </div>

              {orgIdNumber != null && (
                <div className="space-y-3">
                  {membership?.membershipStatus === 'ACTIVE' ? (
                    <Alert tone="success" title="Already a member">
                      User #{userIdNumber} has an ACTIVE membership in this organization.
                    </Alert>
                  ) : (
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <p className="text-sm text-slate-600">
                        {membership
                          ? `The user has a ${membership.membershipStatus.toLowerCase()} membership — creating will reactivate it.`
                          : 'The user has no membership in this organization yet.'}
                      </p>
                      <Button
                        loading={membershipMutation.isPending}
                        onClick={() => membershipMutation.mutate()}
                        className="sm:shrink-0"
                      >
                        <Link2 className="h-4 w-4" aria-hidden="true" />
                        Create membership
                      </Button>
                    </div>
                  )}

                  {pendingRequest && membership?.membershipStatus !== 'ACTIVE' && (
                    <div className="flex flex-col gap-3 rounded-md border border-slate-200 bg-slate-50/60 p-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-navy-900">
                          Pending request from this user
                        </p>
                        <p className="mt-0.5 text-xs text-slate-500">
                          Submitted {formatDateTime(pendingRequest.createdAt)} · Request{' '}
                          #{pendingRequest.id}
                        </p>
                      </div>
                      <Button
                        variant="secondary"
                        loading={approveRequestMutation.isPending}
                        onClick={() => approveRequestMutation.mutate()}
                        className="sm:shrink-0"
                      >
                        <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                        Approve request
                      </Button>
                    </div>
                  )}
                </div>
              )}
            </CardBody>
          </Card>

          {/* Step 3: meter */}
          <Card>
            <CardHeader>
              <SectionHeader
                step="3"
                icon={<Gauge className="h-4 w-4" aria-hidden="true" />}
                title="Activate the meter"
                description="Create a meter and assign it to the consumer. The meter number must be unique."
              />
            </CardHeader>
            <CardBody className="space-y-4">
              {userMeters.length > 0 ? (
                <div className="space-y-2">
                  <Alert tone="success" title={`${userMeters.length} meter(s) already assigned`}>
                    {userMeters.map((meter: MeterSummary) => (
                      <p key={meter.id} className="font-mono">
                        {meter.meterNumber} · {meter.status}
                      </p>
                    ))}
                  </Alert>
                  <p className="text-xs text-slate-500">
                    If the user needs a second meter, create and assign a new one below.
                  </p>
                </div>
              ) : (
                <p className="text-sm text-slate-600">
                  No meters are assigned to this user yet.
                </p>
              )}

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Input
                  label="Meter number"
                  placeholder="e.g. MTR-2026-0042"
                  value={meterNumber}
                  onChange={(e) => setMeterNumber(e.target.value)}
                />
                <Select
                  label="Meter type"
                  options={[
                    { value: 'SMART', label: 'Smart' },
                    { value: 'ANALOG', label: 'Analog' },
                    { value: 'PREPAID', label: 'Prepaid' },
                  ]}
                  value={meterType}
                  onChange={(e) => setMeterType(e.target.value as 'SMART' | 'ANALOG' | 'PREPAID')}
                />
                <Select
                  label="Connection type"
                  options={[
                    { value: 'RESIDENTIAL', label: 'Residential' },
                    { value: 'COMMERCIAL', label: 'Commercial' },
                  ]}
                  value={connectionType}
                  onChange={(e) =>
                    setConnectionType(e.target.value as 'RESIDENTIAL' | 'COMMERCIAL')
                  }
                />
                <Select
                  label="Phase"
                  options={[
                    { value: 'SINGLE_PHASE', label: 'Single phase' },
                    { value: 'THREE_PHASE', label: 'Three phase' },
                  ]}
                  value={phaseType}
                  onChange={(e) => setPhaseType(e.target.value as 'SINGLE_PHASE' | 'THREE_PHASE')}
                />
                <Input
                  label="Sanctioned load (kW)"
                  inputMode="decimal"
                  value={sanctionedLoad}
                  onChange={(e) => setSanctionedLoad(e.target.value)}
                />
              </div>

              <Button
                loading={meterMutation.isPending}
                disabled={!meterNumber.trim() || !sanctionedLoad}
                onClick={() => meterMutation.mutate()}
              >
                <Link2 className="h-4 w-4" aria-hidden="true" />
                Create and assign meter
              </Button>
            </CardBody>
          </Card>

          {/* Step 4: reading */}
          <Card>
            <CardHeader>
              <SectionHeader
                step="4"
                icon={<Gauge className="h-4 w-4" aria-hidden="true" />}
                title="Record and verify the first reading"
                description="Record a meter reading for the consumer, then verify it so it can be billed."
              />
            </CardHeader>
            <CardBody className="space-y-4">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <Input
                  label="Meter number"
                  placeholder={firstMeterNumber || 'e.g. MTR-2026-0042'}
                  value={readingMeterNumberInput}
                  onChange={(e) => setReadingMeterNumberInput(e.target.value)}
                />
                <Input
                  label="Previous reading"
                  inputMode="decimal"
                  value={readingPrev}
                  onChange={(e) => setReadingPrev(e.target.value)}
                />
                <Input
                  label="Current reading"
                  inputMode="decimal"
                  placeholder="e.g. 1250"
                  value={readingCurrent}
                  onChange={(e) => setReadingCurrent(e.target.value)}
                />
                <Input
                  label="Reading date"
                  type="date"
                  value={readingDate}
                  max={today()}
                  onChange={(e) => setReadingDate(e.target.value)}
                />
              </div>

              <Button
                loading={readingMutation.isPending}
                disabled={!readingMeterNumber || !readingCurrent}
                onClick={() => readingMutation.mutate()}
              >
                <Gauge className="h-4 w-4" aria-hidden="true" />
                Record and verify reading
              </Button>

              {createdReading && (
                <Alert tone="success" title="Reading verified">
                  Reading #{createdReading.id} · {createdReading.meterNumber} ·{' '}
                  {createdReading.unitsConsumed} units · {createdReading.status}
                </Alert>
              )}
            </CardBody>
          </Card>

          {/* Step 5: bill */}
          <Card>
            <CardHeader>
              <SectionHeader
                step="5"
                icon={<ReceiptText className="h-4 w-4" aria-hidden="true" />}
                title="Generate the first bill"
                description="Create the consumer's first bill from the verified reading. The consumer will see it and can pay from their wallet."
              />
            </CardHeader>
            <CardBody className="space-y-4">
              {!createdReading ? (
                <p className="text-sm text-slate-600">
                  Record and verify a reading first — the bill is generated from the verified
                  reading.
                </p>
              ) : (
                <>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <Input
                      label="Billing month (1-12)"
                      inputMode="numeric"
                      value={billingMonth}
                      onChange={(e) => setBillingMonth(e.target.value.replace(/\D/g, ''))}
                    />
                    <Input
                      label="Billing year"
                      inputMode="numeric"
                      value={billingYear}
                      onChange={(e) => setBillingYear(e.target.value.replace(/\D/g, ''))}
                    />
                    <Input
                      label="Due date"
                      type="date"
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                    />
                  </div>

                  <Button
                    loading={billMutation.isPending}
                    disabled={!billingMonth || !billingYear || !dueDate}
                    onClick={() => billMutation.mutate()}
                  >
                    <ReceiptText className="h-4 w-4" aria-hidden="true" />
                    Generate bill
                  </Button>

                  {createdBill && (
                    <Alert tone="success" title="Bill generated">
                      Bill #{createdBill.id} · {createdBill.meterNumber} ·{' '}
                      {formatCurrency(createdBill.totalAmount)} · {createdBill.billStatus}
                    </Alert>
                  )}
                </>
              )}
            </CardBody>
          </Card>

          {/* Status summary */}
          <Card>
            <CardHeader>
              <SectionHeader
                step="✓"
                icon={<ShieldCheck className="h-4 w-4" aria-hidden="true" />}
                title={`Account status for user #${userIdNumber}`}
                description="Live state of the prepared account as recorded by the backend."
              />
            </CardHeader>
            <CardBody className="space-y-2">
              <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
                <div className="rounded-md border border-slate-100 bg-slate-50/60 px-3 py-2.5">
                  <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    Organization membership
                  </dt>
                  <dd className="mt-0.5 text-navy-900">
                    {membership
                      ? `${membership.organizationName} · ${membership.membershipStatus}`
                      : 'Not linked'}
                  </dd>
                </div>
                <div className="rounded-md border border-slate-100 bg-slate-50/60 px-3 py-2.5">
                  <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    Meters
                  </dt>
                  <dd className="mt-0.5 font-mono text-navy-900">
                    {userMeters.length > 0
                      ? userMeters.map((m) => m.meterNumber).join(', ')
                      : 'None yet'}
                  </dd>
                </div>
                <div className="rounded-md border border-slate-100 bg-slate-50/60 px-3 py-2.5">
                  <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    Verified reading
                  </dt>
                  <dd className="mt-0.5 text-navy-900">
                    {createdReading
                      ? `#${createdReading.id} · ${createdReading.unitsConsumed} units`
                      : 'Not recorded in this session'}
                  </dd>
                </div>
                <div className="rounded-md border border-slate-100 bg-slate-50/60 px-3 py-2.5">
                  <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    Bill
                  </dt>
                  <dd className="mt-0.5 text-navy-900">
                    {createdBill
                      ? `#${createdBill.id} · ${formatCurrency(createdBill.totalAmount)}`
                      : 'Not generated in this session'}
                  </dd>
                </div>
              </dl>
              <p className="text-xs text-slate-500">
                The consumer now sees daily usage, the bill, and can recharge their wallet and pay
                from it.
              </p>
            </CardBody>
          </Card>
        </>
      )}
    </div>
  )
}
