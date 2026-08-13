import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { resetPassword } from '@/services/auth'
import { ApiError } from '@/types/api'

const schema = z
  .object({
    token: z.string().min(1, 'Reset token is required'),
    newPassword: z
      .string()
      .min(1, 'New password is required')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/,
        'Password must be at least 8 characters with at least one uppercase letter, one lowercase letter, and one digit',
      ),
    confirmNewPassword: z.string().min(1, 'Confirm new password is required'),
  })
  .refine((data) => data.newPassword === data.confirmNewPassword, {
    path: ['confirmNewPassword'],
    message: 'Passwords do not match',
  })

type ResetForm = z.infer<typeof schema>

export function ResetPasswordPage() {
  const [done, setDone] = useState<string | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetForm>({
    resolver: zodResolver(schema),
    defaultValues: { token: '', newPassword: '', confirmNewPassword: '' },
  })

  const onSubmit = async (values: ResetForm) => {
    setServerError(null)
    try {
      const result = await resetPassword({
        token: values.token,
        newPassword: values.newPassword,
        confirmNewPassword: values.confirmNewPassword,
      })
      setDone(result.message)
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Unable to reset your password.',
      )
    }
  }

  return (
    <div className="w-full max-w-md">
      <Card>
        <CardBody className="px-6 py-8 sm:px-8">
          {done ? (
            <div className="text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50">
                <CheckCircle2 className="h-6 w-6 text-emerald-600" aria-hidden="true" />
              </div>
              <h1 className="mt-4 text-xl font-bold text-navy-900">Password reset</h1>
              <p className="mt-2 text-sm text-slate-500">{done}</p>
              <div className="mt-6">
                <Link
                  to="/login"
                  className="inline-flex h-11 items-center rounded-md bg-volt-600 px-4 text-sm font-medium text-white transition-colors hover:bg-volt-700"
                >
                  Sign in with your new password
                </Link>
              </div>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold text-navy-900">Choose a new password</h1>
              <p className="mt-1 text-sm text-slate-500">
                Enter the one-time token you received by email, then choose a new password.
              </p>

              {serverError && (
                <Alert tone="error" className="mt-4">
                  {serverError}
                </Alert>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
                <Input
                  label="Reset token"
                  autoComplete="off"
                  placeholder="Token from your email"
                  error={errors.token?.message}
                  {...register('token')}
                />
                <Input
                  label="New password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="At least 8 characters, with upper, lower and digit"
                  error={errors.newPassword?.message}
                  {...register('newPassword')}
                />
                <Input
                  label="Confirm new password"
                  type="password"
                  autoComplete="new-password"
                  placeholder="Repeat your new password"
                  error={errors.confirmNewPassword?.message}
                  {...register('confirmNewPassword')}
                />
                <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
                  Reset password
                </Button>
              </form>

              <p className="mt-6 text-center text-sm text-slate-500">
                <Link to="/login" className="font-medium text-volt-600 hover:text-volt-700">
                  Back to sign in
                </Link>
              </p>
            </>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
