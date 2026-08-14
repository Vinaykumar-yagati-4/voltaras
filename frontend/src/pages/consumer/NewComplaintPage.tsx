import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ArrowLeft, MessageSquarePlus } from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { Input } from '@/components/ui/Input'
import { LoadingState } from '@/components/ui/LoadingState'
import { Select } from '@/components/ui/Select'
import { createComplaint, getComplaintCategories } from '@/services/complaints'
import { ApiError } from '@/types/api'
import { formatCategoryLabel } from '@/utils/format'

const complaintSchema = z.object({
  categoryId: z.string().min(1, 'Choose a category'),
  subject: z
    .string()
    .min(10, 'Subject must be between 10 and 200 characters')
    .max(200, 'Subject must be between 10 and 200 characters'),
  description: z
    .string()
    .min(20, 'Description must be between 20 and 5000 characters')
    .max(5000, 'Description must be between 20 and 5000 characters'),
})

type ComplaintForm = z.infer<typeof complaintSchema>

export function NewComplaintPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [pageError, setPageError] = useState<string | null>(null)

  const categoriesQuery = useQuery({
    queryKey: ['complaint-categories'],
    queryFn: getComplaintCategories,
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<ComplaintForm>({
    resolver: zodResolver(complaintSchema),
    defaultValues: { categoryId: '', subject: '', description: '' },
  })

  const createMutation = useMutation({
    mutationFn: (values: ComplaintForm) =>
      createComplaint({
        categoryId: Number(values.categoryId),
        subject: values.subject,
        description: values.description,
      }),
    onSuccess: (complaint) => {
      setPageError(null)
      void queryClient.invalidateQueries({ queryKey: ['my-complaints'] })
      void queryClient.invalidateQueries({ queryKey: ['complaint-counts'] })
      navigate(`/consumer/complaints/${complaint.id}`, { replace: true })
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          setError(fieldError.field as keyof ComplaintForm, { message: fieldError.message })
        }
        setPageError(null)
      } else {
        setPageError(error instanceof ApiError ? error.message : 'Unable to raise the complaint.')
      }
    },
  })

  if (categoriesQuery.isLoading) return <LoadingState label="Loading complaint categories…" />

  if (categoriesQuery.isError) {
    return (
      <ErrorState
        title="Could not load categories"
        message={categoriesQuery.error.message}
        onRetry={() => categoriesQuery.refetch()}
      />
    )
  }

  const onSubmit = (values: ComplaintForm) => {
    setPageError(null)
    createMutation.mutate(values)
  }

  return (
    <div className="mx-auto w-full max-w-2xl space-y-6">
      <Link
        to="/consumer/complaints"
        className="inline-flex h-11 items-center gap-1.5 text-sm font-medium text-volt-600 hover:text-volt-700"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to complaints
      </Link>

      <Card>
        <CardHeader className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
            <MessageSquarePlus className="h-5 w-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-navy-900">Raise a complaint</h1>
            <p className="text-sm text-slate-500">
              We will look into it and keep you updated on this ticket.
            </p>
          </div>
        </CardHeader>
        <CardBody>
          {pageError && (
            <Alert tone="error" className="mb-4">
              {pageError}
            </Alert>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <Select
              label="Category"
              options={[
                { value: '', label: 'Select a category…' },
                ...(categoriesQuery.data ?? []).map((category) => ({
                  value: String(category.id),
                  label: `${formatCategoryLabel(category.name)}${category.description ? ` — ${category.description}` : ''}`,
                })),
              ]}
              error={errors.categoryId?.message}
              {...register('categoryId')}
            />
            <Input
              label="Subject"
              placeholder="Short summary of the issue"
              error={errors.subject?.message}
              {...register('subject')}
            />
            <div className="w-full">
              <label htmlFor="description" className="mb-1.5 block text-sm font-medium text-navy-800">
                Description
              </label>
              <textarea
                id="description"
                rows={6}
                placeholder="Describe what happened, when, and any steps you already tried."
                aria-invalid={errors.description ? true : undefined}
                aria-describedby={errors.description ? 'description-error' : undefined}
                className="w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm text-navy-900 shadow-sm transition-colors focus:border-volt-500 focus:outline-none focus:ring-2 focus:ring-volt-500/30"
                {...register('description')}
              />
              {errors.description && (
                <p id="description-error" className="mt-1.5 text-sm text-red-600" role="alert">
                  {errors.description.message}
                </p>
              )}
            </div>

            <div className="pt-2">
              <Button type="submit" size="lg" loading={isSubmitting} className="w-full sm:w-auto">
                Submit complaint
              </Button>
            </div>
          </form>
        </CardBody>
      </Card>
    </div>
  )
}
