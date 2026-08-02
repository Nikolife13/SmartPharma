import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

// Shared shell (top nav + content area) wrapped around every logged-in route -
// see App.jsx, where this sits inside the outer ProtectedRoute and <Outlet/>
// renders whichever page matched.
export default function AppLayout() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="animate-fadeIn">
        <Outlet />
      </main>
    </div>
  );
}
