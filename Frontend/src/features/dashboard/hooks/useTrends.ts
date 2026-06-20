import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type { TrendPoint } from '@/types/analytics.types';

export function useTrends(months = 6) {
  const [trends, setTrends] = useState<TrendPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTrends = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.get<TrendPoint[]>('/analytics/trends', {
        params: { months },
      });
      setTrends(res.data);
    } catch {
      setError('Failed to load trends');
      toast.error('Failed to load trends');
    } finally {
      setIsLoading(false);
    }
  }, [months]);

  useEffect(() => {
    fetchTrends();
  }, [fetchTrends]);

  return { trends, isLoading, error };
}
