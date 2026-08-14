import { useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import {
  ArrowLeft,
  History,
  MessageSquareText,
  MessageSquareWarning,
  RefreshCw,
  Send,
  UserCheck,
  type LucideIcon,
} from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { useAuth } from '@/hooks/useAuth'
import {
  addAdminComplaintComment,
  assignComplaint,
  getAdminComplaintDetail,
  updateComplaintStatus,
  type ComplaintStatus,
} from '@/services/complaints'
import { ApiError } from '@/types/api'
import { formatCategoryLabel, formatDateTime, formatEnumLabel } from '@/utils/format'
import { cn } from '@/utils/cn'

/**
 * Only transitions defined by the complaint service are offered:
 * OPEN → IN_PROGRESS | RESOLVED, IN_PROGRESS → RESOLVED, RESOLVED → CLOSED.
 * CLOSED is terminal and offers no actions.
 */
const ALLOWED_TRANSITIONS: Record<ComplaintStatus, ComplaintStatus[]> = {
  OPEN: ['IN_PROGRESS', 'RESOLVED'],
  IN_PROGRESS: ['RESOLVED'],
  RESOLVED: ['CLOSED'],
  CLOSED: [],
}

const STATUS_ACCENT: Record<ComplaintStatus, string> = {
  OPEN: 'border-t-amber-400',
  IN_PROGRESS: 'border-t-volt-500',
  RESOLVED: 'border-t-emerald-500',
  CLOSED: 'border-t-slate-400',
}

const commentSchema = z.object({
  commentText: z
    .string()
    .min(1, 'Comment is required')
    .max(1000, 'Comment must not exceed 1000 characters'),
})

type CommentForm = z.infer<typeof commentSchema>

const assignSchema = z.object({
  assignedTo: z
    .string()
    .min(1, 'Admin user ID is required')
    .regex(/^\d+$/, 'Admin user ID must be a number'),
})

type AssignForm = z.infer<typeof assignSchema>

function SectionTitle({ icon: Icon, children }: { icon: LucideIcon; children: ReactNode }) {
  return (
    <h2 className="flex items-center gap-2.5 text-base font-semibold text-navy-900">
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-navy-50 text-navy-600">
        <Icon className="h-4 w-4" aria-hidden="true" />
      </span>
      {children}
    </h2>
  )
}

function DetailItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 break-words text-navy-900">{value}</dd>
    </div>
  )
}

