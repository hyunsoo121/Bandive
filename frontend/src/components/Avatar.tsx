interface Props {
  label: string;
  size?: number;
  color?: string;
  /** 헤딩 폰트 + 굵게 (밴드 이니셜용) */
  heading?: boolean;
}

/** 정사각 이니셜 아바타 (모서리 각짐, 모더니스트 룩) */
export function Avatar({ label, size = 34, color = 'var(--color-text)', heading = false }: Props) {
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
      }}
    >
      {label}
    </span>
  );
}
