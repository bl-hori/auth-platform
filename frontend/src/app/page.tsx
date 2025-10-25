import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      {/* Hero Section */}
      <section className="container mx-auto px-4 py-16 sm:py-24">
        <div className="text-center space-y-6">
          <h1 className="text-4xl font-bold tracking-tight sm:text-6xl">
            Auth Platform
          </h1>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            Enterprise Authorization Platform - Phase 1 MVP
          </p>
          <p className="text-lg text-muted-foreground max-w-3xl mx-auto">
            強力なポリシーエンジンとロールベースアクセス制御（RBAC）を提供する、
            エンタープライズグレードの認可プラットフォームです。
          </p>
          <div className="flex gap-4 justify-center pt-4">
            <Button size="lg">今すぐ始める</Button>
            <Button variant="outline" size="lg">
              ドキュメントを見る
            </Button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="container mx-auto px-4 py-16">
        <h2 className="text-3xl font-bold text-center mb-12">主な機能</h2>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>ポリシーベース認可</CardTitle>
              <CardDescription>
                Open Policy Agent (OPA) を使用した柔軟なポリシー管理
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Regoポリシー言語で複雑な認可ロジックを記述。
                バージョン管理とテスト機能を標準装備。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>ロール階層管理</CardTitle>
              <CardDescription>
                階層的なロールとパーミッションの管理
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                組織構造に合わせた柔軟なロール設計。
                継承とスコープベースの権限管理をサポート。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>高速レスポンス</CardTitle>
              <CardDescription>
                マルチレイヤーキャッシングによる高性能
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Caffeine L1 + Redis L2キャッシュ。
                p95レイテンシ10ms以下を実現。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>監査ログ</CardTitle>
              <CardDescription>
                すべての認可判断を記録・追跡
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                タイムスタンプ付きの詳細なログ。
                コンプライアンス要件に対応。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>REST API</CardTitle>
              <CardDescription>
                シンプルで使いやすいAPIインターフェース
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                OpenAPI 3.0準拠。
                認可判断、ユーザー・ロール管理APIを提供。
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>マルチテナント対応</CardTitle>
              <CardDescription>
                組織ごとの完全なデータ分離
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                行レベルセキュリティによるデータ分離。
                組織間のデータ漏洩を防止。
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Demo Form Section */}
      <section className="container mx-auto px-4 py-16">
        <div className="max-w-md mx-auto">
          <Card>
            <CardHeader>
              <CardTitle>コンポーネントサンプル</CardTitle>
              <CardDescription>
                shadcn/uiコンポーネントのデモンストレーション
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">メールアドレス</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="you@example.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">パスワード</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="••••••••"
                />
              </div>
            </CardContent>
            <CardFooter className="flex flex-col gap-2">
              <Button className="w-full">ログイン</Button>
              <Button variant="outline" className="w-full">
                アカウント作成
              </Button>
              <div className="flex gap-2 w-full">
                <Button variant="secondary" className="flex-1">
                  Secondary
                </Button>
                <Button variant="ghost" className="flex-1">
                  Ghost
                </Button>
              </div>
              <div className="flex gap-2 w-full">
                <Button variant="destructive" size="sm" className="flex-1">
                  削除
                </Button>
                <Button variant="outline" size="sm" className="flex-1">
                  キャンセル
                </Button>
                <Button size="sm" className="flex-1">
                  保存
                </Button>
              </div>
            </CardFooter>
          </Card>
        </div>
      </section>

      {/* Stats Section */}
      <section className="container mx-auto px-4 py-16">
        <div className="grid gap-6 md:grid-cols-3">
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>認可リクエスト/秒</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold">10,000+</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>p95レイテンシ</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold">&lt; 10ms</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-3">
              <CardDescription>カバレッジ</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold">80%+</div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t">
        <div className="container mx-auto px-4 py-8">
          <p className="text-center text-sm text-muted-foreground">
            © 2025 Auth Platform. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  )
}
