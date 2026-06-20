import { createContext, useState, useEffect, type ReactNode } from 'react';
import api from '@/lib/api';
import type { User, AuthContextType } from '@/types/auth.types';

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user,      setUser]      = useState<User | null>(null);
  const [token,     setToken]     = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Runs ONCE on mount only (empty deps).
  // Purpose: verify a token that survived a page refresh.
  // When login() is called explicitly we already have the user — no re-verify needed.
  // The old [token] dep caused the effect to re-fire after login(), which made a
  // redundant /users/me call that could fail and clear the token, bouncing the user
  // back to /login even though login had just succeeded.
  useEffect(() => {
    const stored = localStorage.getItem('token');
    if (!stored) {
      setIsLoading(false);
      return;
    }

    api
      .get<User>('/users/me')
      .then((res) => {
        setToken(stored);
        setUser(res.data);
      })
      .catch(() => {
        localStorage.removeItem('token');
      })
      .finally(() => setIsLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const login = (newToken: string, userData: User) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!token && !!user,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
