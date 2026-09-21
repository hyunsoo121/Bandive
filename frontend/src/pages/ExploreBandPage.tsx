import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import * as exploreApi from '../api/explore';
import * as mediaApi from '../api/media';
import * as followApi from '../api/follow';
import type { ExploreBandDetailDto, ExploreVideoDto } from '../api/types';
import { VISIBILITY_LABEL, VISIBILITY_HINT } from '../lib/bandVisibility';
import './ExplorePage.css';

/**
 * "/explore/bands/:bandId" — 가입 없이 밴드 하나를 구경하는 화면. 초대 링크의 "둘러보기",
 * 밴드 홈의 "공유하기" 링크(백엔드 /band/{id} 가 OG 미리보기 후 여기로 리다이렉트)가 여기로 들어온다.
 * ExplorePage 안에서 목록으로 진입할 때도 이 같은 경로를 쓴다 — 그래야 링크 하나로 바로 공유가 된다.
 */
export function ExploreBandPage() {
  const { bandId } = useParams();
  const { user, openLogin, refreshFollowing } = useApp();
  const navigate = useNavigate();

  // 진입 경로가 초대 링크·공유 링크·목록 클릭 등 제각각이라 브라우저 히스토리(navigate(-1))에
  // 기대면 "뒤로"가 엉뚱한 곳으로 갈 수 있다 — 항상 탐색 목록으로 고정.
  const backToExplore = () => navigate('/explore');

  const [detail, setDetail] = useState<ExploreBandDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);

  const load = async () => {
    if (!bandId) return;
    try {
      const dto = await exploreApi.exploreBand(bandId);
      setDetail(dto);
      setNotFound(false);
    } catch {
      setNotFound(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bandId]);

  const toggleLike = async (v: ExploreVideoDto) => {
    if (!user) {
      openLogin();
      return;
    }
    try {
      const res = v.likedByMe
        ? await mediaApi.unlikeMedia(String(v.mediaId))
        : await mediaApi.likeMedia(String(v.mediaId));
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              videos: prev.videos.map((x) =>
                x.mediaId === v.mediaId
                  ? { ...x, likeCount: res.likeCount, likedByMe: res.likedByMe }
                  : x,
              ),
            }
          : prev,
      );
    } catch {
      /* 무시 */
    }
  };

  const follow = async (id: number, cancel: boolean) => {
    if (!user) {
      openLogin();
      return;
    }
    setFollowBusy(true);
    try {
      if (cancel) await followApi.cancelFollow(String(id));
      else await followApi.requestFollow(String(id));
      await load();
      void refreshFollowing();
    } catch {
      /* 무시 */
    } finally {
      setFollowBusy(false);
    }
  };

  if (loading) {
    return (
      <p className="explore__muted" style={{ padding: 16 }}>
        불러오는 중…
      </p>
    );
  }

  if (notFound || !detail) {
    return (
      <div className="explore">
        <header className="explore__head">
          <button type="button" className="explore__back" onClick={backToExplore}>
            ←
          </button>
          <h1 className="explore__title">밴드 구경</h1>
        </header>
        <p className="explore__muted" style={{ padding: 16 }}>
          밴드를 찾을 수 없거나 비공개 상태입니다.
        </p>
      </div>
    );
  }

  const b = detail.band;
  const rel = detail.myRelation;
  const canEnter = rel === 'MEMBER' || b.visibility === 'PUBLIC';

  return (
    <div className="explore">
      <header className="explore__head">
        <button type="button" className="explore__back" onClick={backToExplore}>
          ←
        </button>
        <h1 className="explore__title">밴드 구경</h1>
      </header>

      <div className="explore__band-detail">
        <Avatar
          label={[...b.name][0] ?? '밴'}
          size={56}
          src={b.logoUrl}
          color="var(--color-neutral-600)"
        />
        <strong className="explore__bd-name">{b.name}</strong>
        {b.description && <p className="explore__muted">{b.description}</p>}
        <span className="explore__muted">
          {VISIBILITY_LABEL[b.visibility]} · 멤버 {b.memberCount}명
        </span>

        {(b.visibility === 'FOLLOWERS' || b.visibility === 'PUBLIC') && rel !== 'MEMBER' && (
          <div className="explore__bd-follow">
            {b.visibility === 'FOLLOWERS' && rel !== 'FOLLOWER' && (
              <p className="explore__muted">{VISIBILITY_HINT.FOLLOWERS}</p>
            )}
            {rel === 'PENDING' ? (
              <button
                type="button"
                className="btn btn--sm"
                disabled={followBusy}
                onClick={() => follow(b.id, true)}
              >
                요청 대기 중 · 취소
              </button>
            ) : rel === 'FOLLOWER' ? (
              <button
                type="button"
                className="btn btn--sm"
                disabled={followBusy}
                onClick={() => follow(b.id, true)}
              >
                팔로잉 중 · 취소
              </button>
            ) : (
              <button
                type="button"
                className="btn btn--primary btn--sm"
                disabled={followBusy}
                onClick={() => follow(b.id, false)}
              >
                {b.visibility === 'PUBLIC' ? '팔로우' : '팔로우 요청'}
              </button>
            )}
          </div>
        )}

        {canEnter && (
          <Link className="btn btn--sm" to={`/bands/${b.id}`}>
            이 밴드 페이지로 →
          </Link>
        )}
      </div>

      <div className="explore__body">
        <span className="explore__section">공개 합주 영상</span>
        {detail.videos.length === 0 ? (
          <p className="explore__muted">
            {b.visibility === 'PUBLIC'
              ? '아직 공개된 영상이 없어요.'
              : '팔로우가 승인되면 밴드 페이지에서 볼 수 있어요.'}
          </p>
        ) : (
          detail.videos.map((v) => (
            <div key={v.mediaId} className="explore__video">
              <a
                className="explore__thumb"
                href={v.url}
                target="_blank"
                rel="noreferrer"
                style={v.thumbnailUrl ? undefined : { background: 'var(--color-neutral-300)' }}
              >
                {v.thumbnailUrl && <img src={v.thumbnailUrl} alt="" loading="lazy" />}
                <span className="explore__play" aria-hidden="true" />
              </a>
              <span className="explore__video-text">
                {v.title && <span className="explore__muted">{v.title}</span>}
              </span>
              <button
                type="button"
                className={`explore__like${v.likedByMe ? ' is-liked' : ''}`}
                onClick={() => toggleLike(v)}
              >
                ♥ {v.likeCount}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
