/**
 * Skeleton Loading Component
 *
 * @description Skeleton loading placeholders for content
 */

import { cn } from '@/lib/utils'

/**
 * Props for Skeleton component
 */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type
interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {}

/**
 * Skeleton component for loading states
 *
 * @example
 * ```tsx
 * <Skeleton className="h-12 w-12 rounded-full" />
 * <Skeleton className="h-4 w-[250px]" />
 * ```
 */
function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-muted', className)}
      {...props}
    />
  )
}

export { Skeleton }
