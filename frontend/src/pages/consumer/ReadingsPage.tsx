import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { CalendarDays, Gauge, Zap } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import {
  getDailyUsage,
  getMyReadings,
  type DailyUsageEntry,
  type MeterReading,
} from '@/services/readings'
import { formatBillingMonth, formatCurrency, formatDate, formatDateTime } from '@/utils/format'

/** Accessible consumption chart rendered from real, verified reading data. */
function MonthlyChart({ readings }: { readings: MeterReading[] }) {
  const sorted = useMemo(
    () => [...readings].sort((a, b) => b.billingYear - a.billingYear || b.billingMonth - a.billingMonth),
    [readings],
  )
  const maxUnits = Math.max(1, ...sorted.map((r) => r.unitsConsumed))

  return (
    <div className="mt-2">
      <div className="flex h-40 items-end gap-3" role="img" aria-label="Units consumed per billing period">
        {sorted.map((reading) => {
          const height = Math.max(4, Math.round((reading.unitsConsumed / maxUnits) * 100))
          return (
            <div
              key={reading.id}
              className="flex min-w-0 flex-1 flex-col items-center gap-1.5"
              title={`${formatBillingMonth(reading.billingMonth, reading.billingYear)}: ${reading.unitsConsumed} units`}
            >
              <span className="text-[10px] font-medium text-navy-900">{reading.unitsConsumed}</span>
              <div
                className="w-full max-w-10 rounded-t-md bg-volt-500"
                style={{ height: `${height}px` }}
                aria-hidden="true"
              />
              <span className="truncate text-[10px] text-slate-500">
                {formatBillingMonth(reading.billingMonth, reading.billingYear)}
              </span>
            </div>
          )
        })}
      </div>
      <p className="mt-3 text-xs text-slate-500">
        Units consumed per verified billing period (real meter readings).
      </p>
    </div>
  )
}

/** Last-7-days bar chart computed by the backend from daily readings. */
function DailyUsageChart({ entries }: { entries: DailyUsageEntry[] }) {
  const maxUnits = Math.max(1, ...entries.map((e) => e.units))

  return (
    <div>
      <div
        className="flex h-40 items-end gap-2 sm:gap-3"
        role="img"
        aria-label="Units consumed per day over the last 7 days"
      >
        {entries.map((entry) => {
          const height = Math.max(4, Math.round((entry.units / maxUnits) * 100))
          const hasReading = entry.readingAt != null
          const tooltip = hasReading
            ? `${formatDate(entry.date)}: ${entry.units} units · ${formatCurrency(entry.estimatedCost ?? 0)} · recorded ${formatDateTime(entry.readingAt)}`
            : `${formatDate(entry.date)}: no reading recorded`
          return (
            <div key={entry.date} className="flex min-w-0 flex-1 flex-col items-center gap-1.5" title={tooltip}>
              <span className="text-[10px] font-medium text-navy-900">{entry.units}</span>
              <div
                className={`w-full max-w-10 rounded-t-md ${hasReading ? 'bg-volt-500' : 'bg-slate-200'}`}
                style={{ height: `${height}px` }}
                aria-hidden="true"
              />
              <span className="truncate text-[10px] text-slate-500">{formatDate(entry.date)}</span>
            </div>
          )
        })}
      </div>
      <p className="mt-3 text-xs text-slate-500">
        Units consumed per day from your recorded meter readings. Days without a reading are shown as
        zero.
      </p>
    </div>
  )
}

