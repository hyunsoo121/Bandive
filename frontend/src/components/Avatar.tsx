interface Props {
  label: string;
  size?: number;
  color?: string;
  /** 헤딩 폰트 + 굵게 (밴드 이니셜용) */
  heading?: boolean;
  /** 이미지 URL. 있으면 이니셜 대신 사진을 채운다 */
  src?: string | null;
}

/** 정사각 이니셜 아바타 (모서리 각짐, 모더니스트 룩). src 가 있으면 사진. */
export function Avatar({
  label,
  size = 34,
  color = 'var(--color-text)',
  heading = false,
  src = null,
}: Props) {
  return (
    <span
      style={{
        width: size,
        height: size,
        flex: 'none',
        background: color,
        color: '#fff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontFamily: heading ? 'var(--font-heading)' : 'inherit',
        fontWeight: heading ? 800 : 700,
        fontSize: size * 0.4,
        overflow: 'hidden',
      }}
    >
      {src ? (
        <img
          src={src}
          alt={label}
          style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      ) : (
        label
      )}
    </span>
  );
}
