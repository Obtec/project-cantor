import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client, { apiError } from '../api/client.js';
import { CATEGORIES, ARTICLE_TYPES } from '../labels.js';

export default function SubmitPaper() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', abstractText: '', authorsText: '', keywords: '', category: '', articleType: '' });
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const update = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!file) { setError('논문 PDF 파일을 첨부하세요.'); return; }
    setBusy(true);
    const fd = new FormData();
    fd.append('title', form.title);
    fd.append('abstractText', form.abstractText);
    fd.append('authorsText', form.authorsText);
    fd.append('keywords', form.keywords);
    fd.append('category', form.category);
    fd.append('articleType', form.articleType);
    fd.append('file', file);
    try {
      const { data } = await client.post('/papers', fd);
      navigate(`/papers/${data.id}`);
    } catch (err) {
      setError(apiError(err, '제출에 실패했습니다.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card">
      <h2>논문 제출</h2>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={onSubmit} className="form">
        <label>제목 *
          <input value={form.title} onChange={update('title')} required />
        </label>
        <label>분야 * <span className="muted">(직접 입력 가능)</span>
          <input list="category-suggestions" value={form.category} onChange={update('category')}
            placeholder="예: 수학, 물리학, 컴퓨터과학…" required />
          <datalist id="category-suggestions">
            {CATEGORIES.map((c) => <option key={c} value={c} />)}
          </datalist>
        </label>
        <label>논문 종류 <span className="muted">(직접 입력 가능)</span>
          <input list="type-suggestions" value={form.articleType} onChange={update('articleType')}
            placeholder="예: 원저 논문, 리뷰, 공부 노트…" />
          <datalist id="type-suggestions">
            {ARTICLE_TYPES.map((t) => <option key={t} value={t} />)}
          </datalist>
        </label>
        <label>저자 <span className="muted">(쉼표로 구분)</span>
          <input value={form.authorsText} onChange={update('authorsText')} placeholder="홍길동, 김수학" />
        </label>
        <label>키워드 <span className="muted">(쉼표로 구분)</span>
          <input value={form.keywords} onChange={update('keywords')} placeholder="대수학, 정수론" />
        </label>
        <label>초록
          <textarea rows={8} value={form.abstractText} onChange={update('abstractText')} />
        </label>
        <label>논문 파일 (PDF) *
          <input type="file" accept="application/pdf,.pdf" onChange={(e) => setFile(e.target.files?.[0] || null)} required />
        </label>
        <div className="alert info">
          참고문헌은 업로드한 PDF에서 <strong>자동으로 추출</strong>됩니다. 본문의 참고문헌 목록에
          저널 내 논문 제목이 포함되어 있으면 해당 논문과의 인용 관계가 생성됩니다.
        </div>
        <button className="btn btn-primary" disabled={busy}>{busy ? '제출 중…' : '제출하기'}</button>
      </form>
    </div>
  );
}