export function ReadingsPage() {
  const readingsQuery = useQuery({ queryKey: ['readings'], queryFn: getMyReadings })
  const dailyUsageQuery = useQuery({ queryKey: ['daily-usage'], queryFn: getDailyUsage })

  const verified = useMemo(
    () => (readingsQuery.data ?? []).filter((r) => r.status === 'VERIFIED'),
    [readingsQuery.data],
  )

  if (readingsQuery.isLoading || dailyUsageQuery.isLoading) {
    return <LoadingState label="Loading meter readings…" />
  }

  if (readingsQuery.isError || dailyUsageQuery.isError) {
    const error = readingsQuery.error ?? dailyUsageQuery.error
    return (
      <ErrorState
        title="Could not load meter readings"
        message={error?.message}
        onRetry={() => {
          readingsQuery.refetch()
          dailyUsageQuery.refetch()
        }}
      />
    )
  }

  const usage = dailyUsageQuery.data

  return (
    <div className="space-y-6">
      {/* Daily electricity usage — last 7 days, backend-calculated */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
              <Zap className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-navy-900">Daily usage</h1>
              <p className="font-mono text-xs text-slate-500">{usage?.meterNumber}</p>
            </div>
          </div>
        </CardHeader>
        <CardBody>
          {!usage?.hasReadings ? (
            <EmptyState
              icon={Gauge}
              title="No meter readings recorded yet"
              description="Once your meter is activated, VOLTARAS records your daily readings and your usage will appear here. Complete the account setup steps on your dashboard to get started."
            />
          ) : (
            <>
              <DailyUsageChart entries={usage.dailyUsage} />

              <div className="mt-5 grid grid-cols-2 gap-4 rounded-md bg-slate-50 p-4 sm:grid-cols-3 xl:grid-cols-5">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Today</p>
                  <p className="mt-1 text-lg font-bold text-navy-900">
                    {usage.unitsConsumedToday} units
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Today's estimate
                  </p>
                  <p className="mt-1 text-lg font-bold text-navy-900">
                    {formatCurrency(usage.estimatedTodayCost)}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Per unit (est.)
                  </p>
                  <p className="mt-1 text-lg font-bold text-navy-900">
                    {formatCurrency(usage.estimatedPerUnitCost)}/unit
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    This month
                  </p>
                  <p className="mt-1 text-lg font-bold text-navy-900">
                    {usage.monthUnitsSoFar} units
                  </p>
                </div>
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Month estimate
                  </p>
                  <p className="mt-1 text-lg font-bold text-navy-900">
                    {formatCurrency(usage.estimatedMonthCost)}
                  </p>
                </div>
              </div>

              <p className="mt-3 text-xs text-slate-500">
                Last reading recorded {formatDateTime(usage.latestReadingAt)}. Costs are estimates
                based on the current tariff slabs; your bill is the source of truth.
              </p>
            </>
          )}
        </CardBody>
      </Card>

      {/* Daily rows: date, units, estimated cost, reading timestamp */}
      {usage?.hasReadings && (
        <Card>
          <CardHeader>
            <h2 className="flex items-center gap-2 text-base font-semibold text-navy-900">
              <CalendarDays className="h-4 w-4 text-volt-600" aria-hidden="true" />
              Last 7 days
            </h2>
          </CardHeader>
          <CardBody className="p-0">
            <ul className="divide-y divide-slate-100">
              {[...usage.dailyUsage].reverse().map((entry) => (
                <li
                  key={entry.date}
                  className="flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-navy-900">{formatDate(entry.date)}</p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {entry.readingAt
                        ? `Recorded ${formatDateTime(entry.readingAt)}`
                        : 'No reading recorded on this day'}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <span className="text-sm font-bold text-navy-900">{entry.units} units</span>
                    <Badge tone={entry.readingAt ? 'blue' : 'slate'}>
                      {entry.readingAt ? formatCurrency(entry.estimatedCost ?? 0) : '—'}
                    </Badge>
                  </div>
                </li>
              ))}
            </ul>
          </CardBody>
        </Card>
      )}

      {/* Monthly consumption (verified readings only) */}
      {verified.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-md bg-navy-50 text-navy-600">
                <Gauge className="h-5 w-5" aria-hidden="true" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-navy-900">Monthly consumption</h2>
                <p className="font-mono text-xs text-slate-500">{verified[0].meterNumber}</p>
              </div>
            </div>
          </CardHeader>
          <CardBody>
            <MonthlyChart readings={verified} />
          </CardBody>
        </Card>
      )}

      {/* Verified reading history */}
      {verified.length > 0 && (
        <Card>
          <CardHeader>
            <h2 className="text-base font-semibold text-navy-900">Reading history</h2>
          </CardHeader>
          <CardBody className="p-0">
            <ul className="divide-y divide-slate-100">
              {verified.map((reading) => (
                <li
                  key={reading.id}
                  className="flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                >
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-navy-900">
                      {formatBillingMonth(reading.billingMonth, reading.billingYear)}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {reading.previousReading} to {reading.currentReading} kWh ·{' '}
                      {formatDate(reading.readingDate)}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <span className="text-sm font-bold text-navy-900">
                      {reading.unitsConsumed} units
                    </span>
                    <Badge tone={statusTone(reading.status)}>{reading.status}</Badge>
                  </div>
                </li>
              ))}
            </ul>
          </CardBody>
        </Card>
      )}

      {verified.length === 0 && !usage?.hasReadings && (
        <Card>
          <CardBody>
            <EmptyState
              icon={Gauge}
              title="No verified readings yet"
              description="When a meter reading is verified for your meter it will appear here."
            />
          </CardBody>
        </Card>
      )}
    </div>
  )
}
