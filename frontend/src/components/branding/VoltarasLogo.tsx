import { cn } from '@/utils/cn'

export function VoltarasMark({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex h-8 w-8 items-center justify-center rounded-md bg-navy-900',
        className,
      )}
      aria-hidden="true"
    >
      <svg viewBox="0 0 32 32" className="h-5 w-5" fill="none">
        <path d="M17.8 4 8 18h6.2l-1.6 10L22 14h-6.4z" fill="#5a8ef8" />
      </svg>
    </span>
  )
}

export function VoltarasLogo({
  className,
  light = false,
}: {
  className?: string
  light?: boolean
}) {
  return (
    <span className={cn('inline-flex items-center gap-2', className)}>
      <VoltarasMark />
      <span
        className={cn(
          'text-lg font-bold tracking-wide',
          light ? 'text-white' : 'text-navy-900',
        )}
      >
        VOLTARAS
      </span>
    </span>
  )
}
