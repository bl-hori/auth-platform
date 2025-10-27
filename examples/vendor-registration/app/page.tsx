export default function HomePage() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">取引先登録申請システム</h1>
        <p className="mb-8 text-lg text-muted-foreground">
          Auth Platform 認可機構の実装例
        </p>
        <a
          href="/login"
          className="inline-block rounded-lg bg-primary px-6 py-3 text-primary-foreground hover:bg-primary/90"
        >
          ログイン
        </a>
      </div>
    </div>
  );
}
