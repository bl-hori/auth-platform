import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * Tailwind CSSのクラス名をマージするユーティリティ関数
 *
 * @param inputs - マージするクラス名（文字列、配列、オブジェクト）
 * @returns マージされたクラス名文字列
 *
 * @example
 * ```tsx
 * cn("px-2 py-1", "bg-blue-500")
 * // => "px-2 py-1 bg-blue-500"
 *
 * cn("px-2", { "bg-blue-500": true, "text-white": false })
 * // => "px-2 bg-blue-500"
 * ```
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
