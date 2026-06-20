import type { ReactNode } from 'react';
import { LogOut, User, Mail, Shield } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Build initials from first letter of each word in the full name (max 2)
  const initials =
    user?.fullName
      .split(' ')
      .map((word) => word[0])
      .join('')
      .toUpperCase()
      .slice(0, 2) ?? '?';

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="max-w-lg space-y-4">

      {/* Avatar + name header */}
      <div className="bg-white rounded-2xl shadow-card p-6 flex items-center gap-5">
        <div className="w-16 h-16 rounded-2xl bg-accent-lavender flex items-center justify-center shrink-0">
          <span className="text-2xl font-bold text-ink">{initials}</span>
        </div>
        <div className="min-w-0">
          <p className="text-lg font-bold text-ink truncate">{user?.fullName}</p>
          <p className="text-sm text-ink-muted truncate">{user?.email}</p>
        </div>
      </div>

      {/* Account details */}
      <div className="bg-white rounded-2xl shadow-card divide-y divide-gray-100">
        <InfoRow icon={<User size={16} />}   label="Full name"    value={user?.fullName ?? '—'} />
        <InfoRow icon={<Mail size={16} />}   label="Email"        value={user?.email ?? '—'} />
        <InfoRow icon={<Shield size={16} />} label="Account ID"   value={`#${user?.id ?? '—'}`} />
      </div>

      {/* Sign-out */}
      <div className="bg-white rounded-2xl shadow-card p-4">
        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl
            text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 transition-colors"
        >
          <LogOut size={16} />
          Sign out
        </button>
      </div>

    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3 px-5 py-4">
      <span className="text-ink-muted shrink-0">{icon}</span>
      <span className="text-xs text-ink-muted w-28 shrink-0">{label}</span>
      <span className="text-sm font-medium text-ink">{value}</span>
    </div>
  );
}
