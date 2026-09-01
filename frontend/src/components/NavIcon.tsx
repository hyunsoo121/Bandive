interface Props {
  d1: string;
  d2: string;
  size?: number;
}

export function NavIcon({ d1, d2, size = 20 }: Props) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path d={d1} />
      <path d={d2} />
    </svg>
  );
}
