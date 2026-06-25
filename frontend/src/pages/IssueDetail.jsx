import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function IssueDetail() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ volume: '', number: '', year: '', title: '' });

  const load = () => client.get(`/issues/${id}`).then((r) => setData(r.data)).catch((e) => setError(apiError(e)));
  useEffect(() => { load(); }, [id]);

  const publishIssue = async () => {
    try { await client.post(`/issues/${id}/publish`); load(); }
    catch (e) { setError(apiError(e)); }
  };

  const openEdit = () => {
    setForm({ volume: data.issue.volume, number: data.issue.number, year: data.issue.year, title: data.issue.title || '' });
    setEditing(true);
  };

  const saveEdit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await client.put(`/issues/${id}`, {
        volume: Number(form.volume), number: Number(form.number), year: Number(form.year), title: form.title,
      });
      setEditing(false);
      load();
    } catch (err) { setError(apiError(err, '호 수정에 실패했습니다.')); }
  };

  const removeIssue = async () => {
    if (!window.confirm('이 호를 삭제하시겠습니까?\n수록된 논문은 발행 해제(게재확정 상태로 전환)되며 호 배정이 풀립니다.')) return;
    try {
      await client.delete(`/issues/${id}`);
      navigate('/issues');
    } catch (err) { setError(apiError(err, '호 삭제에 실패했습니다.')); }
  };

  if (error) return <div className="alert error">{error}</div>;
  if (!data) return <p className="muted">불러오는 중…</p>;

  const { issue, articles } = data;

  return (
    <div>
      <div className="breadcrumb"><Link to="/issues">Archive</Link> &rsaquo; <span>{issue.label}</span></div>
      <div className="section-head">
        <h2>{issue.label}</h2>
        <div className="detail-head-actions">
          <span className="issue-tag">{issue.published ? `발행 ${issue.publishedDate}` : '준비중'}</span>
          {hasRole('EDITOR') && (
            <button className="btn btn-ghost btn-sm" onClick={editing ? () => setEditing(false) : openEdit}>
              {editing ? '닫기' : '호 정보 수정'}
            </button>
          )}
          {hasRole('EDITOR') && (
            <button className="btn btn-danger btn-sm" onClick={removeIssue}>호 삭제</button>
          )}
        </div>
      </div>
      {issue.title && !editing && <p className="lead" style={{ fontFamily: 'var(--serif)' }}>{issue.title}</p>}

      {hasRole('EDITOR') && editing && (
        <form className="form-inline card" onSubmit={saveEdit} style={{ padding: '1rem 1.2rem' }}>
          <label>권<input type="number" min={1} value={form.volume} onChange={(e) => setForm({ ...form, volume: e.target.value })} /></label>
          <label>호<input type="number" min={1} value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} /></label>
          <label>연도<input type="number" value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} /></label>
          <label className="grow">제목<input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
          <button className="btn btn-primary">저장</button>
        </form>
      )}

      {hasRole('EDITOR') && !issue.published && !editing && (
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
                <div className="eyebrow">{p.category || '미분류'} · {p.articleType || 'Original Research'}</div>
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
