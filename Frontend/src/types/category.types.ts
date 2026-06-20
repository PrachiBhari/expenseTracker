export type CategoryType = 'INCOME' | 'EXPENSE';

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
  color: string;
  icon?: string;
}

export interface CreateCategoryRequest {
  name: string;
  type: CategoryType;
  color: string;
  icon?: string;
}

export interface UpdateCategoryRequest {
  name: string;
  color: string;
  icon?: string;
}
