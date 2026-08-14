import { useEffect, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import {
  ArrowLeft,
  Building2,
  CalendarDays,
  MapPin,
  PencilLine,
  ShieldCheck,
  ShieldOff,
  Users,
} from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Input } from '@/components/ui/Input'
import { Pagination } from '@/components/ui/Pagination'
import { Select } from '@/components/ui/Select'
import { AdminOrganizationStructure } from '@/pages/admin/AdminOrganizationStructure'
import {
  activateOrganization,
  getAdminOrganization,
  getOrganizationMembers,
  suspendOrganization,
  updateOrganization,
  type OrganizationType,
} from '@/services/organizations'
import { ApiError } from '@/types/api'
import { formatDate, formatDateTime, formatEnumLabel } from '@/utils/format'

const editSchema = z.object({
  name: z.string().min(1, 'Name is required').max(150, 'Name must not exceed 150 characters'),
  organizationType: z.string().min(1, 'Type is required'),
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional(),
  email: z
    .string()
    .max(150, 'Email must not exceed 150 characters')
    .refine((value) => value === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value), {
      message: 'Enter a valid email address',
    })
    .optional(),
  phone: z.string().max(20, 'Phone must not exceed 20 characters').optional(),
  addressLine1: z.string().max(255).optional(),
  addressLine2: z.string().max(255).optional(),
  city: z.string().max(100).optional(),
  state: z.string().max(100).optional(),
  country: z.string().max(100).optional(),
  postalCode: z.string().max(20).optional(),
})

type EditForm = z.infer<typeof editSchema>

const TYPE_OPTIONS: OrganizationType[] = ['HOSTEL', 'INSTITUTION', 'APARTMENT', 'COMMERCIAL']

const MEMBER_PAGE_SIZE = 10

function DetailPanel({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="rounded-md border border-slate-100 bg-slate-50/60 px-3 py-2.5">
      <dt className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 break-words text-sm text-navy-900">{value || '—'}</dd>
    </div>
  )
}

function SectionTitle({
  icon,
  children,
  extra,
}: {
  icon: ReactNode
  children: ReactNode
  extra?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <h2 className="flex items-center gap-2.5 text-base font-semibold text-navy-900">
        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-navy-50 text-navy-600">
          {icon}
        </span>
        {children}
      </h2>
      {extra}
    </div>
  )
}

