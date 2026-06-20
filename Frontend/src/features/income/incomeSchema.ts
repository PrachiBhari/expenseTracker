import { z } from 'zod';

export const incomeSchema = z.object({
  amount:      z.number().positive('Amount must be greater than 0'),
  categoryId:  z.number().positive('Select a category'),
  source:      z.string().optional(),
  description: z.string().optional(),
  incomeDate:  z.string().min(1, 'Date is required'),
});

export type IncomeFormData = z.infer<typeof incomeSchema>;
