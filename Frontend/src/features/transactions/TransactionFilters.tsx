import { Search } from 'lucide-react';
import type { Category } from '@/types/category.types';

export interface FilterState {
  from:       string;
  to:         string;
  categoryId: number | '';
  q:          string;
}

interface TransactionFiltersProps {
  filters:    FilterState;
  categories: Category[];
  onChange:   (filters: FilterState) => void;
  onReset:    () => void;
}

export default function TransactionFilters({
  filters,
  categories,
  onChange,
  onReset,
}: TransactionFiltersProps) {
  const set = <K extends keyof FilterState>(key: K, value: FilterState[K]) =>
    onChange({ ...filters, [key]: value });

  const hasActiveFilters =
    filters.from || filters.to || filters.categoryId !== '' || filters.q;

  return (
    <div className="bg-white rounded-2xl shadow-card p-4 mb-4">
      <div className="flex flex-wrap gap-3 items-end">
        {/* Search */}
        <div className="relative flex-1 min-w-40">
          <Search
            size={15}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-muted"
          />
          <input
            type="text"
            placeholder="Search description…"
            value={filters.q}
            onChange={(e) => set('q', e.target.value)}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-gray-200 text-sm text-ink
              placeholder:text-ink-muted/50 focus:outline-none focus:ring-2
              focus:ring-accent-lavender focus:ring-offset-1 hover:border-gray-300"
          />
        </div>

        {/* Category */}
        <div className="min-w-36">
          <select
            value={filters.categoryId}
            onChange={(e) =>
              set('categoryId', e.target.value === '' ? '' : Number(e.target.value))
            }
            className="w-full h-10 px-3 rounded-xl border border-gray-200 bg-white text-sm text-ink
              focus:outline-none focus:ring-2 focus:ring-accent-lavender focus:ring-offset-1
              hover:border-gray-300"
          >
            <option value="">All categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        {/* Date range */}
        <input
          type="date"
          value={filters.from}
          onChange={(e) => set('from', e.target.value)}
          className="h-10 px-3 rounded-xl border border-gray-200 text-sm text-ink
            focus:outline-none focus:ring-2 focus:ring-accent-lavender focus:ring-offset-1
            hover:border-gray-300"
        />
        <input
          type="date"
          value={filters.to}
          onChange={(e) => set('to', e.target.value)}
          className="h-10 px-3 rounded-xl border border-gray-200 text-sm text-ink
            focus:outline-none focus:ring-2 focus:ring-accent-lavender focus:ring-offset-1
            hover:border-gray-300"
        />

        {/* Reset */}
        {hasActiveFilters && (
          <button
            onClick={onReset}
            className="h-10 px-4 rounded-xl text-sm text-ink-muted hover:bg-gray-100 transition-colors"
          >
            Clear
          </button>
        )}
      </div>
    </div>
  );
}
