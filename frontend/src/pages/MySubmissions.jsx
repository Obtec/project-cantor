import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';
import { articleCode, longDate } from '../labels.js';

export default function MySubmissions() {
  const [papers, setPapers] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    client.get('/papers', { params: { mine: true } })
      .then(({ data }) => setPapers(data))
      .catch((err) => setError(apiError(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <div className="section-head">
        <h2>My Submissions</h2>
        <Link className="btn btn-primary" to="/submit">새 논문 제출</Link>
      </div>
      {error && <div className="alert error">{error}</div>}
      {loading ? (
        <p className="muted">불러오는 중…</p>
      ) : papers.length === 0 ? (
        <p className="muted">아직 제출한 논문이 없습니다.</p>
      ) : (
        <ul className="toc">
          {papers.map((p) => (
            <li key={p.id} className="toc-item">
              <div className="toc-main">
                <h3 className="article-title"><Link to={`/papers/${p.id}`}>{p.title}</Link></h3>
                <div className="toc-citation">
                  {articleCode(p)}<span className="sep">|</span>투고 {longDate(p.createdAt)}
                </div>
              </div>
              <div className="toc-side">
                <StatusBadge status={p.status} />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
