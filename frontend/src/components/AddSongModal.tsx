import { useEffect, useMemo, useRef, useState } from 'react';
import { useApp } from '../store/AppContext';
import { searchTracks } from '../api/songs';
import type { TrackSearchResultDto } from '../api/types';
import { INSTRUMENTS, type Song } from '../types';
import { Modal } from './Modal';
import './AddSongModal.css';

interface Props {
  bandId: string;
  onClose: () => void;
  onSubmitted: () => void;
  /** 있으면 수정 모드 — 제목/아티스트/참고영상/메모/세션 구성을 편집 */
  editing?: Song;
  /** 등록 모드에서 true 면 합주곡으로 바로 등록(관리자가 합주곡 탭에서 열었을 때). 수정 모드에서는 무시 */
  defaultConfirmed?: boolean;
}

type Mode = 'search' | 'manual';

const DEFAULT_SESSIONS: Record<string, number> = { 보컬: 1, 기타: 1, 베이스: 1, 드럼: 1, 건반: 0 };

export function AddSongModal({
  bandId,
  onClose,
  onSubmitted,
  editing,
  defaultConfirmed = false,
}: Props) {
  const { addSong, updateSong } = useApp();
  const isEditing = editing != null;

  const [mode, setMode] = useState<Mode>('search');
  const [q, setQ] = useState('');
  const [results, setResults] = useState<TrackSearchResultDto[]>([]);
  const [searching, setSearching] = useState(false);
  const [pickedId, setPickedId] = useState<string | null>(null);
  const [pickedArt, setPickedArt] = useState<string | null>(null);
  const [title, setTitle] = useState(editing?.title ?? '');
  const [artist, setArtist] = useState(editing?.artist ?? '');
  const [sessions, setSessions] = useState<Record<string, number>>(
    editing ? { ...editing.sessions } : { ...DEFAULT_SESSIONS },
  );
  const [newInst, setNewInst] = useState('');
  const [refUrl, setRefUrl] = useState(editing?.referenceVideoUrl ?? '');
  const [memo, setMemo] = useState(editing?.memo ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reqSeq = useRef(0);

  useEffect(() => {
    if (isEditing || mode !== 'search') return;
    const query = q.trim();
    if (!query) {
      setResults([]);
      setSearching(false);
      return;
    }
    setSearching(true);
    const seq = ++reqSeq.current;
    const t = window.setTimeout(() => {
      searchTracks(query)
        .then((rows) => {
          if (seq === reqSeq.current) setResults(rows);
        })
        .catch(() => {
          if (seq === reqSeq.current) setResults([]);
        })
        .finally(() => {
          if (seq === reqSeq.current) setSearching(false);
        });
    }, 300);
    return () => window.clearTimeout(t);
  }, [q, mode, isEditing]);

  const canSubmit = title.trim().length > 0 && !submitting;

  // 이미 멤버/게스트가 배정된 슬롯 수(악기별) — 수정 모드에서 그만큼 아래로는 줄일 수 없다(서버와 동일한 규칙).
  const assignedCounts = useMemo(() => {
    if (!editing) return {};
    const counts: Record<string, number> = {};
    for (const p of editing.parts) {
      if (p.assigneeId || p.assigneeGuestId) counts[p.instrument] = (counts[p.instrument] ?? 0) + 1;
    }
    return counts;
  }, [editing]);

  const step = (inst: string, delta: number) =>
    setSessions((prev) => {
      const min = assignedCounts[inst] ?? 0;
      return {
        ...prev,
        [inst]: Math.min(10, Math.max(min, (prev[inst] ?? 0) + delta)),
      };
    });

  const removeInst = (inst: string) => {
    if ((assignedCounts[inst] ?? 0) > 0) return; // 배정된 슬롯이 있으면 통째로 지울 수 없음
    setSessions((prev) => {
      const next = { ...prev };
      delete next[inst];
      return next;
    });
  };

  const addInstrument = () => {
    const name = newInst.trim().slice(0, 20);
    if (!name) return;
    setSessions((prev) => ({ ...prev, [name]: prev[name] ?? 1 }));
    setNewInst('');
  };

  const setManualField = (value: string, setter: (v: string) => void) => {
    setter(value);
    setPickedId(null);
    setPickedArt(null);
  };

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      if (editing) {
        await updateSong(editing.id, { title, artist, memo, referenceVideoUrl: refUrl, sessions });
      } else {
        const isSearch = mode === 'search' && pickedId != null;
        await addSong({
          bandId,
          title,
          artist,
          sourceType: isSearch ? 'SEARCH' : 'MANUAL',
          externalTrackId: isSearch ? pickedId : null,
          artworkUrl: isSearch ? pickedArt : null,
          memo,
          referenceVideoUrl: refUrl,
          sessions,
          status: defaultConfirmed ? 'CONFIRMED' : undefined,
        });
      }
      onSubmitted();
    } catch (e: unknown) {
      setError(
        e instanceof Error ? e.message : `곡을 ${editing ? '수정' : '등록'}하지 못했습니다.`,
      );
    } finally {
      setSubmitting(false);
    }
  };

  const isCustom = (inst: string) => !INSTRUMENTS.includes(inst as (typeof INSTRUMENTS)[number]);

  return (
    <Modal
      title={editing ? '곡 수정' : '곡 추가'}
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
            {editing
              ? submitting
                ? '저장 중…'
                : '저장'
              : submitting
                ? '등록 중…'
                : defaultConfirmed
                  ? '합주곡으로 등록'
                  : '위시리스트에 등록'}
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      {editing && (
        <span className="muted" style={{ fontSize: 11 }}>
          제목·아티스트·참고영상·메모·세션 구성을 수정합니다. 이미 배정된 슬롯은 줄일 수 없어요 —
          먼저 배정을 해제해 주세요.
        </span>
      )}
      {!editing && (
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
            onClick={() => {
              setMode('manual');
              setPickedId(null);
              setPickedArt(null);
            }}
          >
            직접 입력
          </button>
        </div>
      )}

      {!editing && mode === 'search' ? (
        <div className="stack" style={{ gap: 8 }}>
          <input
            className="input"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="곡 제목 또는 아티스트 검색"
            autoFocus
          />
          <span className="muted" style={{ fontSize: 10 }}>
            외부 음원 API 검색 결과
          </span>
          <div className="addsong__results">
            {searching && (
              <span className="muted" style={{ fontSize: 12, padding: '8px 0' }}>
                검색 중…
              </span>
            )}
            {!searching &&
              results.map((c) => {
                const on = pickedId === c.externalTrackId;
                return (
                  <button
                    key={c.externalTrackId}
                    type="button"
                    className={`addsong__result${on ? ' is-on' : ''}`}
                    onClick={() => {
                      setPickedId(c.externalTrackId);
                      setPickedArt(c.artworkUrl);
                      setTitle(c.title);
                      setArtist(c.artist);
                    }}
                  >
                    {c.artworkUrl ? (
                      <img
                        className="addsong__result-art"
                        src={c.artworkUrl}
                        alt=""
                        loading="lazy"
                      />
                    ) : (
                      <span className="addsong__result-art addsong__result-art--empty" />
                    )}
                    <span className="stack" style={{ flex: 1, minWidth: 0 }}>
                      <strong style={{ fontSize: 13 }}>{c.title}</strong>
                      <span className="muted" style={{ fontSize: 11 }}>
                        {c.artist}
                      </span>
                    </span>
                    {on && (
                      <span
                        style={{ fontSize: 10, fontWeight: 700, color: 'var(--color-accent-700)' }}
                      >
                        선택됨
                      </span>
                    )}
                  </button>
                );
              })}
            {!searching && q.trim().length > 0 && results.length === 0 && (
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
              onChange={(e) => setManualField(e.target.value, setTitle)}
              placeholder="예: ring ring ring"
            />
          </div>
          <div className="field">
            <label htmlFor="song-artist">아티스트</label>
            <input
              id="song-artist"
              className="input"
              value={artist}
              onChange={(e) => setManualField(e.target.value, setArtist)}
              placeholder="예: 설"
            />
          </div>
        </div>
      )}

      <div
        className="stack"
        style={{ gap: 8, borderTop: '2px solid var(--color-text)', paddingTop: 14 }}
      >
        <span className="kicker">세션 구성</span>
        {Object.keys(sessions).map((inst) => {
          const assigned = assignedCounts[inst] ?? 0;
          return (
            <div key={inst} className="addsong__session">
              <span
                style={{
                  flex: 1,
                  fontSize: 13,
                  color: sessions[inst] > 0 ? undefined : 'var(--color-neutral-400)',
                }}
              >
                {inst}
                {assigned > 0 && (
                  <span className="muted" style={{ fontSize: 10, marginLeft: 6 }}>
                    배정 {assigned}명
                  </span>
                )}
              </span>
              {isCustom(inst) && (
                <button
                  type="button"
                  className="addsong__step addsong__step--x"
                  onClick={() => removeInst(inst)}
                  disabled={assigned > 0}
                  aria-label={`${inst} 삭제`}
                >
                  ✕
                </button>
              )}
              <button
                type="button"
                className="addsong__step"
                onClick={() => step(inst, -1)}
                disabled={(sessions[inst] ?? 0) <= assigned}
                aria-label={`${inst} 감소`}
              >
                −
              </button>
              <span className="addsong__count">{sessions[inst]}</span>
              <button
                type="button"
                className="addsong__step"
                onClick={() => step(inst, 1)}
                aria-label={`${inst} 증가`}
              >
                ＋
              </button>
            </div>
          );
        })}
        <div className="addsong__session">
          <input
            className="input"
            style={{ flex: 1, fontSize: 12, padding: '5px 8px' }}
            value={newInst}
            maxLength={20}
            onChange={(e) => setNewInst(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                addInstrument();
              }
            }}
            placeholder="악기 직접 추가 (예: 실로폰)"
          />
          <button
            type="button"
            className="btn btn--sm"
            onClick={addInstrument}
            disabled={!newInst.trim()}
          >
            추가
          </button>
        </div>
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
      {error && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{error}</span>}
      {!editing && (
        <span className="muted" style={{ fontSize: 11 }}>
          {defaultConfirmed
            ? '합주곡으로 바로 등록되고, 파트는 지금 바로 배정할 수 있습니다.'
            : '등록하면 위시리스트에 올라가고, 파트 배정은 합주곡 승격 후에 지정합니다.'}
        </span>
      )}
    </Modal>
  );
}
