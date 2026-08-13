import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCircle2, Mail } from 'lucide-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { forgotPassword } from '@/services/auth'
import { ApiError } from '@/types/api'

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
})

type ForgotForm = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [done, setDone] = useState<string | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotForm>({
    resolver: zodResolver(schema),
    defaultValues: { email: '' },
  })

  const onSubmit = async (values: ForgotForm) => {
    setServerError(null)
    try {
      const result = await forgotPassword(values.email)
      setDone(result.message)
    } catch (error) {
      setServerError(
        error instanceof ApiError ? error.message : 'Unable to send the reset request.',
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
              <h1 className="mt-4 text-xl font-bold text-navy-900">Check your email</h1>
              <p className="mt-2 text-sm text-slate-500">{done}</p>
              <div className="mt-6">
                <Link
                  to="/login"
                  className="inline-flex h-11 items-center rounded-md bg-volt-600 px-4 text-sm font-medium text-white transition-colors hover:bg-volt-700"
                >
                  Back to sign in
                </Link>
              </div>
            </div>
          ) : (
            <>
              <h1 className="text-2xl font-bold text-navy-900">Reset your password</h1>
              <p className="mt-1 text-sm text-slate-500">
                Enter your email and we will send password reset instructions if an account
                exists.
              </p>

              {serverError && (
                <Alert tone="error" className="mt-4">
                  {serverError}
                </Alert>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
                <Input
                  label="Email address"
                  type="email"
                  autoComplete="email"
                  placeholder="you@example.com"
                  error={errors.email?.message}
                  endAdornment={<Mail className="h-4 w-4 text-slate-400" aria-hidden="true" />}
                  {...register('email')}
                />
                <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
                  Send reset instructions
                </Button>
              </form>

              <p className="mt-6 text-center text-sm text-slate-500">
                Remembered your password?{' '}
                <Link to="/login" className="font-medium text-volt-600 hover:text-volt-700">
                  Sign in
                </Link>
              </p>
            </>
          )}
        </CardBody>
      </Card>
    </div>
  )
}
