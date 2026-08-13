import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, KeyRound, ShieldCheck, User } from 'lucide-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/types/api'
import { cn } from '@/utils/cn'

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginForm = z.infer<typeof schema>

const DEMO_CREDENTIALS = {
  admin: { email: 'sunny.demo@voltaras.local', label: 'Admin' },
  consumer: { email: 'vinay.demo@voltaras.local', label: 'Consumer' },
  password: 'Voltaras@123',
}

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
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  const fillDemo = (email: string) => {
    setValue('email', email, { shouldValidate: true })
    setValue('password', DEMO_CREDENTIALS.password, { shouldValidate: true })
    setServerError(null)
  }

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

      {/* Local Docker demo credentials */}
      <div className="mt-5 rounded-card border border-dashed border-volt-300 bg-volt-50/60 px-5 py-4">
        <div className="flex items-center gap-2">
          <KeyRound className="h-4 w-4 text-volt-600" aria-hidden="true" />
          <p className="text-sm font-semibold text-navy-900">Local Docker demo credentials</p>
        </div>
        <p className="mt-1 text-xs text-slate-500">
          For the local demo stack only. Shared password for both accounts.
        </p>
        <div className="mt-3 space-y-2">
          {[DEMO_CREDENTIALS.admin, DEMO_CREDENTIALS.consumer].map((account) => (
            <div
              key={account.label}
              className="flex items-center justify-between gap-3 rounded-md border border-slate-200 bg-white px-3 py-2"
            >
              <div className="flex min-w-0 items-center gap-2">
                {account.label === 'Admin' ? (
                  <ShieldCheck className="h-4 w-4 shrink-0 text-volt-600" aria-hidden="true" />
                ) : (
                  <User className="h-4 w-4 shrink-0 text-emerald-600" aria-hidden="true" />
                )}
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-navy-900">{account.label}</p>
                  <p className="truncate text-xs text-slate-500">{account.email}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => fillDemo(account.email)}
                className={cn(
                  'inline-flex min-h-11 shrink-0 items-center rounded-md border px-3 text-xs font-medium transition-colors',
                  'border-volt-300 text-volt-700 hover:bg-volt-50',
                )}
              >
                Use
              </button>
            </div>
          ))}
        </div>
        <p className="mt-2 font-mono text-xs text-slate-500">
          Password: <span className="font-semibold text-navy-800">Voltaras@123</span>
        </p>
      </div>
    </div>
  )
}
