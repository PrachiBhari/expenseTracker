export interface ExpenseCategory {
  id: number;
  name: string;
  color: string;
}

export interface Expense {
  id: number;
  amount: number;
  category: ExpenseCategory;
  description?: string;
  expenseDate: string;
  paymentMethod?: string;
  createdAt: string;
}

export interface CreateExpenseRequest {
  amount: number;
  categoryId: number;
  description?: string;
  expenseDate: string;
  paymentMethod?: string;
}

export interface UpdateExpenseRequest {
  amount: number;
  categoryId: number;
  description?: string;
  expenseDate: string;
  paymentMethod?: string;
}
