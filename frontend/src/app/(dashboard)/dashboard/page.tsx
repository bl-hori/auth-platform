'use client'

/**
 * Dashboard Page
 *
 * @description Main dashboard overview page
 */

import { Users, Shield, FileText, Activity } from 'lucide-react'

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

/**
 * Dashboard statistics card
 */
function StatCard({
  title,
  value,
  description,
  icon: Icon,
}: {
  title: string
  value: string
  description: string
  icon: React.ElementType
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        <p className="text-xs text-muted-foreground">{description}</p>
      </CardContent>
    </Card>
  )
}

/**
 * Dashboard page component
 */
export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">
          ダッシュボード
        </h1>
        <p className="text-muted-foreground">
          Auth Platform の概要と統計情報を確認できます
        </p>
      </div>

          {/* Statistics Grid */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <StatCard
              title="総ユーザー数"
              value="0"
              description="組織内の全ユーザー"
              icon={Users}
            />
            <StatCard
              title="アクティブロール"
              value="0"
              description="現在使用中のロール"
              icon={Shield}
            />
            <StatCard
              title="ポリシー数"
              value="0"
              description="定義済みポリシー"
              icon={FileText}
            />
            <StatCard
              title="認証リクエスト"
              value="0"
              description="過去24時間"
              icon={Activity}
            />
          </div>

          {/* Recent Activity */}
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>最近のアクティビティ</CardTitle>
                <CardDescription>
                  直近の認証リクエストと変更履歴
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <p className="text-sm text-muted-foreground">
                    アクティビティがまだありません
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>システムステータス</CardTitle>
                <CardDescription>各コンポーネントの稼働状況</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Backend API</span>
                    <span className="flex items-center text-sm text-green-600">
                      <span className="mr-2 h-2 w-2 rounded-full bg-green-600" />
                      稼働中
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Database</span>
                    <span className="flex items-center text-sm text-green-600">
                      <span className="mr-2 h-2 w-2 rounded-full bg-green-600" />
                      稼働中
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm">Cache (Redis)</span>
                    <span className="flex items-center text-sm text-green-600">
                      <span className="mr-2 h-2 w-2 rounded-full bg-green-600" />
                      稼働中
                    </span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>クイックアクション</CardTitle>
              <CardDescription>よく使う操作へのショートカット</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-3">
                <button className="flex flex-col items-center justify-center rounded-lg border p-4 transition-colors hover:bg-accent">
                  <Users className="mb-2 h-8 w-8 text-primary" />
                  <span className="text-sm font-medium">新規ユーザー作成</span>
                </button>
                <button className="flex flex-col items-center justify-center rounded-lg border p-4 transition-colors hover:bg-accent">
                  <Shield className="mb-2 h-8 w-8 text-primary" />
                  <span className="text-sm font-medium">ロール管理</span>
                </button>
                <button className="flex flex-col items-center justify-center rounded-lg border p-4 transition-colors hover:bg-accent">
                  <FileText className="mb-2 h-8 w-8 text-primary" />
                  <span className="text-sm font-medium">ポリシー編集</span>
                </button>
              </div>
            </CardContent>
          </Card>
        </div>
  )
}
