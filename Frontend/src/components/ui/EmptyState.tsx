import { type ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
}

export default function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      {icon && (
        <div className="text-ink-muted opacity-30 mb-4">{icon}</div>
      )}
      <p className="text-sm font-medium text-ink mb-1">{title}</p>
      {description && (
        <p className="text-xs text-ink-muted mb-5 max-w-xs">{description}</p>
      )}
      {action}
    </div>
  );
}
