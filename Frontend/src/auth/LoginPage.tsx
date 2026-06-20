import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { useAuth } from './useAuth';
import { loginUser } from './authService';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

// Schema is the single source of truth for validation AND TypeScript types
const loginSchema = z.object({
  email:    z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      const response = await loginUser(data);
      login(response.token, response.user);
      toast.success(`Welcome back, ${response.user.fullName}!`);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message ?? 'Invalid email or password');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-cream flex items-center justify-center p-4">
      <div className="w-full max-w-sm">

        {/* Brand */}
        <div className="text-center mb-8">
          <span className="text-3xl font-bold text-ink">Fin</span>
          <span className="text-3xl font-bold text-accent-lavender">Track</span>
          <p className="text-sm text-ink-muted mt-1">Personal Finance Dashboard</p>
        </div>

        {/* Form card */}
        <div className="bg-white rounded-2xl shadow-card p-8">
          <h1 className="text-xl font-semibold text-ink mb-6">Sign in to your account</h1>

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
            <Input
              label="Email address"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" isLoading={isLoading} className="w-full !mt-6">
              Sign in
            </Button>
          </form>
        </div>

        <p className="mt-4 text-center text-sm text-ink-muted">
          Don&apos;t have an account?{' '}
          <Link to="/register" className="text-ink font-medium underline underline-offset-2">
            Create one
          </Link>
        </p>
      </div>
    </div>
  );
}
