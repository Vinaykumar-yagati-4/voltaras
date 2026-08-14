import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  Building2,
  ChevronDown,
  ChevronRight,
  Layers,
  MapPin,
  PencilLine,
  Plus,
  Trash2,
} from 'lucide-react'
import { Alert } from '@/components/ui/Alert'
import { Badge, statusTone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardBody, CardHeader } from '@/components/ui/Card'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { ErrorState } from '@/components/ui/ErrorState'
import { LoadingState } from '@/components/ui/LoadingState'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import {
  createBlock,
  createBuilding,
  createFloor,
  createUnit,
  deleteBlock,
  deleteBuilding,
  deleteFloor,
  deleteUnit,
  getBlockFloors,
  getBuildingBlocks,
  getFloorUnits,
  getOrganizationBuildings,
  updateBlock,
  updateBuilding,
  updateFloor,
  updateUnit,
  updateUnitStatus,
  type Block,
  type Building,
  type Floor,
  type Unit,
  type UnitStatus,
  type UnitType,
} from '@/services/organizations'
import { ApiError } from '@/types/api'
import { formatEnumLabel } from '@/utils/format'

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback
}

function DeleteButton({ onDelete, label }: { onDelete: () => void; label: string }) {
  const [confirming, setConfirming] = useState(false)
  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        aria-label={label}
        title={label}
        onClick={() => setConfirming(true)}
      >
        <Trash2 className="h-4 w-4 text-red-600" aria-hidden="true" />
      </Button>
      <ConfirmDialog
        open={confirming}
        title="Delete permanently"
        description={`Delete this ${label.toLowerCase()}? This action cannot be undone.`}
        confirmLabel="Delete"
        tone="danger"
        onConfirm={onDelete}
        onCancel={() => setConfirming(false)}
      />
    </>
  )
}

// ---------------------------------------------------------------------------
// Units
// ---------------------------------------------------------------------------

const unitSchema = z.object({
  unitNumber: z.string().min(1, 'Unit number is required').max(50),
  unitName: z.string().max(150).optional(),
  unitType: z.string().min(1, 'Unit type is required'),
  capacity: z
    .string()
    .optional()
    .refine((value) => value === '' || /^\d+$/.test(value ?? ''), {
      message: 'Capacity must be a number',
    }),
  description: z.string().max(500).optional(),
})

type UnitForm = z.infer<typeof unitSchema>

const UNIT_TYPE_OPTIONS: UnitType[] = ['ROOM', 'FLAT', 'CLASSROOM', 'LAB', 'OFFICE', 'SHOP', 'OTHER']

/** Allowed unit status transitions mirroring the organization-service rules. */
function unitStatusTargets(current: UnitStatus): UnitStatus[] {
  switch (current) {
    case 'AVAILABLE':
      return ['OCCUPIED', 'INACTIVE', 'MAINTENANCE']
    case 'OCCUPIED':
      return ['AVAILABLE', 'MAINTENANCE']
    case 'INACTIVE':
      return ['AVAILABLE']
    case 'MAINTENANCE':
      return ['AVAILABLE']
  }
}

