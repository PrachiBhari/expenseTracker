import { z } from 'zod';

// z.number() keeps TypeScript types clean — RHF converts strings to numbers via
// valueAsNumber / setValueAs in the register() call, before Zod sees the values.
export const expenseSchema = z.object({
  amount:        z.number().positive('Amount must be greater than 0'),
  categoryId:    z.number().positive('Select a category'),
  description:   z.string().optional(),
  expenseDate:   z.string().min(1, 'Date is required'),
  paymentMethod: z.string().optional(),
});

export type ExpenseFormData = z.infer<typeof expenseSchema>;
