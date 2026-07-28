import { useState } from 'react';
import { ordersService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';
import { IconRobot } from '../components/icons';

function confidenceBadgeClass(score) {
  if (score >= 85) return 'bg-success/10 text-success';
  if (score >= 65) return 'bg-accent/20 text-ink';
  return 'bg-primary/10 text-primary';
}

export default function Orders() {
  const { showToast } = useToast();
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [generated, setGenerated] = useState(false);
  const [decisions, setDecisions] = useState({}); // productId -> 'approved' | 'dismissed'

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const { data } = await ordersService.getSuggestions();
      setSuggestions(data || []);
      setDecisions({});
      setGenerated(true);
    } catch {
      showToast('Failed to generate suggested orders.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const visibleSuggestions = suggestions.filter((s) => decisions[s.productId] !== 'dismissed');

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-extrabold text-ink">Predictive Orders</h1>
          <p className="mt-1 text-sm text-muted">
            AI-forecasted 30-day demand and suggested purchase quantities.
          </p>
        </div>
        <button
          type="button"
          onClick={handleGenerate}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-control bg-accent px-4 py-2.5 text-sm font-semibold text-ink transition-colors hover:brightness-95 disabled:opacity-50"
        >
          {loading ? 'Generating…' : 'Generate Suggested Order'}
        </button>
      </div>

      {loading && (
        <div className="rounded-card bg-surface p-6 shadow-card">
          <SkeletonRows rows={5} cols={7} />
        </div>
      )}

      {!loading && !generated && (
        <div className="flex flex-col items-center rounded-card bg-surface px-4 py-16 text-center shadow-card">
          <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-primary/10 text-primary">
            <IconRobot className="h-12 w-12" />
          </div>
          <h2 className="text-lg font-bold text-ink">No suggestions yet</h2>
          <p className="mt-2 max-w-md text-sm text-muted">
            Click "Generate Suggested Order" to forecast demand from sales history and
            national prescribing trends.
          </p>
        </div>
      )}

      {!loading && generated && visibleSuggestions.length === 0 && (
        <div className="rounded-card bg-surface px-4 py-16 text-center text-sm text-muted shadow-card">
          No suggestions to show.
        </div>
      )}

      {!loading && generated && visibleSuggestions.length > 0 && (
        <div className="overflow-hidden rounded-card bg-surface shadow-card">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border bg-background/60 text-muted">
                  <th className="px-4 py-3 font-medium">Product Name</th>
                  <th className="px-4 py-3 font-medium">Batch</th>
                  <th className="px-4 py-3 font-medium">Current Qty</th>
                  <th className="px-4 py-3 font-medium">Min Threshold</th>
                  <th className="px-4 py-3 font-medium">Forecasted Demand (30d)</th>
                  <th className="px-4 py-3 font-medium">Suggested Order Qty</th>
                  <th className="px-4 py-3 font-medium">Confidence</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleSuggestions.map((s, index) => {
                  const decision = decisions[s.productId];
                  return (
                    <tr
                      key={s.productId}
                      className={`border-b border-border last:border-0 ${
                        index % 2 === 1 ? 'bg-background/40' : ''
                      }`}
                    >
                      <td className="px-4 py-3 font-medium text-ink">{s.productName}</td>
                      <td className="px-4 py-3 text-muted">{s.batchNumber}</td>
                      <td className="px-4 py-3 text-ink">{s.currentQuantity}</td>
                      <td className="px-4 py-3 text-muted">{s.minThreshold}</td>
                      <td className="px-4 py-3 text-ink">{s.forecastedDemand}</td>
                      <td className="px-4 py-3 font-semibold text-ink">{s.suggestedOrderQty}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-control px-2.5 py-1 text-xs font-semibold ${confidenceBadgeClass(
                            s.confidenceScore
                          )}`}
                        >
                          {s.confidenceScore}%
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        {decision === 'approved' ? (
                          <span className="text-xs font-semibold text-success">Approved</span>
                        ) : (
                          <div className="flex gap-2">
                            <button
                              type="button"
                              onClick={() =>
                                setDecisions((prev) => ({ ...prev, [s.productId]: 'approved' }))
                              }
                              className="rounded-control bg-success/10 px-2.5 py-1.5 text-xs font-semibold text-success hover:bg-success/20"
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              onClick={() =>
                                setDecisions((prev) => ({ ...prev, [s.productId]: 'dismissed' }))
                              }
                              className="rounded-control bg-danger/10 px-2.5 py-1.5 text-xs font-semibold text-danger hover:bg-danger/20"
                            >
                              Dismiss
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
