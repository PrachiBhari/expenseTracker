import { useState } from 'react';
import { Plus, Pencil, Trash2, Tag } from 'lucide-react';
import toast from 'react-hot-toast';
import { useCategories } from './hooks/useCategories';
import CategoryForm from './CategoryForm';
import type { CategoryFormData } from './categorySchema';
import type { Category } from '@/types/category.types';
import Button from '@/components/ui/Button';
import { TypeBadge } from '@/components/ui/Badge';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import EmptyState from '@/components/ui/EmptyState';
import { SkeletonCard } from '@/components/ui/Skeleton';

export default function CategoriesPage() {
  const { categories, isLoading, mutating, createCategory, updateCategory, deleteCategory } =
    useCategories();

  const [formOpen, setFormOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | undefined>();
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');
  const incomeCategories  = categories.filter((c) => c.type === 'INCOME');

  const handleOpenCreate = () => {
    setEditingCategory(undefined);
    setFormOpen(true);
  };

  const handleOpenEdit = (category: Category) => {
    setEditingCategory(category);
    setFormOpen(true);
  };

  const handleSave = async (data: CategoryFormData) => {
    try {
      if (editingCategory) {
        await updateCategory(editingCategory.id, {
          name:  data.name,
          color: data.color,
          icon:  data.icon,
        });
        toast.success('Category updated');
      } else {
        await createCategory(data);
        toast.success('Category created');
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message ?? 'Something went wrong');
      throw err; // re-throw so the form stays open
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;
    try {
      await deleteCategory(deletingId);
      toast.success('Category deleted');
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message ?? 'Cannot delete — category is in use');
    } finally {
      setDeletingId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
      </div>
    );
  }

  const CategoryGrid = ({ items, label }: { items: Category[]; label: string }) => (
    <section className="mb-8">
      <h2 className="text-sm font-semibold text-ink-muted uppercase tracking-wide mb-3">
        {label}
      </h2>
      {items.length === 0 ? (
        <p className="text-sm text-ink-muted py-4">No {label.toLowerCase()} yet.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {items.map((cat) => (
            <div
              key={cat.id}
              className="bg-white rounded-2xl shadow-card p-4 flex items-center gap-3 group"
            >
              {/* Colour swatch */}
              <div
                className="w-10 h-10 rounded-xl shrink-0"
                style={{ backgroundColor: cat.color }}
              />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-ink truncate">{cat.name}</p>
                <TypeBadge type={cat.type} />
              </div>
              {/* Actions — visible on hover */}
              <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => handleOpenEdit(cat)}
                  className="p-1.5 rounded-lg text-ink-muted hover:bg-gray-100 transition-colors"
                  aria-label="Edit"
                >
                  <Pencil size={14} />
                </button>
                <button
                  onClick={() => setDeletingId(cat.id)}
                  className="p-1.5 rounded-lg text-ink-muted hover:bg-red-50 hover:text-red-500 transition-colors"
                  aria-label="Delete"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );

  return (
    <>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-lg font-semibold text-ink">Categories</h2>
          <p className="text-sm text-ink-muted">{categories.length} total</p>
        </div>
        <Button onClick={handleOpenCreate}>
          <Plus size={16} />
          New Category
        </Button>
      </div>

      {categories.length === 0 ? (
        <EmptyState
          icon={<Tag size={48} />}
          title="No categories yet"
          description="Create your first category to start organizing transactions."
          action={
            <Button onClick={handleOpenCreate}>
              <Plus size={16} /> New Category
            </Button>
          }
        />
      ) : (
        <>
          <CategoryGrid items={expenseCategories} label="Expense Categories" />
          <CategoryGrid items={incomeCategories}  label="Income Categories" />
        </>
      )}

      <CategoryForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSave={handleSave}
        isSaving={mutating}
        editingCategory={editingCategory}
      />

      <ConfirmDialog
        isOpen={deletingId !== null}
        onClose={() => setDeletingId(null)}
        onConfirm={handleDelete}
        title="Delete Category"
        description="Are you sure? This cannot be undone. Deleting a category that is in use will be blocked."
        confirmLabel="Delete"
        isLoading={mutating}
      />
    </>
  );
}
