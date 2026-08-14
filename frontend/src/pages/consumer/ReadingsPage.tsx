import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Gauge } from 'lucide-react'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { getMyReadings, type MeterReading } from '@/services/readings'
import { formatBillingMonth, formatDate } from '@/utils/format'

/** Accessible consumption chart rendered from real, verified reading data. */
function ConsumptionChart({ readings }: { readings: MeterReading[] }) {
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
              <span className="text-[10px] font-medium text-navy-900">
                {reading.unitsConsumed}
              </span>
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

export function ReadingsPage() {
  const readingsQuery = useQuery({ queryKey: ['readings'], queryFn: getMyReadings })

  const verified = useMemo(
    () => (readingsQuery.data ?? []).filter((r) => r.status === 'VERIFIED'),
    [readingsQuery.data],
  )

  if (readingsQuery.isLoading) return <LoadingState label="Loading meter readings…" />

  if (readingsQuery.isError) {
    return (
      <ErrorState
        title="Could not load meter readings"
        message={readingsQuery.error.message}
        onRetry={() => readingsQuery.refetch()}
      />
    )
  }

  if (verified.length === 0) {
    return (
      <Card>
        <CardBody>
          <EmptyState
            icon={Gauge}
            title="No verified readings yet"
            description="When a meter reading is verified for your meter it will appear here."
          />
        </CardBody>
      </Card>
    )
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-md bg-volt-50 text-volt-600">
              <Gauge className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="text-lg font-bold text-navy-900">Consumption</h1>
              <p className="font-mono text-xs text-slate-500">
                {verified[0].meterNumber}
              </p>
            </div>
          </div>
        </CardHeader>
        <CardBody>
          <ConsumptionChart readings={verified} />
        </CardBody>
      </Card>

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
    </div>
  )
}
