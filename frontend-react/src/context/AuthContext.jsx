import { createContext, useContext, useMemo, useState } from 'react';
import { authService } from '../api/services';

// App-wide login state. Session is just a JWT + a few fields in localStorage (no
// server-side sessions) - persisted here so a page refresh doesn't log the user out.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [username, setUsername] = useState(localStorage.getItem('username'));
  const [role, setRole] = useState(localStorage.getItem('role'));
  const [supplierStatus, setSupplierStatus] = useState(localStorage.getItem('supplierStatus'));

  // Shared by both login and register - both return the same {token, username,
  // role, supplierStatus} shape, so this is the one place session storage happens.
  const persistSession = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role);
    if (data.supplierStatus) {
      localStorage.setItem('supplierStatus', data.supplierStatus);
    } else {
      localStorage.removeItem('supplierStatus');
    }
    setToken(data.token);
    setUsername(data.username);
    setRole(data.role);
    setSupplierStatus(data.supplierStatus || null);
  };

  const login = async (usernameInput, password) => {
    const { data } = await authService.login(usernameInput, password);
    persistSession(data);
    return data;
  };

  const register = async (usernameInput, password, roleInput, email) => {
    const { data } = await authService.register(usernameInput, password, roleInput, email);
    persistSession(data);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('supplierStatus');
    setToken(null);
    setUsername(null);
    setRole(null);
    setSupplierStatus(null);
  };

  const value = useMemo(
    () => ({
      token,
      username,
      role,
      supplierStatus,
      isAuthenticated: Boolean(token),
      isManager: role === 'MANAGER',
      isSupplier: role === 'SUPPLIER',
      // A Supplier who isn't ACTIVE yet is still "a supplier" for routing purposes
      // (see App.jsx), but isSupplierActive gates the actual order data/actions.
      isSupplierActive: role === 'SUPPLIER' && supplierStatus === 'ACTIVE',
      login,
      register,
      logout,
    }),
    [token, username, role, supplierStatus]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
