import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, managerOnly = false, supplierOnly = false }) {
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

  return children;
}
