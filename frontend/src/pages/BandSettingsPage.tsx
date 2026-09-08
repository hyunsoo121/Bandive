import { useRef, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import './BandSettingsPage.css';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

function imageError(file: File): string | null {
  if (!file.type.startsWith('image/')) return '이미지 파일만 올릴 수 있습니다.';
  if (file.size > MAX_IMAGE_BYTES) return '5MB 이하 이미지만 올릴 수 있습니다.';
  return null;
}

export function BandSettingsPage() {
  const {
    currentBand,
    role,
    members,
    user,
    updateBand,
    uploadBandLogo,
    uploadBandBanner,
    transferOwnership,
    deleteBand,
  } = useApp();
  const navigate = useNavigate();
  const logoInput = useRef<HTMLInputElement>(null);
  const bannerInput = useRef<HTMLInputElement>(null);

  const [name, setName] = useState(currentBand?.name ?? '');
  const [note, setNote] = useState(currentBand?.note ?? '');
  const [savingInfo, setSavingInfo] = useState(false);
  const [infoMsg, setInfoMsg] = useState<string | null>(null);

  const [transferTo, setTransferTo] = useState('');
  const [transferArmed, setTransferArmed] = useState(false);
  const [transferBusy, setTransferBusy] = useState(false);

  const [deleteArmed, setDeleteArmed] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [dangerMsg, setDangerMsg] = useState<string | null>(null);

  if (!currentBand) return null;
  if (role !== 'owner') return <Navigate to={`/bands/${currentBand.id}`} replace />;

  const bandId = currentBand.id;
  const infoDirty =
    name.trim().length > 0 &&
    (name.trim() !== currentBand.name || note.trim() !== (currentBand.note ?? '').trim());

  const otherMembers = members.filter((m) => m.bandId === bandId && m.id !== user?.id);

  const saveInfo = async () => {
    if (!infoDirty || savingInfo) return;
    setSavingInfo(true);
    setInfoMsg(null);
    try {
      await updateBand(name, note.trim() || null);
      setInfoMsg('저장했습니다.');
    } catch (e) {
      setInfoMsg(e instanceof Error ? e.message : '저장하지 못했습니다.');
    } finally {
      setSavingInfo(false);
    }
  };

  const onPick = (kind: 'logo' | 'banner') => async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const err = imageError(file);
    if (err) {
      alert(err);
      return;
    }
    try {
      await (kind === 'logo' ? uploadBandLogo(file) : uploadBandBanner(file));
    } catch {
      alert('업로드에 실패했습니다. 다시 시도해 주세요.');
    }
  };

  const runTransfer = async () => {
    if (!transferTo || transferBusy) return;
    setTransferBusy(true);
    setDangerMsg(null);
    try {
      await transferOwnership(transferTo);
      navigate(`/bands/${bandId}/members`);
    } catch (e) {
      setDangerMsg(e instanceof Error ? e.message : '위임하지 못했습니다.');
      setTransferBusy(false);
      setTransferArmed(false);
    }
  };

  const runDelete = async () => {
    if (deleteBusy) return;
    setDeleteBusy(true);
    setDangerMsg(null);
    try {
      await deleteBand();
    } catch (e) {
      setDangerMsg(e instanceof Error ? e.message : '삭제하지 못했습니다.');
      setDeleteBusy(false);
      setDeleteArmed(false);
    }
  };

  return (
    <div className="bandset">
      <header className="bandset__head">
        <button
          type="button"
          className="bandset__back"
          onClick={() => navigate(`/bands/${bandId}`)}
        >
          ← 밴드
        </button>
        <h2>밴드 설정</h2>
      </header>

      <section className="bandset__sec">
        <span className="kicker">밴드 정보</span>
        <div className="field">
          <label htmlFor="bs-name">이름</label>
          <input
            id="bs-name"
            className="input"
            value={name}
            maxLength={100}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="bs-note">소개</label>
          <textarea
            id="bs-note"
            className="input"
            value={note}
            maxLength={500}
            rows={3}
            onChange={(e) => setNote(e.target.value)}
            placeholder="밴드 소개 (선택)"
          />
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <button
            type="button"
            className="btn btn--primary btn--sm"
            disabled={!infoDirty || savingInfo}
            onClick={saveInfo}
          >
            {savingInfo ? '저장 중…' : '저장'}
          </button>
          {infoMsg && (
            <span className="muted" style={{ fontSize: 11 }}>
              {infoMsg}
            </span>
          )}
        </div>
      </section>

      <section className="bandset__sec">
        <span className="kicker">로고 / 배너</span>
        <div className="bandset__imgs">
          <div className="bandset__img">
            <span className="muted" style={{ fontSize: 11 }}>
              로고
            </span>
            {currentBand.logoUrl ? (
              <img src={currentBand.logoUrl} alt="로고" className="bandset__logo" />
            ) : (
              <span className="bandset__logo bandset__logo--empty">{currentBand.initial}</span>
            )}
            <input ref={logoInput} type="file" accept="image/*" hidden onChange={onPick('logo')} />
            <button
              type="button"
              className="btn btn--sm"
              onClick={() => logoInput.current?.click()}
            >
              변경
            </button>
          </div>
          <div className="bandset__img">
            <span className="muted" style={{ fontSize: 11 }}>
              배너
            </span>
            <div
              className="bandset__banner"
              style={
                currentBand.bannerUrl
                  ? { backgroundImage: `url(${currentBand.bannerUrl})` }
                  : undefined
              }
            />
            <input
              ref={bannerInput}
              type="file"
              accept="image/*"
              hidden
              onChange={onPick('banner')}
            />
            <button
              type="button"
              className="btn btn--sm"
              onClick={() => bannerInput.current?.click()}
            >
              변경
            </button>
          </div>
        </div>
      </section>

      <section className="bandset__sec">
        <span className="kicker">관리자 위임</span>
        {otherMembers.length === 0 ? (
          <span className="muted" style={{ fontSize: 12 }}>
            위임할 다른 멤버가 없습니다.
          </span>
        ) : (
          <>
            <select
              className="input"
              value={transferTo}
              onChange={(e) => {
                setTransferTo(e.target.value);
                setTransferArmed(false);
              }}
            >
              <option value="">멤버 선택</option>
              {otherMembers.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
            {transferArmed ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  type="button"
                  className="btn btn--sm bandset__danger"
                  disabled={transferBusy}
                  onClick={runTransfer}
                >
                  {transferBusy ? '위임 중…' : '정말 위임 — 나는 일반 멤버가 됩니다'}
                </button>
                <button
                  type="button"
                  className="btn btn--sm"
                  onClick={() => setTransferArmed(false)}
                >
                  취소
                </button>
              </div>
            ) : (
              <button
                type="button"
                className="btn btn--sm"
                disabled={!transferTo}
                onClick={() => setTransferArmed(true)}
              >
                관리자 넘기기
              </button>
            )}
          </>
        )}
      </section>

      <section className="bandset__sec bandset__sec--danger">
        <span className="kicker">위험 구역</span>
        <span className="muted" style={{ fontSize: 12, lineHeight: 1.5 }}>
          밴드를 삭제하면 곡·일정·영상·멤버·초대 코드가 모두 사라집니다. 되돌릴 수 없습니다.
        </span>
        {deleteArmed ? (
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="btn btn--sm bandset__danger"
              disabled={deleteBusy}
              onClick={runDelete}
            >
              {deleteBusy ? '삭제 중…' : `정말 "${currentBand.name}" 삭제`}
            </button>
            <button type="button" className="btn btn--sm" onClick={() => setDeleteArmed(false)}>
              취소
            </button>
          </div>
        ) : (
          <button
            type="button"
            className="btn btn--sm bandset__danger"
            onClick={() => setDeleteArmed(true)}
          >
            밴드 삭제
          </button>
        )}
        {dangerMsg && (
          <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>{dangerMsg}</span>
        )}
      </section>
    </div>
  );
}
