import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';
import CitationTree from '../components/CitationTree.jsx';
import { recommendationLabel, longDate, articleCode, citation, bibtex, categoryLabel, articleTypeLabel } from '../labels.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function PaperDetail() {
  const { id } = useParams();
  const { hasRole, user } = useAuth();
  const [paper, setPaper] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [versions, setVersions] = useState([]);
  const [resub, setResub] = useState({ file: null, response: '' });
  const [resubBusy, setResubBusy] = useState(false);
  const [requests, setRequests] = useState([]);
  const [reqForm, setReqForm] = useState({ type: 'EDIT', message: '' });
  const [error, setError] = useState('');
  const [copied, setCopied] = useState('');

  const reload = () => client.get(`/papers/${id}`).then(({ data }) => setPaper(data)).catch(() => {});

  useEffect(() => {
    client.get(`/papers/${id}`)
      .then(({ data }) => setPaper(data))
      .catch((err) => setError(apiError(err)));
  }, [id]);

  const isOwner = user && paper && paper.submitter?.id === user.id;

  useEffect(() => {
    if (!paper) return;
    const owner = user && paper.submitter?.id === user.id;
    if (hasRole('EDITOR') || owner) {
      client.get(`/papers/${id}/reviews`)
        .then(({ data }) => setReviews(data))
        .catch(() => {});
      client.get(`/papers/${id}/versions`)
        .then(({ data }) => setVersions(data))
        .catch(() => {});
      client.get(`/papers/${id}/requests`)
        .then(({ data }) => setRequests(data))
        .catch(() => {});
    }
  }, [paper, user, id, hasRole]);

  const submitRequest = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await client.post(`/papers/${id}/requests`, reqForm);
      setReqForm({ type: 'EDIT', message: '' });
      const { data } = await client.get(`/papers/${id}/requests`);
      setRequests(data);
    } catch (err) {
      setError(apiError(err, '요청 전송에 실패했습니다.'));
    }
  };

  const downloadVersion = async (versionNo) => {
    try {
      const res = await client.get(`/papers/${id}/versions/${versionNo}/file`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url; a.download = `v${versionNo}.pdf`; a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) { setError(apiError(err, '다운로드에 실패했습니다.')); }
  };

  const doResubmit = async (e) => {
    e.preventDefault();
    if (!resub.file) { setError('수정본 PDF를 첨부하세요.'); return; }
    setResubBusy(true); setError('');
    const fd = new FormData();
    fd.append('file', resub.file);
    fd.append('responseToReviewers', resub.response);
    try {
      await client.post(`/papers/${id}/resubmit`, fd);
      setResub({ file: null, response: '' });
      await reload();
    } catch (err) {
      setError(apiError(err, '재제출에 실패했습니다.'));
    } finally {
      setResubBusy(false);
    }
  };

  const onDownload = async () => {
    try {
      const res = await client.get(`/papers/${id}/file`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = paper?.fileName || 'paper.pdf';
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(apiError(err, '다운로드에 실패했습니다.'));
    }
  };

  const copyText = async (text, which) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(which);
      setTimeout(() => setCopied(''), 1500);
    } catch { /* noop */ }
  };

  if (error) return <div className="alert error">{error}</div>;
  if (!paper) return <p className="muted">불러오는 중…</p>;

  const keywordList = (paper.keywords || '')
    .split(/[,;]/).map((k) => k.trim()).filter(Boolean);

  return (
    <div>
      <div className="breadcrumb">
        <Link to="/">Articles</Link> &rsaquo; <span>{articleCode(paper)}</span>
      </div>

      <div className="article-layout">
        <article className="article-main">
          <header className="article-header">
            <div className="eyebrow">{categoryLabel(paper.category)} · {articleTypeLabel(paper.articleType)}</div>
            <h1>{paper.title}</h1>
            <div className="article-authors">{paper.authorsText || paper.submitter?.name}</div>
            {paper.submitter && (
              <div className="article-affil">
                교신저자: <Link to={`/authors/${paper.submitter.id}`}>{paper.submitter.name}</Link>
              </div>
            )}
          </header>

          <div className="abstract-box">
            <h3>Abstract</h3>
            <p className="abstract">{paper.abstractText || '초록이 제공되지 않았습니다.'}</p>
          </div>

          {keywordList.length > 0 && (
            <div className="keywords">
              <span className="kw-label">Keywords</span>
              {keywordList.map((k) => <span key={k} className="kw-tag">{k}</span>)}
            </div>
          )}

          {paper.decisionNote && (
            <div className="alert info">
              <strong>편집 결정 메모:</strong> {paper.decisionNote}
            </div>
          )}

          <div className="cite-box">
            <h3>How to cite</h3>
            <div className="cite-text">{citation(paper)}</div>
            <div className="article-actions" style={{ margin: '0.8rem 0 0' }}>
              <button className="btn btn-ghost" onClick={() => copyText(citation(paper), 'cite')}>
                {copied === 'cite' ? '복사됨 ✓' : '인용 복사'}
              </button>
            </div>
          </div>

          <div className="cite-box">
            <h3>BibTeX</h3>
            <pre className="bibtex">{bibtex(paper)}</pre>
            <div className="article-actions" style={{ margin: '0.8rem 0 0' }}>
              <button className="btn btn-ghost" onClick={() => copyText(bibtex(paper), 'bib')}>
                {copied === 'bib' ? '복사됨 ✓' : 'BibTeX 복사'}
              </button>
              <a className="btn btn-ghost"
                 href={`${import.meta.env.VITE_API_BASE || '/api'}/papers/${id}/cite.ris`}>
                RIS 내보내기
              </a>
            </div>
          </div>

          <div className="article-actions">
            {paper.fileName && (
              <button className="btn btn-primary" onClick={onDownload}>
                전문 PDF 다운로드
              </button>
            )}
          </div>

          <section className="reviews">
            <div className="section-head"><h2>인용 관계 (Citation Tree)</h2></div>
            <CitationTree paperId={paper.id} />
          </section>

          {(hasRole('EDITOR') || isOwner) && (
            <section className="reviews">
              <div className="section-head"><h2>Peer Review ({reviews.length})</h2></div>
              {reviews.length === 0 ? (
                <p className="muted">
                  {isOwner && !hasRole('EDITOR')
                    ? '편집 결정이 내려지면 심사 의견이 공개됩니다.'
                    : '아직 제출된 심사가 없습니다.'}
                </p>
              ) : (
                reviews.map((r) => (
                  <div key={r.id} className="review-card">
                    <div className="review-head">
                      <strong>{r.reviewer?.name}</strong>
                      <span className="badge">{recommendationLabel(r.recommendation)}</span>
                      {r.score != null && <span className="muted small">점수 {r.score}/5</span>}
                    </div>
                    {r.commentsToAuthor && <p><strong>저자에게:</strong> {r.commentsToAuthor}</p>}
                    {r.commentsToEditor && <p className="muted"><strong>편집자에게:</strong> {r.commentsToEditor}</p>}
                  </div>
                ))
              )}
            </section>
          )}

          {(hasRole('EDITOR') || isOwner) && versions.length > 0 && (
            <section className="reviews">
              <div className="section-head"><h2>원고 버전 이력</h2></div>
              <ul className="version-list">
                {versions.map((v) => (
                  <li key={v.versionNo} className="version-item">
                    <div>
                      <strong>v{v.versionNo}</strong>{' '}
                      <span className="muted small">{longDate(v.createdAt)}</span>
                      {v.responseToReviewers && (
                        <div className="muted small">응답서: {v.responseToReviewers}</div>
                      )}
                    </div>
                    <button className="btn btn-ghost btn-sm" onClick={() => downloadVersion(v.versionNo)}>
                      PDF
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {isOwner && paper.status === 'REVISION_REQUESTED' && (
            <section className="reviews">
              <div className="section-head"><h2>수정본 재제출</h2></div>
              <form className="form" onSubmit={doResubmit}>
                <label>리뷰어 의견에 대한 응답서
                  <textarea rows={4} value={resub.response}
                    onChange={(e) => setResub({ ...resub, response: e.target.value })} />
                </label>
                <label>수정본 PDF *
                  <input type="file" accept="application/pdf,.pdf"
                    onChange={(e) => setResub({ ...resub, file: e.target.files?.[0] || null })} required />
                </label>
                <button className="btn btn-primary" disabled={resubBusy}>
                  {resubBusy ? '재제출 중…' : '재제출'}
                </button>
              </form>
            </section>
          )}

          {isOwner && (
            <section className="reviews">
              <div className="section-head"><h2>편집장에게 요청</h2></div>
              {requests.length > 0 && (
                <ul className="request-list">
                  {requests.map((r) => (
                    <li key={r.id} className="request-item">
                      <span className={`badge ${r.status === 'PENDING' ? 'status-SUBMITTED' : r.status === 'RESOLVED' ? 'status-PUBLISHED' : 'status-REJECTED'}`}>
                        {r.type === 'EDIT' ? '수정' : '삭제'} · {r.status === 'PENDING' ? '대기' : r.status === 'RESOLVED' ? '처리됨' : '거절됨'}
                      </span>
                      <div className="request-body">
                        <div>{r.message}</div>
                        {r.editorNote && <div className="muted small">편집장: {r.editorNote}</div>}
                        <div className="muted small">{longDate(r.createdAt)}</div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
              <form className="form" onSubmit={submitRequest}>
                <label>요청 유형
                  <select value={reqForm.type} onChange={(e) => setReqForm({ ...reqForm, type: e.target.value })}>
                    <option value="EDIT">수정 요청</option>
                    <option value="DELETE">삭제 요청</option>
                  </select>
                </label>
                <label>내용
                  <textarea rows={3} value={reqForm.message}
                    onChange={(e) => setReqForm({ ...reqForm, message: e.target.value })}
                    placeholder="수정/삭제가 필요한 사유를 적어주세요." />
                </label>
                <button className="btn btn-primary">요청 보내기</button>
              </form>
            </section>
          )}
        </article>

        <aside className="article-aside">
          <div className="aside-card">
            <h4>Article Info</h4>
            <div className="meta-row"><span className="k">상태</span><span className="v"><StatusBadge status={paper.status} /></span></div>
            <div className="meta-row"><span className="k">피인용수</span><span className="v">{paper.citationCount ?? 0}</span></div>
            <div className="meta-row"><span className="k">논문 번호</span><span className="v">{articleCode(paper)}</span></div>
            <div className="meta-row"><span className="k">분야</span><span className="v">{categoryLabel(paper.category)}</span></div>
            <div className="meta-row"><span className="k">유형</span><span className="v">{articleTypeLabel(paper.articleType)}</span></div>
            {paper.issueLabel && (
              <div className="meta-row"><span className="k">수록</span><span className="v">{paper.issueLabel}</span></div>
            )}
            {paper.pages && (
              <div className="meta-row"><span className="k">페이지</span><span className="v">pp. {paper.pages}</span></div>
            )}
            {paper.publishedAt && (
              <div className="meta-row"><span className="k">발행일</span><span className="v">{longDate(paper.publishedAt)}</span></div>
            )}
            <div className="meta-row"><span className="k">투고일</span><span className="v">{longDate(paper.createdAt)}</span></div>
            <div className="meta-row"><span className="k">최종수정</span><span className="v">{longDate(paper.updatedAt)}</span></div>
            <div className="meta-row"><span className="k">라이선스</span><span className="v">CC BY 4.0</span></div>
          </div>
          {paper.fileName && (
            <div className="aside-card">
              <h4>Download</h4>
              <div className="meta-row"><span className="k">파일</span><span className="v small">{paper.fileName}</span></div>
              <button className="btn btn-primary" style={{ width: '100%', marginTop: '0.6rem' }} onClick={onDownload}>
                PDF
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
