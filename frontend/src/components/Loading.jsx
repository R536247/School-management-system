export function Loading({ text = 'Laster...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      <p className="mt-4 text-gray-600">{text}</p>
    </div>
  );
}

export function Skeleton({ width = 'w-full', height = 'h-4', className = '' }) {
  return (
    <div className={`bg-gray-200 animate-pulse rounded ${width} ${height} ${className}`} />
  );
}

export function SkeletonTable({ rows = 5, columns = 4 }) {
  return (
    <div className="space-y-2">
      {Array(rows).fill(null).map((_, i) => (
        <div key={i} className="flex gap-4">
          {Array(columns).fill(null).map((_, j) => (
            <Skeleton key={j} width="flex-1" height="h-10" />
          ))}
        </div>
      ))}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, description }) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      {Icon && <Icon size={48} className="text-gray-400 mb-4" />}
      <h3 className="text-lg font-medium text-gray-900">{title}</h3>
      <p className="text-gray-600 mt-2">{description}</p>
    </div>
  );
}
