import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { KIND_LABEL, toUi } from '../lib/schedule';
import type { MediaItem, MediaKind, ScheduleEvent, Visibility } from '../types';
import { Modal } from './Modal';

interface Props {
  bandId: string;
  schedules: ScheduleEvent[];
  /** 있으면 수정 모드 — 이 영상의 값을 채우고 PATCH 로 저장 */
  editing?: MediaItem;
  onClose: () => void;
  onSubmitted: () => void;
}

const KINDS: MediaKind[] = ['합주', '공연'];
const SCOPES: Visibility[] = ['멤버만', '전체공개'];
const YT_OR_DRIVE = /(youtube\.com|youtu\.be|drive\.google\.com|docs\.google\.com)/i;

export function AddMediaModal({ bandId, schedules, editing, onClose, onSubmitted }: Props) {
  const { addMedia, editMedia, songs } = useApp();
  const confirmedSongs = songs.filter((s) => s.bandId === bandId && s.status === 'CONFIRMED');

  const [url, setUrl] = useState(editing?.url ?? '');
  const [title, setTitle] = useState(editing?.rawTitle ?? '');
  const [kind, setKind] = useState<MediaKind>(editing?.kind ?? '합주');
  const [visibility, setVisibility] = useState<Visibility>(editing?.visibility ?? '멤버만');
  const [scheduleId, setScheduleId] = useState(editing?.scheduleId ?? '');
  const [songId, setSongId] = useState(editing?.songId ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const trimmed = url.trim();
  const isValidUrl = /^https?:\/\/.+/.test(trimmed);
  const suspicious = isValidUrl && !YT_OR_DRIVE.test(trimmed);
  const canSubmit = isValidUrl && !submitting;

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        url,
        title,
        kind,
        visibility,
        scheduleId: scheduleId || null,
        songId: songId || null,
      };
      if (editing) await editMedia(editing.id, payload);
      else await addMedia({ bandId, ...payload });
      onSubmitted();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '영상을 저장하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={editing ? '영상 수정' : '영상 첨부'}
      width={400}
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
            {submitting ? '저장 중…' : editing ? '수정 저장' : '영상 등록'}
          </button>
          <button type="button" className="btn" onClick={onClose}>
            취소
          </button>
        </>
      }
    >
      <div className="field">
        <label htmlFor="media-url">영상 URL</label>
        <input
          id="media-url"
          className="input"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://youtu.be/… 또는 https://drive.google.com/…"
          autoFocus
        />
        {suspicious ? (
          <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>
            유튜브·구글드라이브 링크가 아닌 것 같습니다. 그래도 첨부할 수 있지만 썸네일은 표시되지
            않습니다.
          </span>
        ) : (
          <span className="muted" style={{ fontSize: 11 }}>
            유튜브·구글드라이브 링크는 자동으로 구분되고 썸네일이 표시됩니다.
          </span>
        )}
      </div>

      <div className="field">
        <label htmlFor="media-title">제목 · 선택</label>
        <input
          id="media-title"
          className="input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 5월 정기공연 - 첫 곡"
          maxLength={200}
        />
      </div>

      <div className="field">
        <label>구분</label>
        <div className="seg">
          {KINDS.map((k) => (
            <button
              key={k}
              type="button"
              className={`seg__opt${kind === k ? ' seg__opt--on' : ''}`}
              onClick={() => setKind(k)}
            >
              {k}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <label>공개 범위</label>
        <div className="seg">
          {SCOPES.map((s) => (
            <button
              key={s}
              type="button"
              className={`seg__opt${visibility === s ? ' seg__opt--on' : ''}`}
              onClick={() => setVisibility(s)}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <label htmlFor="media-event">연결할 일정 · 선택</label>
        <select
          id="media-event"
          className="input"
          value={scheduleId}
          onChange={(e) => setScheduleId(e.target.value)}
        >
          <option value="">연결 안 함</option>
          {schedules.map((ev) => {
            const u = toUi(ev);
            return (
              <option key={ev.id} value={ev.id}>
                {u.month + 1}/{u.day} {KIND_LABEL[ev.type]}
                {ev.location ? ` · ${ev.location}` : ''}
              </option>
            );
          })}
        </select>
        <span className="muted" style={{ fontSize: 11 }}>
          연결하면 캘린더의 해당 일정 상세에 이 영상이 함께 표시됩니다.
        </span>
      </div>

      <div className="field">
        <label htmlFor="media-song">연결할 곡 · 선택</label>
        <select
          id="media-song"
          className="input"
          value={songId}
          onChange={(e) => setSongId(e.target.value)}
        >
          <option value="">연결 안 함</option>
          {confirmedSongs.map((s) => (
            <option key={s.id} value={s.id}>
              {s.title}
              {s.artist ? ` · ${s.artist}` : ''}
            </option>
          ))}
        </select>
        <span className="muted" style={{ fontSize: 11 }}>
          합주곡 리스트에 있는 곡만 연결할 수 있습니다.
          {confirmedSongs.length === 0 && ' (아직 합주곡이 없습니다)'}
        </span>
      </div>

      {error && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{error}</span>}
    </Modal>
  );
}
