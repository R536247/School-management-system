export function Card({ children, className = '' }) {
  return (
    <div className={`bg-white rounded-lg border border-gray-200 p-6 shadow-sm ${className}`}>
      {children}
    </div>
  );
}

export function CardHeader({ title, subtitle, action }) {
  return (
    <div className="flex items-center justify-between mb-4 pb-4 border-b border-gray-200">
      <div>
        <h2 className="text-xl font-semibold text-gray-900">{title}</h2>
        {subtitle && <p className="text-sm text-gray-600 mt-1">{subtitle}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  );
}

export function CardBody({ children }) {
  return <div className="space-y-4">{children}</div>;
}

export function CardFooter({ children }) {
  return (
    <div className="mt-4 pt-4 border-t border-gray-200 flex justify-end gap-2">
      {children}
    </div>
  );
}
