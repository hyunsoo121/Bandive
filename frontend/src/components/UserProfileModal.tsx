import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as userApi from '../api/users';
import { toUserProfile } from '../api/mappers';
import type { UserProfile } from '../types';
import { Avatar } from './Avatar';
import { Modal } from './Modal';
import './UserProfileModal.css';

interface Props {
  userId: string;
  onClose: () => void;
}

export function UserProfileModal({ userId, onClose }: Props) {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [zoomed, setZoomed] = useState(false);

  useEffect(() => {
    let alive = true;
    userApi
      .getUserProfile(userId)
      .then((dto) => {
        if (alive) setProfile(toUserProfile(dto));
      })
      .catch((e: unknown) => {
        if (alive) setErr(e instanceof Error ? e.message : '프로필을 불러오지 못했습니다.');
      });
    return () => {
      alive = false;
    };
  }, [userId]);

  const goBand = (bandId: string) => {
    onClose();
    navigate(`/bands/${bandId}`);
  };

  return (
    <>
      <Modal title="프로필" width={360} onClose={onClose}>
        {err ? (
          <span style={{ fontSize: 13, color: 'var(--color-accent)' }}>{err}</span>
        ) : !profile ? (
          <span className="muted" style={{ fontSize: 13 }}>
            불러오는 중…
          </span>
        ) : (
          <div className="stack" style={{ gap: 16 }}>
            <div className="userprofile__head">
              <button
                type="button"
                className="userprofile__avatar-btn"
                disabled={!profile.avatarUrl}
                onClick={() => setZoomed(true)}
                aria-label="프로필 사진 크게 보기"
              >
                <Avatar label={profile.initial} size={64} src={profile.avatarUrl} />
              </button>
              <div className="stack" style={{ gap: 3, minWidth: 0 }}>
                <strong style={{ fontSize: 17 }}>{profile.name}</strong>
                {profile.bio ? (
                  <span style={{ fontSize: 12, color: 'var(--color-neutral-700)' }}>
                    {profile.bio}
                  </span>
                ) : (
                  <span className="muted" style={{ fontSize: 12 }}>
                    소개 없음
                  </span>
                )}
              </div>
            </div>

            <div className="stack" style={{ gap: 6 }}>
              <span className="kicker">
                속한 밴드 {profile.bands.length > 0 ? profile.bands.length : ''}
              </span>
              {profile.bands.length === 0 ? (
                <span className="muted" style={{ fontSize: 12 }}>
                  아직 속한 밴드가 없습니다.
                </span>
              ) : (
                <div className="userprofile__bands">
                  {profile.bands.map((b) => (
                    <button
                      key={b.id}
                      type="button"
                      className="userprofile__band"
                      onClick={() => goBand(b.id)}
                    >
                      <Avatar label={b.initial} size={30} src={b.logoUrl} heading />
                      <span className="stack" style={{ gap: 1, minWidth: 0, flex: 1 }}>
                        <strong style={{ fontSize: 13 }}>{b.name}</strong>
                        <span className="muted" style={{ fontSize: 10 }}>
                          멤버 {b.memberCount}명 · {b.myRole === 'owner' ? '관리자' : '사용자'}
                        </span>
                      </span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>

      {zoomed && profile?.avatarUrl && (
        <div className="userprofile__lightbox" onClick={() => setZoomed(false)}>
          <img src={profile.avatarUrl} alt={profile.name} />
        </div>
      )}
    </>
  );
}
