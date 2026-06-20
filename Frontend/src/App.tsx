import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { ErrorBoundary } from '@/components/ui/ErrorBoundary';
import PageLoader from '@/components/ui/PageLoader';

// Eagerly loaded — needed on the very first paint before auth resolves
import LoginPage from '@/auth/LoginPage';
import RegisterPage from '@/auth/RegisterPage';
import DashboardLayout from '@/components/layout/DashboardLayout';

// Lazy-loaded — split into separate chunks, downloaded only when first visited.
// DashboardPage pulls in Recharts (~350 KB); lazy() keeps it out of the initial bundle.
const DashboardPage    = lazy(() => import('@/features/dashboard/DashboardPage'));
const TransactionsPage = lazy(() => import('@/features/transactions/TransactionsPage'));
const CategoriesPage   = lazy(() => import('@/features/categories/CategoriesPage'));
const ProfilePage      = lazy(() => import('@/features/profile/ProfilePage'));

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>

        {/* Catches any unhandled render error inside the app shell */}
        <ErrorBoundary>
          {/* Shows PageLoader while a lazy chunk is downloading */}
          <Suspense fallback={<PageLoader />}>
            <Routes>
              {/* Public routes — accessible without a token */}
              <Route path="/login"    element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Protected shell — ProtectedRoute guards token validity.
                  DashboardLayout renders <Outlet /> so nested routes appear
                  inside the sidebar + topbar. */}
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <DashboardLayout />
                  </ProtectedRoute>
                }
              >
                <Route index             element={<DashboardPage />} />
                <Route path="transactions" element={<TransactionsPage />} />
                <Route path="categories"   element={<CategoriesPage />} />
                <Route path="profile"      element={<ProfilePage />} />
              </Route>

              {/* Unknown URLs fall back to dashboard */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </Suspense>
        </ErrorBoundary>

        {/* Toaster lives outside Suspense so toasts appear even during lazy-load */}
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              fontFamily: 'Inter, system-ui, sans-serif',
              fontSize: '14px',
              color: '#2E2A33',
            },
          }}
        />

      </AuthProvider>
    </BrowserRouter>
  );
}
