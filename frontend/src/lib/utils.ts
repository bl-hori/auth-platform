/**
 * Utility functions for the application
 */

import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Merge Tailwind CSS classes with proper precedence
 *
 * @description Combines clsx and tailwind-merge to handle conditional
 * classes and proper Tailwind class merging
 *
 * @param inputs - Class values to merge
 * @returns Merged class string
 *
 * @example
 * ```typescript
 * cn('px-4 py-2', isActive && 'bg-blue-500', 'px-6') // -> 'px-6 py-2 bg-blue-500'
 * ```
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Format date to locale string
 *
 * @param date - Date string or Date object
 * @param locale - Locale string (default: 'ja-JP')
 * @returns Formatted date string
 *
 * @example
 * ```typescript
 * formatDate('2025-01-01T00:00:00Z') // -> '2025年1月1日'
 * ```
 */
export function formatDate(date: string | Date, locale = 'ja-JP'): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return dateObj.toLocaleDateString(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

/**
 * Format datetime to locale string
 *
 * @param date - Date string or Date object
 * @param locale - Locale string (default: 'ja-JP')
 * @returns Formatted datetime string
 */
export function formatDateTime(date: string | Date, locale = 'ja-JP'): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return dateObj.toLocaleString(locale, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
