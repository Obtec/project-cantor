import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { formatDate } from '../labels.js';

const RECS = [
  ['ACCEPT', '게재 추천'],
  ['MINOR_REVISION', '경미한 수정'],
  ['MAJOR_REVISION', '대폭 수정'],
  ['REJECT', '게재 불가'],
];

function ReviewForm({ assignment, onDone }) {
  const [rec, setRec] = useState('ACCEPT');
  const [score, setScore] = useState(3);
  const [toAuthor, setToAuthor] = useState('');
  const [toEditor, setToEditor] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await client.post('/reviews', {
        assignmentId: assignment.id,
        recommendation: rec,
        score: Number(score),
        commentsToAuthor: toAuthor,
        commentsToEditor: toEditor,
      });
      onDone();
    } catch (err) {
      setError(apiError(err, '심사 제출에 실패했습니다.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="form review-form" onSubmit={submit}>
      {error && <div className="alert error">{error}</div>}
      <label>추천 의견
        <select value={rec} onChange={(e) => setRec(e.target.value)}>
          {RECS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
      </label>
      <label>점수 (1~5)
        <input type="number" min={1} max={5} value={score} onChange={(e) => setScore(e.target.value)} />
      </label>
      <label>저자에게 전달할 의견
        <textarea rows={3} value={toAuthor} onChange={(e) => setToAuthor(e.target.value)} />
      </label>
      <label>편집자에게만 전달할 의견
        <textarea rows={2} value={toEditor} onChange={(e) => setToEditor(e.target.value)} />
      </label>
      <button className="btn btn-primary" disabled={busy}>{busy ? '제출 중…' : '심사 제출'}</button>
    </form>
  );
}

export default function ReviewDashboard() {
  const [assignments, setAssignments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    client.get('/reviews/assigned')
      .then(({ data }) => setAssignments(data))
      .catch((err) => setError(apiError(err)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  return (
    <div>
      <h2>심사 대시보드</h2>
      {error && <div className="alert error">{error}</div>}
      {loading ? (
        <p className="muted">불러오는 중…</p>
      ) : assignments.length === 0 ? (
        <p className="muted">배정된 심사가 없습니다.</p>
      ) : (
        assignments.map((a) => (
          <div key={a.id} className="card">
            <div className="detail-head">
              <h3><Link to={`/papers/${a.paperId}`}>{a.paperTitle}</Link></h3>
              <span className={`badge ${a.reviewSubmitted ? 'status-PUBLISHED' : 'status-SUBMITTED'}`}>
                {a.reviewSubmitted ? '심사 완료' : '심사 대기'}
              </span>
            </div>
            {a.dueDate && <p className="muted small">마감일: {a.dueDate}</p>}
            <p className="muted small">배정일: {formatDate(a.createdAt)}</p>
            {a.reviewSubmitted
              ? <p className="muted">이미 심사를 제출했습니다.</p>
              : <ReviewForm assignment={a} onDone={load} />}
          </div>
        ))
      )}
    </div>
  );
}
