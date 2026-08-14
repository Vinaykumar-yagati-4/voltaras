export function formatDateTime(value: string | number | Date | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(value: string | number | Date | null | undefined): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

/** Indian Rupee formatting, e.g. ₹2,784.75 — the only wallet/bill currency. */
export function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

/** Billing month label, e.g. "Jul 2026" from month=7, year=2026. */
export function formatBillingMonth(month: number, year: number): string {
  const date = new Date(year, month - 1, 1)
  if (Number.isNaN(date.getTime())) return `${month}/${year}`
  return date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' })
}

/**
 * Human-readable complaint category label, e.g. "BILLING_ISSUE" → "Billing issue".
 * Unknown names fall back to a title-cased, underscore-spaced rendering.
 */
export function formatCategoryLabel(name: string | null | undefined): string {
  return formatEnumLabel(name)
}

/**
 * Human-readable label for any enum value, e.g. "IN_PROGRESS" → "In progress"
 * and "APARTMENT" → "Apartment". Falls back to '—' for empty values.
 */
export function formatEnumLabel(value: string | null | undefined): string {
  if (!value) return '—'
  return value
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}
