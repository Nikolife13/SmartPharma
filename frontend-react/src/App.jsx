import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Inventory from './pages/Inventory';
import Orders from './pages/Orders';
import Analytics from './pages/Analytics';
import SupplierOrders from './pages/SupplierOrders';
import SupplierApprovals from './pages/SupplierApprovals';

// Opposite of ProtectedRoute: keeps a logged-in user off /login and /register by
// bouncing them straight to their home page instead.
function GuestRoute({ children }) {
  const { isAuthenticated, isSupplier } = useAuth();
  if (!isAuthenticated) return children;
  return <Navigate to={isSupplier ? '/supplier/orders' : '/dashboard'} replace />;
}

// Route table for the whole app. Nested inside the outer ProtectedRoute+AppLayout
// route are all the logged-in pages; each one that needs a role restriction wraps
// itself in a second, more specific ProtectedRoute (see the managerOnly/supplierOnly/
// blockSupplier flags below).
export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route
              path="/login"
              element={
                <GuestRoute>
                  <Login />
                </GuestRoute>
              }
            />
            <Route
              path="/register"
              element={
                <GuestRoute>
                  <Register />
                </GuestRoute>
              }
            />

            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute blockSupplier>
                    <Dashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/inventory"
                element={
                  <ProtectedRoute blockSupplier>
                    <Inventory />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute managerOnly>
                    <Orders />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/analytics"
                element={
                  <ProtectedRoute managerOnly>
                    <Analytics />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/manager/suppliers"
                element={
                  <ProtectedRoute managerOnly>
                    <SupplierApprovals />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/supplier/orders"
                element={
                  <ProtectedRoute supplierOnly>
                    <SupplierOrders />
                  </ProtectedRoute>
                }
              />
            </Route>

            <Route path="*" element={<HomeRedirect />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}

// Catch-all for unmatched URLs and "/" - sends everyone to the right home page for
// their role instead of a 404.
function HomeRedirect() {
  const { isAuthenticated, isSupplier } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={isSupplier ? '/supplier/orders' : '/dashboard'} replace />;
}
