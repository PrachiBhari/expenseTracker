import { useLocation } from 'react-router-dom';
import { Menu } from 'lucide-react';
import { useAuth } from '@/auth/useAuth';

const pageTitles: Record<string, string> = {
  '/':             'Dashboard',
  '/transactions': 'Transactions',
  '/categories':   'Categories',
  '/profile':      'Profile',
};

interface TopbarProps {
  onMenuClick: () => void;
}

export default function Topbar({ onMenuClick }: TopbarProps) {
  const { pathname } = useLocation();
  const { user } = useAuth();
  const title = pageTitles[pathname] ?? 'FinTrack';

  return (
    <header className="h-14 bg-white border-b border-gray-100 flex items-center px-4 gap-3 shrink-0">
      {/* Hamburger — visible only on mobile */}
      <button
        onClick={onMenuClick}
        className="p-2 rounded-xl text-ink-muted hover:bg-gray-100 transition-colors lg:hidden"
        aria-label="Open menu"
      >
        <Menu size={20} />
      </button>

      <h1 className="text-base font-semibold text-ink flex-1">{title}</h1>

      {/* User pill — right side */}
      {user && (
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-cream">
          <div className="w-6 h-6 rounded-full bg-accent-lavender flex items-center justify-center text-xs font-semibold text-white shrink-0">
            {user.fullName.charAt(0).toUpperCase()}
          </div>
          <span className="text-xs font-medium text-ink hidden sm:block">
            {user.fullName}
          </span>
        </div>
      )}
    </header>
  );
}
