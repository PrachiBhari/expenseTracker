interface SkeletonProps {
  className?: string;
}

export function SkeletonLine({ className = '' }: SkeletonProps) {
  return (
    <div className={`animate-pulse rounded-lg bg-gray-100 ${className}`} />
  );
}

export function SkeletonCard({ className = '' }: SkeletonProps) {
  return (
    <div className={`bg-white rounded-2xl shadow-card p-6 space-y-3 ${className}`}>
      <SkeletonLine className="h-3 w-20" />
      <SkeletonLine className="h-7 w-32" />
      <SkeletonLine className="h-3 w-16" />
    </div>
  );
}

export function SkeletonRow() {
  return (
    <div className="flex items-center gap-4 py-3 px-4">
      <SkeletonLine className="h-8 w-8 rounded-full" />
      <div className="flex-1 space-y-2">
        <SkeletonLine className="h-3 w-32" />
        <SkeletonLine className="h-3 w-20" />
      </div>
      <SkeletonLine className="h-4 w-16" />
    </div>
  );
}
