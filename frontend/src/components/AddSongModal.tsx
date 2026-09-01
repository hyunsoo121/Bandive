import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { INSTRUMENTS, type Instrument } from '../types';
import { SONG_CATALOG } from '../mock/data';
import { Modal } from './Modal';
import './AddSongModal.css';

interface Props {
  bandId: string;
  onClose: () => void;
  onSubmitted: () => void;
}

type Mode = 'search' | 'manual';

const DEFAULT_SESSIONS: Record<Instrument, number> = {
  보컬: 1,
  기타: 1,
  베이스: 1,
  드럼: 1,
  건반: 0,
};

export function AddSongModal({ bandId, onClose, onSubmitted }: Props) {
  const { addSong } = useApp();

  const [mode, setMode] = useState<Mode>('search');
  const [q, setQ] = useState('');
  const [pickedTitle, setPickedTitle] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [artist, setArtist] = useState('');
  const [sessions, setSessions] = useState<Record<Instrument, number>>(DEFAULT_SESSIONS);
  const [refUrl, setRefUrl] = useState('');
  const [memo, setMemo] = useState('');

  const query = q.trim().toLowerCase();
  const results = (query
    ? SONG_CATALOG.filter((c) => `${c.title} ${c.artist}`.toLowerCase().includes(query))
    : SONG_CATALOG
  ).slice(0, 5);

  const canSubmit = title.trim().length > 0 && artist.trim().length > 0;

  const step = (inst: Instrument, delta: number) =>
    setSessions((prev) => ({ ...prev, [inst]: Math.min(4, Math.max(0, prev[inst] + delta)) }));

  const submit = () => {
    if (!canSubmit) return;
    addSong({
      bandId,
      title,
      artist,
      sourceType: mode === 'search' ? 'SEARCH' : 'MANUAL',
      memo,
      referenceVideoUrl: refUrl,
      sessions,
    });
    onSubmitted();
  };

  return (
    <Modal
      title="곡 추가"
      width={420}
      onClose={onClose}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            disabled={!canSubmit}
            onClick={submit}
          >
            위시리스트에 등록
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="seg">
        <button
          type="button"
          className={`seg__opt${mode === 'search' ? ' seg__opt--on' : ''}`}
          onClick={() => setMode('search')}
        >
          곡 검색
        </button>
        <button
          type="button"
          className={`seg__opt${mode === 'manual' ? ' seg__opt--on' : ''}`}
          onClick={() => setMode('manual')}
        >
          직접 입력
        </button>
      </div>

      {mode === 'search' ? (
        <div className="stack" style={{ gap: 8 }}>
          <input
            className="input"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="곡 제목 또는 아티스트 검색"
            autoFocus
          />
          <span className="muted" style={{ fontSize: 10 }}>
            외부 음원 API(Spotify 등) 검색 결과 · 목업 단계에선 샘플 카탈로그
          </span>
          <div className="addsong__results">
            {results.map((c) => {
              const on = pickedTitle === c.title;
              return (
                <button
                  key={`${c.title}-${c.artist}`}
                  type="button"
                  className={`addsong__result${on ? ' is-on' : ''}`}
                  onClick={() => {
                    setPickedTitle(c.title);
                    setTitle(c.title);
                    setArtist(c.artist);
                  }}
                >
                  <span className="addsong__result-art" />
                  <span className="stack" style={{ flex: 1, minWidth: 0 }}>
                    <strong style={{ fontSize: 13 }}>{c.title}</strong>
                    <span className="muted" style={{ fontSize: 11 }}>
                      {c.artist}
                    </span>
                  </span>
                  {on && (
                    <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-accent-700)' }}>
                      선택됨
                    </span>
                  )}
                </button>
              );
            })}
            {results.length === 0 && (
              <span className="muted" style={{ fontSize: 12, padding: '8px 0' }}>
                검색 결과가 없습니다. 직접 입력으로 등록해 주세요.
              </span>
            )}
          </div>
        </div>
      ) : (
        <div className="stack" style={{ gap: 12 }}>
          <div className="field">
            <label htmlFor="song-title">곡 제목</label>
            <input
              id="song-title"
              className="input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: ring ring ring"
            />
          </div>
          <div className="field">
            <label htmlFor="song-artist">아티스트</label>
            <input
              id="song-artist"
              className="input"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              placeholder="예: 설"
            />
          </div>
        </div>
      )}

      <div className="stack" style={{ gap: 8, borderTop: '2px solid var(--color-text)', paddingTop: 14 }}>
        <span className="kicker">세션 구성</span>
        {INSTRUMENTS.map((inst) => (
          <div key={inst} className="addsong__session">
            <span style={{ flex: 1, fontSize: 13, color: sessions[inst] > 0 ? undefined : 'var(--color-neutral-400)' }}>
              {inst}
            </span>
            <button type="button" className="addsong__step" onClick={() => step(inst, -1)} aria-label={`${inst} 감소`}>
              −
            </button>
            <span className="addsong__count">{sessions[inst]}</span>
            <button type="button" className="addsong__step" onClick={() => step(inst, 1)} aria-label={`${inst} 증가`}>
              ＋
            </button>
          </div>
        ))}
      </div>

      <div className="field">
        <label htmlFor="song-ref">참고 영상</label>
        <input
          id="song-ref"
          className="input"
          value={refUrl}
          onChange={(e) => setRefUrl(e.target.value)}
          placeholder="유튜브 URL (선택)"
        />
      </div>
      <div className="field">
        <label htmlFor="song-memo">비고 / 메모</label>
        <textarea
          id="song-memo"
          className="input"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          placeholder="키, 편곡 방향, 준비물 등 (선택)"
        />
      </div>
      <span className="muted" style={{ fontSize: 11 }}>
        등록하면 위시리스트에 올라가고, 파트 배정은 합주곡 승격 후에 지정합니다.
      </span>
    </Modal>
  );
}
