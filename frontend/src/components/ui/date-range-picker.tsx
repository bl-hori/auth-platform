/**
 * Date Range Picker Component
 *
 * @description Component for selecting date ranges
 */

'use client'

import { useState } from 'react'
import { Calendar } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

interface DateRangePickerProps {
  startDate: string
  endDate: string
  onStartDateChange: (date: string) => void
  onEndDateChange: (date: string) => void
}

/**
 * Date range picker component
 */
export function DateRangePicker({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
}: DateRangePickerProps) {
  const [showPicker, setShowPicker] = useState(false)

  /**
   * Set predefined range
   */
  const setRange = (range: 'today' | 'week' | 'month' | '3months') => {
    const now = new Date()
    const end = now.toISOString().split('T')[0]
    let start: string

    switch (range) {
      case 'today':
        start = end
        break
      case 'week':
        start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
          .toISOString()
          .split('T')[0]
        break
      case 'month':
        start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
          .toISOString()
          .split('T')[0]
        break
      case '3months':
        start = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000)
          .toISOString()
          .split('T')[0]
        break
    }

    onStartDateChange(start)
    onEndDateChange(end)
    setShowPicker(false)
  }

  /**
   * Clear date range
   */
  const clearRange = () => {
    onStartDateChange('')
    onEndDateChange('')
    setShowPicker(false)
  }

  const hasDateRange = startDate || endDate

  return (
    <div className="relative">
      <Button
        variant="outline"
        onClick={() => setShowPicker(!showPicker)}
        className="w-[280px] justify-start text-left font-normal"
      >
        <Calendar className="mr-2 h-4 w-4" />
        {hasDateRange ? (
          <span>
            {startDate || '開始日'} - {endDate || '終了日'}
          </span>
        ) : (
          <span className="text-muted-foreground">期間を選択</span>
        )}
      </Button>

      {showPicker && (
        <Card className="absolute top-full left-0 mt-2 z-50 w-[400px]">
          <CardHeader>
            <CardTitle className="text-sm">期間を選択</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRange('today')}
              >
                今日
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRange('week')}
              >
                過去7日間
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRange('month')}
              >
                過去30日間
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRange('3months')}
              >
                過去90日間
              </Button>
            </div>

            <div className="space-y-2">
              <div>
                <Label htmlFor="start-date">開始日</Label>
                <Input
                  id="start-date"
                  type="date"
                  value={startDate}
                  onChange={e => onStartDateChange(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="end-date">終了日</Label>
                <Input
                  id="end-date"
                  type="date"
                  value={endDate}
                  onChange={e => onEndDateChange(e.target.value)}
                />
              </div>
            </div>

            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={clearRange}>
                クリア
              </Button>
              <Button size="sm" onClick={() => setShowPicker(false)}>
                適用
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
