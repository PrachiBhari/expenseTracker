import { useState } from 'react';
import { Plus, ChevronLeft, ChevronRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { useExpenses } from '@/features/expenses/hooks/useExpenses';
import { useIncomes }  from '@/features/income/hooks/useIncomes';
import { useCategories } from '@/features/categories/hooks/useCategories';
import TransactionTable,  { type TransactionRow }         from './TransactionTable';
import TransactionForm,   { type TransactionFormData, type TransactionInitialData } from './TransactionForm';
import TransactionFilters, { type FilterState }           from './TransactionFilters';
import type { Expense } from '@/types/expense.types';
import type { Income }  from '@/types/income.types';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import Button from '@/components/ui/Button';

type Tab = 'EXPENSE' | 'INCOME';

const EMPTY_FILTERS: FilterState = { from: '', to: '', categoryId: '', q: '' };

// Normalise an Expense into the generic row shape the table needs
function toExpenseRow(e: Expense): TransactionRow {
  return {
    id:            e.id,
    type:          'EXPENSE',
    date:          e.expenseDate,
    categoryName:  e.category.name,
    categoryColor: e.category.color,
    description:   e.description,
    amount:        e.amount,
    extra:         e.paymentMethod,
  };
}

// Normalise an Income into the same row shape
function toIncomeRow(i: Income): TransactionRow {
  return {
    id:            i.id,
    type:          'INCOME',
    date:          i.incomeDate,
    categoryName:  i.category.name,
    categoryColor: i.category.color,
    description:   i.description,
    amount:        i.amount,
    extra:         i.source,
  };
}

export default function TransactionsPage() {
  const [activeTab,    setActiveTab]    = useState<Tab>('EXPENSE');
  const [filters,      setFilters]      = useState<FilterState>(EMPTY_FILTERS);
  const [formOpen,     setFormOpen]     = useState(false);
  const [editingData,  setEditingData]  = useState<TransactionInitialData | undefined>();
  const [deletingId,   setDeletingId]   = useState<number | null>(null);

  // Build API filters from the UI filter state
  const apiFilters = {
    from:       filters.from       || undefined,
    to:         filters.to         || undefined,
    categoryId: filters.categoryId !== '' ? (filters.categoryId as number) : undefined,
    q:          filters.q          || undefined,
  };

  const expenseHook = useExpenses(activeTab === 'EXPENSE' ? apiFilters : {});
  const incomeHook  = useIncomes (activeTab === 'INCOME'  ? apiFilters : {});
  const { categories } = useCategories();

  // The active hook and its typed source data
  const activeExpenses = expenseHook.expenses;
  const activeIncomes  = incomeHook.incomes;

  const rows: TransactionRow[] =
    activeTab === 'EXPENSE'
      ? activeExpenses.map(toExpenseRow)
      : activeIncomes.map(toIncomeRow);

  const isLoading   = activeTab === 'EXPENSE' ? expenseHook.isLoading  : incomeHook.isLoading;
  const isMutating  = activeTab === 'EXPENSE' ? expenseHook.mutating   : incomeHook.mutating;
  const totalPages  = activeTab === 'EXPENSE' ? expenseHook.totalPages  : incomeHook.totalPages;
  const currentPage = activeTab === 'EXPENSE' ? expenseHook.currentPage : incomeHook.currentPage;

  const visibleCategories = categories.filter((c) => c.type === activeTab);

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setFilters(EMPTY_FILTERS); // reset filters when switching tabs
  };

  const handleOpenCreate = () => {
    setEditingData(undefined);
    setFormOpen(true);
  };

  const handleOpenEdit = (id: number) => {
    if (activeTab === 'EXPENSE') {
      const found = activeExpenses.find((e) => e.id === id);
      if (!found) return;
      setEditingData({
        id:            found.id,
        amount:        found.amount,
        categoryId:    found.category.id,
        description:   found.description,
        date:          found.expenseDate,
        paymentMethod: found.paymentMethod,
      });
    } else {
      const found = activeIncomes.find((i) => i.id === id);
      if (!found) return;
      setEditingData({
        id:          found.id,
        amount:      found.amount,
        categoryId:  found.category.id,
        description: found.description,
        date:        found.incomeDate,
        source:      found.source,
      });
    }
    setFormOpen(true);
  };

  const handleSave = async (data: TransactionFormData) => {
    try {
      if (activeTab === 'EXPENSE') {
        const body = {
          amount:        data.amount,
          categoryId:    data.categoryId,
          description:   data.description,
          expenseDate:   data.date,
          paymentMethod: data.paymentMethod,
        };
        if (editingData) {
          await expenseHook.updateExpense(editingData.id, body);
          toast.success('Expense updated');
        } else {
          await expenseHook.createExpense(body);
          toast.success('Expense added');
        }
      } else {
        const body = {
          amount:      data.amount,
          categoryId:  data.categoryId,
          source:      data.source,
          description: data.description,
          incomeDate:  data.date,
        };
        if (editingData) {
          await incomeHook.updateIncome(editingData.id, body);
          toast.success('Income updated');
        } else {
          await incomeHook.createIncome(body);
          toast.success('Income added');
        }
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message ?? 'Something went wrong');
      throw err; // keep modal open on error
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;
    try {
      if (activeTab === 'EXPENSE') {
        await expenseHook.deleteExpense(deletingId);
        toast.success('Expense deleted');
      } else {
        await incomeHook.deleteIncome(deletingId);
        toast.success('Income deleted');
      }
    } catch {
      toast.error('Failed to delete');
    } finally {
      setDeletingId(null);
    }
  };

  const handlePageChange = (newPage: number) => {
    setFilters((prev) => ({ ...prev, page: newPage } as FilterState));
    if (activeTab === 'EXPENSE') {
      expenseHook.refetch();
    } else {
      incomeHook.refetch();
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <>
      {/* Header row */}
      <div className="flex items-center justify-between mb-4">
        {/* Tabs */}
        <div className="flex gap-1 bg-white rounded-xl shadow-card p-1">
          {(['EXPENSE', 'INCOME'] as Tab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => handleTabChange(tab)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                activeTab === tab
                  ? 'bg-ink text-white'
                  : 'text-ink-muted hover:text-ink hover:bg-gray-100'
              }`}
            >
              {tab === 'EXPENSE' ? 'Expenses' : 'Income'}
            </button>
          ))}
        </div>

        <Button onClick={handleOpenCreate}>
          <Plus size={16} />
          Add {activeTab === 'EXPENSE' ? 'Expense' : 'Income'}
        </Button>
      </div>

      {/* Filters */}
      <TransactionFilters
        filters={filters}
        categories={visibleCategories}
        onChange={setFilters}
        onReset={() => setFilters(EMPTY_FILTERS)}
      />

      {/* Table */}
      <TransactionTable
        rows={rows}
        isLoading={isLoading}
        onEdit={handleOpenEdit}
        onDelete={(id) => setDeletingId(id)}
      />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-4">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="p-2 rounded-xl border border-gray-200 hover:bg-white disabled:opacity-40 transition-colors"
          >
            <ChevronLeft size={16} />
          </button>
          <span className="text-sm text-ink-muted">
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="p-2 rounded-xl border border-gray-200 hover:bg-white disabled:opacity-40 transition-colors"
          >
            <ChevronRight size={16} />
          </button>
        </div>
      )}

      {/* Add / Edit modal */}
      <TransactionForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        type={activeTab}
        categories={visibleCategories}
        initialData={editingData}
        onSave={handleSave}
        isSaving={isMutating}
      />

      {/* Delete confirmation */}
      <ConfirmDialog
        isOpen={deletingId !== null}
        onClose={() => setDeletingId(null)}
        onConfirm={handleDelete}
        title={`Delete ${activeTab === 'EXPENSE' ? 'Expense' : 'Income'}`}
        description="Are you sure? This action cannot be undone."
        isLoading={isMutating}
      />
    </>
  );
}
