import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ordersService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';
import { IconOrders } from '../components/icons';
import { formatDate } from '../utils/dates';

// Supplier-only "Incoming Orders" page. Gated on isSupplierActive - a PENDING
// supplier sees only PendingApprovalScreen below, never real order data.
function orderStatusBadgeClass(status) {
  if (status === 'APPROVED') return 'bg-success/10 text-success';
  if (status === 'PARTIALLY_APPROVED') return 'bg-accent/20 text-ink';
  if (status === 'REJECTED') return 'bg-danger/10 text-danger';
  return 'bg-primary/10 text-primary'; // PENDING
}

function PendingApprovalScreen() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center px-4 py-24 text-center">
      <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-primary/10 text-primary">
        <IconOrders className="h-10 w-10" />
      </div>
      <h1 className="text-xl font-extrabold text-ink">Your account is pending approval</h1>
      <p className="mt-3 max-w-md text-sm text-muted">
        A SmartPharma manager needs to approve your supplier account before you can see or
        respond to orders. Check back soon.
      </p>
    </div>
  );
}

// Starting point for the "respond to this order" form - defaults every item to
// available at its full requested quantity, since that's the common case; the
// supplier only needs to touch the items that are actually a problem.
function initDraft(order) {
  const items = {};
  order.items.forEach((item) => {
    items[item.id] = { available: true, confirmedQty: item.requestedQty };
  });
  return { items, expectedDeliveryDate: '', supplierNote: '' };
}

