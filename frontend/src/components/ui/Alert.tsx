import type { ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react'
import { cn } from '@/utils/cn'

type Tone = 'info' | 'success' | 'warning' | 'error'

const config: Record<Tone, { icon: typeof Info; classes: string; iconClasses: string }> = {
  info: {
    icon: Info,
    classes: 'border-volt-200 bg-volt-50 text-navy-800',
    iconClasses: 'text-volt-600',
  },
  success: {
    icon: CheckCircle2,
    classes: 'border-emerald-200 bg-emerald-50 text-emerald-900',
    iconClasses: 'text-emerald-600',
  },
  warning: {
    icon: AlertTriangle,
    classes: 'border-amber-200 bg-amber-50 text-amber-900',
    iconClasses: 'text-amber-600',
  },
  error: {
    icon: XCircle,
    classes: 'border-red-200 bg-red-50 text-red-800',
    iconClasses: 'text-red-600',
  },
}

export function Alert({
  tone = 'info',
  title,
  children,
  className,
}: {
  tone?: Tone
  title?: string
  children?: ReactNode
  className?: string
}) {
  const { icon: Icon, classes, iconClasses } = config[tone]
  return (
    <div
      role={tone === 'error' ? 'alert' : 'status'}
      className={cn('flex items-start gap-2.5 rounded-md border px-4 py-3 text-sm', classes, className)}
    >
      <Icon className={cn('mt-0.5 h-4 w-4 shrink-0', iconClasses)} aria-hidden="true" />
      <div className="min-w-0">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className={title ? 'mt-0.5' : ''}>{children}</div>}
      </div>
    </div>
  )
}
