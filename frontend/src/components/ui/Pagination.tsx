import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/utils/cn'

interface PaginationProps {
  page: number // zero-based
  totalPages: number
  onChange: (page: number) => void
  className?: string
}

/**
 * Accessible pager with 44px touch targets and visible focus states.
 * Renders nothing when there is only one page.
 */
export function Pagination({ page, totalPages, onChange, className }: PaginationProps) {
  if (totalPages <= 1) return null

  const hasPrevious = page > 0
  const hasNext = page < totalPages - 1

  const pageItems: number[] = []
  const start = Math.max(0, page - 2)
  const end = Math.min(totalPages - 1, page + 2)
  for (let i = start; i <= end; i += 1) pageItems.push(i)

  return (
    <nav
      aria-label="Pagination"
      className={cn('flex flex-wrap items-center justify-center gap-2', className)}
    >
      <button
        type="button"
        disabled={!hasPrevious}
        onClick={() => onChange(page - 1)}
        className="inline-flex h-11 min-w-11 items-center justify-center gap-1 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-800 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label="Previous page"
        title="Previous page"
      >
        <ChevronLeft className="h-4 w-4" aria-hidden="true" />
        <span className="hidden sm:inline">Previous</span>
      </button>

      {start > 0 && (
        <>
          <button
            type="button"
            onClick={() => onChange(0)}
            className="inline-flex h-11 min-w-11 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-800 transition-colors hover:bg-slate-50"
            aria-label="Go to page 1"
          >
            1
          </button>
          {start > 1 && <span className="px-1 text-sm text-slate-400">…</span>}
        </>
      )}

      {pageItems.map((item) => (
        <button
          key={item}
          type="button"
          onClick={() => onChange(item)}
          aria-current={item === page ? 'page' : undefined}
          className={cn(
            'inline-flex h-11 min-w-11 items-center justify-center rounded-md border px-3 text-sm font-medium transition-colors',
            item === page
              ? 'border-volt-600 bg-volt-600 text-white'
              : 'border-slate-300 bg-white text-navy-800 hover:bg-slate-50',
          )}
        >
          {item + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="px-1 text-sm text-slate-400">…</span>}
          <button
            type="button"
            onClick={() => onChange(totalPages - 1)}
            className="inline-flex h-11 min-w-11 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-800 transition-colors hover:bg-slate-50"
            aria-label={`Go to page ${totalPages}`}
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        type="button"
        disabled={!hasNext}
        onClick={() => onChange(page + 1)}
        className="inline-flex h-11 min-w-11 items-center justify-center gap-1 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-navy-800 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        aria-label="Next page"
        title="Next page"
      >
        <span className="hidden sm:inline">Next</span>
        <ChevronRight className="h-4 w-4" aria-hidden="true" />
      </button>
    </nav>
  )
}
