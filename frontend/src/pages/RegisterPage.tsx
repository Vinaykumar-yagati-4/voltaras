import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardBody } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { useAuth } from '@/hooks/useAuth'
import { ApiError } from '@/types/api'
import type { RegisterPayload } from '@/types/auth'

/**
 * Client-side validation mirrors auth-service RegisterRequest exactly:
 * fullName 2-100, email, phone ^[0-9]{10}$, password with 1 upper/1 lower/1 digit
 * min 8, confirmPassword match, address max 500.
 */
const schema = z
  .object({
    fullName: z
      .string()
      .min(1, 'Full name is required')
      .min(2, 'Full name must be between 2 and 100 characters')
      .max(100, 'Full name must be between 2 and 100 characters'),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Email must be a valid email address')
      .max(255, 'Email must not exceed 255 characters'),
    phone: z.string().min(1, 'Phone number is required').regex(/^[0-9]{10}$/, 'Phone number must be exactly 10 digits'),
    password: z
      .string()
      .min(1, 'Password is required')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/,
        'Password must be at least 8 characters with at least one uppercase letter, one lowercase letter, and one digit',
      ),
    confirmPassword: z.string().min(1, 'Confirm password is required'),
    address: z.string().min(1, 'Address is required').max(500, 'Address must not exceed 500 characters'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })

type RegisterForm = z.infer<typeof schema>

export function RegisterPage() {
  const { registerAndLogin } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      fullName: '',
      email: '',
      phone: '',
      password: '',
      confirmPassword: '',
      address: '',
    },
  })

  const onSubmit = async (values: RegisterForm) => {
    setServerError(null)
    const payload: RegisterPayload = {
      fullName: values.fullName,
      email: values.email,
      phone: values.phone,
      password: values.password,
      confirmPassword: values.confirmPassword,
      address: values.address,
    }
    try {
      const user = await registerAndLogin(payload)
      navigate(user.role === 'ADMIN' ? '/admin' : '/consumer', { replace: true })
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 409) {
          setServerError('An account with this email or phone already exists.')
        } else if (error.fieldErrors.length > 0) {
          for (const fieldError of error.fieldErrors) {
            setError(fieldError.field as keyof RegisterForm, { message: fieldError.message })
          }
        } else {
          setServerError(error.message)
        }
      } else {
        setServerError('Unable to create your account. Please try again.')
      }
    }
  }

  return (
    <div className="w-full max-w-md">
      <Card>
        <CardBody className="px-6 py-8 sm:px-8">
          <h1 className="text-2xl font-bold text-navy-900">Create your account</h1>
          <p className="mt-1 text-sm text-slate-500">
            Register as a VOLTARAS consumer. New accounts start with the consumer role.
          </p>

          {serverError && (
            <Alert tone="error" className="mt-4">
              {serverError}
            </Alert>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
            <Input
              label="Full name"
              autoComplete="name"
              placeholder="Your full name"
              error={errors.fullName?.message}
              {...register('fullName')}
            />
            <Input
              label="Email address"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Phone number"
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              placeholder="10-digit mobile number"
              error={errors.phone?.message}
              {...register('phone')}
            />
            <Input
              label="Password"
              type="password"
              autoComplete="new-password"
              placeholder="At least 8 characters, with upper, lower and digit"
              hint="At least 8 characters with one uppercase letter, one lowercase letter and one digit."
              error={errors.password?.message}
              {...register('password')}
            />
            <Input
              label="Confirm password"
              type="password"
              autoComplete="new-password"
              placeholder="Repeat your password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Input
              label="Address"
              autoComplete="street-address"
              placeholder="Flat / street / locality"
              error={errors.address?.message}
              {...register('address')}
            />
            <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
              Create account
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-volt-600 hover:text-volt-700">
              Sign in
            </Link>
          </p>
        </CardBody>
      </Card>
    </div>
  )
}
