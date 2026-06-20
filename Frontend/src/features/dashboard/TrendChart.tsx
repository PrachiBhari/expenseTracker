import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { TrendPoint } from '@/types/analytics.types';
import { formatShortCurrency, formatCurrency } from '@/lib/utils';
import { SkeletonCard } from '@/components/ui/Skeleton';
import EmptyState from '@/components/ui/EmptyState';

interface TrendChartProps {
  trends:    TrendPoint[];
  isLoading: boolean;
}

const tooltipFormatter = (
  value: number | string | readonly (number | string)[] | undefined
): [string, string] => [formatCurrency(Number(value ?? 0)), ''];

export default function TrendChart({ trends, isLoading }: TrendChartProps) {
  if (isLoading) return <SkeletonCard className="h-80" />;

  return (
    <div className="bg-white rounded-2xl shadow-card p-6">
      <h3 className="text-sm font-semibold text-ink mb-1">Income vs Expenses</h3>
      <p className="text-xs text-ink-muted mb-4">Monthly trend — last 6 months</p>

      {trends.length === 0 ? (
        <EmptyState
          title="No trend data yet"
          description="Add transactions over time to see your monthly trend."
        />
      ) : (
        <ResponsiveContainer width="100%" height={260}>
          <BarChart
            data={trends}
            margin={{ top: 4, right: 8, left: 8, bottom: 0 }}
            barCategoryGap="30%"
          >
            <CartesianGrid strokeDasharray="3 3" stroke="#F3F0EC" vertical={false} />
            <XAxis
              dataKey="month"
              tick={{ fontSize: 11, fill: '#6B6478' }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tickFormatter={(v: number) => formatShortCurrency(v)}
              tick={{ fontSize: 11, fill: '#6B6478' }}
              axisLine={false}
              tickLine={false}
              width={56}
            />
            <Tooltip
              formatter={tooltipFormatter}
              cursor={{ fill: '#FAF7F0' }}
              contentStyle={{
                border:       '1px solid #E5E4E7',
                borderRadius: '12px',
                fontSize:     '12px',
              }}
            />
            <Legend
              iconType="circle"
              iconSize={8}
              wrapperStyle={{ fontSize: '12px' }}
            />
            <Bar dataKey="totalIncome"  name="Income"  fill="#B8E0D2" radius={[4, 4, 0, 0]} />
            <Bar dataKey="totalExpense" name="Expense" fill="#F8C8DC" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
