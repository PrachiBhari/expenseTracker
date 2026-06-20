import { useState } from 'react';
import { useSummary }           from './hooks/useSummary';
import { useCategoryBreakdown } from './hooks/useCategoryBreakdown';
import { useTrends }            from './hooks/useTrends';
import SummaryCards  from './SummaryCards';
import CategoryDonut from './CategoryDonut';
import TrendChart    from './TrendChart';
import {
  type Period,
  PERIOD_LABELS,
  getPeriodDates,
} from './periodUtils';

const PERIOD_OPTIONS: Period[] = ['this-month', 'last-month', 'last-6-months', 'custom'];

export default function DashboardPage() {
  const [period,     setPeriod]     = useState<Period>('this-month');
  const [customFrom, setCustomFrom] = useState('');
  const [customTo,   setCustomTo]   = useState('');

  // Resolve the active date range
  const { from, to } =
    period === 'custom'
      ? { from: customFrom, to: customTo }
      : getPeriodDates(period);

  const { summary,   isLoading: summaryLoading   } = useSummary(from, to);
  const { breakdown, isLoading: breakdownLoading } = useCategoryBreakdown(from, to);
  const { trends,    isLoading: trendsLoading    } = useTrends(6);

  return (
    <div className="space-y-6">

      {/* Period selector */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex gap-1 bg-white rounded-xl shadow-card p-1">
          {PERIOD_OPTIONS.map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                period === p
                  ? 'bg-ink text-white'
                  : 'text-ink-muted hover:text-ink hover:bg-gray-100'
              }`}
            >
              {PERIOD_LABELS[p]}
            </button>
          ))}
        </div>

        {/* Custom date inputs — only visible when Custom is selected */}
        {period === 'custom' && (
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={customFrom}
              onChange={(e) => setCustomFrom(e.target.value)}
              className="h-9 px-3 rounded-xl border border-gray-200 text-xs text-ink
                focus:outline-none focus:ring-2 focus:ring-accent-lavender focus:ring-offset-1"
            />
            <span className="text-ink-muted text-xs">to</span>
            <input
              type="date"
              value={customTo}
              onChange={(e) => setCustomTo(e.target.value)}
              className="h-9 px-3 rounded-xl border border-gray-200 text-xs text-ink
                focus:outline-none focus:ring-2 focus:ring-accent-lavender focus:ring-offset-1"
            />
          </div>
        )}
      </div>

      {/* Summary cards */}
      <SummaryCards summary={summary} isLoading={summaryLoading} />

      {/* Charts grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <CategoryDonut breakdown={breakdown} isLoading={breakdownLoading} />
        <TrendChart    trends={trends}       isLoading={trendsLoading}    />
      </div>

    </div>
  );
}
