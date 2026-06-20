import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { categorySchema, type CategoryFormData } from './categorySchema';
import type { Category } from '@/types/category.types';
import Modal from '@/components/ui/Modal';
import Input from '@/components/ui/Input';
import Select from '@/components/ui/Select';
import Button from '@/components/ui/Button';

// Predefined pastel palette that matches the design system
const PRESET_COLORS = [
  '#F8C8DC', '#FFF1A8', '#C8B6E2', '#B8E0D2',
  '#FFD6A5', '#A8D8EA', '#95E1D3', '#FCE38A',
  '#D4A5A5', '#C3E8C3', '#B5CCFF', '#FDDCB5',
];

interface CategoryFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: CategoryFormData) => Promise<void>;
  isSaving: boolean;
  editingCategory?: Category;
}

export default function CategoryForm({
  isOpen,
  onClose,
  onSave,
  isSaving,
  editingCategory,
}: CategoryFormProps) {
  const isEditMode = !!editingCategory;

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    defaultValues: { color: PRESET_COLORS[0] },
  });

  // Pre-fill the form when editing; clear it when creating
  useEffect(() => {
    if (editingCategory) {
      reset({
        name:  editingCategory.name,
        type:  editingCategory.type,
        color: editingCategory.color,
        icon:  editingCategory.icon ?? '',
      });
    } else {
      reset({ name: '', type: 'EXPENSE', color: PRESET_COLORS[0], icon: '' });
    }
  }, [editingCategory, reset]);

  const selectedColor = watch('color');

  const onSubmit = async (data: CategoryFormData) => {
    await onSave(data);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditMode ? 'Edit Category' : 'New Category'}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Category name"
          placeholder="e.g. Food, Rent, Salary"
          error={errors.name?.message}
          {...register('name')}
        />

        {/* Type is locked in edit mode so existing transactions aren't orphaned */}
        <Select
          label="Type"
          disabled={isEditMode}
          error={errors.type?.message}
          {...register('type')}
        >
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </Select>

        {/* Colour swatches */}
        <div className="flex flex-col gap-1">
          <p className="text-sm font-medium text-ink">Colour</p>
          <div className="grid grid-cols-6 gap-2">
            {PRESET_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => setValue('color', color, { shouldValidate: true })}
                className={`w-8 h-8 rounded-full transition-transform hover:scale-110 ${
                  selectedColor === color
                    ? 'ring-2 ring-offset-2 ring-ink scale-110'
                    : ''
                }`}
                style={{ backgroundColor: color }}
                aria-label={`Select colour ${color}`}
              />
            ))}
          </div>
          {errors.color && (
            <p className="text-xs text-red-500">{errors.color.message}</p>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="ghost" type="button" onClick={onClose} disabled={isSaving}>
            Cancel
          </Button>
          <Button type="submit" isLoading={isSaving}>
            {isEditMode ? 'Save changes' : 'Create category'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