export function AdminOrganizationDetailPage() {
  const { organizationId } = useParams<{ organizationId: string }>()
  const id = Number(organizationId)
  const queryClient = useQueryClient()
  const [pageError, setPageError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [confirmAction, setConfirmAction] = useState<'suspend' | 'activate' | null>(null)
  const [memberPage, setMemberPage] = useState(0)

  // Start at the first member page whenever a different organization is opened.
  useEffect(() => {
    setMemberPage(0)
  }, [id])

  const organizationQuery = useQuery({
    queryKey: ['admin-organization', id],
    queryFn: () => getAdminOrganization(id),
    enabled: Number.isFinite(id),
  })

  const membersQuery = useQuery({
    queryKey: ['organization-members', id],
    queryFn: () => getOrganizationMembers(id, 0, 50),
    enabled: Number.isFinite(id),
  })

  const editForm = useForm<EditForm>({
    resolver: zodResolver(editSchema),
    values: {
      name: organizationQuery.data?.name ?? '',
      organizationType: organizationQuery.data?.organizationType ?? '',
      description: organizationQuery.data?.description ?? '',
      email: organizationQuery.data?.email ?? '',
      phone: organizationQuery.data?.phone ?? '',
      addressLine1: organizationQuery.data?.addressLine1 ?? '',
      addressLine2: organizationQuery.data?.addressLine2 ?? '',
      city: organizationQuery.data?.city ?? '',
      state: organizationQuery.data?.state ?? '',
      country: organizationQuery.data?.country ?? '',
      postalCode: organizationQuery.data?.postalCode ?? '',
    },
  })

  const invalidateOrganization = () => {
    void queryClient.invalidateQueries({ queryKey: ['admin-organization', id] })
    void queryClient.invalidateQueries({ queryKey: ['admin-organizations'] })
  }

  const statusMutation = useMutation({
    mutationFn: (action: 'suspend' | 'activate') =>
      action === 'suspend' ? suspendOrganization(id) : activateOrganization(id),
    onSuccess: (organization) => {
      setConfirmAction(null)
      setPageError(null)
      setSuccessMessage(
        `Organization ${organization.status === 'ACTIVE' ? 'activated' : 'suspended'}.`,
      )
      invalidateOrganization()
    },
    onError: (error: unknown) => {
      setConfirmAction(null)
      setPageError(error instanceof ApiError ? error.message : 'Unable to update the organization.')
    },
  })

  const editMutation = useMutation({
    mutationFn: (values: EditForm) =>
      updateOrganization(id, {
        name: values.name,
        organizationType: values.organizationType as OrganizationType,
        description: values.description || undefined,
        email: values.email || undefined,
        phone: values.phone || undefined,
        addressLine1: values.addressLine1 || undefined,
        addressLine2: values.addressLine2 || undefined,
        city: values.city || undefined,
        state: values.state || undefined,
        country: values.country || undefined,
        postalCode: values.postalCode || undefined,
      }),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      setSuccessMessage('Organization details saved.')
      invalidateOrganization()
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          editForm.setError(fieldError.field as keyof EditForm, { message: fieldError.message })
        }
        setPageError(null)
      } else {
        setPageError(
          error instanceof ApiError
            ? error.message
            : 'Unable to save the organization. You may need an owner or admin role within this organization.',
        )
      }
    },
  })

  if (organizationQuery.isLoading) return <LoadingState label="Loading organization…" />

  if (organizationQuery.isError || !Number.isFinite(id) || !organizationQuery.data) {
    return (
      <ErrorState
        title="Organization not found"
        message={organizationQuery.error?.message ?? 'This organization does not exist.'}
        onRetry={() => organizationQuery.refetch()}
      />
    )
  }

  const organization = organizationQuery.data
  const allMembers = membersQuery.data?.content ?? []
  const memberTotal = membersQuery.data?.totalElements ?? allMembers.length
  const memberPages = Math.max(1, Math.ceil(allMembers.length / MEMBER_PAGE_SIZE))
  const visibleMembers = allMembers.slice(
    memberPage * MEMBER_PAGE_SIZE,
    (memberPage + 1) * MEMBER_PAGE_SIZE,
  )
  const confirmTitle = confirmAction === 'suspend' ? 'Suspend organization' : 'Activate organization'
  const memberStart = memberPage * MEMBER_PAGE_SIZE + 1
  const memberEnd = Math.min(allMembers.length, (memberPage + 1) * MEMBER_PAGE_SIZE)

  return (
    <div className="mx-auto w-full max-w-4xl space-y-5">
      <Link
        to="/admin/organizations"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 transition-colors hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to organizations
      </Link>

      {pageError && (
        <Alert tone="error" className="mb-4">
          {pageError}
        </Alert>
      )}
      {successMessage && (
        <Alert tone="success" className="mb-4">
          {successMessage}
        </Alert>
      )}

      {/* Identity */}
      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-card bg-volt-600 text-white shadow-sm">
              <Building2 className="h-6 w-6" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <h1 className="break-words text-xl font-bold tracking-tight text-navy-900">
                {organization.name}
              </h1>
              <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
                <span className="font-mono">{organization.organizationCode}</span>
                {organization.city && (
                  <>
                    <span aria-hidden="true">·</span>
                    <span className="inline-flex items-center gap-1">
                      <MapPin className="h-3 w-3" aria-hidden="true" />
                      {organization.city}
                    </span>
                  </>
                )}
              </p>
            </div>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <Badge tone={statusTone(organization.status)}>
              {formatEnumLabel(organization.status)}
            </Badge>
            <Badge tone="blue">{formatEnumLabel(organization.organizationType)}</Badge>
          </div>
        </CardHeader>

        <CardBody className="space-y-5">
          {editing ? (
            <form
              onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
              className="space-y-4"
              noValidate
            >
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Input
                  label="Name"
                  error={editForm.formState.errors.name?.message}
                  {...editForm.register('name')}
                />
                <Select
                  label="Type"
                  options={[
                    { value: '', label: 'Select a type…' },
                    ...TYPE_OPTIONS.map((type) => ({
                      value: type,
                      label: formatEnumLabel(type),
                    })),
                  ]}
                  error={editForm.formState.errors.organizationType?.message}
                  {...editForm.register('organizationType')}
                />
                <Input
                  label="Email"
                  error={editForm.formState.errors.email?.message}
                  {...editForm.register('email')}
                />
                <Input
                  label="Phone"
                  error={editForm.formState.errors.phone?.message}
                  {...editForm.register('phone')}
                />
                <Input
                  label="Address line 1"
                  error={editForm.formState.errors.addressLine1?.message}
                  {...editForm.register('addressLine1')}
                />
                <Input
                  label="Address line 2"
                  error={editForm.formState.errors.addressLine2?.message}
                  {...editForm.register('addressLine2')}
                />
                <Input
                  label="City"
                  error={editForm.formState.errors.city?.message}
                  {...editForm.register('city')}
                />
                <Input
                  label="State"
                  error={editForm.formState.errors.state?.message}
                  {...editForm.register('state')}
                />
                <Input
                  label="Country"
                  error={editForm.formState.errors.country?.message}
                  {...editForm.register('country')}
                />
                <Input
                  label="Postal code"
                  error={editForm.formState.errors.postalCode?.message}
                  {...editForm.register('postalCode')}
                />
              </div>
              <div className="w-full">
                <label
                  htmlFor="org-description"
                  className="mb-1.5 block text-sm font-medium text-navy-800"
                >
                  Description
                </label>
                <textarea
                  id="org-description"
                  rows={3}
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm text-navy-900 shadow-sm focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                  {...editForm.register('description')}
                />
                {editForm.formState.errors.description && (
                  <p className="mt-1.5 text-sm text-red-600" role="alert">
                    {editForm.formState.errors.description.message}
                  </p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                <Button type="submit" loading={editMutation.isPending}>
                  Save changes
                </Button>
                <Button variant="secondary" onClick={() => setEditing(false)}>
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <>
              {organization.description && (
                <p className="whitespace-pre-wrap border-l-2 border-volt-200 pl-3 text-sm leading-relaxed text-slate-700">
                  {organization.description}
                </p>
              )}

              <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
                <DetailPanel label="Email" value={organization.email} />
                <DetailPanel label="Phone" value={organization.phone} />
                <DetailPanel label="Address" value={organization.addressLine1} />
                <DetailPanel label="City" value={organization.city} />
                <DetailPanel label="State" value={organization.state} />
                <DetailPanel label="Country" value={organization.country} />
                <DetailPanel label="Postal code" value={organization.postalCode} />
                <DetailPanel
                  label="Created by"
                  value={
                    organization.createdByAuthUserId
                      ? `User #${organization.createdByAuthUserId}`
                      : null
                  }
                />
                <DetailPanel label="Registered" value={formatDate(organization.createdAt)} />
              </dl>

              <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-4">
                <Button variant="secondary" onClick={() => setEditing(true)}>
                  <PencilLine className="h-4 w-4" aria-hidden="true" />
                  Edit organization
                </Button>
                {organization.status !== 'SUSPENDED' ? (
                  <Button
                    variant="secondary"
                    onClick={() => setConfirmAction('suspend')}
                    disabled={organization.status === 'INACTIVE'}
                  >
                    <ShieldOff className="h-4 w-4" aria-hidden="true" />
                    Suspend
                  </Button>
                ) : (
                  <Button variant="success" onClick={() => setConfirmAction('activate')}>
                    <ShieldCheck className="h-4 w-4" aria-hidden="true" />
                    Activate
                  </Button>
                )}
              </div>
              <p className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
                <CalendarDays className="h-3.5 w-3.5" aria-hidden="true" />
                Registered {formatDateTime(organization.createdAt)} · Last updated{' '}
                {formatDateTime(organization.updatedAt)}
              </p>
            </>
          )}
        </CardBody>
      </Card>

      {/* Members */}
      <Card>
        <CardHeader>
          <SectionTitle
            icon={<Users className="h-4 w-4" aria-hidden="true" />}
            extra={
              <span className="inline-flex h-7 items-center rounded-md bg-navy-50 px-2.5 text-xs font-semibold text-navy-800 ring-1 ring-inset ring-navy-100">
                {memberTotal}
              </span>
            }
          >
            Members ({memberTotal})
          </SectionTitle>
        </CardHeader>
        <CardBody className="p-0">
          {membersQuery.isLoading ? (
            <LoadingState label="Loading members…" />
          ) : membersQuery.isError ? (
            <ErrorState
              title="Could not load members"
              message={membersQuery.error?.message}
              onRetry={() => membersQuery.refetch()}
            />
          ) : allMembers.length === 0 ? (
            <EmptyState
              icon={Users}
              title="No members"
              description="Members of this organization will appear here."
            />
          ) : (
            <>
              <ul className="divide-y divide-slate-100">
                {visibleMembers.map((membership) => (
                  <li
                    key={membership.id}
                    className="flex flex-col gap-2 px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-navy-50 text-sm font-bold text-navy-700 ring-1 ring-inset ring-navy-100">
                        #{membership.authUserId}
                      </span>
                      <div className="min-w-0">
                        <p className="break-words text-sm font-semibold text-navy-900">
                          User #{membership.authUserId}
                        </p>
                        <p className="mt-0.5 text-xs text-slate-500">
                          Joined {formatDate(membership.joinedAt)}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-2 sm:pl-0">
                      <Badge tone="blue">{formatEnumLabel(membership.membershipRole)}</Badge>
                      <Badge tone={statusTone(membership.membershipStatus)}>
                        {formatEnumLabel(membership.membershipStatus)}
                      </Badge>
                    </div>
                  </li>
                ))}
              </ul>
              <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 px-5 py-4 sm:flex-row">
                <p className="text-xs text-slate-500">
                  Showing {memberStart}–{memberEnd} of {allMembers.length}
                </p>
                <Pagination
                  page={memberPage}
                  totalPages={memberPages}
                  onChange={setMemberPage}
                  className="sm:justify-end"
                />
              </div>
            </>
          )}
        </CardBody>
      </Card>

      {/* Structure hierarchy */}
      <AdminOrganizationStructure organizationId={id} />

      <ConfirmDialog
        open={confirmAction !== null}
        title={confirmTitle}
        description={
          confirmAction === 'suspend'
            ? `Suspend "${organization.name}"? Suspended organizations can be reactivated at any time.`
            : `Activate "${organization.name}"? The organization will be usable again.`
        }
        confirmLabel={confirmTitle}
        tone={confirmAction === 'suspend' ? 'danger' : 'primary'}
        loading={statusMutation.isPending}
        onConfirm={() => confirmAction && statusMutation.mutate(confirmAction)}
        onCancel={() => setConfirmAction(null)}
      />
    </div>
  )
}
