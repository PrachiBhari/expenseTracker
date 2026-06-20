export type Period = 'this-month' | 'last-month' | 'last-6-months' | 'custom';

export const PERIOD_LABELS: Record<Period, string> = {
  'this-month':    'This Month',
  'last-month':    'Last Month',
  'last-6-months': 'Last 6 Months',
  'custom':        'Custom',
};

// Returns ISO date strings (YYYY-MM-DD) for any non-custom period
export function getPeriodDates(period: Exclude<Period, 'custom'>): { from: string; to: string } {
  const now   = new Date();
  const year  = now.getFullYear();
  const month = now.getMonth();

  const toISO = (d: Date) => d.toISOString().split('T')[0];

  switch (period) {
    case 'this-month':
      return {
        from: toISO(new Date(year, month, 1)),
        to:   toISO(new Date(year, month + 1, 0)),
      };
    case 'last-month':
      return {
        from: toISO(new Date(year, month - 1, 1)),
        to:   toISO(new Date(year, month, 0)),
      };
    case 'last-6-months':
      return {
        from: toISO(new Date(year, month - 5, 1)),
        to:   toISO(new Date(year, month + 1, 0)),
      };
  }
}
