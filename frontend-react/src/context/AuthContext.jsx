import { createContext, useContext, useMemo, useState } from 'react';
import { authService } from '../api/services';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [username, setUsername] = useState(localStorage.getItem('username'));
  const [role, setRole] = useState(localStorage.getItem('role'));

  const persistSession = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', data.username);
    localStorage.setItem('role', data.role);
    setToken(data.token);
    setUsername(data.username);
    setRole(data.role);
  };

  const login = async (usernameInput, password) => {
    const { data } = await authService.login(usernameInput, password);
    persistSession(data);
    return data;
  };

  const register = async (usernameInput, password) => {
    const { data } = await authService.register(usernameInput, password);
    persistSession(data);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setToken(null);
    setUsername(null);
    setRole(null);
  };

  const value = useMemo(
    () => ({
      token,
      username,
      role,
      isAuthenticated: Boolean(token),
      isManager: role === 'MANAGER',
      login,
      register,
      logout,
    }),
    [token, username, role]
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
