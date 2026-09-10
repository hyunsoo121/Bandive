import { useRef, useState } from 'react';
import { useApp } from '../store/AppContext';
import { Avatar } from './Avatar';
import { Modal } from './Modal';

interface Props {
  onClose: () => void;
}

const PW_RULE = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;
const MAX_AVATAR_BYTES = 5 * 1024 * 1024;

export function ProfileModal({ onClose }: Props) {
  const { user, updateProfile, uploadAvatar, removeAvatar, changePassword } = useApp();

  const [nickname, setNickname] = useState(user?.name ?? '');
  const [bio, setBio] = useState(user?.bio ?? '');
  const [savingName, setSavingName] = useState(false);
  const [nameMsg, setNameMsg] = useState<string | null>(null);

  const avatarInput = useRef<HTMLInputElement>(null);
  const [avatarBusy, setAvatarBusy] = useState(false);
  const [avatarMsg, setAvatarMsg] = useState<string | null>(null);

  const [curPw, setCurPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [newPw2, setNewPw2] = useState('');
  const [savingPw, setSavingPw] = useState(false);
  const [pwMsg, setPwMsg] = useState<{ ok: boolean; text: string } | null>(null);

  if (!user) return null;
  const isLocal = user.loginProvider === 'local';

  const nameDirty =
    nickname.trim().length > 0 &&
    (nickname.trim() !== user.name || bio.trim() !== (user.bio ?? ''));

  const pickAvatar = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file || avatarBusy) return;
    if (!file.type.startsWith('image/')) {
      setAvatarMsg('이미지 파일만 올릴 수 있습니다.');
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      setAvatarMsg('5MB 이하 이미지만 올릴 수 있습니다.');
      return;
    }
    setAvatarBusy(true);
    setAvatarMsg(null);
    try {
      await uploadAvatar(file);
    } catch (err) {
      setAvatarMsg(err instanceof Error ? err.message : '사진을 올리지 못했습니다.');
    } finally {
      setAvatarBusy(false);
    }
  };

  const clearAvatar = async () => {
    if (avatarBusy) return;
    setAvatarBusy(true);
    setAvatarMsg(null);
    try {
      await removeAvatar();
    } catch (err) {
      setAvatarMsg(err instanceof Error ? err.message : '사진을 지우지 못했습니다.');
    } finally {
      setAvatarBusy(false);
    }
  };

  const saveName = async () => {
    if (!nameDirty || savingName) return;
    setSavingName(true);
    setNameMsg(null);
    try {
      await updateProfile(nickname, bio);
      setNameMsg('저장했습니다.');
    } catch (e) {
      setNameMsg(e instanceof Error ? e.message : '저장하지 못했습니다.');
    } finally {
      setSavingName(false);
    }
  };

  const pwValid = curPw.length > 0 && PW_RULE.test(newPw) && newPw === newPw2 && !savingPw;

  const savePw = async () => {
    if (!pwValid) return;
    setSavingPw(true);
    setPwMsg(null);
    try {
      await changePassword(curPw, newPw);
      setCurPw('');
      setNewPw('');
      setNewPw2('');
      setPwMsg({ ok: true, text: '비밀번호를 변경했습니다.' });
    } catch (e) {
      setPwMsg({ ok: false, text: e instanceof Error ? e.message : '변경하지 못했습니다.' });
    } finally {
      setSavingPw(false);
    }
  };

  return (
    <Modal title="내 정보" width={400} onClose={onClose}>
      <div className="stack" style={{ gap: 16 }}>
        <div className="field">
          <label>프로필 사진</label>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar label={user.initial} size={56} src={user.avatarUrl} />
            <input ref={avatarInput} type="file" accept="image/*" hidden onChange={pickAvatar} />
            <button
              type="button"
              className="btn btn--sm"
              disabled={avatarBusy}
              onClick={() => avatarInput.current?.click()}
            >
              {avatarBusy ? '처리 중…' : user.avatarUrl ? '변경' : '사진 추가'}
            </button>
            {user.avatarUrl && (
              <button
                type="button"
                className="btn btn--sm"
                disabled={avatarBusy}
                onClick={clearAvatar}
              >
                제거
              </button>
            )}
          </div>
          {avatarMsg && (
            <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>{avatarMsg}</span>
          )}
        </div>

        <div className="field">
          <label htmlFor="pf-nick">닉네임</label>
          <input
            id="pf-nick"
            className="input"
            value={nickname}
            maxLength={50}
            onChange={(e) => setNickname(e.target.value)}
          />
        </div>

        <div className="field">
          <label htmlFor="pf-bio">한 줄 소개</label>
          <input
            id="pf-bio"
            className="input"
            value={bio}
            maxLength={100}
            placeholder="예: 메인 보컬 · 재즈 좋아함"
            onChange={(e) => setBio(e.target.value)}
          />
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 4 }}>
            <button
              type="button"
              className="btn btn--primary btn--sm"
              disabled={!nameDirty || savingName}
              onClick={saveName}
            >
              {savingName ? '저장 중…' : '닉네임·소개 저장'}
            </button>
            {nameMsg && (
              <span className="muted" style={{ fontSize: 11 }}>
                {nameMsg}
              </span>
            )}
          </div>
        </div>

        <div className="field">
          <label>로그인 방식</label>
          <span style={{ fontSize: 13 }}>
            {isLocal ? `이메일 · ${user.email}` : '카카오 로그인'}
          </span>
        </div>

        {isLocal && (
          <div
            className="stack"
            style={{ gap: 8, borderTop: '2px solid var(--color-text)', paddingTop: 14 }}
          >
            <span className="kicker">비밀번호 변경</span>
            <input
              className="input"
              type="password"
              placeholder="현재 비밀번호"
              autoComplete="current-password"
              value={curPw}
              onChange={(e) => setCurPw(e.target.value)}
            />
            <input
              className="input"
              type="password"
              placeholder="새 비밀번호 (8자 이상, 영문+숫자)"
              autoComplete="new-password"
              value={newPw}
              onChange={(e) => setNewPw(e.target.value)}
            />
            <input
              className="input"
              type="password"
              placeholder="새 비밀번호 확인"
              autoComplete="new-password"
              value={newPw2}
              onChange={(e) => setNewPw2(e.target.value)}
            />
            {newPw.length > 0 && !PW_RULE.test(newPw) && (
              <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>
                8자 이상, 영문과 숫자를 모두 포함해야 합니다.
              </span>
            )}
            {newPw2.length > 0 && newPw !== newPw2 && (
              <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>
                새 비밀번호가 일치하지 않습니다.
              </span>
            )}
            <button
              type="button"
              className="btn btn--primary btn--sm"
              disabled={!pwValid}
              onClick={savePw}
            >
              {savingPw ? '변경 중…' : '비밀번호 변경'}
            </button>
            {pwMsg && (
              <span
                style={{
                  fontSize: 11,
                  color: pwMsg.ok ? 'var(--color-neutral-600)' : 'var(--color-accent)',
                }}
              >
                {pwMsg.text}
              </span>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
