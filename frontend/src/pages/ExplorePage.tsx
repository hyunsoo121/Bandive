import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import * as exploreApi from '../api/explore';
import * as mediaApi from '../api/media';
import * as followApi from '../api/follow';
import type {
  ExploreBandDetailDto,
  ExploreBandDto,
  ExploreTrackDto,
  ExploreVideoDto,
} from '../api/types';
import { VISIBILITY_LABEL, VISIBILITY_HINT } from '../lib/bandVisibility';
import './ExplorePage.css';

type Tab = 'songs' | 'bands' | 'following';

const has = (hay: string | null | undefined, needle: string) =>
  (hay ?? '').toLowerCase().includes(needle);

export function ExplorePage() {
  const { user, openLogin, following, followingLoading, refreshFollowing, unfollowBand } = useApp();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<Tab>(params.get('tab') === 'following' ? 'following' : 'songs');
  const [q, setQ] = useState('');
  const [tracks, setTracks] = useState<ExploreTrackDto[] | null>(null);
  const [bands, setBands] = useState<ExploreBandDto[] | null>(null);

  const [openTrack, setOpenTrack] = useState<string | null>(null);
  const [videos, setVideos] = useState<ExploreVideoDto[]>([]);
  const [videosLoading, setVideosLoading] = useState(false);

  const [openBand, setOpenBand] = useState<ExploreBandDetailDto | null>(null);
  const [bandLoading, setBandLoading] = useState(false);
  const [followBusy, setFollowBusy] = useState(false);

  useEffect(() => {
    void exploreApi
      .exploreSongs()
      .then(setTracks)
      .catch(() => setTracks([]));
    void exploreApi
      .exploreBands()
      .then(setBands)
      .catch(() => setBands([]));
  }, []);

  useEffect(() => {
    if (user) void refreshFollowing();
  }, [user, refreshFollowing]);

  // 로그아웃 상태에서 팔로잉 탭이 열려 있으면 곡 탭으로
  useEffect(() => {
    if (!user && tab === 'following') setTab('songs');
  }, [user, tab]);

  const needle = q.trim().toLowerCase();
  const shownTracks = (tracks ?? []).filter(
    (t) => !needle || has(t.title, needle) || has(t.artist, needle),
  );
  const shownBands = (bands ?? []).filter(
    (b) => !needle || has(b.name, needle) || has(b.description, needle),
  );
  const shownFollowing = following.filter((f) => !needle || has(f.name, needle));

  const toggleTrack = async (trackId: string) => {
    if (openTrack === trackId) {
      setOpenTrack(null);
      return;
    }
    setOpenTrack(trackId);
    setVideos([]);
    setVideosLoading(true);
    try {
      setVideos(await exploreApi.exploreSongVideos(trackId));
    } catch {
      setVideos([]);
    } finally {
      setVideosLoading(false);
    }
  };

  const openBandDetail = async (bandId: number) => {
    setBandLoading(true);
    try {
      setOpenBand(await exploreApi.exploreBand(String(bandId)));
    } catch {
      setOpenBand(null);
    } finally {
      setBandLoading(false);
    }
  };

  const reloadBand = async (bandId: number) => {
    try {
      setOpenBand(await exploreApi.exploreBand(String(bandId)));
    } catch {
      /* keep */
    }
  };

  const toggleLike = async (v: ExploreVideoDto, scope: 'track' | 'band') => {
    if (!user) {
      openLogin();
      return;
    }
    try {
      const res = v.likedByMe
        ? await mediaApi.unlikeMedia(String(v.mediaId))
        : await mediaApi.likeMedia(String(v.mediaId));
      const patch = (x: ExploreVideoDto) =>
        x.mediaId === v.mediaId ? { ...x, likeCount: res.likeCount, likedByMe: res.likedByMe } : x;
      if (scope === 'track') setVideos((prev) => prev.map(patch));
      else setOpenBand((prev) => (prev ? { ...prev, videos: prev.videos.map(patch) } : prev));
    } catch {
      /* 무시 */
    }
  };

  const follow = async (bandId: number, cancel: boolean) => {
    if (!user) {
      openLogin();
      return;
    }
    setFollowBusy(true);
    try {
      if (cancel) await followApi.cancelFollow(String(bandId));
      else await followApi.requestFollow(String(bandId));
      await reloadBand(bandId);
      if (user) void refreshFollowing();
    } catch {
      /* 무시 */
    } finally {
      setFollowBusy(false);
    }
  };

  const stopFollowing = async (bandId: string) => {
    setFollowBusy(true);
    try {
      await unfollowBand(bandId);
    } catch {
      /* 무시 */
    } finally {
      setFollowBusy(false);
    }
  };

  const videoRow = (v: ExploreVideoDto, scope: 'track' | 'band', showBand: boolean) => (
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
        {showBand && <span className="explore__video-band">{v.bandName}</span>}
        {v.title && <span className="explore__muted">{v.title}</span>}
      </span>
      <button
        type="button"
        className={`explore__like${v.likedByMe ? ' is-liked' : ''}`}
        onClick={() => toggleLike(v, scope)}
      >
        ♥ {v.likeCount}
      </button>
    </div>
  );

  // ── 밴드 상세 (탐색 안에서 구경) ──
  if (openBand) {
    const b = openBand.band;
    const rel = openBand.myRelation;
    const canEnter = rel === 'MEMBER' || b.visibility === 'PUBLIC';
    return (
      <div className="explore">
        <header className="explore__head">
          <button type="button" className="explore__back" onClick={() => setOpenBand(null)}>
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
          {openBand.videos.length === 0 ? (
            <p className="explore__muted">
              {b.visibility === 'PUBLIC'
                ? '아직 공개된 영상이 없어요.'
                : '팔로우가 승인되면 밴드 페이지에서 볼 수 있어요.'}
            </p>
          ) : (
            openBand.videos.map((v) => videoRow(v, 'band', false))
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="explore">
      <header className="explore__head">
        <h1 className="explore__title">탐색</h1>
      </header>

      <div className="explore__tabs">
        <button
          type="button"
          className={`explore__tab${tab === 'songs' ? ' is-on' : ''}`}
          onClick={() => setTab('songs')}
        >
          곡
        </button>
        <button
          type="button"
          className={`explore__tab${tab === 'bands' ? ' is-on' : ''}`}
          onClick={() => setTab('bands')}
        >
          밴드
        </button>
        {user && (
          <button
            type="button"
            className={`explore__tab${tab === 'following' ? ' is-on' : ''}`}
            onClick={() => setTab('following')}
          >
            팔로잉{following.length > 0 ? ` ${following.length}` : ''}
          </button>
        )}
      </div>

      <div className="explore__search">
        <input
          className="input"
          value={q}
          placeholder={
            tab === 'songs'
              ? '곡·아티스트 검색'
              : tab === 'bands'
                ? '밴드 이름 검색'
                : '팔로우한 밴드 검색'
          }
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {tab === 'songs' && (
        <div className="explore__body">
          {tracks === null && <p className="explore__muted">불러오는 중…</p>}
          {tracks !== null && shownTracks.length === 0 && (
            <p className="explore__muted">
              {needle
                ? '검색 결과가 없어요.'
                : '아직 공개된 합주 영상이 없어요. 밴드를 전체공개로 두고 영상을 공개하면 여기에 모입니다.'}
            </p>
          )}
          {shownTracks.map((t) => (
            <div key={t.externalTrackId} className="explore__track">
              <button
                type="button"
                className="explore__track-head"
                onClick={() => toggleTrack(t.externalTrackId)}
              >
                {t.artworkUrl ? (
                  <img className="explore__art" src={t.artworkUrl} alt="" loading="lazy" />
                ) : (
                  <span className="explore__art explore__art--empty" />
                )}
                <span className="explore__track-text">
                  <strong>{t.title}</strong>
                  <span className="explore__muted">{t.artist ?? '아티스트 미상'}</span>
                </span>
                <span className="explore__track-meta">
                  영상 {t.videoCount} · 밴드 {t.bandCount}
                  <span className="explore__caret">
                    {openTrack === t.externalTrackId ? '▲' : '▼'}
                  </span>
                </span>
              </button>

              {openTrack === t.externalTrackId && (
                <div className="explore__videos">
                  {videosLoading && <p className="explore__muted">불러오는 중…</p>}
                  {videos.map((v) => videoRow(v, 'track', true))}
                  {!videosLoading && videos.length === 0 && (
                    <p className="explore__muted">영상을 불러오지 못했습니다.</p>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {tab === 'bands' && (
        <div className="explore__body">
          {bands === null && <p className="explore__muted">불러오는 중…</p>}
          {bandLoading && <p className="explore__muted">여는 중…</p>}
          {bands !== null && shownBands.length === 0 && (
            <p className="explore__muted">
              {needle ? '검색 결과가 없어요.' : '공개된 밴드가 없어요.'}
            </p>
          )}
          {shownBands.map((b) => (
            <button
              key={b.id}
              type="button"
              className="explore__band"
              onClick={() => openBandDetail(b.id)}
            >
              <Avatar
                label={[...b.name][0] ?? '밴'}
                size={40}
                src={b.logoUrl}
                color="var(--color-neutral-600)"
              />
              <span className="explore__band-text">
                <strong>{b.name}</strong>
                {b.description && <span className="explore__muted">{b.description}</span>}
                <span className="explore__muted">
                  {VISIBILITY_LABEL[b.visibility]} · 멤버 {b.memberCount}명
                </span>
              </span>
            </button>
          ))}
        </div>
      )}

      {tab === 'following' && (
        <div className="explore__body">
          {followingLoading && following.length === 0 && (
            <p className="explore__muted">불러오는 중…</p>
          )}
          {!followingLoading && shownFollowing.length === 0 && (
            <p className="explore__muted">
              {needle
                ? '검색 결과가 없어요.'
                : '아직 팔로우한 밴드가 없어요. 밴드 탭에서 팔로워 공개 밴드를 찾아 팔로우해보세요.'}
            </p>
          )}
          {shownFollowing.map((f) => {
            const inner = (
              <>
                <Avatar
                  label={[...f.name][0] ?? '밴'}
                  size={40}
                  src={f.logoUrl}
                  color="var(--color-neutral-600)"
                />
                <span className="explore__band-text">
                  <strong>{f.name}</strong>
                  <span className="explore__muted">
                    멤버 {f.memberCount}명 ·{' '}
                    {f.status === 'APPROVED' ? '팔로우 중' : '요청 대기 중'}
                  </span>
                </span>
              </>
            );
            return (
              <div key={f.bandId} className="explore__follow-row">
                {f.status === 'APPROVED' ? (
                  <Link className="explore__band explore__band--link" to={`/bands/${f.bandId}`}>
                    {inner}
                  </Link>
                ) : (
                  <div className="explore__band">{inner}</div>
                )}
                <button
                  type="button"
                  className="btn btn--sm"
                  disabled={followBusy}
                  onClick={() => stopFollowing(f.bandId)}
                >
                  {f.status === 'APPROVED' ? '팔로우 취소' : '요청 취소'}
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
