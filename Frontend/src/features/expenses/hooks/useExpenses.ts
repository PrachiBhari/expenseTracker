import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type { Expense, CreateExpenseRequest, UpdateExpenseRequest } from '@/types/expense.types';
import type { PageResponse } from '@/types/api.types';

export interface ExpenseFilters {
  from?: string;
  to?: string;
  categoryId?: number;
  q?: string;
  page?: number;
  size?: number;
}

export function useExpenses(filters: ExpenseFilters = {}) {
  const [data, setData] = useState<PageResponse<Expense> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mutating, setMutating] = useState(false);

  // Destructure to primitives so useCallback deps are stable
  const { from, to, categoryId, q, page = 0, size = 10 } = filters;

  const fetchExpenses = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size,
        sort: 'expenseDate,desc',
        ...(from       && { from }),
        ...(to         && { to }),
        ...(categoryId && { categoryId }),
        ...(q          && { q }),
      };
      const res = await api.get<PageResponse<Expense>>('/expenses', { params });
      setData(res.data);
    } catch {
      setError('Failed to load expenses');
      toast.error('Failed to load expenses');
    } finally {
      setIsLoading(false);
    }
  }, [from, to, categoryId, q, page, size]);

  useEffect(() => {
    fetchExpenses();
  }, [fetchExpenses]);

  const createExpense = async (body: CreateExpenseRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.post('/expenses', body);
      await fetchExpenses();
    } finally {
      setMutating(false);
    }
  };

  const updateExpense = async (id: number, body: UpdateExpenseRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.put(`/expenses/${id}`, body);
      await fetchExpenses();
    } finally {
      setMutating(false);
    }
  };

  const deleteExpense = async (id: number): Promise<void> => {
    setMutating(true);
    try {
      await api.delete(`/expenses/${id}`);
      await fetchExpenses();
    } finally {
      setMutating(false);
    }
  };

  return {
    expenses:      data?.content       ?? [],
    totalPages:    data?.totalPages    ?? 0,
    totalElements: data?.totalElements ?? 0,
    currentPage:   data?.number        ?? 0,
    isLoading,
    error,
    mutating,
    refetch: fetchExpenses,
    createExpense,
    updateExpense,
    deleteExpense,
  };
}
