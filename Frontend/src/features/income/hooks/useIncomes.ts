import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type { Income, CreateIncomeRequest, UpdateIncomeRequest } from '@/types/income.types';
import type { PageResponse } from '@/types/api.types';

export interface IncomeFilters {
  from?: string;
  to?: string;
  categoryId?: number;
  q?: string;
  page?: number;
  size?: number;
}

export function useIncomes(filters: IncomeFilters = {}) {
  const [data, setData] = useState<PageResponse<Income> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mutating, setMutating] = useState(false);

  const { from, to, categoryId, q, page = 0, size = 10 } = filters;

  const fetchIncomes = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size,
        sort: 'incomeDate,desc',
        ...(from       && { from }),
        ...(to         && { to }),
        ...(categoryId && { categoryId }),
        ...(q          && { q }),
      };
      const res = await api.get<PageResponse<Income>>('/incomes', { params });
      setData(res.data);
    } catch {
      setError('Failed to load income');
      toast.error('Failed to load income');
    } finally {
      setIsLoading(false);
    }
  }, [from, to, categoryId, q, page, size]);

  useEffect(() => {
    fetchIncomes();
  }, [fetchIncomes]);

  const createIncome = async (body: CreateIncomeRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.post('/incomes', body);
      await fetchIncomes();
    } finally {
      setMutating(false);
    }
  };

  const updateIncome = async (id: number, body: UpdateIncomeRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.put(`/incomes/${id}`, body);
      await fetchIncomes();
    } finally {
      setMutating(false);
    }
  };

  const deleteIncome = async (id: number): Promise<void> => {
    setMutating(true);
    try {
      await api.delete(`/incomes/${id}`);
      await fetchIncomes();
    } finally {
      setMutating(false);
    }
  };

  return {
    incomes:       data?.content       ?? [],
    totalPages:    data?.totalPages    ?? 0,
    totalElements: data?.totalElements ?? 0,
    currentPage:   data?.number        ?? 0,
    isLoading,
    error,
    mutating,
    refetch: fetchIncomes,
    createIncome,
    updateIncome,
    deleteIncome,
  };
}
