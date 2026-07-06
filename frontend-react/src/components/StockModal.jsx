import { useState } from 'react';
import Modal from './Modal';
import FormField, { inputClasses } from './FormField';

const REASONS = ['SALE', 'EXPIRED', 'RESTOCK'];

export default function StockModal({ product, mode, onClose, onSubmit }) {
  const isStockIn = mode === 'in';
  const [quantity, setQuantity] = useState('');
  const [reason, setReason] = useState(isStockIn ? 'RESTOCK' : 'SALE');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const parsed = Number(quantity);
    if (!quantity || parsed <= 0) {
      setError('Enter a quantity greater than zero.');
      return;
    }

    setSubmitting(true);
    try {
      const quantityChange = isStockIn ? parsed : -parsed;
      await onSubmit(product.id, quantityChange, reason);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal title={`${isStockIn ? 'Stock In' : 'Stock Out'} — ${product.name}`} onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <p className="mb-4 text-sm text-muted">
          Current quantity: <span className="font-semibold text-ink">{product.currentQuantity}</span>
        </p>

        <FormField label="Quantity" error={error}>
          <input
            type="number"
            min="1"
            value={quantity}
            onChange={(event) => {
              setQuantity(event.target.value);
              setError('');
            }}
            className={inputClasses(Boolean(error))}
            placeholder="e.g. 10"
            autoFocus
          />
        </FormField>

        <FormField label="Reason">
          <select
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            className={inputClasses(false)}
          >
            {REASONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </FormField>

        <div className="mt-2 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-control border border-border px-4 py-2 text-sm font-medium text-ink hover:bg-background"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="rounded-control bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-dark disabled:opacity-60"
          >
            {submitting ? 'Applying…' : 'Apply'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
