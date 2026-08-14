import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Save, UserRound } from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ErrorState } from '@/components/ui/ErrorState'
import { Input } from '@/components/ui/Input'
import { LoadingState } from '@/components/ui/LoadingState'
import { Select } from '@/components/ui/Select'
import { createMyProfile, getMyProfile, updateMyProfile } from '@/services/profile'
import { ApiError } from '@/types/api'
import { formatDate } from '@/utils/format'

const profileSchema = z.object({
  fullName: z
    .string()
    .min(2, 'Full name must be between 2 and 100 characters')
    .max(100, 'Full name must be between 2 and 100 characters'),
  phone: z
    .string()
    .regex(/^[0-9]{10}$/, 'Phone number must be exactly 10 digits')
    .or(z.literal('')),
  address: z.string().max(500, 'Address must not exceed 500 characters'),
  city: z.string().max(100, 'City must not exceed 100 characters'),
  state: z.string().max(100, 'State must not exceed 100 characters'),
  country: z.string().max(100, 'Country must not exceed 100 characters'),
  postalCode: z
    .string()
    .regex(/^[A-Za-z0-9\- ]{3,10}$/, 'Postal code must be 3 to 10 alphanumeric characters')
    .or(z.literal('')),
  gender: z.enum(['', 'MALE', 'FEMALE', 'OTHER']),
})

type ProfileForm = z.infer<typeof profileSchema>

function toFormValue(value: string | null | undefined): string {
  return value ?? ''
}

export function ProfilePage() {
  const queryClient = useQueryClient()
  const [pageError, setPageError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getMyProfile,
    retry: (failureCount, error) => {
      // A missing profile (404) is not a transient error — show the create form.
      if (error instanceof ApiError && error.status === 404) return false
      return failureCount < 2
    },
  })

  const isMissing = profileQuery.isError && profileQuery.error instanceof ApiError && profileQuery.error.status === 404

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: '',
      phone: '',
      address: '',
      city: '',
      state: '',
      country: '',
      postalCode: '',
      gender: '',
    },
  })

  const initialValues = useMemo<ProfileForm | null>(() => {
    const profile = profileQuery.data
    if (!profile) return null
    return {
      fullName: profile.fullName,
      phone: toFormValue(profile.phone),
      address: toFormValue(profile.address),
      city: toFormValue(profile.city),
      state: toFormValue(profile.state),
      country: toFormValue(profile.country),
      postalCode: toFormValue(profile.postalCode),
      gender: (toFormValue(profile.gender) as ProfileForm['gender']) ?? '',
    }
  }, [profileQuery.data])

  useEffect(() => {
    if (initialValues) reset(initialValues)
  }, [initialValues, reset])

  const saveMutation = useMutation({
    mutationFn: (values: ProfileForm) => {
      const payload = {
        fullName: values.fullName,
        phone: values.phone || null,
        address: values.address || null,
        city: values.city || null,
        state: values.state || null,
        country: values.country || null,
        postalCode: values.postalCode || null,
        gender: (values.gender || null) as 'MALE' | 'FEMALE' | 'OTHER' | null,
      }
      return isMissing ? createMyProfile(payload) : updateMyProfile(payload)
    },
    onSuccess: () => {
      setPageError(null)
      setSuccess(true)
      void queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
    onError: (error: unknown) => {
      setSuccess(false)
      if (error instanceof ApiError && error.fieldErrors.length > 0) {
        for (const fieldError of error.fieldErrors) {
          setError(fieldError.field as keyof ProfileForm, { message: fieldError.message })
        }
        setPageError(null)
      } else {
        setPageError(error instanceof ApiError ? error.message : 'Unable to save your profile.')
      }
    },
  })

  const onSubmit = (values: ProfileForm) => {
    setSuccess(false)
    saveMutation.mutate(values)
  }

  if (profileQuery.isLoading) return <LoadingState label="Loading your profile…" />

  if (profileQuery.isError && !isMissing) {
    return (
      <ErrorState
        title="Could not load your profile"
        message={profileQuery.error.message}
        onRetry={() => profileQuery.refetch()}
      />
    )
  }

  return (
    <div className="mx-auto w-full max-w-2xl space-y-6">
      <Card>
        <CardHeader className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-md bg-navy-900 text-white">
            <UserRound className="h-5 w-5" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-navy-900">My profile</h1>
            <p className="text-sm text-slate-500">
              {isMissing ? 'Create your profile to manage your account details.' : 'Keep your account details up to date.'}
            </p>
          </div>
        </CardHeader>
        <CardBody>
          {pageError && (
            <Alert tone="error" className="mb-4">
              {pageError}
            </Alert>
          )}
          {success && (
            <Alert tone="success" title="Profile saved" className="mb-4">
              Your profile information was updated successfully.
            </Alert>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <Input
              label="Full name"
              placeholder="Your full name"
              error={errors.fullName?.message}
              {...register('fullName')}
            />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Input
                label="Phone"
                inputMode="numeric"
                placeholder="10-digit mobile number"
                error={errors.phone?.message}
                {...register('phone')}
              />
              <Select
                label="Gender"
                options={[
                  { value: '', label: 'Not specified' },
                  { value: 'MALE', label: 'Male' },
                  { value: 'FEMALE', label: 'Female' },
                  { value: 'OTHER', label: 'Other' },
                ]}
                error={errors.gender?.message}
                {...register('gender')}
              />
            </div>
            <Input
              label="Address"
              placeholder="House / flat details"
              error={errors.address?.message}
              {...register('address')}
            />
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Input
                label="City"
                placeholder="City"
                error={errors.city?.message}
                {...register('city')}
              />
              <Input
                label="State"
                placeholder="State"
                error={errors.state?.message}
                {...register('state')}
              />
              <Input
                label="Country"
                placeholder="Country"
                error={errors.country?.message}
                {...register('country')}
              />
            </div>
            <Input
              label="Postal code"
              placeholder="PIN code"
              error={errors.postalCode?.message}
              {...register('postalCode')}
            />

            <div className="pt-2">
              <Button type="submit" size="lg" loading={isSubmitting} className="w-full sm:w-auto">
                <Save className="h-4 w-4" aria-hidden="true" />
                {isMissing ? 'Create profile' : 'Save changes'}
              </Button>
            </div>
          </form>

          {profileQuery.data && (
            <p className="mt-4 text-xs text-slate-500">
              Profile updated on {formatDate(profileQuery.data.updatedAt)}.
            </p>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
