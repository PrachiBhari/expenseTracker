export default function PageLoader() {
  return (
    <div
      className="flex items-center justify-center h-screen bg-cream"
      aria-busy="true"
      aria-label="Loading page"
    >
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-4 border-accent-lavender border-t-transparent rounded-full animate-spin" />
        <p className="text-xs text-ink-muted">Loading…</p>
      </div>
    </div>
  );
}
