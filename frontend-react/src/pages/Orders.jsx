import { IconRobot } from '../components/icons';

export default function Orders() {
  return (
    <div className="mx-auto flex max-w-7xl flex-col items-center px-4 py-20 text-center sm:px-6 lg:px-8">
      <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-primary/10 text-primary">
        <IconRobot className="h-12 w-12" />
      </div>
      <h1 className="text-2xl font-extrabold text-ink">Predictive Orders — Coming Soon</h1>
      <p className="mt-3 max-w-md text-sm text-muted">
        Our AI is learning from past sales to suggest optimal purchase orders. Stay tuned!
      </p>
      <button
        type="button"
        disabled
        className="mt-8 rounded-control bg-accent px-5 py-2.5 text-sm font-semibold text-ink opacity-50"
      >
        Generate Suggested Order
      </button>
    </div>
  );
}
