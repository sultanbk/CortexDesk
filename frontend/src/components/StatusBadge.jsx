export default function StatusBadge({ status }) {
  const map = {
    NEW: { label: "NEW", className: "status-new" },
    IN_PROGRESS: { label: "IN PROGRESS", className: "status-in-progress" },
    RESOLVED: { label: "RESOLVED", className: "status-resolved" },
    CLOSED: { label: "CLOSED", className: "status-closed" },
    REOPENED: { label: "REOPENED", className: "status-reopened" },
  };

  const info = map[status] || { label: status || "UNKNOWN", className: "status-closed" };

  return (
    <span className={`status-badge ${info.className}`}>{info.label}</span>
  );
}
