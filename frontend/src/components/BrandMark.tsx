interface Props {
  size?: number;
  /** 로고 옆에 워드마크 표시 */
  wordmark?: boolean;
}

/** 밴디브 로고 (블록 세리프 'ㅂ' 모티프) */
export function BrandMark({ size = 24, wordmark = false }: Props) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <svg width={size} height={size} viewBox="0 0 44 44" aria-label="밴디브 로고">
        <rect width="44" height="44" fill="#201e1d" />
        <rect x="9" y="8" width="6" height="28" fill="#f3f2f2" />
        <rect x="15" y="8" width="12" height="5" fill="#f3f2f2" />
        <rect x="15" y="19" width="14" height="5" fill="#f3f2f2" />
        <rect x="15" y="31" width="14" height="5" fill="#f3f2f2" />
        <rect x="27" y="8" width="5" height="16" fill="#ec3013" />
        <rect x="29" y="19" width="5" height="17" fill="#ec3013" />
      </svg>
      {wordmark && (
        <span
          style={{
            fontFamily: 'var(--font-heading)',
            fontWeight: 800,
            fontSize: size * 0.66,
            letterSpacing: '-0.01em',
          }}
        >
          밴디브
        </span>
      )}
    </span>
  );
}
