import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function IssueDetail() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  const load = () => client.get(`/issues/${id}`).then((r) => setData(r.data)).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); }, [id]);

  const publishIssue = async () => {
    try { await client.post(`/issues/${id}/publish`); load(); }
    catch (e) { setError(apiError(e)); }
  };

  if (error) return <div className="alert error">{error}</div>;
  if (!data) return <p className="muted">불러오는 중…</p>;

  const { issue, articles } = data;

  return (
    <div>
      <div className="breadcrumb"><Link to="/issues">Archive</Link> &rsaquo; <span>{issue.label}</span></div>
      <div className="section-head">
        <h2>{issue.label}</h2>
        <span className="issue-tag">{issue.published ? `발행 ${issue.publishedDate}` : '준비중'}</span>
      </div>
      {issue.title && <p className="lead" style={{ fontFamily: 'var(--serif)' }}>{issue.title}</p>}

      {hasRole('EDITOR') && !issue.published && (
        <div className="alert info" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>이 호를 발행 처리할 수 있습니다.</span>
          <button className="btn btn-primary btn-sm" onClick={publishIssue}>호 발행</button>
        </div>
      )}

      <div className="section-head" style={{ marginTop: '1.5rem' }}><h2>Table of Contents</h2></div>
      {articles.length === 0 ? (
        <p className="muted">아직 이 호에 배정된 논문이 없습니다.</p>
      ) : (
        <ul className="toc">
          {articles.map((p) => (
            <li key={p.id} className="toc-item">
              <div className="toc-main">
                <div className="eyebrow">{p.category || '논문'}</div>
                <h3 className="article-title"><Link to={`/papers/${p.id}`}>{p.title}</Link></h3>
                <div className="authors">{p.authorsText || p.submitter?.name}</div>
                <div className="toc-citation">
                  {p.articleCode}{p.pages ? ` · pp. ${p.pages}` : ''}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
