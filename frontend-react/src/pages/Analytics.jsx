import { IconChart } from '../components/icons';

export default function Analytics() {
  return (
    <div className="mx-auto flex max-w-7xl flex-col items-center px-4 py-20 text-center sm:px-6 lg:px-8">
      <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-primary/10 text-primary">
        <IconChart className="h-12 w-12" />
      </div>
      <h1 className="text-2xl font-extrabold text-ink">Sales Analytics — Coming Soon</h1>
      <p className="mt-3 max-w-md text-sm text-muted">
        Interactive sales charts and reporting are on the way, so you can track performance
        trends at a glance.
      </p>
    </div>
  );
}
