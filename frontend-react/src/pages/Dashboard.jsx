import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardService } from '../api/services';
import { useAuth } from '../context/AuthContext';
import { SkeletonRows } from '../components/Skeleton';
import { formatDate } from '../utils/dates';
import dashboardBanner from '../assets/dashboard-banner.png';

const REFRESH_INTERVAL_MS = 30000;

export default function Dashboard() {
  const { username } = useAuth();
  const [lowStock, setLowStock] = useState([]);
  const [nearExpiry, setNearExpiry] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDashboard = useCallback(async () => {
    try {
      const { data } = await dashboardService.getSummary();
      setLowStock(data.lowStock || []);
      setNearExpiry(data.nearExpiry || []);
      setError('');
    } catch {
      setError('Unable to load dashboard data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboard();
    const interval = setInterval(fetchDashboard, REFRESH_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [fetchDashboard]);

  const today = new Date().toLocaleDateString('en-IE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  const hasIssues = lowStock.length > 0 || nearExpiry.length > 0;

  return (
    <div>
      <img
        src={dashboardBanner}
        alt="SmartPharma automated workflow: PMR to stock automation to PCRS claims"
        className="h-auto max-h-80 w-full object-cover"
      />

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-8 flex flex-col justify-between gap-2 sm:flex-row sm:items-end">
          <div>
            <h1 className="text-2xl font-extrabold text-ink">Hello, {username}</h1>
            <p className="text-sm text-muted">{today}</p>
          </div>
          <Link
            to="/inventory"
            className="inline-flex w-fit items-center rounded-control border border-border px-4 py-2 text-sm font-medium text-ink hover:bg-surface"
          >
            View Full Inventory
          </Link>
        </div>

        {error && (
          <div className="mb-6 rounded-control border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">
            {error}
          </div>
        )}

        {!loading && !hasIssues && !error && (
          <div className="mb-8 rounded-card border border-success/20 bg-success/5 px-6 py-4 text-sm font-medium text-success">
            All stock levels are healthy.
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {/* Low Stock Card */}
          <section className="rounded-card bg-surface p-6 shadow-card">
            <h2 className="mb-4 text-lg font-bold text-ink">Low Stock</h2>
            {loading ? (
              <SkeletonRows rows={4} cols={3} />
            ) : lowStock.length === 0 ? (
              <p className="text-sm text-muted">No low-stock items right now.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-border text-muted">
                      <th className="pb-2 font-medium">Product</th>
                      <th className="pb-2 font-medium">Quantity</th>
                      <th className="pb-2 font-medium">Threshold</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lowStock.map((product) => {
                      const critical = product.currentQuantity <= product.minThreshold;
                      return (
                        <tr
                          key={product.id}
                          className={`border-b border-border last:border-0 ${
                            critical ? 'bg-danger/5' : ''
                          }`}
                        >
                          <td className="py-2.5 font-medium text-ink">{product.name}</td>
                          <td className={`py-2.5 ${critical ? 'font-semibold text-danger' : 'text-ink'}`}>
                            {product.currentQuantity}
                          </td>
                          <td className="py-2.5 text-muted">{product.minThreshold}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          {/* Near Expiry Card */}
          <section className="rounded-card bg-surface p-6 shadow-card">
            <h2 className="mb-4 text-lg font-bold text-ink">Near Expiry</h2>
            {loading ? (
              <SkeletonRows rows={4} cols={3} />
            ) : nearExpiry.length === 0 ? (
              <p className="text-sm text-muted">No products nearing expiry.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-border text-muted">
                      <th className="pb-2 font-medium">Product</th>
                      <th className="pb-2 font-medium">Expiry Date</th>
                      <th className="pb-2 font-medium">Quantity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {nearExpiry.map((product) => (
                      <tr key={product.id} className="border-b border-border bg-accent/10 last:border-0">
                        <td className="py-2.5 font-medium text-ink">{product.name}</td>
                        <td className="py-2.5 font-semibold text-accent">
                          {formatDate(product.expiryDate)}
                        </td>
                        <td className="py-2.5 text-ink">{product.currentQuantity}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
