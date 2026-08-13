import type { HTMLAttributes } from 'react'
import { cn } from '@/utils/cn'

type Tone = 'navy' | 'blue' | 'green' | 'amber' | 'red' | 'slate'

const tones: Record<Tone, string> = {
  navy: 'bg-navy-100 text-navy-800 ring-navy-200',
  blue: 'bg-volt-50 text-volt-700 ring-volt-200',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  slate: 'bg-slate-100 text-slate-600 ring-slate-200',
}

/**
 * Distinct, accessible status colours:
 * green = done/positive, blue = actively being worked, amber = waiting,
 * red = problem, slate = no longer relevant.
 */
export function statusTone(status: string): Tone {
  const s = status.toUpperCase()
  if (
    s.includes('SUCCESS') ||
    s.includes('ACTIVE') ||
    s.includes('VERIFIED') ||
    s.includes('RESOLVED') ||
    s.includes('PAID') ||
    s.includes('COMPLETED')
  ) {
    return 'green'
  }
  if (s.includes('IN_PROGRESS') || s.includes('PROCESSING') || s.includes('REVIEW')) {
    return 'blue'
  }
  if (s.includes('OPEN') || s.includes('PENDING') || s.includes('SUBMITTED')) {
    return 'amber'
  }
  if (
    s.includes('OVERDUE') ||
    s.includes('CANCELLED') ||
    s.includes('REJECTED') ||
    s.includes('FAILED') ||
    s.includes('SUSPEND') ||
    s.includes('ERROR')
  ) {
    return 'red'
  }
  if (s.includes('CLOSED') || s.includes('EXPIRED')) {
    return 'slate'
  }
  return 'navy'
}

export function priorityTone(priority: string): Tone {
  const p = priority.toUpperCase()
  if (p.includes('URGENT')) return 'red'
  if (p.includes('HIGH')) return 'amber'
  if (p.includes('NORMAL') || p.includes('MEDIUM')) return 'blue'
  return 'slate'
}

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone
}

export function Badge({ tone = 'slate', className, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset',
        tones[tone],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  )
}
