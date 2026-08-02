import { useEffect, useState } from 'react';
import { supplierAdminService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';

// Manager-only page for moderating supplier signups - approve turns a PENDING
// supplier into ACTIVE (able to see/respond to orders), reject blocks them.
function statusBadgeClass(status) {
  if (status === 'ACTIVE') return 'bg-success/10 text-success';
  if (status === 'REJECTED') return 'bg-danger/10 text-danger';
  return 'bg-accent/20 text-ink'; // PENDING
}

export default function SupplierApprovals() {
  const { showToast } = useToast();
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const { data } = await supplierAdminService.list();
      setSuppliers(data || []);
    } catch {
      showToast('Failed to load suppliers.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const setStatus = async (id, status) => {
    setUpdatingId(id);
    try {
      await supplierAdminService.updateStatus(id, status);
      showToast(`Supplier ${status === 'ACTIVE' ? 'approved' : 'rejected'}.`, 'success');
      await load();
    } catch {
      showToast('Failed to update supplier status.', 'error');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-extrabold text-ink">Supplier Approvals</h1>
      <p className="mt-1 text-sm text-muted">
        Suppliers must be approved before they can view or respond to orders.
      </p>

      <div className="mt-6 overflow-hidden rounded-card bg-surface shadow-card">
        {loading && (
          <div className="p-6">
            <SkeletonRows rows={4} cols={4} />
          </div>
        )}

        {!loading && suppliers.length === 0 && (
          <div className="px-4 py-16 text-center text-sm text-muted">No supplier accounts yet.</div>
        )}

        {!loading && suppliers.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-border bg-background/60 text-muted">
                  <th className="px-4 py-3 font-medium">Username</th>
                  <th className="px-4 py-3 font-medium">Email</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {suppliers.map((s, index) => (
                  <tr
                    key={s.id}
                    className={`border-b border-border last:border-0 ${
                      index % 2 === 1 ? 'bg-background/40' : ''
                    }`}
                  >
                    <td className="px-4 py-3 font-medium text-ink">{s.username}</td>
                    <td className="px-4 py-3 text-muted">{s.email || '—'}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex rounded-control px-2.5 py-1 text-xs font-semibold ${statusBadgeClass(
                          s.supplierStatus
                        )}`}
                      >
                        {s.supplierStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {s.supplierStatus === 'PENDING' ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => setStatus(s.id, 'ACTIVE')}
                            disabled={updatingId === s.id}
                            className="rounded-control bg-success/10 px-2.5 py-1.5 text-xs font-semibold text-success hover:bg-success/20 disabled:opacity-50"
                          >
                            Approve
                          </button>
                          <button
                            type="button"
                            onClick={() => setStatus(s.id, 'REJECTED')}
                            disabled={updatingId === s.id}
                            className="rounded-control bg-danger/10 px-2.5 py-1.5 text-xs font-semibold text-danger hover:bg-danger/20 disabled:opacity-50"
                          >
                            Reject
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-muted">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
