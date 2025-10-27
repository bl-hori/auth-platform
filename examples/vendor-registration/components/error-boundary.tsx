'use client';

import React from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

/**
 * エラーバウンダリの状態
 */
interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * エラーバウンダリのプロパティ
 */
interface ErrorBoundaryProps {
  children: React.ReactNode;
  fallback?: (error: Error, reset: () => void) => React.ReactNode;
}

/**
 * エラーバウンダリコンポーネント
 *
 * 子コンポーネントでエラーが発生した場合にキャッチして、
 * フォールバックUIを表示します。
 */
export class ErrorBoundary extends React.Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  resetError = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError && this.state.error) {
      // カスタムフォールバックが提供されている場合はそれを使用
      if (this.props.fallback) {
        return this.props.fallback(this.state.error, this.resetError);
      }

      // デフォルトのエラー表示
      return (
        <div className="container flex min-h-[400px] items-center justify-center py-12">
          <Card className="max-w-lg">
            <CardHeader>
              <CardTitle className="text-destructive">エラーが発生しました</CardTitle>
              <CardDescription>
                申し訳ございません。予期しないエラーが発生しました。
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-md bg-destructive/10 p-4">
                <p className="text-sm font-mono text-destructive">
                  {this.state.error.message}
                </p>
              </div>
              <div className="flex gap-2">
                <Button onClick={this.resetError}>再試行</Button>
                <Button
                  variant="outline"
                  onClick={() => window.location.href = '/'}
                >
                  ホームに戻る
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}

/**
 * エラー表示コンポーネント
 *
 * エラーメッセージとリトライボタンを表示します。
 */
export function ErrorDisplay({
  error,
  onRetry,
  title = 'エラーが発生しました',
}: {
  error: string | Error;
  onRetry?: () => void;
  title?: string;
}) {
  const errorMessage = typeof error === 'string' ? error : error.message;

  return (
    <Card className="border-destructive">
      <CardHeader>
        <CardTitle className="text-destructive">{title}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-md bg-destructive/10 p-4">
          <p className="text-sm text-destructive">{errorMessage}</p>
        </div>
        {onRetry && (
          <Button onClick={onRetry} variant="outline">
            再試行
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
