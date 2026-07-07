import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import FormField, { inputClasses } from '../components/FormField';
import { IconEye, IconEyeOff } from '../components/icons';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const nextErrors = {};
    if (!username.trim()) nextErrors.username = 'Username is required.';
    if (!password) nextErrors.password = 'Password is required.';
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError('');
    if (!validate()) return;

    setSubmitting(true);
    try {
      await login(username, password);
      navigate('/dashboard');
    } catch (error) {
      setFormError(error.response?.data?.message || 'Bad credentials. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-card bg-surface shadow-card md:grid-cols-2">
        {/* Illustration side */}
        <div className="hidden flex-col justify-center gap-4 bg-primary/5 p-10 md:flex">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary text-2xl font-extrabold text-white">
            +
          </div>
          <h2 className="text-2xl font-bold text-ink">Pharmacy inventory, simplified.</h2>
          <p className="text-sm text-muted">
            Track stock levels, catch expiring medicine early, and keep your pharmacy
            running smoothly with SmartPharma.
          </p>
          <svg viewBox="0 0 200 160" className="mt-4 h-40 w-full text-primary/20">
            <rect x="10" y="20" width="180" height="120" rx="12" fill="currentColor" />
            <rect x="30" y="45" width="60" height="10" rx="4" className="fill-primary/40" />
            <rect x="30" y="65" width="100" height="10" rx="4" className="fill-primary/40" />
            <rect x="30" y="85" width="80" height="10" rx="4" className="fill-primary/40" />
          </svg>
        </div>

        {/* Form side */}
        <div className="p-8 sm:p-10">
          <h1 className="text-2xl font-extrabold text-ink">Welcome Back</h1>
          <p className="mt-1 text-sm text-muted">Sign in to your SmartPharma account.</p>

          {formError && (
            <div className="mt-4 rounded-control border border-danger/30 bg-danger/5 px-3 py-2 text-sm text-danger">
              {formError}
            </div>
          )}

          <form className="mt-6" onSubmit={handleSubmit} noValidate>
            <FormField label="Username" error={errors.username}>
              <input
                type="text"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                onBlur={validate}
                className={inputClasses(Boolean(errors.username))}
                placeholder="e.g. jsmith"
                autoComplete="username"
              />
            </FormField>

            <FormField label="Password" error={errors.password}>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  onBlur={validate}
                  className={`${inputClasses(Boolean(errors.password))} pr-10`}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-ink"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <IconEyeOff className="h-5 w-5" /> : <IconEye className="h-5 w-5" />}
                </button>
              </div>
            </FormField>

            <div className="mb-5 text-right">
              <a href="#" className="text-sm font-medium text-primary hover:text-primary-dark">
                Forgot Password?
              </a>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-control bg-primary py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-dark disabled:opacity-60"
            >
              {submitting ? 'Signing in…' : 'Sign In'}
            </button>
          </form>

          <div className="my-6 flex items-center gap-3">
            <div className="h-px flex-1 bg-border" />
            <span className="text-xs text-muted">OR</span>
            <div className="h-px flex-1 bg-border" />
          </div>

          <div className="flex gap-3">
            <button
              type="button"
              className="flex-1 rounded-control border border-border py-2.5 text-sm font-medium text-ink hover:bg-background"
            >
              Google
            </button>
            <button
              type="button"
              className="flex-1 rounded-control border border-border py-2.5 text-sm font-medium text-ink hover:bg-background"
            >
              Apple
            </button>
          </div>

          <p className="mt-6 text-center text-sm text-muted">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="font-medium text-primary hover:text-primary-dark">
              Register
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
