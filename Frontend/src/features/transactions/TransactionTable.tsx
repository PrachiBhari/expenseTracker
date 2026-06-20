import { Pencil, Trash2 } from 'lucide-react';
import Badge from '@/components/ui/Badge';
import { SkeletonRow } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';

export interface TransactionRow {
  id: number;
  type: 'EXPENSE' | 'INCOME';
  date: string;
  categoryName: string;
  categoryColor: string;
  description?: string;
  amount: number;
  extra?: string; // paymentMethod (expense) or source (income)
}

interface TransactionTableProps {
  rows: TransactionRow[];
  isLoading: boolean;
  onEdit:   (id: number) => void;
  onDelete: (id: number) => void;
}

function formatAmount(amount: number, type: 'EXPENSE' | 'INCOME') {
  const formatted = new Intl.NumberFormat('en-IN', {
    style:    'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(amount);

  return type === 'EXPENSE' ? `−${formatted}` : `+${formatted}`;
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day:   '2-digit',
    month: 'short',
    year:  'numeric',
  });
}

export default function TransactionTable({
  rows,
  isLoading,
  onEdit,
  onDelete,
}: TransactionTableProps) {
  if (isLoading) {
    return (
      <div className="bg-white rounded-2xl shadow-card divide-y divide-gray-50">
        {Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} />)}
      </div>
    );
  }

  if (rows.length === 0) {
    return (
      <div className="bg-white rounded-2xl shadow-card">
        <EmptyState
          title="No transactions found"
          description="Try adjusting your filters, or add your first transaction."
        />
      </div>
    );
  }

  return (
    <div className="bg-white rounded-2xl shadow-card overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100">
            <th className="text-left px-4 py-3 text-xs font-semibold text-ink-muted uppercase tracking-wide">Date</th>
            <th className="text-left px-4 py-3 text-xs font-semibold text-ink-muted uppercase tracking-wide">Category</th>
            <th className="text-left px-4 py-3 text-xs font-semibold text-ink-muted uppercase tracking-wide hidden sm:table-cell">Description</th>
            <th className="text-right px-4 py-3 text-xs font-semibold text-ink-muted uppercase tracking-wide">Amount</th>
            <th className="px-4 py-3" />
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {rows.map((row) => (
            <tr key={`${row.type}-${row.id}`} className="hover:bg-gray-50/60 transition-colors group">
              <td className="px-4 py-3 text-ink-muted whitespace-nowrap">
                {formatDate(row.date)}
              </td>
              <td className="px-4 py-3">
                <Badge label={row.categoryName} color={row.categoryColor} />
              </td>
              <td className="px-4 py-3 text-ink hidden sm:table-cell max-w-[200px] truncate">
                {row.description || row.extra || '—'}
              </td>
              <td className={`px-4 py-3 text-right font-medium tabular-nums whitespace-nowrap ${
                row.type === 'INCOME' ? 'text-emerald-600' : 'text-rose-600'
              }`}>
                {formatAmount(row.amount, row.type)}
              </td>
              <td className="px-4 py-3">
                <div className="flex gap-1 justify-end opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    onClick={() => onEdit(row.id)}
                    className="p-1.5 rounded-lg text-ink-muted hover:bg-gray-100 transition-colors"
                    aria-label="Edit"
                  >
                    <Pencil size={14} />
                  </button>
                  <button
                    onClick={() => onDelete(row.id)}
                    className="p-1.5 rounded-lg text-ink-muted hover:bg-red-50 hover:text-red-500 transition-colors"
                    aria-label="Delete"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
