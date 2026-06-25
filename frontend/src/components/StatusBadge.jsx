import { statusLabel } from '../labels.js';

export default function StatusBadge({ status }) {
  return <span className={`badge status-${status}`}>{statusLabel(status)}</span>;
}
