interface BadgeProps {
  label: string;
  color?: string;
}

export default function Badge({ label, color }: BadgeProps) {
  if (color) {
    return (
      <span
        className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
        style={{
          backgroundColor: color + '33',
          color: color,
        }}
      >
        {label}
      </span>
    );
  }

  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-ink-muted">
      {label}
    </span>
  );
}

export function TypeBadge({ type }: { type: 'INCOME' | 'EXPENSE' }) {
  return type === 'INCOME' ? (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-accent-mint/40 text-emerald-700">
      Income
    </span>
  ) : (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-accent-pink/40 text-rose-700">
      Expense
    </span>
  );
}
