import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { formatDate } from '../labels.js';

export default function ReviewDashboard() {
  const [assignments, setAssignments] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    client.get('/reviews/assigned')
      .then(({ data }) => setAssignments(data))
      .catch((err) => setError(apiError(err)))
      .finally(() => setLoading(false));
  }, []);

  const pending = assignments.filter((a) => !a.reviewSubmitted);

  return (
    <div>
      <div className="section-head">
        <h2>내 심사 목록</h2>
        <span className="issue-tag">대기 {pending.length} · 전체 {assignments.length}</span>
      </div>

      {error && <div className="alert error">{error}</div>}
      {loading ? (
        <p className="muted">불러오는 중…</p>
      ) : assignments.length === 0 ? (
        <p className="muted">배정된 심사가 없습니다.</p>
      ) : (
        <ul className="toc">
          {assignments.map((a) => (
            <li key={a.id} className="toc-item">
              <div className="toc-main">
                <div className="eyebrow">블라인드 테스트</div>
                <h3 className="article-title">
                  <Link to={`/reviews/${a.id}`}>{a.paperTitle}</Link>
                </h3>
                <div className="toc-citation">
                  {a.dueDate ? `마감 ${a.dueDate}` : '마감일 없음'}
                  <span className="sep">|</span>배정 {formatDate(a.createdAt)}
                </div>
              </div>
              <div className="toc-side">
                <span className={`badge ${a.reviewSubmitted ? 'status-PUBLISHED' : 'status-SUBMITTED'}`}>
                  {a.reviewSubmitted ? '심사 완료' : '심사 대기'}
                </span>
                <Link className="pdf-link" to={`/reviews/${a.id}`}>
                  {a.reviewSubmitted ? '내용 보기 ›' : '심사하기 ›'}
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