function CreateUnitForm({
  floorId,
  onCreated,
  onCancel,
}: {
  floorId: number
  onCreated: () => void
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<UnitForm>({
    resolver: zodResolver(unitSchema),
    defaultValues: { unitNumber: '', unitName: '', unitType: '', capacity: '', description: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: UnitForm) =>
      createUnit(floorId, {
        unitNumber: values.unitNumber,
        unitName: values.unitName || undefined,
        unitType: values.unitType as UnitType,
        capacity: values.capacity ? Number(values.capacity) : undefined,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      form.reset()
      setError(null)
      onCreated()
      void queryClient.invalidateQueries({ queryKey: ['floor-units', floorId] })
    },
    onError: (err: unknown) => setError(errorMessage(err, 'Unable to create the unit.')),
  })
  return (
    <form
      onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      className="space-y-3 rounded-md border border-slate-200 bg-slate-50 p-4"
      noValidate
    >
      {error && <Alert tone="error">{error}</Alert>}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Input
          label="Unit number"
          placeholder="e.g. 101"
          error={form.formState.errors.unitNumber?.message}
          {...form.register('unitNumber')}
        />
        <Input
          label="Unit name"
          placeholder="Optional"
          error={form.formState.errors.unitName?.message}
          {...form.register('unitName')}
        />
        <Select
          label="Unit type"
          options={[
            { value: '', label: 'Select a type…' },
            ...UNIT_TYPE_OPTIONS.map((type) => ({ value: type, label: formatEnumLabel(type) })),
          ]}
          error={form.formState.errors.unitType?.message}
          {...form.register('unitType')}
        />
        <Input
          label="Capacity"
          inputMode="numeric"
          placeholder="Optional"
          error={form.formState.errors.capacity?.message}
          {...form.register('capacity')}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" loading={mutation.isPending}>
          Add unit
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function UnitRow({ unit, floorId }: { unit: Unit; floorId: number }) {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)
  const [statusTarget, setStatusTarget] = useState<UnitStatus | ''>('')

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['floor-units', floorId] })
    void queryClient.invalidateQueries({ queryKey: ['unit-status'] })
  }

  const editForm = useForm<UnitForm>({
    resolver: zodResolver(unitSchema),
    values: {
      unitNumber: unit.unitNumber,
      unitName: unit.unitName ?? '',
      unitType: unit.unitType,
      capacity: unit.capacity === null || unit.capacity === undefined ? '' : String(unit.capacity),
      description: unit.description ?? '',
    },
  })

  const editMutation = useMutation({
    mutationFn: (values: UnitForm) =>
      updateUnit(unit.id, {
        unitName: values.unitName || undefined,
        unitType: values.unitType as UnitType,
        capacity: values.capacity ? Number(values.capacity) : undefined,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      invalidate()
    },
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to update the unit.')),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteUnit(unit.id),
    onSuccess: () => invalidate(),
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to delete the unit.')),
  })

  const statusMutation = useMutation({
    mutationFn: (status: UnitStatus) => updateUnitStatus(unit.id, status),
    onSuccess: () => {
      setStatusTarget('')
      setPageError(null)
      invalidate()
    },
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to change the unit status.')),
  })

  const targets = unitStatusTargets(unit.status)

  return (
    <li className="rounded-md border border-slate-100 bg-white px-4 py-3">
      {pageError && <Alert tone="error" className="mb-3">{pageError}</Alert>}
      {editing ? (
        <form
          onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
          className="space-y-3"
          noValidate
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Unit number"
              error={editForm.formState.errors.unitNumber?.message}
              {...editForm.register('unitNumber')}
            />
            <Input
              label="Unit name"
              error={editForm.formState.errors.unitName?.message}
              {...editForm.register('unitName')}
            />
            <Select
              label="Unit type"
              options={UNIT_TYPE_OPTIONS.map((type) => ({
                value: type,
                label: formatEnumLabel(type),
              }))}
              error={editForm.formState.errors.unitType?.message}
              {...editForm.register('unitType')}
            />
            <Input
              label="Capacity"
              inputMode="numeric"
              error={editForm.formState.errors.capacity?.message}
              {...editForm.register('capacity')}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm" loading={editMutation.isPending}>
              Save
            </Button>
            <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="break-words text-sm font-medium text-navy-900">
              {unit.unitNumber}
              {unit.unitName ? ` — ${unit.unitName}` : ''}
            </p>
            <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-slate-500">
              <span>{formatEnumLabel(unit.unitType)}</span>
              {unit.capacity !== null && unit.capacity !== undefined && (
                <>
                  <span aria-hidden="true">·</span>
                  <span>Capacity {unit.capacity}</span>
                </>
              )}
              {unit.description && (
                <>
                  <span aria-hidden="true">·</span>
                  <span className="break-all">{unit.description}</span>
                </>
              )}
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <Badge tone={statusTone(unit.status)}>{formatEnumLabel(unit.status)}</Badge>
            {targets.length > 0 && (
              <div className="flex items-center gap-1.5">
                <label htmlFor={`unit-status-${unit.id}`} className="sr-only">
                  Change unit status
                </label>
                <select
                  id={`unit-status-${unit.id}`}
                  value={statusTarget}
                  onChange={(event) => setStatusTarget(event.target.value as UnitStatus | '')}
                  className="h-11 rounded-md border border-slate-300 bg-white px-2 text-sm text-navy-900"
                >
                  <option value="">Change…</option>
                  {targets.map((target) => (
                    <option key={target} value={target}>
                      {formatEnumLabel(target)}
                    </option>
                  ))}
                </select>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={!statusTarget || statusMutation.isPending}
                  loading={statusMutation.isPending}
                  onClick={() => statusTarget && statusMutation.mutate(statusTarget)}
                >
                  Apply
                </Button>
              </div>
            )}
            <Button
              variant="ghost"
              size="sm"
              aria-label="Edit unit"
              title="Edit unit"
              onClick={() => setEditing(true)}
            >
              <PencilLine className="h-4 w-4" aria-hidden="true" />
            </Button>
            <DeleteButton
              label="Delete unit"
              onDelete={() => deleteMutation.mutate()}
            />
          </div>
        </div>
      )}
    </li>
  )
}

function UnitsSection({ floorId }: { floorId: number }) {
  const [creating, setCreating] = useState(false)
  const unitsQuery = useQuery({
    queryKey: ['floor-units', floorId],
    queryFn: () => getFloorUnits(floorId),
  })
  const units = unitsQuery.data ?? []
  return (
    <div className="mt-3 space-y-2 pl-4 sm:pl-8">
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          Units ({unitsQuery.data?.length ?? 0})
        </p>
        <Button variant="ghost" size="sm" onClick={() => setCreating((value) => !value)}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add unit
        </Button>
      </div>
      {creating && (
        <CreateUnitForm floorId={floorId} onCreated={() => setCreating(false)} onCancel={() => setCreating(false)} />
      )}
      {unitsQuery.isLoading ? (
        <p className="text-sm text-slate-500">Loading units…</p>
      ) : unitsQuery.isError ? (
        <ErrorState
          title="Could not load units"
          message={unitsQuery.error?.message}
          onRetry={() => unitsQuery.refetch()}
        />
      ) : units.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-200 px-4 py-3 text-sm text-slate-500">
          No units on this floor yet.
        </p>
      ) : (
        <ul className="space-y-2">
          {units.map((unit) => (
            <UnitRow key={unit.id} unit={unit} floorId={floorId} />
          ))}
        </ul>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Floors
// ---------------------------------------------------------------------------

const floorSchema = z.object({
  floorNumber: z
    .string()
    .min(1, 'Floor number is required')
    .regex(/^-?\d+$/, 'Floor number must be a number'),
  name: z.string().max(150).optional(),
  description: z.string().max(500).optional(),
})

type FloorForm = z.infer<typeof floorSchema>

function CreateFloorForm({
  blockId,
  onCreated,
  onCancel,
}: {
  blockId: number
  onCreated: () => void
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<FloorForm>({
    resolver: zodResolver(floorSchema),
    defaultValues: { floorNumber: '', name: '', description: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: FloorForm) =>
      createFloor(blockId, {
        floorNumber: Number(values.floorNumber),
        name: values.name || undefined,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      form.reset()
      setError(null)
      onCreated()
      void queryClient.invalidateQueries({ queryKey: ['block-floors', blockId] })
    },
    onError: (err: unknown) => setError(errorMessage(err, 'Unable to create the floor.')),
  })
  return (
    <form
      onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      className="space-y-3 rounded-md border border-slate-200 bg-slate-50 p-4"
      noValidate
    >
      {error && <Alert tone="error">{error}</Alert>}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Input
          label="Floor number"
          inputMode="numeric"
          placeholder="e.g. 2"
          error={form.formState.errors.floorNumber?.message}
          {...form.register('floorNumber')}
        />
        <Input
          label="Floor name"
          placeholder="Optional"
          error={form.formState.errors.name?.message}
          {...form.register('name')}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" loading={mutation.isPending}>
          Add floor
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function FloorRow({ floor, blockId }: { floor: Floor; blockId: number }) {
  const queryClient = useQueryClient()
  const [expanded, setExpanded] = useState(false)
  const [editing, setEditing] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)
  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['block-floors', blockId] })
  }
  const editForm = useForm<FloorForm>({
    resolver: zodResolver(floorSchema),
    values: {
      floorNumber: String(floor.floorNumber),
      name: floor.name ?? '',
      description: floor.description ?? '',
    },
  })
  const editMutation = useMutation({
    mutationFn: (values: FloorForm) =>
      updateFloor(floor.id, {
        floorNumber: Number(values.floorNumber),
        name: values.name || undefined,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      invalidate()
    },
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to update the floor.')),
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteFloor(floor.id),
    onSuccess: () => invalidate(),
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to delete the floor.')),
  })
  return (
    <li className="rounded-md border border-slate-100 bg-white px-4 py-3">
      {pageError && <Alert tone="error" className="mb-3">{pageError}</Alert>}
      {editing ? (
        <form
          onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
          className="space-y-3"
          noValidate
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Floor number"
              inputMode="numeric"
              error={editForm.formState.errors.floorNumber?.message}
              {...editForm.register('floorNumber')}
            />
            <Input
              label="Floor name"
              error={editForm.formState.errors.name?.message}
              {...editForm.register('name')}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm" loading={editMutation.isPending}>
              Save
            </Button>
            <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onClick={() => setExpanded((value) => !value)}
            className="flex min-h-11 min-w-0 items-center gap-2 text-left"
            aria-expanded={expanded}
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            ) : (
              <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            )}
            <span className="min-w-0">
              <span className="block break-words text-sm font-medium text-navy-900">
                Floor {floor.floorNumber}
                {floor.name ? ` — ${floor.name}` : ''}
              </span>
              <span className="block text-xs text-slate-500">
                {floor.description || 'No description'}
              </span>
            </span>
          </button>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={statusTone(floor.status)}>{formatEnumLabel(floor.status)}</Badge>
            <Button
              variant="ghost"
              size="sm"
              aria-label="Edit floor"
              title="Edit floor"
              onClick={() => setEditing(true)}
            >
              <PencilLine className="h-4 w-4" aria-hidden="true" />
            </Button>
            <DeleteButton label="Delete floor" onDelete={() => deleteMutation.mutate()} />
          </div>
        </div>
      )}
      {expanded && <UnitsSection floorId={floor.id} />}
    </li>
  )
}

function FloorsSection({ blockId }: { blockId: number }) {
  const [creating, setCreating] = useState(false)
  const floorsQuery = useQuery({
    queryKey: ['block-floors', blockId],
    queryFn: () => getBlockFloors(blockId),
  })
  const floors = floorsQuery.data ?? []
  return (
    <div className="mt-3 space-y-2 pl-4 sm:pl-8">
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          Floors ({floorsQuery.data?.length ?? 0})
        </p>
        <Button variant="ghost" size="sm" onClick={() => setCreating((value) => !value)}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add floor
        </Button>
      </div>
      {creating && (
        <CreateFloorForm blockId={blockId} onCreated={() => setCreating(false)} onCancel={() => setCreating(false)} />
      )}
      {floorsQuery.isLoading ? (
        <p className="text-sm text-slate-500">Loading floors…</p>
      ) : floorsQuery.isError ? (
        <ErrorState
          title="Could not load floors"
          message={floorsQuery.error?.message}
          onRetry={() => floorsQuery.refetch()}
        />
      ) : floors.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-200 px-4 py-3 text-sm text-slate-500">
          No floors in this block yet.
        </p>
      ) : (
        <ul className="space-y-2">
          {floors.map((floor) => (
            <FloorRow key={floor.id} floor={floor} blockId={blockId} />
          ))}
        </ul>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Blocks
// ---------------------------------------------------------------------------

const blockSchema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  code: z.string().min(1, 'Code is required').max(50),
  description: z.string().max(500).optional(),
})

type BlockForm = z.infer<typeof blockSchema>

function CreateBlockForm({
  buildingId,
  onCreated,
  onCancel,
}: {
  buildingId: number
  onCreated: () => void
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<BlockForm>({
    resolver: zodResolver(blockSchema),
    defaultValues: { name: '', code: '', description: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: BlockForm) =>
      createBlock(buildingId, {
        name: values.name,
        code: values.code,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      form.reset()
      setError(null)
      onCreated()
      void queryClient.invalidateQueries({ queryKey: ['building-blocks', buildingId] })
    },
    onError: (err: unknown) => setError(errorMessage(err, 'Unable to create the block.')),
  })
  return (
    <form
      onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      className="space-y-3 rounded-md border border-slate-200 bg-slate-50 p-4"
      noValidate
    >
      {error && <Alert tone="error">{error}</Alert>}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Input
          label="Block name"
          placeholder="e.g. Tower A"
          error={form.formState.errors.name?.message}
          {...form.register('name')}
        />
        <Input
          label="Block code"
          placeholder="e.g. TOWER-A"
          error={form.formState.errors.code?.message}
          {...form.register('code')}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" loading={mutation.isPending}>
          Add block
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function BlockRow({ block, buildingId }: { block: Block; buildingId: number }) {
  const queryClient = useQueryClient()
  const [expanded, setExpanded] = useState(false)
  const [editing, setEditing] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)
  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['building-blocks', buildingId] })
  }
  const editForm = useForm<BlockForm>({
    resolver: zodResolver(blockSchema),
    values: {
      name: block.name,
      code: block.code,
      description: block.description ?? '',
    },
  })
  const editMutation = useMutation({
    mutationFn: (values: BlockForm) =>
      updateBlock(block.id, {
        name: values.name,
        description: values.description || undefined,
      }),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      invalidate()
    },
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to update the block.')),
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteBlock(block.id),
    onSuccess: () => invalidate(),
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to delete the block.')),
  })
  return (
    <li className="rounded-md border border-slate-100 bg-white px-4 py-3">
      {pageError && <Alert tone="error" className="mb-3">{pageError}</Alert>}
      {editing ? (
        <form
          onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
          className="space-y-3"
          noValidate
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Block name"
              error={editForm.formState.errors.name?.message}
              {...editForm.register('name')}
            />
            <Input
              label="Block code"
              error={editForm.formState.errors.code?.message}
              {...editForm.register('code')}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm" loading={editMutation.isPending}>
              Save
            </Button>
            <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onClick={() => setExpanded((value) => !value)}
            className="flex min-h-11 min-w-0 items-center gap-2 text-left"
            aria-expanded={expanded}
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            ) : (
              <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            )}
            <span className="min-w-0">
              <span className="block break-words text-sm font-medium text-navy-900">
                {block.name}
              </span>
              <span className="block font-mono text-xs text-slate-500">{block.code}</span>
            </span>
          </button>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={statusTone(block.status)}>{formatEnumLabel(block.status)}</Badge>
            <Button
              variant="ghost"
              size="sm"
              aria-label="Edit block"
              title="Edit block"
              onClick={() => setEditing(true)}
            >
              <PencilLine className="h-4 w-4" aria-hidden="true" />
            </Button>
            <DeleteButton label="Delete block" onDelete={() => deleteMutation.mutate()} />
          </div>
        </div>
      )}
      {expanded && <FloorsSection blockId={block.id} />}
    </li>
  )
}

function BlocksSection({ buildingId }: { buildingId: number }) {
  const [creating, setCreating] = useState(false)
  const blocksQuery = useQuery({
    queryKey: ['building-blocks', buildingId],
    queryFn: () => getBuildingBlocks(buildingId),
  })
  const blocks = blocksQuery.data ?? []
  return (
    <div className="mt-3 space-y-2 pl-4 sm:pl-8">
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          Blocks ({blocksQuery.data?.length ?? 0})
        </p>
        <Button variant="ghost" size="sm" onClick={() => setCreating((value) => !value)}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add block
        </Button>
      </div>
      {creating && (
        <CreateBlockForm buildingId={buildingId} onCreated={() => setCreating(false)} onCancel={() => setCreating(false)} />
      )}
      {blocksQuery.isLoading ? (
        <p className="text-sm text-slate-500">Loading blocks…</p>
      ) : blocksQuery.isError ? (
        <ErrorState
          title="Could not load blocks"
          message={blocksQuery.error?.message}
          onRetry={() => blocksQuery.refetch()}
        />
      ) : blocks.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-200 px-4 py-3 text-sm text-slate-500">
          No blocks in this building yet.
        </p>
      ) : (
        <ul className="space-y-2">
          {blocks.map((block) => (
            <BlockRow key={block.id} block={block} buildingId={buildingId} />
          ))}
        </ul>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Buildings
// ---------------------------------------------------------------------------

const buildingSchema = z.object({
  name: z.string().min(1, 'Name is required').max(150),
  code: z.string().min(1, 'Code is required').max(50),
  description: z.string().max(500).optional(),
  address: z.string().max(500).optional(),
})

type BuildingForm = z.infer<typeof buildingSchema>

function CreateBuildingForm({
  organizationId,
  onCreated,
  onCancel,
}: {
  organizationId: number
  onCreated: () => void
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const form = useForm<BuildingForm>({
    resolver: zodResolver(buildingSchema),
    defaultValues: { name: '', code: '', description: '', address: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: BuildingForm) =>
      createBuilding(organizationId, {
        name: values.name,
        code: values.code,
        description: values.description || undefined,
        address: values.address || undefined,
      }),
    onSuccess: () => {
      form.reset()
      setError(null)
      onCreated()
      void queryClient.invalidateQueries({ queryKey: ['organization-buildings', organizationId] })
    },
    onError: (err: unknown) => setError(errorMessage(err, 'Unable to create the building.')),
  })
  return (
    <form
      onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      className="space-y-3 rounded-md border border-slate-200 bg-slate-50 p-4"
      noValidate
    >
      {error && <Alert tone="error">{error}</Alert>}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Input
          label="Building name"
          placeholder="e.g. Tower B"
          error={form.formState.errors.name?.message}
          {...form.register('name')}
        />
        <Input
          label="Building code"
          placeholder="e.g. TOWER-B"
          error={form.formState.errors.code?.message}
          {...form.register('code')}
        />
        <Input
          label="Address"
          placeholder="Optional"
          error={form.formState.errors.address?.message}
          {...form.register('address')}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" loading={mutation.isPending}>
          Add building
        </Button>
        <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}

function BuildingRow({ building, organizationId }: { building: Building; organizationId: number }) {
  const queryClient = useQueryClient()
  const [expanded, setExpanded] = useState(false)
  const [editing, setEditing] = useState(false)
  const [pageError, setPageError] = useState<string | null>(null)
  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['organization-buildings', organizationId] })
  }
  const editForm = useForm<BuildingForm>({
    resolver: zodResolver(buildingSchema),
    values: {
      name: building.name,
      code: building.code,
      description: building.description ?? '',
      address: building.address ?? '',
    },
  })
  const editMutation = useMutation({
    mutationFn: (values: BuildingForm) =>
      updateBuilding(building.id, {
        name: values.name,
        description: values.description || undefined,
        address: values.address || undefined,
      }),
    onSuccess: () => {
      setEditing(false)
      setPageError(null)
      invalidate()
    },
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to update the building.')),
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteBuilding(building.id),
    onSuccess: () => invalidate(),
    onError: (err: unknown) => setPageError(errorMessage(err, 'Unable to delete the building.')),
  })
  return (
    <li className="rounded-md border border-slate-100 bg-white px-4 py-3">
      {pageError && <Alert tone="error" className="mb-3">{pageError}</Alert>}
      {editing ? (
        <form
          onSubmit={editForm.handleSubmit((values) => editMutation.mutate(values))}
          className="space-y-3"
          noValidate
        >
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Building name"
              error={editForm.formState.errors.name?.message}
              {...editForm.register('name')}
            />
            <Input
              label="Building code"
              error={editForm.formState.errors.code?.message}
              {...editForm.register('code')}
            />
            <Input
              label="Address"
              error={editForm.formState.errors.address?.message}
              {...editForm.register('address')}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="sm" loading={editMutation.isPending}>
              Save
            </Button>
            <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            type="button"
            onClick={() => setExpanded((value) => !value)}
            className="flex min-h-11 min-w-0 items-center gap-2 text-left"
            aria-expanded={expanded}
          >
            {expanded ? (
              <ChevronDown className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            ) : (
              <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" aria-hidden="true" />
            )}
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-navy-50 text-navy-600 ring-1 ring-inset ring-navy-100">
              <Building2 className="h-4 w-4" aria-hidden="true" />
            </span>
            <span className="min-w-0">
              <span className="block break-words text-sm font-medium text-navy-900">
                {building.name}
              </span>
              <span className="block font-mono text-xs text-slate-500">{building.code}</span>
              {building.address && (
                <span className="mt-0.5 flex items-center gap-1 text-xs text-slate-500">
                  <MapPin className="h-3 w-3" aria-hidden="true" />
                  <span className="break-all">{building.address}</span>
                </span>
              )}
            </span>
          </button>
          <div className="flex shrink-0 items-center gap-2">
            <Badge tone={statusTone(building.status)}>{formatEnumLabel(building.status)}</Badge>
            <Button
              variant="ghost"
              size="sm"
              aria-label="Edit building"
              title="Edit building"
              onClick={() => setEditing(true)}
            >
              <PencilLine className="h-4 w-4" aria-hidden="true" />
            </Button>
            <DeleteButton label="Delete building" onDelete={() => deleteMutation.mutate()} />
          </div>
        </div>
      )}
      {expanded && <BlocksSection buildingId={building.id} />}
    </li>
  )
}

export function AdminOrganizationStructure({ organizationId }: { organizationId: number }) {
  const [creating, setCreating] = useState(false)
  const buildingsQuery = useQuery({
    queryKey: ['organization-buildings', organizationId],
    queryFn: () => getOrganizationBuildings(organizationId),
  })
  const buildings = buildingsQuery.data ?? []

  return (
    <Card className="border-l-4 border-l-volt-500">
      <CardHeader className="flex flex-wrap items-center justify-between gap-2">
        <div className="min-w-0">
          <h2 className="flex items-center gap-2.5 text-base font-semibold text-navy-900">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-volt-600 text-white">
              <Layers className="h-4 w-4" aria-hidden="true" />
            </span>
            Buildings &amp; structure
          </h2>
          <p className="mt-1.5 pl-10 text-xs text-slate-500">
            Manage the building → block → floor → unit hierarchy for this organization.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setCreating((value) => !value)}>
          <Plus className="h-4 w-4" aria-hidden="true" />
          Add building
        </Button>
      </CardHeader>
      <CardBody className="space-y-3">
        {creating && (
          <CreateBuildingForm
            organizationId={organizationId}
            onCreated={() => setCreating(false)}
            onCancel={() => setCreating(false)}
          />
        )}
        {buildingsQuery.isLoading ? (
          <LoadingState label="Loading buildings…" />
        ) : buildingsQuery.isError ? (
          <ErrorState
            title="Could not load buildings"
            message={buildingsQuery.error?.message}
            onRetry={() => buildingsQuery.refetch()}
          />
        ) : buildings.length === 0 ? (
          <EmptyState
            icon={Building2}
            title="No buildings yet"
            description="Add a building to start building the organization hierarchy — buildings contain blocks, floors and units."
          />
        ) : (
          <ul className="space-y-2">
            {buildings.map((building) => (
              <BuildingRow key={building.id} building={building} organizationId={organizationId} />
            ))}
          </ul>
        )}
      </CardBody>
    </Card>
  )
}
