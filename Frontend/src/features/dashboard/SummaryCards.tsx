import { TrendingUp, TrendingDown, Wallet, Percent } from 'lucide-react';
import type { SummaryResponse } from '@/types/analytics.types';
import { formatCurrency } from '@/lib/utils';
import { SkeletonCard } from '@/components/ui/Skeleton';

interface SummaryCardsProps {
  summary:   SummaryResponse | null;
  isLoading: boolean;
}

interface StatCardProps {
  label:      string;
  value:      string;
  icon:       React.ReactNode;
  accent:     string; // Tailwind bg class for the icon bubble
  valueColor?: string;
}

function StatCard({ label, value, icon, accent, valueColor = 'text-ink' }: StatCardProps) {
  return (
    <div className="bg-white rounded-2xl shadow-card p-5 flex items-start gap-4">
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${accent}`}>
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium text-ink-muted mb-1">{label}</p>
        <p className={`text-xl font-bold tabular-nums ${valueColor}`}>{value}</p>
      </div>
    </div>
  );
}

export default function SummaryCards({ summary, isLoading }: SummaryCardsProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
      </div>
    );
  }

  const income   = summary?.totalIncome  ?? 0;
  const expense  = summary?.totalExpense ?? 0;
  const balance  = summary?.netBalance   ?? 0;
  const savingsRate = income > 0 ? (balance / income) * 100 : 0;

  const cards: StatCardProps[] = [
    {
      label:  'Total Income',
      value:  formatCurrency(income),
      icon:   <TrendingUp  size={18} className="text-emerald-600" />,
      accent: 'bg-accent-mint/40',
    },
    {
      label:  'Total Expenses',
      value:  formatCurrency(expense),
      icon:   <TrendingDown size={18} className="text-rose-600" />,
      accent: 'bg-accent-pink/40',
    },
    {
      label:      'Net Balance',
      value:      formatCurrency(balance),
      icon:       <Wallet size={18} className={balance >= 0 ? 'text-indigo-500' : 'text-red-500'} />,
      accent:     balance >= 0 ? 'bg-accent-lavender/30' : 'bg-red-50',
      valueColor: balance >= 0 ? 'text-ink' : 'text-red-600',
    },
    {
      label:      'Savings Rate',
      value:      `${savingsRate.toFixed(1)}%`,
      icon:       <Percent size={18} className="text-amber-600" />,
      accent:     'bg-accent-butter/60',
      valueColor: savingsRate >= 20 ? 'text-emerald-600' : 'text-ink',
    },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => (
        <StatCard key={card.label} {...card} />
      ))}
    </div>
  );
}
