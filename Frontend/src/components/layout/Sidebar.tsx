import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Tag,
  User,
  LogOut,
  X,
} from 'lucide-react';
import { useAuth } from '@/auth/useAuth';

const navItems = [
  { label: 'Dashboard',    path: '/',             icon: LayoutDashboard },
  { label: 'Transactions', path: '/transactions', icon: ArrowLeftRight  },
  { label: 'Categories',   path: '/categories',   icon: Tag             },
];

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { logout, user } = useAuth();

  return (
    <>
      {/* Mobile backdrop — click outside to close */}
      {isOpen && (
        <div
          className="fixed inset-0 z-20 bg-ink/20 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`
          fixed inset-y-0 left-0 z-30 w-60 bg-white border-r border-gray-100 flex flex-col
          transition-transform duration-200 ease-in-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}
          lg:relative lg:translate-x-0 lg:z-auto
        `}
      >
        {/* Logo row */}
        <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between shrink-0">
          <div>
            <span className="text-xl font-bold text-ink">Fin</span>
            <span className="text-xl font-bold text-accent-lavender">Track</span>
          </div>
          {/* X button shown only on mobile */}
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-ink-muted hover:bg-gray-100 transition-colors lg:hidden"
            aria-label="Close sidebar"
          >
            <X size={18} />
          </button>
        </div>

        {/* Nav links */}
        <nav className="flex-1 px-3 py-4 space-y-0.5">
          {navItems.map(({ label, path, icon: Icon }) => (
            <NavLink
              key={path}
              to={path}
              end={path === '/'}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-accent-lavender/20 text-ink'
                    : 'text-ink-muted hover:bg-gray-50 hover:text-ink'
                }`
              }
            >
              <Icon size={17} strokeWidth={1.8} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Bottom — profile + logout */}
        <div className="px-3 py-4 border-t border-gray-100 space-y-0.5 shrink-0">
          <NavLink
            to="/profile"
            onClick={onClose}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-accent-lavender/20 text-ink'
                  : 'text-ink-muted hover:bg-gray-50 hover:text-ink'
              }`
            }
          >
            <User size={17} strokeWidth={1.8} />
            <span className="truncate">{user?.fullName ?? 'Profile'}</span>
          </NavLink>

          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium
              text-ink-muted hover:bg-gray-50 hover:text-ink transition-colors"
          >
            <LogOut size={17} strokeWidth={1.8} />
            Logout
          </button>
        </div>
      </aside>
    </>
  );
}
