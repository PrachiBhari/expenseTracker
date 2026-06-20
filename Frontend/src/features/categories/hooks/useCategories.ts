import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import type {
  Category,
  CategoryType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@/types/category.types';

export function useCategories(typeFilter?: CategoryType) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mutating, setMutating] = useState(false);

  const fetchCategories = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = typeFilter ? { type: typeFilter } : {};
      const res = await api.get<Category[]>('/categories', { params });
      setCategories(res.data);
    } catch {
      setError('Failed to load categories');
      toast.error('Failed to load categories');
    } finally {
      setIsLoading(false);
    }
  }, [typeFilter]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const createCategory = async (data: CreateCategoryRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.post('/categories', data);
      await fetchCategories();
    } finally {
      setMutating(false);
    }
  };

  const updateCategory = async (id: number, data: UpdateCategoryRequest): Promise<void> => {
    setMutating(true);
    try {
      await api.put(`/categories/${id}`, data);
      await fetchCategories();
    } finally {
      setMutating(false);
    }
  };

  const deleteCategory = async (id: number): Promise<void> => {
    setMutating(true);
    try {
      await api.delete(`/categories/${id}`);
      await fetchCategories();
    } finally {
      setMutating(false);
    }
  };

  return {
    categories,
    isLoading,
    error,
    mutating,
    refetch: fetchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
  };
}
