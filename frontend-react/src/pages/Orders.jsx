import { useEffect, useState } from 'react';
import { ordersService, supplierAdminService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';
import { IconRobot } from '../components/icons';
import { formatDate } from '../utils/dates';

function confidenceBadgeClass(score) {
  if (score >= 85) return 'bg-success/10 text-success';
  if (score >= 65) return 'bg-accent/20 text-ink';
  return 'bg-primary/10 text-primary';
}

function orderStatusBadgeClass(status) {
  if (status === 'APPROVED') return 'bg-success/10 text-success';
  if (status === 'PARTIALLY_APPROVED') return 'bg-accent/20 text-ink';
  if (status === 'REJECTED') return 'bg-danger/10 text-danger';
  return 'bg-primary/10 text-primary'; // PENDING
}

export default function Orders() {
  const { showToast } = useToast();
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [generated, setGenerated] = useState(false);
  const [selected, setSelected] = useState({}); // productId -> true
  const [quantities, setQuantities] = useState({}); // productId -> qty

  const [suppliers, setSuppliers] = useState([]);
  const [supplierId, setSupplierId] = useState('');
  const [sending, setSending] = useState(false);

  const [myOrders, setMyOrders] = useState([]);
  const [ordersLoading, setOrdersLoading] = useState(true);

  const loadSuppliers = async () => {
    try {
      const { data } = await supplierAdminService.list('ACTIVE');
      setSuppliers(data || []);
    } catch {
      showToast('Failed to load suppliers.', 'error');
    }
  };

  const loadOrders = async () => {
    setOrdersLoading(true);
    try {
      const { data } = await ordersService.getAll();
      setMyOrders(data || []);
    } catch {
      showToast('Failed to load sent orders.', 'error');
    } finally {
      setOrdersLoading(false);
    }
  };

  useEffect(() => {
    loadSuppliers();
    loadOrders();
  }, []);

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const { data } = await ordersService.getSuggestions();
      setSuggestions(data || []);
      setSelected({});
      setQuantities(
        Object.fromEntries((data || []).map((s) => [s.productId, s.suggestedOrderQty]))
      );
      setGenerated(true);
    } catch {
      showToast('Failed to generate suggested orders.', 'error');
    } finally {
      setLoading(false);
    }
  };

  const toggleSelected = (productId) => {
    setSelected((prev) => ({ ...prev, [productId]: !prev[productId] }));
  };

  const selectedCount = Object.values(selected).filter(Boolean).length;

  const handleSendOrder = async () => {
    if (!supplierId) {
      showToast('Choose a supplier first.', 'error');
      return;
    }
    const items = suggestions
      .filter((s) => selected[s.productId])
      .map((s) => ({ productId: s.productId, quantity: quantities[s.productId] || s.suggestedOrderQty }));
    if (items.length === 0) {
      showToast('Select at least one product.', 'error');
      return;
    }

    setSending(true);
    try {
      await ordersService.create(Number(supplierId), items);
      showToast('Order sent to supplier.', 'success');
      setSelected({});
      await loadOrders();
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to send order.', 'error');
    } finally {
      setSending(false);
    }
  };

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

      {!loading && generated && suggestions.length === 0 && (
        <div className="rounded-card bg-surface px-4 py-16 text-center text-sm text-muted shadow-card">
          No suggestions to show.
        </div>
      )}

      {!loading && generated && suggestions.length > 0 && (
        <div className="overflow-hidden rounded-card bg-surface shadow-card">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border bg-background/60 text-muted">
                  <th className="px-4 py-3 font-medium"></th>
                  <th className="px-4 py-3 font-medium">Product Name</th>
                  <th className="px-4 py-3 font-medium">Current Qty</th>
                  <th className="px-4 py-3 font-medium">Min Threshold</th>
                  <th className="px-4 py-3 font-medium">Forecasted Demand (30d)</th>
                  <th className="px-4 py-3 font-medium">Order Qty</th>
                  <th className="px-4 py-3 font-medium">Confidence</th>
                </tr>
              </thead>
              <tbody>
                {suggestions.map((s, index) => (
                  <tr
                    key={s.productId}
                    className={`border-b border-border last:border-0 ${
                      index % 2 === 1 ? 'bg-background/40' : ''
                    }`}
                  >
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={Boolean(selected[s.productId])}
                        onChange={() => toggleSelected(s.productId)}
                        className="h-4 w-4 rounded border-border"
                      />
                    </td>
                    <td className="px-4 py-3 font-medium text-ink">{s.productName}</td>
                    <td className="px-4 py-3 text-ink">{s.currentQuantity}</td>
                    <td className="px-4 py-3 text-muted">{s.minThreshold}</td>
                    <td className="px-4 py-3 text-ink">{s.forecastedDemand}</td>
                    <td className="px-4 py-3">
                      <input
                        type="number"
                        min="0"
                        value={quantities[s.productId] ?? s.suggestedOrderQty}
                        onChange={(event) =>
                          setQuantities((prev) => ({
                            ...prev,
                            [s.productId]: Number(event.target.value),
                          }))
                        }
                        className="w-20 rounded-control border border-border px-2 py-1 text-sm"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex rounded-control px-2.5 py-1 text-xs font-semibold ${confidenceBadgeClass(
                          s.confidenceScore
                        )}`}
                      >
                        {s.confidenceScore}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex flex-col gap-3 border-t border-border p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <label className="text-sm font-medium text-ink">Send to supplier:</label>
              <select
                value={supplierId}
                onChange={(event) => setSupplierId(event.target.value)}
                className="rounded-control border border-border px-3 py-2 text-sm"
              >
                <option value="">Choose a supplier…</option>
                {suppliers.map((sup) => (
                  <option key={sup.id} value={sup.id}>
                    {sup.username}
                  </option>
                ))}
              </select>
              {suppliers.length === 0 && (
                <span className="text-xs text-muted">
                  No active suppliers yet — approve one in Supplier Approvals.
                </span>
              )}
            </div>
            <button
              type="button"
              onClick={handleSendOrder}
              disabled={sending || selectedCount === 0}
              className="inline-flex items-center justify-center rounded-control bg-primary px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-primary-dark disabled:opacity-50"
            >
              {sending ? 'Sending…' : `Send Order (${selectedCount})`}
            </button>
          </div>
        </div>
      )}

      <div className="mt-10">
        <h2 className="mb-4 text-lg font-bold text-ink">Sent Orders</h2>
        {ordersLoading && (
          <div className="rounded-card bg-surface p-6 shadow-card">
            <SkeletonRows rows={3} cols={5} />
          </div>
        )}
        {!ordersLoading && myOrders.length === 0 && (
          <div className="rounded-card bg-surface px-4 py-10 text-center text-sm text-muted shadow-card">
            No orders sent yet.
          </div>
        )}
        {!ordersLoading && myOrders.length > 0 && (
          <div className="overflow-hidden rounded-card bg-surface shadow-card">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border bg-background/60 text-muted">
                    <th className="px-4 py-3 font-medium">Order</th>
                    <th className="px-4 py-3 font-medium">Supplier</th>
                    <th className="px-4 py-3 font-medium">Items</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Sent</th>
                    <th className="px-4 py-3 font-medium">Expected Delivery</th>
                  </tr>
                </thead>
                <tbody>
                  {myOrders.map((order, index) => (
                    <tr
                      key={order.id}
                      className={`border-b border-border last:border-0 ${
                        index % 2 === 1 ? 'bg-background/40' : ''
                      }`}
                    >
                      <td className="px-4 py-3 font-medium text-ink">#{order.id}</td>
                      <td className="px-4 py-3 text-ink">{order.supplierUsername}</td>
                      <td className="px-4 py-3 text-muted">
                        {order.items.map((i) => `${i.productName} (${i.requestedQty})`).join(', ')}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex rounded-control px-2.5 py-1 text-xs font-semibold ${orderStatusBadgeClass(
                            order.status
                          )}`}
                        >
                          {order.status.replaceAll('_', ' ')}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-muted">{formatDate(order.createdAt)}</td>
                      <td className="px-4 py-3 text-muted">
                        {order.expectedDeliveryDate ? formatDate(order.expectedDeliveryDate) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
