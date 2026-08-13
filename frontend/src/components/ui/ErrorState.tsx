import { WifiOff } from 'lucide-react'
import { Button } from '@/components/ui/Button'

export function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
}: {
  title?: string
  message?: string
  onRetry?: () => void
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-14 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-card bg-red-50">
        <WifiOff className="h-6 w-6 text-red-500" aria-hidden="true" />
      </div>
      <p className="mt-1 text-sm font-semibold text-navy-900">{title}</p>
      {message && <p className="max-w-sm text-sm text-slate-500">{message}</p>}
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry} className="mt-3">
          Try again
        </Button>
      )}
    </div>
  )
}
