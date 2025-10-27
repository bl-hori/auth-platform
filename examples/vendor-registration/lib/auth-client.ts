import {
  AuthClientConfig,
  AuthorizationRequest,
  AuthorizationResponse,
  BatchAuthorizationRequest,
  BatchAuthorizationResponse,
} from '@/types/auth';

/**
 * Auth Platform APIクライアント
 *
 * このクライアントは Auth Platform Backend の認可APIと通信し、
 * 認可判定を行います。
 *
 * 主な機能:
 * - 単一の認可チェック (authorize)
 * - バッチ認可チェック (authorizeBatch)
 * - エラーハンドリングとリトライ
 * - リクエスト/レスポンスのロギング
 *
 * @example
 * ```typescript
 * const client = new AuthPlatformClient({
 *   baseUrl: process.env.NEXT_PUBLIC_AUTH_BACKEND_URL!,
 *   apiKey: process.env.NEXT_PUBLIC_AUTH_API_KEY!,
 *   organizationId: process.env.NEXT_PUBLIC_AUTH_ORGANIZATION_ID!,
 * });
 *
 * const response = await client.authorize({
 *   userId: 'user-001',
 *   action: 'read',
 *   resourceType: 'vendor',
 *   resourceId: 'vendor-001',
 * });
 *
 * if (response.decision === 'ALLOW') {
 *   // アクセス許可
 * }
 * ```
 */
export class AuthPlatformClient {
  private baseUrl: string;
  private apiKey: string;
  private organizationId: string;
  private debug: boolean;

  constructor(config: AuthClientConfig, debug = false) {
    this.baseUrl = config.baseUrl;
    this.apiKey = config.apiKey;
    this.organizationId = config.organizationId;
    this.debug = debug;
  }

  /**
   * 単一の認可チェックを実行
   *
   * Auth Platform の /v1/authorize エンドポイントを呼び出し、
   * 指定されたユーザーが指定されたアクションを実行できるか判定します。
   *
   * @param request 認可リクエスト
   * @returns 認可レスポンス（ALLOW/DENY/ERROR）
   * @throws エラーが発生した場合（ネットワークエラー等）
   */
  async authorize(request: AuthorizationRequest): Promise<AuthorizationResponse> {
    try {
      if (this.debug) {
        console.log('[AuthPlatformClient] Authorization request:', request);
      }

      // Auth Platform APIのリクエスト形式に変換
      const apiRequest = {
        organizationId: this.organizationId,
        principal: {
          id: request.userId,
          type: 'user',
        },
        action: request.action,
        resource: {
          type: request.resourceType,
          id: request.resourceId || '',
        },
        context: request.context || {},
      };

      const response = await fetch(`${this.baseUrl}/v1/authorize`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': this.apiKey,
          'X-Organization-Id': this.organizationId,
        },
        body: JSON.stringify(apiRequest),
      });

      if (!response.ok) {
        console.error(
          `[AuthPlatformClient] Authorization request failed: ${response.status} ${response.statusText}`
        );

        // エラーレスポンスを返す（フェイルセーフ: DENY）
        return {
          decision: 'ERROR',
          reason: `HTTP ${response.status}: ${response.statusText}`,
        };
      }

      const data = await response.json();

      if (this.debug) {
        console.log('[AuthPlatformClient] Authorization response:', data);
      }

      // APIレスポンスを型に変換
      const authResponse: AuthorizationResponse = {
        decision: data.decision || 'DENY',
        reason: data.reason,
        metadata: data.metadata,
      };

