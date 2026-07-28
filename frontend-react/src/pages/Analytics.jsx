import { useEffect, useState } from 'react';
import { Line, Bar, Doughnut } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js';
import { analyticsService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend);

// Validated against the dataviz skill's palette checker: the app's brand teal/green
// pair fails as a categorical set (too close for colorblind and full-vision readers),
// so the reason breakdown uses this separately-validated blue/aqua/red triple instead.
const REASON_ORDER = ['SALE', 'RESTOCK', 'EXPIRED'];
const REASON_COLORS = { SALE: '#2a78d6', RESTOCK: '#1baf7a', EXPIRED: '#e34948' };
const REASON_LABELS = { SALE: 'Sale', RESTOCK: 'Restock', EXPIRED: 'Expired' };

const PRIMARY = '#00796B';

function ChartCard({ title, loading, children }) {
  return (
    <section className="rounded-card bg-surface p-6 shadow-card">
      <h2 className="mb-4 text-lg font-bold text-ink">{title}</h2>
      {loading ? <SkeletonRows rows={5} cols={1} /> : <div className="h-72">{children}</div>}
    </section>
  );
}

export default function Analytics() {
  const { showToast } = useToast();
  const [period, setPeriod] = useState('monthly');
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    analyticsService
      .getSummary(period)
      .then(({ data }) => {
        if (!cancelled) setSummary(data);
      })
      .catch(() => {
        if (!cancelled) showToast('Failed to load analytics.', 'error');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const trendData = summary && {
    labels: summary.salesTrend.map((p) => p.label),
    datasets: [
      {
        label: 'Units Sold',
        data: summary.salesTrend.map((p) => p.totalUnits),
        borderColor: PRIMARY,
        backgroundColor: PRIMARY,
        tension: 0.3,
        pointRadius: 3,
      },
    ],
  };

  const topProductsData = summary && {
    labels: summary.topProducts.map((p) => p.productName),
    datasets: [
      {
        label: 'Units Sold',
        data: summary.topProducts.map((p) => p.totalUnits),
        backgroundColor: PRIMARY,
        borderRadius: 4,
      },
    ],
  };

  const orderedReasons = summary
    ? REASON_ORDER.map((r) => summary.reasonBreakdown.find((x) => x.reason === r)).filter(Boolean)
    : [];
  const reasonTotal = orderedReasons.reduce((sum, r) => sum + r.count, 0);
  const reasonData = summary && {
    labels: orderedReasons.map(
      (r) => `${REASON_LABELS[r.reason]} (${reasonTotal ? Math.round((r.count / reasonTotal) * 100) : 0}%)`
    ),
    datasets: [
      {
        data: orderedReasons.map((r) => r.count),
        backgroundColor: orderedReasons.map((r) => REASON_COLORS[r.reason]),
        borderColor: '#FFFFFF',
        borderWidth: 2,
      },
    ],
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">Sales Analytics</h1>
          <p className="mt-1 text-sm text-muted">Trends from recorded stock movements.</p>
        </div>
        <div className="inline-flex rounded-control border border-border bg-surface p-1">
          {['weekly', 'monthly'].map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => setPeriod(option)}
              className={`rounded-control px-4 py-1.5 text-sm font-medium capitalize transition-colors ${
                period === option ? 'bg-primary text-white' : 'text-muted hover:text-ink'
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ChartCard title="Sales Trend" loading={loading}>
          {trendData && (
            <Line
              data={trendData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } },
              }}
            />
          )}
        </ChartCard>

        <ChartCard title="Top Products" loading={loading}>
          {topProductsData && (
            <Bar
              data={topProductsData}
              options={{
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { x: { beginAtZero: true } },
              }}
            />
          )}
        </ChartCard>

        <ChartCard title="Transaction Breakdown" loading={loading}>
          {reasonData && (
            <Doughnut
              data={reasonData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
              }}
            />
          )}
        </ChartCard>
      </div>
    </div>
  );
}
