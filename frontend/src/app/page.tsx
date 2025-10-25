export default function Home() {
  return (
    <div className="grid grid-rows-[20px_1fr_20px] items-center justify-items-center min-h-screen p-8 pb-20 gap-16 sm:p-20">
      <main className="flex flex-col gap-8 row-start-2 items-center sm:items-start">
        <h1 className="text-4xl font-bold text-center sm:text-left">
          Auth Platform
        </h1>
        <p className="text-lg text-center sm:text-left max-w-2xl text-muted-foreground">
          Enterprise Authorization Platform - Phase 1 MVP
        </p>
        <div className="flex gap-4 items-center flex-col sm:flex-row">
          <a
            className="rounded-md bg-primary text-primary-foreground hover:bg-primary/90 px-8 py-3 text-sm font-medium transition-colors"
            href="/login"
          >
            ログイン
          </a>
          <a
            className="rounded-md border border-input bg-background hover:bg-accent hover:text-accent-foreground px-8 py-3 text-sm font-medium transition-colors"
            href="/docs"
          >
            ドキュメント
          </a>
        </div>
      </main>
      <footer className="row-start-3 flex gap-6 flex-wrap items-center justify-center">
        <p className="text-sm text-muted-foreground">
          © 2025 Auth Platform. All rights reserved.
        </p>
      </footer>
    </div>
  )
}
