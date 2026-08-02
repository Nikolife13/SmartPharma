import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Route guard used all over App.jsx. Plain auth check by default; the three flags
// layer on role restrictions (this is client-side UX only - the real enforcement
// is the @PreAuthorize checks on the backend, this just avoids showing a page the
// API would reject anyway).
export default function ProtectedRoute({
  children,
  managerOnly = false,
  supplierOnly = false,
  blockSupplier = false,
}) {
  const { isAuthenticated, isManager, isSupplier } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (managerOnly && !isManager) {
    return <Navigate to={isSupplier ? '/supplier/orders' : '/dashboard'} replace />;
  }

  if (supplierOnly && !isSupplier) {
    return <Navigate to="/dashboard" replace />;
  }

  if (blockSupplier && isSupplier) {
    return <Navigate to="/supplier/orders" replace />;
  }

  return children;
}
