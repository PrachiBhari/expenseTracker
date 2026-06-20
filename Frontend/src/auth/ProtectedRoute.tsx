import { Navigate } from 'react-router-dom';
import { useAuth } from './useAuth';
import type { ReactNode } from 'react';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();

  // Wait until we know if the stored token is valid before making routing decisions
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen bg-cream">
        <div className="w-8 h-8 border-4 border-accent-lavender border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
