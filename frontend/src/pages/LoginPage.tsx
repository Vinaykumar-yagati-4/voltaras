import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/types/api'

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginForm = z.infer<typeof schema>


export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [showPassword, setShowPassword] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)

  const state = location.state as { reason?: string } | null
  const sessionExpired = state?.reason === 'session-expired'

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })


  const onSubmit = async (values: LoginForm) => {
    setServerError(null)
    try {
      const user = await login(values.email, values.password)
      navigate(user.role === 'ADMIN' ? '/admin' : '/consumer', { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError(error.message)
      } else {
        setServerError('Unable to sign in. Please try again.')
      }
    }
  }

  return (
    <div className="w-full max-w-md">
      <Card>
        <CardBody className="px-6 py-8 sm:px-8">
          <h1 className="text-2xl font-bold text-navy-900">Welcome back</h1>
          <p className="mt-1 text-sm text-slate-500">
            Sign in to your VOLTARAS account to manage your electricity services.
          </p>

          {sessionExpired && (
            <Alert tone="warning" title="Your session expired" className="mt-4">
              Please sign in again to continue.
            </Alert>
          )}
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
              {...register('email')}
            />
            <Input
              label="Password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="••••••••"
              error={errors.password?.message}
              endAdornment={
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-500 hover:text-navy-800"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              }
              {...register('password')}
            />
            <div className="flex items-center justify-between">
              <Link
                to="/forgot-password"
                className="text-sm font-medium text-volt-600 hover:text-volt-700"
              >
                Forgot password?
              </Link>
            </div>
            <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
              Sign in
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            New to VOLTARAS?{' '}
            <Link to="/register" className="font-medium text-volt-600 hover:text-volt-700">
              Create an account
            </Link>
          </p>
        </CardBody>
      </Card>
    </div>
  )
}
