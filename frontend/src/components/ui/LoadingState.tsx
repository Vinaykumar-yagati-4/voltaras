import { Spinner } from '@/components/ui/Spinner'
import { cn } from '@/utils/cn'

export function LoadingState({
  label = 'Loading…',
  fullScreen = false,
  className,
}: {
  label?: string
  fullScreen?: boolean
  className?: string
}) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex flex-col items-center justify-center gap-3 py-16 text-slate-500',
        fullScreen && 'min-h-screen',
        className,
      )}
    >
      <Spinner size="lg" />
      <p className="text-sm font-medium">{label}</p>
    </div>
  )
}
