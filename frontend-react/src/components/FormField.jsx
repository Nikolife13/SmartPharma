export default function FormField({ label, error, children }) {
  return (
    <div className="mb-4">
      {label && (
        <label className="mb-1.5 block text-sm font-medium text-ink">{label}</label>
      )}
      {children}
      {error && <p className="mt-1 text-sm text-danger">{error}</p>}
    </div>
  );
}

export const inputClasses = (hasError) =>
  `w-full rounded-control border px-3 py-2.5 text-sm text-ink placeholder:text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 ${
    hasError ? 'border-danger' : 'border-border focus:border-primary'
  }`;
