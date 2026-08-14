import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import { ArrowLeft, MessageSquareWarning, PencilLine, Send } from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, priorityTone, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import {
  addComplaintComment,
  getComplaintDetail,
  updateComplaint,
} from '@/services/complaints'
import { ApiError } from '@/types/api'
import { formatCategoryLabel, formatDateTime } from '@/utils/format'

const commentSchema = z.object({
  commentText: z
    .string()
    .min(1, 'Comment is required')
    .max(1000, 'Comment must not exceed 1000 characters'),
})

type CommentForm = z.infer<typeof commentSchema>

const editSchema = z.object({
  subject: z
    .string()
    .min(10, 'Subject must be between 10 and 200 characters')
    .max(200, 'Subject must be between 10 and 200 characters'),
  description: z
    .string()
    .min(20, 'Description must be between 20 and 5000 characters')
    .max(5000, 'Description must be between 20 and 5000 characters'),
})

type EditForm = z.infer<typeof editSchema>

export function ComplaintDetailPage() {
  const { complaintId } = useParams<{ complaintId: string }>()
  const id = Number(complaintId)
  const queryClient = useQueryClient()
  const [pageError, setPageError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)

  const complaintQuery = useQuery({
    queryKey: ['complaint', id],
    queryFn: () => getComplaintDetail(id),
    enabled: Number.isFinite(id),
  })

  const commentForm = useForm<CommentForm>({
    resolver: zodResolver(commentSchema),
    defaultValues: { commentText: '' },
  })

  const editForm = useForm<EditForm>({
    resolver: zodResolver(editSchema),
    values: {
      subject: complaintQuery.data?.subject ?? '',
      description: complaintQuery.data?.description ?? '',
    },
  })

  const commentMutation = useMutation({
    mutationFn: (values: CommentForm) => addComplaintComment(id, values.commentText),
    onSuccess: () => {
      commentForm.reset()
      setPageError(null)
      void queryClient.invalidateQueries({ queryKey: ['complaint', id] })
    },
    onError: (error: unknown) => {
      setPageError(error instanceof ApiError ? error.message : 'Unable to add the comment.')
    },
  })

  const editMutation = useMutation({
    mutationFn: (values: EditForm) => updateComplaint(id, values),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      void queryClient.invalidateQueries({ queryKey: ['complaint', id] })
      void queryClient.invalidateQueries({ queryKey: ['my-complaints'] })
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          editForm.setError(fieldError.field as keyof EditForm, { message: fieldError.message })
        }
        setPageError(null)
      } else {
        setPageError(error instanceof ApiError ? error.message : 'Unable to save changes.')
      }
    },
  })

  if (complaintQuery.isLoading) return <LoadingState label="Loading complaint…" />

  if (complaintQuery.isError || !Number.isFinite(id) || !complaintQuery.data) {
    return (
      <ErrorState
        title="Complaint not found"
        message={complaintQuery.error?.message ?? 'This complaint does not exist or is not yours.'}
        onRetry={() => complaintQuery.refetch()}
      />
    )
  }

  const complaint = complaintQuery.data

  return (
    <div className="mx-auto w-full max-w-3xl space-y-6">
      <Link
        to="/consumer/complaints"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to complaints
      </Link>

      {pageError && (
        <Alert tone="error" className="mb-4">
          {pageError}
        </Alert>
      )}

      <Card>
        <CardHeader className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
              <MessageSquareWarning className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <h1 className="text-lg font-bold text-navy-900">{complaint.subject}</h1>
              <p className="font-mono text-xs text-slate-500">{complaint.ticketNumber}</p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={priorityTone(complaint.priority)}>{complaint.priority}</Badge>
            <Badge tone={statusTone(complaint.status)}>{complaint.status}</Badge>
          </div>
        </CardHeader>
        <CardBody>
          {editing ? (
            <form
              onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
              className="space-y-4"
              noValidate
            >
              <div className="w-full">
                <label htmlFor="edit-subject" className="mb-1.5 block text-sm font-medium text-navy-800">
                  Subject
                </label>
                <input
                  id="edit-subject"
                  className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm text-navy-900 shadow-sm focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                  {...editForm.register('subject')}
                />
                {editForm.formState.errors.subject && (
                  <p className="mt-1.5 text-sm text-red-600" role="alert">
                    {editForm.formState.errors.subject.message}
                  </p>
                )}
              </div>
              <div className="w-full">
                <label htmlFor="edit-description" className="mb-1.5 block text-sm font-medium text-navy-800">
                  Description
                </label>
                <textarea
                  id="edit-description"
                  rows={5}
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
              {complaint.description && (
                <p className="whitespace-pre-wrap text-sm text-slate-700">{complaint.description}</p>
              )}
              <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-500">
                <span>Category: {formatCategoryLabel(complaint.categoryName)}</span>
                <span>Raised {formatDateTime(complaint.createdAt)}</span>
                <span>Last updated {formatDateTime(complaint.updatedAt)}</span>
              </div>
              {complaint.status === 'OPEN' && (
                <Button
                  variant="secondary"
                  size="sm"
                  className="mt-4"
                  onClick={() => setEditing(true)}
                >
                  <PencilLine className="h-4 w-4" aria-hidden="true" />
                  Edit complaint
                </Button>
              )}
            </>
          )}
        </CardBody>
      </Card>

      {/* Status history */}
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-navy-900">Status history</h2>
        </CardHeader>
        <CardBody>
          <ol className="space-y-4">
            {[...complaint.statusHistory].reverse().map((entry, index) => (
              <li key={index} className="flex gap-3">
                <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-volt-500" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="text-sm font-medium text-navy-900">
                    {entry.toStatus}
                    {entry.fromStatus ? ` (was ${entry.fromStatus})` : ''}
                  </p>
                  <p className="text-xs text-slate-500">{formatDateTime(entry.changedAt)}</p>
                </div>
              </li>
            ))}
          </ol>
        </CardBody>
      </Card>

      {/* Comments */}
      <Card>
        <CardHeader>
          <h2 className="text-base font-semibold text-navy-900">
            Comments ({complaint.comments.length})
          </h2>
        </CardHeader>
        <CardBody className="space-y-4">
          {complaint.comments.length === 0 ? (
            <p className="text-sm text-slate-500">No comments yet.</p>
          ) : (
            <ul className="space-y-3">
              {complaint.comments.map((comment) => (
                <li key={comment.id} className="rounded-md border border-slate-100 bg-slate-50 px-4 py-3">
                  <p className="whitespace-pre-wrap text-sm text-slate-700">{comment.commentText}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {comment.adminComment ? 'VOLTARAS support' : 'You'} ·{' '}
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
                <label htmlFor="comment-text" className="mb-1.5 block text-sm font-medium text-navy-800">
                  Add a comment
                </label>
                <textarea
                  id="comment-text"
                  rows={3}
                  placeholder="Share additional details or ask a question."
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
    </div>
  )
}
