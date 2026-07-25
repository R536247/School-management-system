import { X, AlertCircle, CheckCircle, Info } from 'lucide-react';
import { useState } from 'react';

export default function Alert({ message, type = 'info', dismissible = true, onDismiss }) {
  const [isVisible, setIsVisible] = useState(true);

  const handleDismiss = () => {
    setIsVisible(false);
    onDismiss?.();
  };

  if (!isVisible) return null;

  const styles = {
    info: {
      bg: 'bg-blue-50',
      border: 'border-blue-200',
      text: 'text-blue-800',
      icon: 'text-blue-600',
    },
    success: {
      bg: 'bg-green-50',
      border: 'border-green-200',
      text: 'text-green-800',
      icon: 'text-green-600',
    },
    warning: {
      bg: 'bg-yellow-50',
      border: 'border-yellow-200',
      text: 'text-yellow-800',
      icon: 'text-yellow-600',
    },
    error: {
      bg: 'bg-red-50',
      border: 'border-red-200',
      text: 'text-red-800',
      icon: 'text-red-600',
    },
  };

  const style = styles[type];

  const icons = {
    info: <Info size={20} />,
    success: <CheckCircle size={20} />,
    warning: <AlertCircle size={20} />,
    error: <AlertCircle size={20} />,
  };

  return (
    <div className={`${style.bg} border ${style.border} rounded-lg p-4 flex items-start gap-3`}>
      <div className={`flex-shrink-0 ${style.icon}`}>
        {icons[type]}
      </div>
      <div className={`flex-grow ${style.text}`}>
        {message}
      </div>
      {dismissible && (
        <button
          onClick={handleDismiss}
          className={`flex-shrink-0 ${style.icon} hover:opacity-75`}
        >
          <X size={20} />
        </button>
      )}
    </div>
  );
}
