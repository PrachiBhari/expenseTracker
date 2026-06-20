export interface IncomeCategory {
  id: number;
  name: string;
  color: string;
}

export interface Income {
  id: number;
  amount: number;
  category: IncomeCategory;
  source?: string;
  description?: string;
  incomeDate: string;
  createdAt: string;
}

export interface CreateIncomeRequest {
  amount: number;
  categoryId: number;
  source?: string;
  description?: string;
  incomeDate: string;
}

export interface UpdateIncomeRequest {
  amount: number;
  categoryId: number;
  source?: string;
  description?: string;
  incomeDate: string;
}
