// 사과나무 로고. size로 크기 조절.
export default function AppleTreeLogo({ size = 36 }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      role="img"
      aria-label="Cantor Journal 사과나무 로고"
    >
      {/* 수관 (잎) */}
      <circle cx="32" cy="22" r="14" fill="#3fae5a" />
      <circle cx="20" cy="26" r="9" fill="#47c065" />
      <circle cx="44" cy="26" r="9" fill="#47c065" />
      <circle cx="32" cy="16" r="9" fill="#5bd07a" />
      {/* 줄기 */}
      <rect x="29.5" y="34" width="5" height="20" rx="2.5" fill="#7a4a23" />
      {/* 가지 */}
      <path d="M32 40 C24 38 22 44 18 46" stroke="#7a4a23" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      <path d="M32 40 C40 38 42 44 46 46" stroke="#7a4a23" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      {/* 사과들 */}
      <circle cx="22" cy="22" r="3.4" fill="#e23b3b" />
      <circle cx="42" cy="24" r="3.4" fill="#e23b3b" />
      <circle cx="33" cy="28" r="3.4" fill="#e23b3b" />
      <circle cx="30" cy="14" r="3.0" fill="#f25555" />
      {/* 잎 하이라이트 */}
      <circle cx="27" cy="18" r="1.4" fill="#bff0c9" />
    </svg>
  );
}
