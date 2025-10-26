'use client'

/**
 * Login Page
 *
 * @description Authentication page for API key-based login
 */

import { AlertCircle, Lock } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useState, type FormEvent } from 'react'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/contexts/auth-context'

/**
 * Login page component with form validation
 */
export default function LoginPage() {
  const router = useRouter()
  const { login } = useAuth()
  const [apiKey, setApiKey] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  /**
   * Handle form submission
   */
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError(null)

    // Validation
    if (!apiKey.trim()) {
      setError('APIキーを入力してください')
      return
    }

    setIsLoading(true)

    try {
      await login(apiKey)
      // Redirect to dashboard after successful login
      router.push('/dashboard')
    } catch (err) {
      console.error('Login failed:', err)
      setError(
        err instanceof Error
          ? err.message
          : 'ログインに失敗しました。APIキーを確認してください。'
      )
    } finally {
      setIsLoading(false)
    }
  }

  /**
   * Use development API key
   */
  const useDevelopmentKey = () => {
    const devApiKey = process.env.NEXT_PUBLIC_API_KEY || 'dev-api-key-12345'
    setApiKey(devApiKey)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 px-4 py-12 dark:from-gray-900 dark:to-gray-800">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
            <Lock className="h-6 w-6 text-primary" />
          </div>
          <CardTitle className="text-2xl font-bold">Auth Platform</CardTitle>
          <CardDescription>
            APIキーを入力してログインしてください
            <br />
            開発環境では下のボタンでテストキーを使用できます
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="apiKey">API Key</Label>
              <Input
                id="apiKey"
                name="apiKey"
                type="password"
                placeholder="dev-api-key-12345"
                value={apiKey}
                onChange={e => setApiKey(e.target.value)}
                disabled={isLoading}
                autoComplete="off"
                autoFocus
                data-testid="api-key-input"
              />
              <p className="text-xs text-muted-foreground">
                開発環境では .env.local に設定されたAPIキーを使用してください
              </p>
            </div>

            {error && (
              <Alert variant="destructive" data-testid="error-message">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <Button
                type="submit"
                className="w-full"
                disabled={isLoading}
                data-testid="login-button"
              >
                {isLoading ? 'ログイン中...' : 'ログイン'}
              </Button>

              {process.env.NODE_ENV === 'development' && (
                <Button
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={useDevelopmentKey}
                  disabled={isLoading}
                >
                  開発用APIキーを使用
                </Button>
              )}
            </div>
          </form>

          <div className="mt-6 border-t pt-6">
            <div className="space-y-2 text-center text-xs text-muted-foreground">
              <p>
                <strong>開発環境:</strong> NEXT_PUBLIC_API_KEY
              </p>
              <p>
                バックエンドAPI:{' '}
                {process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