export default function SupplierOrders() {
  const { isSupplierActive } = useAuth();
  const { showToast } = useToast();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const [draft, setDraft] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const loadOrders = async () => {
    setLoading(true);
    try {
      const { data } = await ordersService.getAll();
      setOrders(data || []);
    } catch {
      showToast('Failed to load orders.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isSupplierActive) loadOrders();
  }, [isSupplierActive]);

  if (!isSupplierActive) {
    return <PendingApprovalScreen />;
  }

  const startResponding = (order) => {
    setExpandedId(order.id);
    setDraft(initDraft(order));
  };

  const updateItem = (itemId, patch) => {
    setDraft((prev) => ({
      ...prev,
      items: { ...prev.items, [itemId]: { ...prev.items[itemId], ...patch } },
    }));
  };

  const submitResponse = async (orderId) => {
    if (!draft.expectedDeliveryDate) {
      showToast('Set an expected delivery date.', 'error');
      return;
    }
    const items = Object.entries(draft.items).map(([itemId, value]) => ({
      itemId: Number(itemId),
      available: value.available,
      confirmedQty: value.available ? value.confirmedQty : 0,
    }));

    setSubmitting(true);
    try {
      await ordersService.respond(orderId, items, draft.expectedDeliveryDate, draft.supplierNote);
      showToast('Response sent to manager.', 'success');
      setExpandedId(null);
      setDraft(null);
      await loadOrders();
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to send response.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-extrabold text-ink">Incoming Orders</h1>
      <p className="mt-1 text-sm text-muted">
        Orders sent to you by SmartPharma. Confirm availability and a delivery date.
      </p>

      <div className="mt-6">
        {loading && (
          <div className="rounded-card bg-surface p-6 shadow-card">
            <SkeletonRows rows={4} cols={5} />
          </div>
        )}

        {!loading && orders.length === 0 && (
          <div className="rounded-card bg-surface px-4 py-16 text-center text-sm text-muted shadow-card">
            No orders yet.
          </div>
        )}

        {!loading &&
          orders.map((order) => (
            <div key={order.id} className="mb-4 overflow-hidden rounded-card bg-surface shadow-card">
              <div className="flex flex-col gap-2 border-b border-border p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <span className="font-semibold text-ink">Order #{order.id}</span>
                  <span className="ml-3 text-xs text-muted">{formatDate(order.createdAt)}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span
                    className={`inline-flex rounded-control px-2.5 py-1 text-xs font-semibold ${orderStatusBadgeClass(
                      order.status
                    )}`}
                  >
                    {order.status.replaceAll('_', ' ')}
                  </span>
                  {order.status === 'PENDING' && expandedId !== order.id && (
                    <button
                      type="button"
                      onClick={() => startResponding(order)}
                      className="rounded-control bg-primary px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary-dark"
                    >
                      Respond
                    </button>
                  )}
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-border bg-background/60 text-muted">
                      <th className="px-4 py-2 font-medium">Product</th>
                      <th className="px-4 py-2 font-medium">Requested</th>
                      {expandedId === order.id ? (
                        <>
                          <th className="px-4 py-2 font-medium">Available?</th>
                          <th className="px-4 py-2 font-medium">Confirmed Qty</th>
                        </>
                      ) : (
                        <th className="px-4 py-2 font-medium">Status</th>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {order.items.map((item) => (
                      <tr key={item.id} className="border-b border-border last:border-0">
                        <td className="px-4 py-2 text-ink">{item.productName}</td>
                        <td className="px-4 py-2 text-muted">{item.requestedQty}</td>
                        {expandedId === order.id ? (
                          <>
                            <td className="px-4 py-2">
                              <select
                                value={draft.items[item.id]?.available ? 'yes' : 'no'}
                                onChange={(event) =>
                                  updateItem(item.id, { available: event.target.value === 'yes' })
                                }
                                className="rounded-control border border-border px-2 py-1 text-sm"
                              >
                                <option value="yes">Available</option>
                                <option value="no">Unavailable</option>
                              </select>
                            </td>
                            <td className="px-4 py-2">
                              <input
                                type="number"
                                min="0"
                                disabled={!draft.items[item.id]?.available}
                                value={draft.items[item.id]?.confirmedQty ?? 0}
                                onChange={(event) =>
                                  updateItem(item.id, { confirmedQty: Number(event.target.value) })
                                }
                                className="w-20 rounded-control border border-border px-2 py-1 text-sm disabled:opacity-40"
                              />
                            </td>
                          </>
                        ) : (
                          <td className="px-4 py-2 text-muted">
                            {item.status === 'PENDING' ? 'Awaiting response' : item.status}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {order.status !== 'PENDING' && (
                <div className="border-t border-border px-4 py-3 text-sm text-muted">
                  Expected delivery:{' '}
                  {order.expectedDeliveryDate ? formatDate(order.expectedDeliveryDate) : '—'}
                  {order.supplierNote && <span> · {order.supplierNote}</span>}
                </div>
              )}

              {expandedId === order.id && (
                <div className="flex flex-col gap-3 border-t border-border p-4 sm:flex-row sm:items-end sm:justify-between">
                  <div className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-end">
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted">
                        Expected delivery date
                      </label>
                      <input
                        type="date"
                        value={draft.expectedDeliveryDate}
                        onChange={(event) =>
                          setDraft((prev) => ({ ...prev, expectedDeliveryDate: event.target.value }))
                        }
                        className="rounded-control border border-border px-3 py-2 text-sm"
                      />
                    </div>
                    <div className="flex-1">
                      <label className="mb-1 block text-xs font-medium text-muted">
                        Note (optional)
                      </label>
                      <input
                        type="text"
                        value={draft.supplierNote}
                        onChange={(event) =>
                          setDraft((prev) => ({ ...prev, supplierNote: event.target.value }))
                        }
                        className="w-full rounded-control border border-border px-3 py-2 text-sm"
                        placeholder="e.g. partial shipment, backorder..."
                      />
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        setExpandedId(null);
                        setDraft(null);
                      }}
                      className="rounded-control border border-border px-4 py-2 text-sm font-medium text-ink hover:bg-background"
                    >
                      Cancel
                    </button>
                    <button
                      type="button"
                      onClick={() => submitResponse(order.id)}
                      disabled={submitting}
                      className="rounded-control bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-dark disabled:opacity-50"
                    >
                      {submitting ? 'Sending…' : 'Submit Response'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
      </div>
    </div>
  );
}
