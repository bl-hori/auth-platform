'use client'

/**
 * Authentication Context Provider
 *
 * @description Manages authentication state across the application
 * including user data, organization context, and API key storage
 */

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react'

import { apiClient } from '@/lib/api-client'
import type { User, Organization, AuthContext } from '@/types'
import { UserStatus, OrganizationStatus } from '@/types'

/**
 * Storage keys for authentication data
 */
const STORAGE_KEYS = {
  API_KEY: 'auth_api_key',
  USER: 'auth_user',
  ORGANIZATION: 'auth_organization',
} as const

/**
 * Authentication context instance
 */
const AuthContextInstance = createContext<AuthContext | undefined>(undefined)

/**
 * Props for AuthProvider component
 */
interface AuthProviderProps {
  children: ReactNode
}

/**
 * Authentication Provider Component
 *
 * @description Provides authentication state and methods to the application.
 * Automatically restores session from localStorage on mount.
 *
 * @example
 * ```tsx
 * <AuthProvider>
 *   <App />
 * </AuthProvider>
 * ```
 */
export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  /**
   * Logout and clear session
   *
   * @example
   * ```typescript
   * logout()
   * ```
   */
  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEYS.API_KEY)
    localStorage.removeItem(STORAGE_KEYS.USER)
    localStorage.removeItem(STORAGE_KEYS.ORGANIZATION)
    setUser(null)
    setOrganization(null)
  }, [])

  /**
   * Restore session from localStorage
   */
  const restoreSession = useCallback(async () => {
    setIsLoading(true)
    try {
      const storedApiKey = localStorage.getItem(STORAGE_KEYS.API_KEY)
      const storedUser = localStorage.getItem(STORAGE_KEYS.USER)
      const storedOrg = localStorage.getItem(STORAGE_KEYS.ORGANIZATION)

      if (storedApiKey && storedUser && storedOrg) {
        setUser(JSON.parse(storedUser))
        setOrganization(JSON.parse(storedOrg))

        // Verify API key is still valid
        const isHealthy = await apiClient.healthCheck()
        if (!isHealthy) {
          // API key is invalid, clear session
          logout()
        }
      }
    } catch (error) {
      console.error('Failed to restore session:', error)
      logout()
    } finally {
      setIsLoading(false)
    }
  }, [logout])

  /**
   * Initialize session on mount
   */
  useEffect(() => {
    restoreSession()
  }, [restoreSession])

  /**
   * Login with API key
   *
   * @param apiKey - API key for authentication
   * @throws {Error} If authentication fails
   *
   * @example
   * ```typescript
   * await login('dev-api-key-12345')
   * ```
   */
  const login = useCallback(async (apiKey: string) => {
    setIsLoading(true)
    try {
      // Store API key temporarily for the request
      localStorage.setItem(STORAGE_KEYS.API_KEY, apiKey)

      // For development, we'll create a mock user and organization
      // In production, this should fetch from the backend
      const mockUser: User = {
        id: 'user-001',
        organizationId: 'org-001',
        email: 'admin@example.com',
        username: 'admin',
        displayName: 'Admin User',
        status: UserStatus.ACTIVE,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }

      const mockOrganization: Organization = {
        id: 'org-001',
        name: 'default',
        displayName: 'Default Organization',
        status: OrganizationStatus.ACTIVE,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }

      // Verify API connectivity
      const isHealthy = await apiClient.healthCheck()
      if (!isHealthy) {
        throw new Error('API is not accessible. Please check your connection.')
      }

      // Store session data
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(mockUser))
      localStorage.setItem(
        STORAGE_KEYS.ORGANIZATION,
        JSON.stringify(mockOrganization)
      )

      setUser(mockUser)
      setOrganization(mockOrganization)
    } catch (error) {
      // Clear invalid API key
      localStorage.removeItem(STORAGE_KEYS.API_KEY)
      throw error
    } finally {
      setIsLoading(false)
    }
  }, [])

  const value: AuthContext = {
    user,
    organization,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
  }

  return (
    <AuthContextInstance.Provider value={value}>
      {children}
    </AuthContextInstance.Provider>
  )
}

/**
 * Hook to access authentication context
 *
 * @returns Authentication context
 * @throws {Error} If used outside AuthProvider
 *
 * @example
 * ```tsx
 * const { user, isAuthenticated, login, logout } = useAuth()
 * ```
 */
export function useAuth(): AuthContext {
  const context = useContext(AuthContextInstance)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
