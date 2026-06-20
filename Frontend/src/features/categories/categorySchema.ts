import { z } from 'zod';

export const categorySchema = z.object({
  name:  z.string().min(1, 'Name is required').max(80, 'Name must be 80 characters or less'),
  type:  z.enum(['INCOME', 'EXPENSE'] as const),
  color: z.string().min(1, 'Select a colour'),
  icon:  z.string().optional(),
});

export type CategoryFormData = z.infer<typeof categorySchema>;
