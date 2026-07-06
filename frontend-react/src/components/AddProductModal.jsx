import { useState } from 'react';
import Modal from './Modal';
import FormField, { inputClasses } from './FormField';

const initialForm = {
  name: '',
  batchNumber: '',
  expiryDate: '',
  minThreshold: '',
  currentQuantity: '',
};

export default function AddProductModal({ onClose, onSubmit }) {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (field) => (event) => {
    setForm((prev) => ({ ...prev, [field]: event.target.value }));
  };

  const validate = () => {
    const nextErrors = {};
    if (!form.name.trim()) nextErrors.name = 'Product name is required.';
    if (!form.batchNumber.trim()) nextErrors.batchNumber = 'Batch number is required.';
    if (!form.expiryDate) nextErrors.expiryDate = 'Expiry date is required.';
    if (form.minThreshold === '' || Number(form.minThreshold) < 0)
      nextErrors.minThreshold = 'Enter a valid threshold.';
    if (form.currentQuantity === '' || Number(form.currentQuantity) < 0)
      nextErrors.currentQuantity = 'Enter a valid quantity.';
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    try {
      await onSubmit({
        name: form.name.trim(),
        batchNumber: form.batchNumber.trim(),
        expiryDate: form.expiryDate,
        minThreshold: Number(form.minThreshold),
        currentQuantity: Number(form.currentQuantity),
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal title="Add Product" onClose={onClose}>
      <form onSubmit={handleSubmit} noValidate>
        <FormField label="Product Name" error={errors.name}>
          <input
            type="text"
            value={form.name}
            onChange={handleChange('name')}
            className={inputClasses(Boolean(errors.name))}
            placeholder="e.g. Amoxicillin 500mg"
          />
        </FormField>

        <FormField label="Batch Number" error={errors.batchNumber}>
          <input
            type="text"
            value={form.batchNumber}
            onChange={handleChange('batchNumber')}
            className={inputClasses(Boolean(errors.batchNumber))}
            placeholder="e.g. B12345"
          />
        </FormField>

        <FormField label="Expiry Date" error={errors.expiryDate}>
          <input
            type="date"
            value={form.expiryDate}
            onChange={handleChange('expiryDate')}
            className={inputClasses(Boolean(errors.expiryDate))}
          />
        </FormField>

        <div className="grid grid-cols-2 gap-4">
          <FormField label="Min Threshold" error={errors.minThreshold}>
            <input
              type="number"
              min="0"
              value={form.minThreshold}
              onChange={handleChange('minThreshold')}
              className={inputClasses(Boolean(errors.minThreshold))}
              placeholder="0"
            />
          </FormField>

          <FormField label="Initial Quantity" error={errors.currentQuantity}>
            <input
              type="number"
              min="0"
              value={form.currentQuantity}
              onChange={handleChange('currentQuantity')}
              className={inputClasses(Boolean(errors.currentQuantity))}
              placeholder="0"
            />
          </FormField>
        </div>

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
            className="rounded-control bg-accent px-4 py-2 text-sm font-semibold text-ink transition-colors hover:brightness-95 disabled:opacity-60"
          >
            {submitting ? 'Adding…' : 'Add Product'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
