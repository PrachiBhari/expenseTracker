import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { CategoryBreakdown } from '@/types/analytics.types';
import { formatCurrency } from '@/lib/utils';
import { SkeletonCard } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';

interface CategoryDonutProps {
  breakdown: CategoryBreakdown[];
  isLoading: boolean;
}

// ValueType in Recharts v3 is number | string | (number|string)[] | undefined
const tooltipFormatter = (
  value: number | string | readonly (number | string)[] | undefined
): [string, string] => [formatCurrency(Number(value ?? 0)), ''];

export default function CategoryDonut({ breakdown, isLoading }: CategoryDonutProps) {
  if (isLoading) return <SkeletonCard className="h-80" />;

  return (
    <div className="bg-white rounded-2xl shadow-card p-6">
      <h3 className="text-sm font-semibold text-ink mb-1">Spending by Category</h3>
      <p className="text-xs text-ink-muted mb-4">Current period — expense breakdown</p>

      {breakdown.length === 0 ? (
        <EmptyState
          title="No expense data"
          description="Add expenses to see category breakdown."
        />
      ) : (
        <ResponsiveContainer width="100%" height={260}>
          <PieChart>
            <Pie
              data={breakdown}
              dataKey="total"
              nameKey="name"
              cx="50%"
              cy="50%"
              innerRadius={65}
              outerRadius={100}
              paddingAngle={3}
            >
              {breakdown.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={entry.color || '#C8B6E2'}
                />
              ))}
            </Pie>
            <Tooltip formatter={tooltipFormatter} />
            <Legend
              iconType="circle"
              iconSize={8}
              wrapperStyle={{ fontSize: '12px' }}
            />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
