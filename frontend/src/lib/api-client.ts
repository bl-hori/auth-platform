/**
 * API Client Configuration
 *
 * @description Provides centralized HTTP client for backend API communication
 * with automatic API key authentication and error handling
 */

/**
 * API configuration from environment variables
 */
export const API_CONFIG = {
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  apiKey: process.env.NEXT_PUBLIC_API_KEY || '',
} as const

/**
 * Custom error class for API errors
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public response?: unknown
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * HTTP request options interface
 */
export interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean>
}

/**
 * Generic API client with automatic API key authentication
 *
 * @description Handles HTTP requests with automatic headers, error handling,
 * and JSON parsing
 *
 * @example
 * ```typescript
 * const data = await apiClient.get<User[]>('/v1/users')
 * ```
 */
class ApiClient {
  private baseURL: string
  private apiKey: string

  constructor(baseURL: string, apiKey: string) {
    this.baseURL = baseURL
    this.apiKey = apiKey
  }

  /**
   * Build URL with query parameters
   */
  private buildURL(
    path: string,
    params?: Record<string, string | number | boolean>
  ): string {
    const url = new URL(path, this.baseURL)

    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        url.searchParams.append(key, String(value))
      })
    }

    return url.toString()
  }

  /**
   * Get default headers including API key
   */
  private getHeaders(customHeaders?: HeadersInit): HeadersInit {
    return {
      'Content-Type': 'application/json',
      'X-API-Key': this.apiKey,
      ...customHeaders,
    }
  }

  /**
   * Handle API response and errors
   */
  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new ApiError(
        response.status,
        errorData.message || `HTTP ${response.status}: ${response.statusText}`,
        errorData
      )
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return undefined as T
    }

    return response.json()
  }

  /**
   * Perform GET request
   *
   * @param path - API endpoint path
   * @param options - Request options including query parameters
   * @returns Promise resolving to typed response data
   *
   * @example
   * ```typescript
   * const users = await apiClient.get<User[]>('/v1/users', {
   *   params: { page: 1, size: 20 }
   * })
   * ```
   */
  async get<T>(path: string, options?: RequestOptions): Promise<T> {
    const url = this.buildURL(path, options?.params)

    const response = await fetch(url, {
      ...options,
      method: 'GET',
      headers: this.getHeaders(options?.headers),
    })

    return this.handleResponse<T>(response)
  }

  /**
   * Perform POST request
   *
   * @param path - API endpoint path
   * @param data - Request body data
   * @param options - Request options
   * @returns Promise resolving to typed response data
   *
   * @example
   * ```typescript
   * const newUser = await apiClient.post<User>('/v1/users', {
   *   email: 'user@example.com',
   *   name: 'John Doe'
   * })
   * ```
   */
  async post<T>(
    path: string,
    data?: unknown,
    options?: RequestOptions
  ): Promise<T> {
    const url = this.buildURL(path, options?.params)

    const response = await fetch(url, {
      ...options,
      method: 'POST',
      headers: this.getHeaders(options?.headers),
      body: data ? JSON.stringify(data) : undefined,
    })

    return this.handleResponse<T>(response)
  }

  /**
   * Perform PUT request
   */
  async put<T>(
    path: string,
    data?: unknown,
    options?: RequestOptions
  ): Promise<T> {
    const url = this.buildURL(path, options?.params)

    const response = await fetch(url, {
      ...options,
      method: 'PUT',
      headers: this.getHeaders(options?.headers),
      body: data ? JSON.stringify(data) : undefined,
    })

    return this.handleResponse<T>(response)
  }

  /**
   * Perform DELETE request
   */
  async delete<T>(path: string, options?: RequestOptions): Promise<T> {
    const url = this.buildURL(path, options?.params)

    const response = await fetch(url, {
      ...options,
      method: 'DELETE',
      headers: this.getHeaders(options?.headers),
    })

    return this.handleResponse<T>(response)
  }

  /**
   * Check API connectivity
   *
   * @returns Promise resolving to true if API is reachable
   */
  async healthCheck(): Promise<boolean> {
    try {
      await this.get('/actuator/health')
      return true
    } catch {
      return false
    }
  }
}

/**
 * Default API client instance
 */
export const apiClient = new ApiClient(API_CONFIG.baseURL, API_CONFIG.apiKey)
