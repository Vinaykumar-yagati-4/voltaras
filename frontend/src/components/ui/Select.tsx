import { forwardRef, useId, type SelectHTMLAttributes } from 'react'
import { cn } from '@/utils/cn'

interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  options: SelectOption[]
  error?: string
  hint?: string
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, options, error, hint, className, id: idProp, ...props },
  ref,
) {
  const autoId = useId()
  const id = idProp ?? autoId
  const errorId = `${id}-error`
  const hintId = `${id}-hint`

  return (
    <div className="w-full">
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-navy-800">
        {label}
      </label>
      <select
        ref={ref}
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        className={cn(
          'h-11 w-full rounded-md border bg-white px-3 text-sm text-navy-900 shadow-sm',
          'transition-colors',
          error
            ? 'border-red-400 focus:border-red-500'
            : 'border-slate-300 focus:border-volt-500',
          'focus:outline-none focus:ring-2 focus:ring-volt-500/30',
          'disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500',
          className,
        )}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <p id={errorId} className="mt-1.5 text-sm text-red-600" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p id={hintId} className="mt-1.5 text-sm text-slate-500">
          {hint}
        </p>
      ) : null}
    </div>
  )
})
