import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type { SummaryResponse } from '@/types/analytics.types';

export function useSummary(from: string, to: string) {
  const [summary, setSummary] = useState<SummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSummary = useCallback(async () => {
    // Don't fetch until both dates are set (important for custom period)
    if (!from || !to) {
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      const res = await api.get<SummaryResponse>('/analytics/summary', {
        params: { from, to },
      });
      setSummary(res.data);
    } catch {
      setError('Failed to load summary');
      toast.error('Failed to load summary');
    } finally {
      setIsLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  return { summary, isLoading, error };
}
