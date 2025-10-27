'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { login } from '@/lib/session';
import { useSession } from '@/lib/session-context';
import { MOCK_USERS } from '@/lib/mock-data';
import { ROLE_LABELS, UserRole } from '@/types/auth';

/**
 * ログインページ
 *
 * デモ用のシンプルなログイン機能を提供します。
 * - メールアドレスまたはロールでログイン
 * - 実際のアプリケーションでは、バックエンドAPIで認証を行います
 */
export default function LoginPage() {
  const router = useRouter();
  const { refreshSession } = useSession();
  const [email, setEmail] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole>('applicant');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /**
   * メールアドレスでログイン
   */
  const handleEmailLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const user = login(email);
      if (user) {
        refreshSession();
        router.push('/dashboard');
      } else {
        setError('ユーザーが見つかりません');
      }
    } catch (err) {
      setError('ログインに失敗しました');
    } finally {
      setLoading(false);
    }
  };

  /**
   * ロールでログイン（デモ用）
   */
  const handleRoleLogin = () => {
    setLoading(true);
    setError('');

    try {
      const user = login('', selectedRole);
      if (user) {
        refreshSession();
        router.push('/dashboard');
      } else {
        setError('ログインに失敗しました');
      }
    } catch (err) {
      setError('ログインに失敗しました');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container flex min-h-[calc(100vh-8rem)] items-center justify-center py-12">
      <div className="mx-auto w-full max-w-md space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-bold">ログイン</h1>
          <p className="text-muted-foreground">
            取引先登録申請システムにログインしてください
          </p>
        </div>

        {error && (
          <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <Card>
          <CardHeader>
            <CardTitle>メールアドレスでログイン</CardTitle>
            <CardDescription>登録済みのメールアドレスを入力してください</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleEmailLogin} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">メールアドレス</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="example@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'ログイン中...' : 'ログイン'}
              </Button>
            </form>
          </CardContent>
        </Card>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-background px-2 text-muted-foreground">または</span>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>ロールで選択（デモ用）</CardTitle>
            <CardDescription>
              テスト用にロールを選択してログインできます
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="role">ロール</Label>
              <Select
                id="role"
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value as UserRole)}
              >
                <option value="applicant">{ROLE_LABELS.applicant}</option>
                <option value="approver">{ROLE_LABELS.approver}</option>
                <option value="admin">{ROLE_LABELS.admin}</option>
              </Select>
            </div>
            <Button onClick={handleRoleLogin} variant="outline" className="w-full" disabled={loading}>
              {loading ? 'ログイン中...' : `${ROLE_LABELS[selectedRole]}としてログイン`}
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm">デモユーザー</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-sm">
              {MOCK_USERS.map((user) => (
                <div key={user.id} className="flex justify-between">
                  <span className="text-muted-foreground">{user.email}</span>
                  <span className="font-medium">{ROLE_LABELS[user.role]}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-muted-foreground">
          これはデモアプリケーションです。実際の認証は行われません。
        </p>
      </div>
    </div>
  );
}
