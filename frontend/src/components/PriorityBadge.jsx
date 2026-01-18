/**
 * PriorityBadge Component
 * 
 * Displays ticket priority using Bootstrap badges with accessibility symbols
 * - Color-blind accessible (uses symbols + colors)
 * - WCAG AA compliant contrast ratios
 * - Semantic color coding (red=urgent, orange=medium, green=low)
 */

export default function PriorityBadge({ priority }) {
  const priorityConfig = {
    HIGH: { 
      class: 'bg-danger', 
      symbol: '↑↑', 
      text: 'HIGH',
      ariaLabel: 'High Priority'
    },
    MEDIUM: { 
      class: 'bg-warning text-dark', 
      symbol: '↑', 
      text: 'MEDIUM',
      ariaLabel: 'Medium Priority'
    },
    LOW: { 
      class: 'bg-success', 
      symbol: '➜', 
      text: 'LOW',
      ariaLabel: 'Low Priority'
    },
    'NOT SET': { 
      class: 'bg-secondary', 
      symbol: '—', 
      text: 'NOT SET',
      ariaLabel: 'Priority Not Set'
    },
  };

  const config = priorityConfig[priority] || priorityConfig['NOT SET'];

  return (
    <span 
      className={`badge ${config.class}`}
      title={`Priority: ${config.text}`}
      aria-label={config.ariaLabel}
    >
      <span aria-hidden="true">{config.symbol}</span>
      {' '}{config.text}
    </span>
  );
}
