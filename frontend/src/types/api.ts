export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorDetail {
  code?: string
  message?: string
  details?: ApiFieldError[]
}

/**
 * Error body used by all VOLTARAS services (see docs/BACKEND_API_CONTRACT.md).
 * The API Gateway emits a slightly different shape for 401s, which is
 * normalized here too.
 */
export interface ApiErrorPayload {
  success?: boolean
  status?: number
  error?: ApiErrorDetail
  message?: string
  timestamp?: string
  path?: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors: ApiFieldError[]
  readonly payload?: ApiErrorPayload

  constructor(
    message: string,
    status: number,
    code?: string,
    fieldErrors: ApiFieldError[] = [],
    payload?: ApiErrorPayload,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.payload = payload
  }
}

/** Spring `Page` envelope shared by paginated endpoints. */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements?: number
  first?: boolean
  last?: boolean
  empty?: boolean
}
