import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { IconDashboard, IconInventory, IconOrders, IconAnalytics, IconBell } from './icons';

const navLinkClasses = ({ isActive }) =>
  `flex items-center gap-2 rounded-control px-3 py-2 text-sm font-medium transition-colors ${
    isActive
      ? 'bg-primary/10 text-primary'
      : 'text-muted hover:bg-background hover:text-ink'
  }`;

export default function Navbar() {
  const { username, role, isManager, logout } = useAuth();

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-surface shadow-soft">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-8">
          <span className="text-xl font-extrabold tracking-tight text-primary">
            SmartPharma
          </span>
          <nav className="hidden items-center gap-1 md:flex">
            <NavLink to="/dashboard" className={navLinkClasses}>
              <IconDashboard className="h-4 w-4" />
              Dashboard
            </NavLink>
            <NavLink to="/inventory" className={navLinkClasses}>
              <IconInventory className="h-4 w-4" />
              Inventory
            </NavLink>
            {isManager && (
              <NavLink to="/orders" className={navLinkClasses}>
                <IconOrders className="h-4 w-4" />
                Orders
              </NavLink>
            )}
            {isManager && (
              <NavLink to="/analytics" className={navLinkClasses}>
                <IconAnalytics className="h-4 w-4" />
                Analytics
              </NavLink>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-4">
          <button
            type="button"
            className="relative rounded-full p-2 text-muted hover:bg-background hover:text-ink"
            aria-label="Notifications"
          >
            <IconBell className="h-5 w-5" />
          </button>
          <div className="hidden text-right sm:block">
            <p className="text-sm font-semibold text-ink">{username}</p>
            <p className="text-xs text-muted">{role}</p>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
            {username?.charAt(0).toUpperCase()}
          </div>
          <button
            type="button"
            onClick={logout}
            className="rounded-control border border-border px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-background"
          >
            Logout
          </button>
        </div>
      </div>

      {/* Mobile nav row */}
      <nav className="flex items-center gap-1 overflow-x-auto border-t border-border px-4 py-2 md:hidden">
        <NavLink to="/dashboard" className={navLinkClasses}>
          <IconDashboard className="h-4 w-4" />
          Dashboard
        </NavLink>
        <NavLink to="/inventory" className={navLinkClasses}>
          <IconInventory className="h-4 w-4" />
          Inventory
        </NavLink>
        {isManager && (
          <NavLink to="/orders" className={navLinkClasses}>
            <IconOrders className="h-4 w-4" />
            Orders
          </NavLink>
        )}
        {isManager && (
          <NavLink to="/analytics" className={navLinkClasses}>
            <IconAnalytics className="h-4 w-4" />
            Analytics
          </NavLink>
        )}
      </nav>
    </header>
  );
}
