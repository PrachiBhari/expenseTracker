import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { Category } from '@/types/category.types';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import Select from '@/components/ui/Select';
import Button from '@/components/ui/Button';

// z.number() (not coerce) keeps input/output types consistent with RHF.
// HTML string → number conversion is handled by valueAsNumber / setValueAs in register().
const transactionSchema = z.object({
  amount:        z.number().positive('Amount must be greater than 0'),
  categoryId:    z.number().positive('Select a category'),
  description:   z.string().optional(),
  date:          z.string().min(1, 'Date is required'),
  paymentMethod: z.string().optional(),
  source:        z.string().optional(),
});

export type TransactionFormData = z.infer<typeof transactionSchema>;

export interface TransactionInitialData {
  id:            number;
  amount:        number;
  categoryId:    number;
  description?:  string;
  date:          string;
  paymentMethod?: string;
  source?:       string;
}

interface TransactionFormProps {
  isOpen:       boolean;
  onClose:      () => void;
  type:         'EXPENSE' | 'INCOME';
  categories:   Category[];
  initialData?: TransactionInitialData;
  onSave:       (data: TransactionFormData) => Promise<void>;
  isSaving:     boolean;
}

const today = new Date().toISOString().split('T')[0];

export default function TransactionForm({
  isOpen,
  onClose,
  type,
  categories,
  initialData,
  onSave,
  isSaving,
}: TransactionFormProps) {
  const isEditMode = !!initialData;
  const label = type === 'EXPENSE' ? 'Expense' : 'Income';

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TransactionFormData>({
    resolver: zodResolver(transactionSchema),
    defaultValues: { date: today },
  });

  // Sync form whenever initialData changes (edit → create transitions)
  useEffect(() => {
    if (initialData) {
      reset({
        amount:        initialData.amount,
        categoryId:    initialData.categoryId,
        description:   initialData.description ?? '',
        date:          initialData.date,
        paymentMethod: initialData.paymentMethod ?? '',
        source:        initialData.source ?? '',
      });
    } else {
      reset({ date: today, description: '', paymentMethod: '', source: '' });
    }
  }, [initialData, reset]);

  const onSubmit = async (data: TransactionFormData) => {
    await onSave(data);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditMode ? `Edit ${label}` : `Add ${label}`}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* valueAsNumber tells RHF to parse the string to a number before Zod sees it */}
        <Input
          label="Amount"
          type="number"
          step="0.01"
          min="0.01"
          placeholder="0.00"
          error={errors.amount?.message}
          {...register('amount', { valueAsNumber: true })}
        />

        {/* setValueAs converts the selected option string "4" to number 4 */}
        <Select
          label="Category"
          error={errors.categoryId?.message}
          {...register('categoryId', { setValueAs: (v: string) => Number(v) })}
        >
          <option value="">Select category</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}
            </option>
          ))}
        </Select>

        <Input
          label="Date"
          type="date"
          error={errors.date?.message}
          {...register('date')}
        />

        <Input
          label="Description (optional)"
          placeholder="What was this for?"
          {...register('description')}
        />

        {type === 'EXPENSE' && (
          <Input
            label="Payment method (optional)"
            placeholder="UPI, Cash, Card…"
            {...register('paymentMethod')}
          />
        )}

        {type === 'INCOME' && (
          <Input
            label="Source (optional)"
            placeholder="Freelance, Salary…"
            {...register('source')}
          />
        )}

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSaving}>
            Cancel
          </Button>
          <Button type="submit" isLoading={isSaving}>
            {isEditMode ? 'Save changes' : `Add ${label}`}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
