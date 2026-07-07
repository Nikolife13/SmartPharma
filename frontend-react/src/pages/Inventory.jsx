import { useEffect, useMemo, useState } from 'react';
import { productService } from '../api/services';
import { useToast } from '../context/ToastContext';
import { SkeletonRows } from '../components/Skeleton';
import AddProductModal from '../components/AddProductModal';
import StockModal from '../components/StockModal';
import { IconPlus, IconArrowUp, IconArrowDown, IconSearch } from '../components/icons';
import { formatDate, isNearExpiry } from '../utils/dates';
import inventoryBanner from '../assets/inventory-banner.png';

const PAGE_SIZE = 20;

export default function Inventory() {
  const { showToast } = useToast();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  const [showAddModal, setShowAddModal] = useState(false);
  const [stockAction, setStockAction] = useState(null); // { product, mode }

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const { data } = await productService.getAll();
      setProducts(data || []);
      setError('');
    } catch {
      setError('Unable to load inventory.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const filteredProducts = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return products;
    return products.filter((product) => product.name.toLowerCase().includes(term));
  }, [products, search]);

  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PAGE_SIZE));
  const pagedProducts = filteredProducts.slice(
    (page - 1) * PAGE_SIZE,
    page * PAGE_SIZE
  );

  const handleAddProduct = async (payload) => {
    try {
      await productService.create(payload);
      showToast('Product added successfully.');
      setShowAddModal(false);
      fetchProducts();
    } catch {
      showToast('Failed to add product.', 'error');
    }
  };

  const handleStockUpdate = async (id, quantityChange, reason) => {
    try {
      await productService.updateStock(id, quantityChange, reason);
      showToast('Stock updated successfully.');
      setStockAction(null);
      fetchProducts();
    } catch {
      showToast('Failed to update stock.', 'error');
    }
  };

  return (
    <div>
      <img
        src={inventoryBanner}
        alt="Pharmacy staff reviewing stock records on a tablet"
        className="h-auto max-h-80 w-full object-cover"
      />

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <h1 className="text-2xl font-extrabold text-ink">Inventory</h1>
        <button
          type="button"
          onClick={() => setShowAddModal(true)}
          className="inline-flex items-center gap-2 rounded-control bg-accent px-4 py-2.5 text-sm font-semibold text-ink transition-colors hover:brightness-95"
        >
          <IconPlus className="h-4 w-4" />
          Add Product
        </button>
      </div>

      <div className="mb-4 flex items-center gap-3">
        <div className="relative w-full max-w-sm">
          <IconSearch className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <input
            type="text"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(1);
            }}
            placeholder="Search products by name…"
            className="w-full rounded-control border border-border py-2.5 pl-9 pr-3 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>
      </div>

      {error && (
        <div className="mb-4 rounded-control border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-card bg-surface shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border bg-background/60 text-muted">
                <th className="px-4 py-3 font-medium">Product Name</th>
                <th className="px-4 py-3 font-medium">Batch</th>
                <th className="px-4 py-3 font-medium">Quantity</th>
                <th className="px-4 py-3 font-medium">Min Threshold</th>
                <th className="px-4 py-3 font-medium">Expiry Date</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-4 py-6">
                    <SkeletonRows rows={6} cols={6} />
                  </td>
                </tr>
              ) : pagedProducts.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-muted">
                    No products found.
                  </td>
                </tr>
              ) : (
                pagedProducts.map((product, index) => {
                  const lowStock = product.currentQuantity <= product.minThreshold;
                  const nearExpiry = isNearExpiry(product.expiryDate);
                  return (
                    <tr
                      key={product.id}
                      className={`border-b border-border last:border-0 ${
                        index % 2 === 1 ? 'bg-background/40' : ''
                      }`}
                    >
                      <td className="px-4 py-3 font-medium text-ink">{product.name}</td>
                      <td className="px-4 py-3 text-muted">{product.batchNumber}</td>
                      <td
                        className={`px-4 py-3 ${
                          lowStock ? 'font-semibold text-danger' : 'text-ink'
                        }`}
                      >
                        {product.currentQuantity}
                      </td>
                      <td className="px-4 py-3 text-muted">{product.minThreshold}</td>
                      <td
                        className={`px-4 py-3 ${
                          nearExpiry ? 'font-semibold text-accent' : 'text-ink'
                        }`}
                      >
                        {formatDate(product.expiryDate)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => setStockAction({ product, mode: 'in' })}
                            className="inline-flex items-center gap-1 rounded-control bg-success/10 px-2.5 py-1.5 text-xs font-semibold text-success hover:bg-success/20"
                            title="Stock In"
                          >
                            <IconArrowUp className="h-3.5 w-3.5" />
                            In
                          </button>
                          <button
                            type="button"
                            onClick={() => setStockAction({ product, mode: 'out' })}
                            className="inline-flex items-center gap-1 rounded-control bg-danger/10 px-2.5 py-1.5 text-xs font-semibold text-danger hover:bg-danger/20"
                            title="Stock Out"
                          >
                            <IconArrowDown className="h-3.5 w-3.5" />
                            Out
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {!loading && totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-3">
            <p className="text-sm text-muted">
              Page {page} of {totalPages}
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setPage((prev) => Math.max(1, prev - 1))}
                disabled={page === 1}
                className="rounded-control border border-border px-3 py-1.5 text-sm font-medium text-ink hover:bg-background disabled:opacity-40"
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
                disabled={page === totalPages}
                className="rounded-control border border-border px-3 py-1.5 text-sm font-medium text-ink hover:bg-background disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {showAddModal && (
        <AddProductModal onClose={() => setShowAddModal(false)} onSubmit={handleAddProduct} />
      )}

      {stockAction && (
        <StockModal
          product={stockAction.product}
          mode={stockAction.mode}
          onClose={() => setStockAction(null)}
          onSubmit={handleStockUpdate}
        />
      )}
      </div>
    </div>
  );
}
