export const STATUS_LABELS = {
  SUBMITTED: '제출됨',
  UNDER_REVIEW: '심사중',
  REVISION_REQUESTED: '수정요청',
  ACCEPTED: '게재확정',
  REJECTED: '반려',
  PUBLISHED: '게재',
};

export const RECOMMENDATION_LABELS = {
  ACCEPT: '게재 추천',
  MINOR_REVISION: '경미한 수정',
  MAJOR_REVISION: '대폭 수정',
  REJECT: '게재 불가',
};

export const ROLE_LABELS = {
  AUTHOR: '저자',
  REVIEWER: '리뷰어',
  EDITOR: '편집자',
  ADMIN: '관리자',
};

export function statusLabel(s) {
  return STATUS_LABELS[s] || s;
}

export function recommendationLabel(r) {
  return RECOMMENDATION_LABELS[r] || r;
}

export function formatDate(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

// 학술지 스타일 메타데이터 헬퍼 (저널 권/호/논문번호는 발행 단계 도입 전까지 표시용으로 생성)
export const JOURNAL_NAME = 'Cantor Journal';
export const JOURNAL_ABBR = 'Cantor J.';

// 다학제 저널: 투고 가능한 분야 목록
export const CATEGORIES = [
  '수학',
  '물리학',
  '화학',
  '생명과학',
  '의학·약학',
  '컴퓨터과학',
  '공학',
  '지구·환경과학',
  '사회과학',
  '경제·경영',
  '인문학',
  '예술·디자인',
  '학제간연구',
  '기타',
];

export function categoryLabel(c) {
  return c && c.trim() ? c : '미분류';
}

// 논문 종류 추천 목록(자유 입력 가능)
export const ARTICLE_TYPES = [
  '원저 논문',
  '리뷰 논문',
  '단신 (Short Communication)',
  '공부 노트',
  '에세이',
  '기타',
];

export function articleTypeLabel(t) {
  return t && t.trim() ? t : 'Original Research';
}

export function year(iso) {
  if (!iso) return new Date().getFullYear();
  try { return new Date(iso).getFullYear(); } catch { return new Date().getFullYear(); }
}

// 논문 식별자: 서버가 부여한 영구 코드 우선, 없으면 생성(CJ-2026-0001)
export function articleCode(paper) {
  if (paper?.articleCode) return paper.articleCode;
  const y = year(paper?.createdAt);
  const n = String(paper?.id ?? 0).padStart(4, '0');
  return `CJ-${y}-${n}`;
}

export function longDate(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
  } catch {
    return iso;
  }
}

// 인용 문자열: 저자 (연도). 제목. Cantor Journal, CJ-2026-0001.
export function citation(paper) {
  const authors = paper?.authorsText || paper?.submitter?.name || '';
  const y = year(paper?.createdAt);
  const title = paper?.title || '';
  return `${authors} (${y}). ${title}. ${JOURNAL_NAME}, ${articleCode(paper)}.`;
}

// BibTeX 항목 (@article)
export function bibtex(paper) {
  const authors = (paper?.authorsText || paper?.submitter?.name || '')
    .split(/[,;]/).map((s) => s.trim()).filter(Boolean).join(' and ');
  const y = year(paper?.createdAt);
  const key = articleCode(paper);
  return [
    `@article{${key},`,
    `  title   = {${paper?.title || ''}},`,
    `  author  = {${authors}},`,
    `  journal = {${JOURNAL_NAME}},`,
    `  year    = {${y}},`,
    paper?.keywords ? `  keywords = {${paper.keywords}},` : null,
    `  note    = {${key}}`,
    `}`,
  ].filter(Boolean).join('\n');
}
