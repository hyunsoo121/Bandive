import { useState } from 'react';
import { useApp } from '../store/AppContext';
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
  const [title, setTitle] = useState('');
  const [kind, setKind] = useState<MediaKind>('합주');
  const [visibility, setVisibility] = useState<Visibility>('멤버만');
  const [scheduleDay, setScheduleDay] = useState('');

  const canSubmit = url.trim().length > 0 && title.trim().length > 0;

  const submit = () => {
    if (!canSubmit) return;
    addMedia({
      bandId,
      url,
      title,
      kind,
      visibility,
      scheduleDay: scheduleDay ? Number(scheduleDay) : null,
    });
    onSubmitted();
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
            영상 등록
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
          placeholder="youtu.be/… 또는 drive.google.com/…"
          autoFocus
        />
      </div>

      <div className="field">
        <label htmlFor="media-title">제목</label>
        <input
          id="media-title"
          className="input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="예: 8/29 정기 합주 풀영상"
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
          value={scheduleDay}
          onChange={(e) => setScheduleDay(e.target.value)}
        >
          <option value="">연결 안 함</option>
          {schedules.map((ev) => (
            <option key={ev.id} value={String(ev.day)}>
              8/{ev.day} {ev.title}
            </option>
          ))}
        </select>
        <span className="muted" style={{ fontSize: 11 }}>
          연결하면 캘린더의 해당 일정 상세에 이 영상이 함께 표시됩니다.
        </span>
      </div>
    </Modal>
  );
}
