import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client.js';
import { formatDate } from '../labels.js';

export default function NotificationBell() {
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const navigate = useNavigate();

  const loadCount = useCallback(() => {
    client.get('/notifications/unread-count')
      .then(({ data }) => setCount(data.count))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadCount();
    const t = setInterval(loadCount, 30000); // 30초마다 갱신
    return () => clearInterval(t);
  }, [loadCount]);

  useEffect(() => {
    const onClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) {
      client.get('/notifications').then(({ data }) => setItems(data)).catch(() => {});
    }
  };

  const openItem = async (n) => {
    try { if (!n.read) await client.post(`/notifications/${n.id}/read`); } catch { /* noop */ }
    setOpen(false);
    loadCount();
    if (n.link) navigate(n.link);
  };

  const markAll = async () => {
    try { await client.post('/notifications/read-all'); } catch { /* noop */ }
    setItems((prev) => prev.map((n) => ({ ...n, read: true })));
    setCount(0);
  };

  return (
    <div className="notif" ref={ref}>
      <button className="notif-btn" onClick={toggle} aria-label="알림">
        🔔
        {count > 0 && <span className="notif-badge">{count > 9 ? '9+' : count}</span>}
      </button>
      {open && (
        <div className="notif-panel">
          <div className="notif-head">
            <span>알림</span>
            {count > 0 && <button className="btn-link-sm" onClick={markAll}>모두 읽음</button>}
          </div>
          {items.length === 0 ? (
            <div className="notif-empty muted small">알림이 없습니다.</div>
          ) : (
            <ul className="notif-list">
              {items.map((n) => (
                <li
                  key={n.id}
                  className={`notif-item ${n.read ? '' : 'unread'}`}
                  onClick={() => openItem(n)}
                >
                  <div className="notif-msg">{n.message}</div>
                  <div className="muted small">{formatDate(n.createdAt)}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
