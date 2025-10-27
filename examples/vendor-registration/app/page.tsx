import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function HomePage() {
  return (
    <div className="container flex min-h-[calc(100vh-8rem)] items-center justify-center py-12">
      <div className="mx-auto max-w-2xl text-center">
        <h1 className="mb-4 text-4xl font-bold tracking-tight">取引先登録申請システム</h1>
        <p className="mb-8 text-lg text-muted-foreground">
          Auth Platform 認可機構の実装例
        </p>

        <div className="mb-8 grid gap-4 md:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">申請者</CardTitle>
              <CardDescription>取引先情報を入力して申請</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                自分の申請を作成・編集・削除できます
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">承認者</CardTitle>
              <CardDescription>申請内容を確認して承認/却下</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                全ての申請を閲覧・承認できます
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">管理者</CardTitle>
              <CardDescription>全ての申請を管理</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                全ての操作を実行できます
              </p>
            </CardContent>
          </Card>
        </div>

        <div className="flex justify-center gap-4">
          <Link href="/login">
            <Button size="lg">ログイン</Button>
          </Link>
          <Link href="/dashboard">
            <Button size="lg" variant="outline">
              ダッシュボード
            </Button>
          </Link>
        </div>

        <div className="mt-8 text-sm text-muted-foreground">
          <p>このアプリケーションは Auth Platform の認可機構を使用した実装例です</p>
        </div>
      </div>
    </div>
  );
}
