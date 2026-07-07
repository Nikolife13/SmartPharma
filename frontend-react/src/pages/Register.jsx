import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import FormField, { inputClasses } from '../components/FormField';
import { IconEye, IconEyeOff } from '../components/icons';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const nextErrors = {};
    if (!username.trim()) nextErrors.username = 'Username is required.';
    if (!password) {
      nextErrors.password = 'Password is required.';
    } else if (password.length < 6) {
      nextErrors.password = 'Password must be at least 6 characters.';
    }
    if (confirmPassword !== password) {
      nextErrors.confirmPassword = 'Passwords do not match.';
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError('');
    if (!validate()) return;

    setSubmitting(true);
    try {
      await register(username, password);
      navigate('/dashboard');
    } catch (error) {
      setFormError(error.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-card bg-surface shadow-card md:grid-cols-2">
        <div className="hidden flex-col justify-center gap-4 bg-primary/5 p-10 md:flex">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary text-2xl font-extrabold text-white">
            +
          </div>
          <h2 className="text-2xl font-bold text-ink">Join SmartPharma today.</h2>
          <p className="text-sm text-muted">
            Create an account as a Pharmacist or Manager to start managing inventory and
            catching stock issues before they become problems.
          </p>
          <svg viewBox="0 0 200 160" className="mt-4 h-40 w-full text-primary/20">
            <circle cx="100" cy="80" r="60" fill="currentColor" />
            <circle cx="100" cy="65" r="18" className="fill-primary/50" />
            <rect x="65" y="95" width="70" height="35" rx="17" className="fill-primary/50" />
          </svg>
        </div>

        <div className="p-8 sm:p-10">
          <h1 className="text-2xl font-extrabold text-ink">Create Account</h1>
          <p className="mt-1 text-sm text-muted">Register to get started with SmartPharma.</p>

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
                placeholder="Choose a username"
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
                  placeholder="At least 6 characters"
                  autoComplete="new-password"
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

            <FormField label="Confirm Password" error={errors.confirmPassword}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                onBlur={validate}
                className={inputClasses(Boolean(errors.confirmPassword))}
                placeholder="Re-enter your password"
                autoComplete="new-password"
              />
            </FormField>

            <button
              type="submit"
              disabled={submitting}
              className="mt-2 w-full rounded-control bg-primary py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-dark disabled:opacity-60"
            >
              {submitting ? 'Creating account…' : 'Create Account'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-muted">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-primary hover:text-primary-dark">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
