export interface SummaryResponse {
  from: string;
  to: string;
  totalIncome: number;
  totalExpense: number;
  netBalance: number;
}

export interface CategoryBreakdown {
  categoryId: number;
  name: string;
  color: string;
  total: number;
}

export interface TrendPoint {
  month: string;
  totalIncome: number;
  totalExpense: number;
}
