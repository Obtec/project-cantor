import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';
import { recommendationLabel, formatDate } from '../labels.js';

const DECISIONS = [
  ['ACCEPT', '게재 확정'],
  ['PUBLISH', '게재'],
  ['REVISION', '수정 요청'],
  ['REJECT', '반려'],
];

function PaperManager({ paper, reviewers, issues, onChanged }) {
  const [assignments, setAssignments] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [reviewerId, setReviewerId] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [decision, setDecision] = useState('ACCEPT');
  const [note, setNote] = useState('');
  const [pub, setPub] = useState({ issueId: '', pageStart: '', pageEnd: '' });
  const [error, setError] = useState('');

  const load = useCallback(() => {
    client.get(`/papers/${paper.id}/assignments`).then(({ data }) => setAssignments(data)).catch(() => {});
    client.get(`/papers/${paper.id}/reviews`).then(({ data }) => setReviews(data)).catch(() => {});
  }, [paper.id]);

  useEffect(load, [load]);

  const assign = async (e) => {
    e.preventDefault();
    setError('');
    if (!reviewerId) { setError('리뷰어를 선택하세요.'); return; }
    try {
      await client.post(`/papers/${paper.id}/assign`, {
        reviewerId: Number(reviewerId),
        dueDate: dueDate || null,
      });
      setReviewerId('');
      setDueDate('');
      load();
      onChanged();
    } catch (err) {
      setError(apiError(err, '배정에 실패했습니다.'));
    }
  };

  const remove = async () => {
    if (!window.confirm(`'${paper.title}' 논문을 삭제하시겠습니까?\n연결된 심사·배정 정보와 첨부 파일이 함께 삭제되며 되돌릴 수 없습니다.`)) {
      return;
    }
    setError('');
    try {
      await client.delete(`/papers/${paper.id}`);
      onChanged();
    } catch (err) {
      setError(apiError(err, '삭제에 실패했습니다.'));
    }
  };

  const decide = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await client.post(`/papers/${paper.id}/decision`, { decision, note });
      setNote('');
      onChanged();
    } catch (err) {
      setError(apiError(err, '결정 처리에 실패했습니다.'));
    }
  };

  const publish = async (e) => {
    e.preventDefault();
    setError('');
    if (!pub.issueId) { setError('발행할 호를 선택하세요.'); return; }
    try {
      await client.post(`/papers/${paper.id}/publish`, {
        issueId: Number(pub.issueId),
        pageStart: pub.pageStart ? Number(pub.pageStart) : null,
        pageEnd: pub.pageEnd ? Number(pub.pageEnd) : null,
      });
      onChanged();
    } catch (err) {
      setError(apiError(err, '발행에 실패했습니다.'));
    }
  };

  return (
    <div className="card">
      <div className="detail-head">
        <h3><Link to={`/papers/${paper.id}`}>{paper.title}</Link></h3>
        <div className="detail-head-actions">
          <StatusBadge status={paper.status} />
          <button className="btn btn-danger btn-sm" onClick={remove}>삭제</button>
        </div>
      </div>
      <p className="muted small">{paper.authorsText || paper.submitter?.name} · {formatDate(paper.updatedAt)}</p>
      {error && <div className="alert error">{error}</div>}

      <div className="grid-2">
        <section>
          <h4>리뷰어 배정</h4>
          <form className="form" onSubmit={assign}>
            <label>리뷰어
              <select value={reviewerId} onChange={(e) => setReviewerId(e.target.value)}>
                <option value="">선택…</option>
                {reviewers.map((r) => (
                  <option key={r.id} value={r.id}>{r.name} ({r.email})</option>
                ))}
              </select>
            </label>
            <label>마감일 <span className="muted">(선택)</span>
              <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
            </label>
            <button className="btn btn-primary">배정</button>
          </form>
          <ul className="assignment-list">
            {assignments.map((a) => (
              <li key={a.id}>
                {a.reviewer?.name}
                <span className={`badge ${a.reviewSubmitted ? 'status-PUBLISHED' : 'status-SUBMITTED'}`}>
                  {a.reviewSubmitted ? '완료' : a.status === 'DECLINED' ? '거절' : '대기'}
                </span>
              </li>
            ))}
            {assignments.length === 0 && <li className="muted">배정된 리뷰어 없음</li>}
          </ul>
        </section>

        <section>
          <h4>게재 결정</h4>
          <form className="form" onSubmit={decide}>
            <label>결정
              <select value={decision} onChange={(e) => setDecision(e.target.value)}>
                {DECISIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </label>
            <label>메모 <span className="muted">(저자에게 표시)</span>
              <textarea rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
            </label>
            <button className="btn btn-primary">결정 적용</button>
          </form>
        </section>
      </div>

      <section className="publish-row">
        <h4>발행 (호 배정)</h4>
        <form className="form-inline" onSubmit={publish}>
          <label>호
            <select value={pub.issueId} onChange={(e) => setPub({ ...pub, issueId: e.target.value })}>
              <option value="">선택…</option>
              {issues.map((i) => <option key={i.id} value={i.id}>{i.label}</option>)}
            </select>
          </label>
          <label>시작p<input type="number" value={pub.pageStart} onChange={(e) => setPub({ ...pub, pageStart: e.target.value })} /></label>
          <label>끝p<input type="number" value={pub.pageEnd} onChange={(e) => setPub({ ...pub, pageEnd: e.target.value })} /></label>
          <button className="btn btn-primary">게재 발행</button>
        </form>
        {paper.issueLabel && <p className="muted small">현재: {paper.issueLabel}{paper.pages ? ` · pp. ${paper.pages}` : ''}</p>}
      </section>

      <section className="reviews">
        <h4>접수된 심사 ({reviews.length})</h4>
        {reviews.length === 0 ? (
          <p className="muted">아직 없음</p>
        ) : reviews.map((r) => (
          <div key={r.id} className="review-card">
            <div className="review-head">
              <strong>{r.reviewer?.name}</strong>
              <span className="badge">{recommendationLabel(r.recommendation)}</span>
              {r.score != null && <span className="muted">점수 {r.score}/5</span>}
            </div>
            {r.commentsToAuthor && <p><strong>저자:</strong> {r.commentsToAuthor}</p>}
            {r.commentsToEditor && <p className="muted"><strong>편집자:</strong> {r.commentsToEditor}</p>}
          </div>
        ))}
      </section>
    </div>
  );
}

export default function EditorDashboard() {
  const [papers, setPapers] = useState([]);
  const [reviewers, setReviewers] = useState([]);
  const [issues, setIssues] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    Promise.all([
      client.get('/papers'),
      client.get('/editor/reviewers'),
      client.get('/issues'),
    ])
      .then(([p, r, i]) => { setPapers(p.data); setReviewers(r.data); setIssues(i.data); })
      .catch((err) => setError(apiError(err)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(load, [load]);

  return (
    <div>
      <h2>편집 대시보드</h2>
      {error && <div className="alert error">{error}</div>}
      {loading ? (
        <p className="muted">불러오는 중…</p>
      ) : papers.length === 0 ? (
        <p className="muted">제출된 논문이 없습니다.</p>
      ) : (
        papers.map((p) => (
          <PaperManager key={p.id} paper={p} reviewers={reviewers} issues={issues} onChanged={load} />
        ))
      )}
    </div>
  );
}