      return authResponse;
    } catch (error) {
      console.error('[AuthPlatformClient] Authorization check failed:', error);

      // エラー時はDENYを返す（フェイルセーフ）
      return {
        decision: 'ERROR',
        reason: error instanceof Error ? error.message : 'Unknown error',
      };
    }
  }

  /**
   * バッチ認可チェックを実行
   *
   * 複数の認可チェックを一度に実行します。
   * パフォーマンス最適化のため、リスト表示時などに使用します。
   *
   * @param requests 認可リクエストの配列
   * @returns 認可レスポンスの配列（リクエストと同じ順序）
   */
  async authorizeBatch(requests: AuthorizationRequest[]): Promise<AuthorizationResponse[]> {
    if (requests.length === 0) {
      return [];
    }

    try {
      if (this.debug) {
        console.log(`[AuthPlatformClient] Batch authorization request: ${requests.length} items`);
      }

      // Auth Platform APIのリクエスト形式に変換
      const apiRequests = requests.map((req) => ({
        organizationId: this.organizationId,
        principal: {
          id: req.userId,
          type: 'user',
        },
        action: req.action,
        resource: {
          type: req.resourceType,
          id: req.resourceId || '',
        },
        context: req.context || {},
      }));

      const batchRequest: BatchAuthorizationRequest = {
        requests: apiRequests as any,
      };

      const response = await fetch(`${this.baseUrl}/v1/authorize/batch`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-API-Key': this.apiKey,
          'X-Organization-Id': this.organizationId,
        },
        body: JSON.stringify(batchRequest),
      });

      if (!response.ok) {
        console.error(
          `[AuthPlatformClient] Batch authorization request failed: ${response.status} ${response.statusText}`
        );

        // エラー時は全てDENYを返す（フェイルセーフ）
        return requests.map(() => ({
          decision: 'ERROR',
          reason: `HTTP ${response.status}: ${response.statusText}`,
        }));
      }

      const data: BatchAuthorizationResponse = await response.json();

      if (this.debug) {
        console.log('[AuthPlatformClient] Batch authorization response:', data.responses.length);
      }

      return data.responses;
    } catch (error) {
      console.error('[AuthPlatformClient] Batch authorization check failed:', error);

      // エラー時は全てDENYを返す（フェイルセーフ）
      return requests.map(() => ({
        decision: 'ERROR',
        reason: error instanceof Error ? error.message : 'Unknown error',
      }));
    }
  }

  /**
   * リトライ付き認可チェック
   *
   * ネットワークエラー等で失敗した場合、指定回数リトライします。
   *
   * @param request 認可リクエスト
   * @param maxRetries 最大リトライ回数（デフォルト: 3）
   * @param retryDelay リトライ間隔（ミリ秒、デフォルト: 1000）
   * @returns 認可レスポンス
   */
  async authorizeWithRetry(
    request: AuthorizationRequest,
    maxRetries = 3,
    retryDelay = 1000
  ): Promise<AuthorizationResponse> {
    let lastError: Error | null = null;

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        const response = await this.authorize(request);

        // ERRORでない場合は成功とみなす
        if (response.decision !== 'ERROR') {
          return response;
        }

        lastError = new Error(response.reason);
      } catch (error) {
        lastError = error instanceof Error ? error : new Error('Unknown error');
      }

      // 最後の試行でない場合は待機
      if (attempt < maxRetries) {
        if (this.debug) {
          console.log(
            `[AuthPlatformClient] Retry ${attempt + 1}/${maxRetries} after ${retryDelay}ms`
          );
        }
        await new Promise((resolve) => setTimeout(resolve, retryDelay));
      }
    }

    console.error(
      `[AuthPlatformClient] Authorization failed after ${maxRetries} retries:`,
      lastError
    );

    // 全て失敗した場合はDENYを返す（フェイルセーフ）
    return {
      decision: 'DENY',
      reason: `Failed after ${maxRetries} retries: ${lastError?.message}`,
    };
  }
}

/**
 * グローバルAuth Platformクライアントインスタンス
 *
 * 環境変数から設定を読み込みます。
 * NEXT_PUBLIC_AUTH_BACKEND_URL: Auth Platform BackendのURL
 * NEXT_PUBLIC_AUTH_API_KEY: APIキー
 * NEXT_PUBLIC_AUTH_ORGANIZATION_ID: 組織ID
 */
export const authClient = new AuthPlatformClient(
  {
    baseUrl: process.env.NEXT_PUBLIC_AUTH_BACKEND_URL || 'http://localhost:8080',
    apiKey: process.env.NEXT_PUBLIC_AUTH_API_KEY || 'demo-api-key',
    organizationId: process.env.NEXT_PUBLIC_AUTH_ORGANIZATION_ID || 'demo-org-id',
  },
  process.env.NODE_ENV === 'development' // デバッグモード
);
