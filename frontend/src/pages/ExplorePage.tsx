import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { Avatar } from '../components/Avatar';
import * as exploreApi from '../api/explore';
import * as mediaApi from '../api/media';
import type { ExploreBandDto, ExploreTrackDto, ExploreVideoDto } from '../api/types';
import { VISIBILITY_LABEL } from '../lib/bandVisibility';
import './ExplorePage.css';

type Tab = 'songs' | 'bands' | 'following';

const has = (hay: string | null | undefined, needle: string) =>
  (hay ?? '').toLowerCase().includes(needle);

export function ExplorePage() {
  const { user, openLogin, following, followingLoading, refreshFollowing, unfollowBand } = useApp();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<Tab>(params.get('tab') === 'following' ? 'following' : 'bands');
  const [q, setQ] = useState('');
  const [tracks, setTracks] = useState<ExploreTrackDto[] | null>(null);
  const [bands, setBands] = useState<ExploreBandDto[] | null>(null);

  const [openTrack, setOpenTrack] = useState<string | null>(null);
  const [videos, setVideos] = useState<ExploreVideoDto[]>([]);
  const [videosLoading, setVideosLoading] = useState(false);

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

  const toggleLike = async (v: ExploreVideoDto) => {
    if (!user) {
      openLogin();
      return;
    }
    try {
      const res = v.likedByMe
        ? await mediaApi.unlikeMedia(String(v.mediaId))
        : await mediaApi.likeMedia(String(v.mediaId));
      setVideos((prev) =>
        prev.map((x) =>
          x.mediaId === v.mediaId
            ? { ...x, likeCount: res.likeCount, likedByMe: res.likedByMe }
            : x,
        ),
      );
    } catch {
      /* 무시 */
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

  const videoRow = (v: ExploreVideoDto) => (
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
        <span className="explore__video-band">{v.bandName}</span>
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
  );

  return (
    <div className="explore">
      <header className="explore__head">
        <h1 className="explore__title">탐색</h1>
      </header>

      <div className="explore__tabs">
        <button
          type="button"
          className={`explore__tab${tab === 'bands' ? ' is-on' : ''}`}
          onClick={() => setTab('bands')}
        >
          밴드
        </button>
        <button
          type="button"
          className={`explore__tab${tab === 'songs' ? ' is-on' : ''}`}
          onClick={() => setTab('songs')}
        >
          곡
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
                  {videos.map((v) => videoRow(v))}
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
              onClick={() => navigate(`/explore/bands/${b.id}`)}
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