export function AdminComplaintDetailPage() {
  const { complaintId } = useParams<{ complaintId: string }>()
  const id = Number(complaintId)
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [pageError, setPageError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [confirmStatus, setConfirmStatus] = useState<ComplaintStatus | null>(null)

  const complaintQuery = useQuery({
    queryKey: ['admin-complaint', id],
    queryFn: () => getAdminComplaintDetail(id),
    enabled: Number.isFinite(id),
  })

  const commentForm = useForm<CommentForm>({
    resolver: zodResolver(commentSchema),
    defaultValues: { commentText: '' },
  })

  const assignForm = useForm<AssignForm>({
    resolver: zodResolver(assignSchema),
    defaultValues: { assignedTo: user ? String(user.userId) : '' },
  })

  const invalidateComplaint = () => {
    void queryClient.invalidateQueries({ queryKey: ['admin-complaint', id] })
    void queryClient.invalidateQueries({ queryKey: ['admin-complaints'] })
    void queryClient.invalidateQueries({ queryKey: ['complaint-status-counts'] })
  }

  const statusMutation = useMutation({
    mutationFn: (status: ComplaintStatus) => updateComplaintStatus(id, status),
    onSuccess: (result) => {
      setConfirmStatus(null)
      setPageError(null)
      setSuccessMessage(
        `Status updated to ${formatEnumLabel(result.currentStatus)} for ${result.ticketNumber}.`,
      )
      invalidateComplaint()
    },
    onError: (error: unknown) => {
      setConfirmStatus(null)
      setPageError(error instanceof ApiError ? error.message : 'Unable to update the status.')
    },
  })

  const assignMutation = useMutation({
    mutationFn: (values: AssignForm) => assignComplaint(id, Number(values.assignedTo)),
    onSuccess: () => {
      setPageError(null)
      setSuccessMessage('Complaint assigned.')
      invalidateComplaint()
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          assignForm.setError(fieldError.field as keyof AssignForm, { message: fieldError.message })
        }
        setPageError(null)
      } else {
        setPageError(error instanceof ApiError ? error.message : 'Unable to assign the complaint.')
      }
    },
  })

  const commentMutation = useMutation({
    mutationFn: (values: CommentForm) => addAdminComplaintComment(id, values.commentText),
    onSuccess: () => {
      commentForm.reset()
      setPageError(null)
      setSuccessMessage('Comment added.')
      invalidateComplaint()
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          commentForm.setError(fieldError.field as keyof CommentForm, {
            message: fieldError.message,
          })
        }
        setPageError(null)
      } else {
        setPageError(error instanceof ApiError ? error.message : 'Unable to add the comment.')
      }
    },
  })

  if (complaintQuery.isLoading) return <LoadingState label="Loading complaint…" />

  if (complaintQuery.isError || !Number.isFinite(id) || !complaintQuery.data) {
    return (
      <ErrorState
        title="Complaint not found"
        message={complaintQuery.error?.message ?? 'This complaint does not exist.'}
        onRetry={() => complaintQuery.refetch()}
      />
    )
  }

  const complaint = complaintQuery.data
  const allowedTargets = ALLOWED_TRANSITIONS[complaint.status] ?? []
  const canAssign = complaint.status === 'OPEN' || complaint.status === 'IN_PROGRESS'
  const confirmLabel =
    confirmStatus === 'RESOLVED'
      ? 'Resolve complaint'
      : confirmStatus === 'CLOSED'
        ? 'Close complaint'
        : 'Update status'

  const requestTransition = (status: ComplaintStatus) => {
    if (status === 'RESOLVED' || status === 'CLOSED') {
      setConfirmStatus(status)
      return
    }
    statusMutation.mutate(status)
  }

  const historyEntries = [...complaint.statusHistory].reverse()

  return (
    <div className="mx-auto w-full max-w-3xl space-y-5">
      <Link
        to="/admin/complaints"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 transition-colors hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to complaints
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

      {/* Summary */}
      <Card className={cn('border-t-4', STATUS_ACCENT[complaint.status])}>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-volt-600 text-white shadow-sm">
              <MessageSquareWarning className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <p className="font-mono text-xs font-medium text-slate-500">
                {complaint.ticketNumber}
              </p>
              <h1 className="break-words text-lg font-bold leading-snug text-navy-900">
                {complaint.subject}
              </h1>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={priorityTone(complaint.priority)}>
              {formatEnumLabel(complaint.priority)}
            </Badge>
            <Badge tone={statusTone(complaint.status)}>
              {formatEnumLabel(complaint.status)}
            </Badge>
          </div>
        </CardHeader>
        <CardBody className="space-y-5">
          {complaint.description && (
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                Description
              </p>
              <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-700">
                {complaint.description}
              </p>
            </div>
          )}

          <dl className="grid grid-cols-1 gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
            <DetailItem
              label="Category"
              value={formatCategoryLabel(complaint.categoryName)}
            />
            <DetailItem label="Consumer" value={`Consumer #${complaint.consumerId}`} />
            <DetailItem label="Raised" value={formatDateTime(complaint.createdAt)} />
            <DetailItem
              label="Assigned to"
              value={complaint.assignedTo ? `Admin #${complaint.assignedTo}` : 'Unassigned'}
            />
            <DetailItem
              label="Resolved"
              value={complaint.resolvedAt ? formatDateTime(complaint.resolvedAt) : '—'}
            />
            <DetailItem
              label="Closed"
              value={complaint.closedAt ? formatDateTime(complaint.closedAt) : '—'}
            />
          </dl>
        </CardBody>
      </Card>

      {/* Lifecycle actions */}
      {allowedTargets.length > 0 && (
        <Card>
          <CardHeader>
            <SectionTitle icon={RefreshCw}>Update status</SectionTitle>
          </CardHeader>
          <CardBody>
            <p className="text-sm text-slate-500">
              Move this complaint to the next lifecycle stage. Every change is recorded in the
              status history and notifies the consumer.
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              {allowedTargets.map((target) => (
                <Button
                  key={target}
                  variant={target === 'CLOSED' ? 'secondary' : 'primary'}
                  loading={statusMutation.isPending}
                  disabled={statusMutation.isPending}
                  onClick={() => requestTransition(target)}
                >
                  {target === 'IN_PROGRESS'
                    ? 'Start working on it'
                    : target === 'RESOLVED'
                      ? 'Mark as resolved'
                      : 'Close complaint'}
                </Button>
              ))}
            </div>
          </CardBody>
        </Card>
      )}

      {/* Assignment */}
      {canAssign && (
        <Card>
          <CardHeader>
            <SectionTitle icon={UserCheck}>Assignment</SectionTitle>
          </CardHeader>
          <CardBody>
            <form
              onSubmit={assignForm.handleSubmit((values) => assignMutation.mutate(values))}
              className="flex flex-col gap-3 sm:flex-row sm:items-end"
              noValidate
            >
              <div className="w-full sm:max-w-52">
                <label
                  htmlFor="assign-admin-id"
                  className="mb-1.5 block text-sm font-medium text-navy-800"
                >
                  Admin user ID
                </label>
                <input
                  id="assign-admin-id"
                  inputMode="numeric"
                  className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-navy-900 shadow-sm focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                  {...assignForm.register('assignedTo')}
                />
                {assignForm.formState.errors.assignedTo && (
                  <p className="mt-1.5 text-sm text-red-600" role="alert">
                    {assignForm.formState.errors.assignedTo.message}
                  </p>
                )}
              </div>
              <Button type="submit" variant="secondary" loading={assignMutation.isPending}>
                <UserCheck className="h-4 w-4" aria-hidden="true" />
                Assign
              </Button>
            </form>
            <p className="mt-3 text-xs text-slate-500">
              Assigning to yourself routes the ticket to your inbox. A complaint can be assigned
              while it is open or in progress.
            </p>
          </CardBody>
        </Card>
      )}

      {/* Status history */}
      <Card>
        <CardHeader>
          <SectionTitle icon={History}>Status history</SectionTitle>
        </CardHeader>
        <CardBody>
          {historyEntries.length === 0 ? (
            <p className="text-sm text-slate-500">No status changes recorded.</p>
          ) : (
            <ol className="relative space-y-5">
              <span
                aria-hidden="true"
                className="absolute bottom-3 left-[5px] top-3 w-px bg-slate-200"
              />
              {historyEntries.map((entry, index) => (
                <li key={index} className="relative flex gap-3">
                  <span
                    className={cn(
                      'relative z-10 mt-1 h-3 w-3 shrink-0 rounded-full ring-4 ring-white',
                      index === 0 ? 'bg-volt-500' : 'bg-slate-300',
                    )}
                    aria-hidden="true"
                  />
                  <div className="min-w-0">
                    <p className="break-words text-sm font-medium text-navy-900">
                      {formatEnumLabel(entry.toStatus)}
                      {entry.fromStatus && (
                        <span className="font-normal text-slate-500">
                          {' '}
                          · from {formatEnumLabel(entry.fromStatus)}
                        </span>
                      )}
                    </p>
                    <p className="text-xs text-slate-500">
                      {formatDateTime(entry.changedAt)} · by user #{entry.changedBy}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </CardBody>
      </Card>

      {/* Comments */}
      <Card>
        <CardHeader>
          <SectionTitle icon={MessageSquareText}>
            Comments ({complaint.comments.length})
          </SectionTitle>
        </CardHeader>
        <CardBody className="space-y-4">
          {complaint.comments.length === 0 ? (
            <p className="text-sm text-slate-500">No comments yet.</p>
          ) : (
            <ul className="space-y-3">
              {complaint.comments.map((comment) => (
                <li
                  key={comment.id}
                  className={cn(
                    'rounded-md border px-4 py-3',
                    comment.adminComment
                      ? 'border-volt-100 bg-volt-50/40'
                      : 'border-slate-100 bg-slate-50',
                  )}
                >
                  <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-700">
                    {comment.commentText}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {comment.adminComment ? 'VOLTARAS support' : `Consumer #${comment.authorId}`} ·{' '}
                    {formatDateTime(comment.createdAt)}
                  </p>
                </li>
              ))}
            </ul>
          )}

          {complaint.status !== 'CLOSED' && (
            <form
              onSubmit={commentForm.handleSubmit((values) => commentMutation.mutate(values))}
              className="space-y-3"
              noValidate
            >
              <div className="w-full">
                <label
                  htmlFor="admin-comment-text"
                  className="mb-1.5 block text-sm font-medium text-navy-800"
                >
                  Add a resolution note
                </label>
                <textarea
                  id="admin-comment-text"
                  rows={3}
                  placeholder="Share an update with the consumer."
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm text-navy-900 shadow-sm focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                  {...commentForm.register('commentText')}
                />
                {commentForm.formState.errors.commentText && (
                  <p className="mt-1.5 text-sm text-red-600" role="alert">
                    {commentForm.formState.errors.commentText.message}
                  </p>
                )}
              </div>
              <Button type="submit" loading={commentMutation.isPending}>
                <Send className="h-4 w-4" aria-hidden="true" />
                Post comment
              </Button>
            </form>
          )}
        </CardBody>
      </Card>

      <ConfirmDialog
        open={confirmStatus !== null}
        title={confirmLabel}
        description={`Move ${complaint.ticketNumber} to "${formatEnumLabel(confirmStatus)}"? The consumer will be notified and the change will be recorded in the status history.`}
        confirmLabel={confirmLabel}
        loading={statusMutation.isPending}
        onConfirm={() => confirmStatus && statusMutation.mutate(confirmStatus)}
        onCancel={() => setConfirmStatus(null)}
      />
    </div>
  )
}
