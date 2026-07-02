import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { statusLabel } from '../labels.js';

function formatMetric(m) {
  if (m.integer) return Math.round(m.value).toLocaleString();
  return m.value.toFixed(2);
}

export default function AuthorProfile() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    client.get(`/authors/${id}/metrics`)
      .then((res) => setData(res.data))
      .catch((err) => setError(apiError(err)));
  }, [id]);

  if (error) return <div className="alert error">{error}</div>;
  if (!data) return <p className="muted">불러오는 중…</p>;

  return (
    <div>
      <div className="breadcrumb">
        <Link to="/">Articles</Link> &rsaquo; <span>저자</span>
      </div>

      <div className="author-head">
        <h2>{data.author.name}</h2>
        {data.affiliation && <div className="muted">{data.affiliation}</div>}
      </div>

      <div className="section-head"><h2>Author Metrics</h2></div>
      <div className="metric-grid">
        {data.metrics.map((m) => (
          <div key={m.key} className="metric-card" title={m.description}>
            <div className="metric-value">{formatMetric(m)}</div>
            <div className="metric-label">{m.label}</div>
          </div>
        ))}
      </div>

      <div className="section-head" style={{ marginTop: '2rem' }}>
        <h2>논문</h2>
        <span className="issue-tag">{data.paperCount}편</span>
      </div>
      {data.papers.length === 0 ? (
        <p className="muted">게재된 논문이 없습니다.</p>
      ) : (
        <ul className="toc">
          {data.papers.map((p) => (
            <li key={p.id} className="toc-item">
              <div className="toc-main">
                <h3 className="article-title"><Link to={`/papers/${p.id}`}>{p.title}</Link></h3>
                <div className="toc-citation">{statusLabel(p.status)}</div>
              </div>
              <div className="toc-side">
                <span className="badge">피인용 {p.citationCount}</span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
