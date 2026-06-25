import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import StatusBadge from '../components/StatusBadge.jsx';
import { articleCode, longDate, categoryLabel, CATEGORIES } from '../labels.js';

const SIZE = 10;

export default function PaperList() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0, page: 0 });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  // 검색어/분야 변경 시 첫 페이지로
  useEffect(() => { setPage(0); }, [query, category]);

  useEffect(() => {
    const t = setTimeout(() => {
      setLoading(true);
      client.get('/papers/search', { params: { q: query || undefined, category: category || undefined, page, size: SIZE } })
        .then(({ data }) => setData(data))
        .catch((err) => setError(apiError(err)))
        .finally(() => setLoading(false));
    }, 250); // 디바운스
    return () => clearTimeout(t);
  }, [query, category, page]);

  const papers = data.content;

  return (
    <div>
      <section className="journal-hero">
        <h1>Aims &amp; Scope</h1>
        <p className="lead">
          Cantor Journal은 학문 전 분야의 독창적인 연구를 게재하는
          오픈액세스 다학제(multidisciplinary) 동료심사 학술지입니다.
        </p>
        <p className="scope">
          수학·물리학·화학·생명과학·의학·컴퓨터과학·공학·지구환경과학·사회과학·경제경영·
          인문학·예술 및 학제간 연구 등 모든 분야의 원저 논문(Original Research)과 리뷰 논문을
          환영합니다. 모든 투고 논문은 편집진의 검토와 동료심사를 거쳐 게재 여부가 결정됩니다.
        </p>
      </section>

      <div className="section-head">
        <h2>Articles</h2>
        <span className="issue-tag">{data.totalElements}편</span>
      </div>

      <div className="page-head">
        <span className="muted small">{data.totalElements}편 중 {papers.length}편 표시</span>
        <div className="list-controls">
          <select className="search" value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">전체 분야</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <input
            className="search"
            placeholder="제목·저자·키워드·초록 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      {error && <div className="alert error">{error}</div>}
      {loading ? (
        <p className="muted">불러오는 중…</p>
      ) : papers.length === 0 ? (
        <p className="muted">검색 결과가 없습니다.</p>
      ) : (
        <>
          <ul className="toc">
            {papers.map((p) => (
              <li key={p.id} className="toc-item">
                <div className="toc-main">
                  <div className="eyebrow">{categoryLabel(p.category)}</div>
                  <h3 className="article-title">
                    <Link to={`/papers/${p.id}`}>{p.title}</Link>
                  </h3>
                  <div className="authors">{p.authorsText || p.submitter?.name}</div>
                  {p.abstractText && <p className="abstract-excerpt">{p.abstractText}</p>}
                  <div className="toc-citation">
                    {articleCode(p)}
                    {p.issueLabel && <><span className="sep">|</span>{p.issueLabel}{p.pages ? ` pp. ${p.pages}` : ''}</>}
                    <span className="sep">|</span>
                    {longDate(p.createdAt)}
                  </div>
                </div>
                <div className="toc-side">
                  <StatusBadge status={p.status} />
                  <span className="muted small">피인용 {p.citationCount ?? 0}</span>
                  {p.fileName && <Link className="pdf-link" to={`/papers/${p.id}`}>PDF 보기 ›</Link>}
                </div>
              </li>
            ))}
          </ul>

          {data.totalPages > 1 && (
            <div className="pager">
              <button className="btn btn-ghost btn-sm" disabled={page <= 0} onClick={() => setPage(page - 1)}>‹ 이전</button>
              <span className="muted small">{page + 1} / {data.totalPages}</span>
              <button className="btn btn-ghost btn-sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>다음 ›</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
