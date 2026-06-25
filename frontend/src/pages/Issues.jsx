import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Issues() {
  const { hasRole } = useAuth();
  const [issues, setIssues] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ volume: 1, number: 1, year: new Date().getFullYear(), title: '' });

  const load = () => client.get('/issues').then(({ data }) => setIssues(data)).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); }, []);

  const create = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await client.post('/issues', {
        volume: Number(form.volume), number: Number(form.number), year: Number(form.year), title: form.title,
      });
      setForm({ ...form, title: '' });
      load();
    } catch (err) { setError(apiError(err, '호 생성 실패')); }
  };

  return (
    <div>
      <div className="section-head"><h2>Archive — 권/호</h2></div>
      {error && <div className="alert error">{error}</div>}

      {hasRole('EDITOR') && (
        <div className="card">
          <h3 style={{ marginTop: 0 }}>새 호 만들기</h3>
          <form className="form-inline" onSubmit={create}>
            <label>권<input type="number" min={1} value={form.volume} onChange={(e) => setForm({ ...form, volume: e.target.value })} /></label>
            <label>호<input type="number" min={1} value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} /></label>
            <label>연도<input type="number" value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} /></label>
            <label className="grow">제목(선택)<input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
            <button className="btn btn-primary">생성</button>
          </form>
        </div>
      )}

      {issues.length === 0 ? (
        <p className="muted">발행된 호가 없습니다.</p>
      ) : (
        <ul className="toc">
          {issues.map((i) => (
            <li key={i.id} className="toc-item">
              <div className="toc-main">
                <h3 className="article-title"><Link to={`/issues/${i.id}`}>{i.label}</Link></h3>
                {i.title && <div className="authors">{i.title}</div>}
                <div className="toc-citation">
                  {i.articleCount}편 {i.published ? `· 발행 ${i.publishedDate}` : '· 준비중'}
                </div>
              </div>
              <div className="toc-side">
                <span className={`badge ${i.published ? 'status-PUBLISHED' : 'status-SUBMITTED'}`}>
                  {i.published ? '발행' : '준비중'}
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
