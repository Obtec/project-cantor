import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { openPdfInNewTab } from '../api/pdf.js';
import { categoryLabel } from '../labels.js';

const RECS = [
  ['ACCEPT', '게재 추천'],
  ['MINOR_REVISION', '경미한 수정'],
  ['MAJOR_REVISION', '대폭 수정'],
  ['REJECT', '게재 불가'],
];

export default function ReviewSubmit() {
  const { assignmentId } = useParams();
  const navigate = useNavigate();
  const [assignment, setAssignment] = useState(null);
  const [paper, setPaper] = useState(null);
  const [rec, setRec] = useState('ACCEPT');
  const [score, setScore] = useState(3);
  const [toAuthor, setToAuthor] = useState('');
  const [toEditor, setToEditor] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    client.get('/reviews/assigned')
      .then(({ data }) => {
        const a = data.find((x) => String(x.id) === String(assignmentId));
        if (!a) { setMissing(true); return; }
        setAssignment(a);
        client.get(`/papers/${a.paperId}`).then((r) => setPaper(r.data)).catch(() => {});
      })
      .catch((err) => setError(apiError(err)));
  }, [assignmentId]);

  const downloadPdf = async () => {
    try {
      const res = await client.get(`/papers/${assignment.paperId}/file`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url; a.download = paper?.fileName || 'paper.pdf'; a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) { setError(apiError(err, '다운로드에 실패했습니다.')); }
  };

  const viewPdf = async () => {
    try {
      await openPdfInNewTab(`/papers/${assignment.paperId}/file`);
    } catch (err) { setError(apiError(err, '파일을 여는 데 실패했습니다.')); }
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      await client.post('/reviews', {
        assignmentId: Number(assignmentId),
        recommendation: rec,
        score: Number(score),
        commentsToAuthor: toAuthor,
        commentsToEditor: toEditor,
      });
      navigate('/reviews');
    } catch (err) {
      setError(apiError(err, '심사 제출에 실패했습니다.'));
    } finally {
      setBusy(false);
    }
  };

  if (missing) {
    return (
      <div className="card">
        <h2>접근할 수 없는 심사</h2>
        <p className="muted">본인에게 배정된 심사만 열람할 수 있습니다.</p>
        <Link className="btn btn-ghost" to="/reviews">심사 목록으로</Link>
      </div>
    );
  }
  if (!assignment) return <p className="muted">불러오는 중…</p>;

  return (
    <div>
      <div className="breadcrumb"><Link to="/reviews">심사 목록</Link> &rsaquo; <span>심사</span></div>

      <article className="card">
        <div className="eyebrow">{paper ? categoryLabel(paper.category) : ''} · 블라인드 테스트</div>
        <h2>{assignment.paperTitle}</h2>
        <p className="muted small">저자 정보는 블라인드 테스트 정책에 따라 가려집니다.</p>
        {paper && (
          <>
            <div className="abstract-box">
              <h3>Abstract</h3>
              <p className="abstract">{paper.abstractText || '초록이 제공되지 않았습니다.'}</p>
            </div>
            {paper.keywords && <p className="muted small"><strong>Keywords:</strong> {paper.keywords}</p>}
            {paper.fileName && (
              <div className="article-actions">
                <button className="btn btn-primary" onClick={viewPdf}>전문 PDF 보기</button>
                <button className="btn btn-ghost" onClick={downloadPdf}>전문 PDF 다운로드</button>
              </div>
            )}
          </>
        )}
      </article>

      {assignment.reviewSubmitted ? (
        <div className="card"><p className="muted">이미 이 논문에 대한 심사를 제출했습니다.</p></div>
      ) : (
        <form className="card form" onSubmit={submit}>
          <h3 style={{ marginTop: 0 }}>심사 의견 제출</h3>
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
            <textarea rows={4} value={toAuthor} onChange={(e) => setToAuthor(e.target.value)} />
          </label>
          <label>편집자에게만 전달할 의견
            <textarea rows={3} value={toEditor} onChange={(e) => setToEditor(e.target.value)} />
          </label>
          <button className="btn btn-primary" disabled={busy}>{busy ? '제출 중…' : '심사 제출'}</button>
        </form>
      )}
    </div>
  );
}
