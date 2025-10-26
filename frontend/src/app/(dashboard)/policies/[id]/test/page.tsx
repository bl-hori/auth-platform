/**
 * Policy Test Page
 *
 * @description Page for testing policy execution with sample inputs
 */

'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Play, CheckCircle, XCircle } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { LoadingSpinner } from '@/components/ui/loading-spinner'
import { useToast } from '@/hooks/use-toast'
import { PolicyEditor } from '@/components/policies/policy-editor'
import {
  getPolicy,
  testPolicy,
  type PolicyTestRequest,
  type PolicyTestResponse,
} from '@/lib/api/policies'
import type { Policy } from '@/types'

const DEFAULT_TEST_INPUT = `{
  "method": "GET",
  "path": ["users", "user-001"],
  "user": {
    "id": "user-001",
    "role": "admin",
    "permissions": ["user:read", "user:write"]
  },
  "resource": {
    "type": "user",
    "id": "user-001",
    "owner": "user-001"
  }
}`

/**
 * Policy test page component
 */
export default function PolicyTestPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const router = useRouter()
  const { toast } = useToast()

  const [policy, setPolicy] = useState<Policy | null>(null)
  const [loading, setLoading] = useState(true)
  const [testInput, setTestInput] = useState(DEFAULT_TEST_INPUT)
  const [testResult, setTestResult] = useState<PolicyTestResponse | null>(null)
  const [testing, setTesting] = useState(false)
  const [inputError, setInputError] = useState<string>('')

  /**
   * Load policy data
   */
  useEffect(() => {
    const loadData = async () => {
      setLoading(true)
      try {
        const { id } = await params
        const policyData = await getPolicy(id)
        setPolicy(policyData)
      } catch (error) {
        console.error('Failed to load policy:', error)
        toast({
          title: 'エラー',
          description: 'ポリシー情報の読み込みに失敗しました',
          variant: 'destructive',
        })
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [params, toast])

  /**
   * Validate JSON input
   */
  const validateInput = (input: string): boolean => {
    try {
      JSON.parse(input)
      setInputError('')
      return true
    } catch (error) {
      setInputError('無効なJSON形式です')
      return false
    }
  }

  /**
   * Handle input change
   */
  const handleInputChange = (value: string) => {
    setTestInput(value)
    validateInput(value)
  }

  /**
   * Handle test execution
   */
  const handleRunTest = async () => {
    if (!policy) return
    if (!validateInput(testInput)) {
      toast({
        title: 'エラー',
        description: 'テスト入力のJSON形式が無効です',
        variant: 'destructive',
      })
      return
    }

    setTesting(true)
    setTestResult(null)
    try {
      const input = JSON.parse(testInput)
      const request: PolicyTestRequest = { input }

      const result = await testPolicy(policy.id, request)
      setTestResult(result)

      toast({
        title: '成功',
        description: 'テストを実行しました',
        variant: 'success',
      })
    } catch (error) {
      console.error('Failed to test policy:', error)
      toast({
        title: 'エラー',
        description: 'テストの実行に失敗しました',
        variant: 'destructive',
      })
    } finally {
      setTesting(false)
    }
  }

  /**
   * Load predefined test case
   */
  const loadTestCase = (testCase: 'admin' | 'user' | 'guest') => {
    const testCases = {
      admin: `{
  "method": "GET",
  "path": ["users"],
  "user": {
    "id": "user-001",
    "role": "admin",
    "permissions": ["user:read", "user:write", "role:read", "role:write"]
  }
}`,
      user: `{
  "method": "GET",
  "path": ["users", "user-002"],
  "user": {
    "id": "user-002",
    "role": "user",
    "permissions": ["user:read"]
  },
  "resource": {
    "type": "user",
    "id": "user-002",
    "owner": "user-002"
  }
}`,
      guest: `{
  "method": "POST",
  "path": ["users"],
  "user": {
    "id": "guest-001",
    "role": "guest",
    "permissions": []
  }
}`,
    }

    setTestInput(testCases[testCase])
    setInputError('')
  }

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!policy) {
    return (
      <div className="space-y-6">
        <Alert variant="destructive">
          <AlertDescription>ポリシーが見つかりません</AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.push(`/policies/${policy.id}`)}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">ポリシーテスト</h1>
          <p className="text-muted-foreground">{policy.displayName}</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>テスト入力 (JSON)</CardTitle>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => loadTestCase('admin')}
                  >
                    管理者
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => loadTestCase('user')}
                  >
                    ユーザー
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => loadTestCase('guest')}
                  >
                    ゲスト
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <PolicyEditor
                value={testInput}
                onChange={handleInputChange}
                readOnly={testing}
                height="400px"
              />
              {inputError && (
                <Alert variant="destructive">
                  <AlertDescription>{inputError}</AlertDescription>
                </Alert>
              )}
              <Button
                onClick={handleRunTest}
                disabled={testing || !!inputError}
                className="w-full"
              >
                <Play className="mr-2 h-4 w-4" />
                {testing ? 'テスト実行中...' : 'テスト実行'}
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>ポリシーコード</CardTitle>
            </CardHeader>
            <CardContent>
              <PolicyEditor
                value={policy.regoCode}
                onChange={() => {}}
                readOnly={true}
                height="300px"
              />
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>テスト結果</CardTitle>
          </CardHeader>
          <CardContent>
            {!testResult ? (
              <div className="flex h-64 items-center justify-center">
                <p className="text-muted-foreground">
                  テストを実行すると結果がここに表示されます
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="rounded-lg border p-4">
                  <div className="flex items-center gap-2 mb-2">
                    {testResult.result ? (
                      <>
                        <CheckCircle className="h-5 w-5 text-green-600" />
                        <Badge variant="success">許可 (ALLOW)</Badge>
                      </>
                    ) : (
                      <>
                        <XCircle className="h-5 w-5 text-destructive" />
                        <Badge variant="destructive">拒否 (DENY)</Badge>
                      </>
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground">
                    判定: {testResult.result ? 'アクセスが許可されました' : 'アクセスが拒否されました'}
                  </p>
                </div>

                {testResult.duration !== undefined && (
                  <div className="rounded-lg border p-4">
                    <p className="text-sm font-medium mb-1">実行時間</p>
                    <p className="text-2xl font-bold">{testResult.duration}ms</p>
                  </div>
                )}

                {testResult.output && (
                  <div className="space-y-2">
                    <p className="text-sm font-medium">出力データ</p>
                    <div className="rounded-lg bg-muted p-4">
                      <pre className="text-sm overflow-x-auto">
                        {JSON.stringify(testResult.output, null, 2)}
                      </pre>
                    </div>
                  </div>
                )}

                {testResult.errors && testResult.errors.length > 0 && (
                  <Alert variant="destructive">
                    <AlertDescription>
                      <p className="font-medium mb-2">エラー:</p>
                      <ul className="list-disc pl-4 space-y-1">
                        {testResult.errors.map((error, index) => (
                          <li key={index}>{error}</li>
                        ))}
                      </ul>
                    </AlertDescription>
                  </Alert>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
