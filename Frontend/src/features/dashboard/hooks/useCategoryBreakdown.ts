import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type { CategoryBreakdown } from '@/types/analytics.types';
import type { CategoryType } from '@/types/category.types';

export function useCategoryBreakdown(from: string, to: string, type: CategoryType = 'EXPENSE') {
  const [breakdown, setBreakdown] = useState<CategoryBreakdown[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBreakdown = useCallback(async () => {
    if (!from || !to) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.get<CategoryBreakdown[]>('/analytics/by-category', {
        params: { from, to, type },
      });
      setBreakdown(res.data);
    } catch {
      setError('Failed to load category breakdown');
      toast.error('Failed to load category breakdown');
    } finally {
      setIsLoading(false);
    }
  }, [from, to, type]);

  useEffect(() => {
    fetchBreakdown();
  }, [fetchBreakdown]);

  return { breakdown, isLoading, error };
}
