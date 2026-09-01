import { INSTRUMENTS, type Song } from '../types';

export interface PartSlot {
  /** assignments 맵의 키. 예: "기타#2" */
  key: string;
  /** 화면 표시 라벨. 예: "기타 2" (같은 악기 1개면 "기타") */
  label: string;
}

/** 곡의 세션 구성(악기별 인원)으로부터 파트 슬롯 목록 생성 */
export function slotsOf(song: Song): PartSlot[] {
  const out: PartSlot[] = [];
  for (const inst of INSTRUMENTS) {
    const n = song.sessions[inst] ?? 0;
    for (let i = 1; i <= n; i++) {
      out.push({ key: `${inst}#${i}`, label: n > 1 ? `${inst} ${i}` : inst });
    }
  }
  return out;
}

/** "보컬 1", "기타 2" 형태의 세션 구성 칩 텍스트 */
export function sessionChips(song: Song): string[] {
  return INSTRUMENTS.filter((i) => (song.sessions[i] ?? 0) > 0).map(
    (i) => `${i} ${song.sessions[i]}`,
  );
}
