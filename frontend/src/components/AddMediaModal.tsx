import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { KIND_LABEL, toUi } from '../lib/schedule';
import type { MediaKind, ScheduleEvent, Visibility } from '../types';
import { Modal } from './Modal';

interface Props {
  bandId: string;
  schedules: ScheduleEvent[];
  onClose: () => void;
  onSubmitted: () => void;
}

const KINDS: MediaKind[] = ['합주', '공연'];
const SCOPES: Visibility[] = ['멤버만', '링크 공개'];

export function AddMediaModal({ bandId, schedules, onClose, onSubmitted }: Props) {
  const { addMedia } = useApp();

  const [url, setUrl] = useState('');
  const [kind, setKind] = useState<MediaKind>('합주');
  const [visibility, setVisibility] = useState<Visibility>('멤버만');
  const [scheduleId, setScheduleId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = /^https?:\/\/.+/.test(url.trim()) && !submitting;

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      await addMedia({
        bandId,
        url,
        kind,
        visibility,
        scheduleId: scheduleId || null,
      });
      onSubmitted();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '영상을 등록하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="영상 첨부"
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
            {submitting ? '등록 중…' : '영상 등록'}
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
        <span className="muted" style={{ fontSize: 11 }}>
          유튜브·구글드라이브 링크는 자동으로 구분됩니다. 제목은 따로 저장하지 않습니다.
        </span>
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

      {error && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{error}</span>}
    </Modal>
  );
}
