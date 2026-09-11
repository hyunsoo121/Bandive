import { useRef } from 'react';
import { Link } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { useGuard } from '../hooks/useGuard';
import { KIND_LABEL, nextSchedule, toUi } from '../lib/schedule';
import './HomePage.css';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** 업로드 전 클라 검증. 통과 못 하면 사유 문자열, 통과면 null. */
function imageError(file: File): string | null {
  if (!file.type.startsWith('image/')) return '이미지 파일만 올릴 수 있습니다.';
  if (file.size > MAX_IMAGE_BYTES) return '5MB 이하 이미지만 올릴 수 있습니다.';
  return null;
}

const STRIPE_SHADES = [
  ['#9b9797', '#bab6b6'],
  ['#7d7979', '#9b9797'],
  ['#605d5d', '#7d7979'],
];
const stripe = (a: string, b: string) =>
  `repeating-linear-gradient(135deg, ${a} 0 9px, ${b} 9px 18px)`;

export function HomePage() {
  const {
    currentBand,
    role,
    songs: allSongs,
    media: allMedia,
    schedules,
    uploadBandLogo,
    uploadBandBanner,
  } = useApp();
  const guard = useGuard();
  const logoInput = useRef<HTMLInputElement>(null);
  const bannerInput = useRef<HTMLInputElement>(null);

  if (!currentBand) return null;
  const bandId = currentBand.id;

  const songs = allSongs.filter((s) => s.bandId === bandId);
  const confirmed = songs.filter((s) => s.status === 'CONFIRMED');
  const wishlist = songs.filter((s) => s.status === 'WISHLIST');
  const media = allMedia.filter((m) => m.bandId === bandId);
  const recentSongs = [...songs].sort((a, b) => b.addedOrder - a.addedOrder).slice(0, 3);
  const upcomingRaw = nextSchedule(schedules);
  const upcoming = upcomingRaw ? toUi(upcomingRaw) : null;
  const dday = upcoming
    ? Math.max(0, Math.ceil((upcoming.at.getTime() - Date.now()) / 86_400_000))
    : 0;
  const isOwner = role === 'owner';

  const base = `/bands/${bandId}`;
  const going = upcoming?.counts.attending ?? 0;

  const onPick = (kind: 'logo' | 'banner') => async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // 같은 파일 다시 고를 수 있게
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

  const logo = currentBand.logoUrl ? (
    <img className="home__band-logo" src={currentBand.logoUrl} alt={currentBand.name} />
  ) : (
    currentBand.initial
  );

  return (
    <div className="home">
      {/* 배너 + 밴드 헤더 */}
      <div
        className="home__banner"
        style={
          currentBand.bannerUrl
            ? {
                backgroundImage: `url(${currentBand.bannerUrl})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
              }
            : undefined
        }
      >
        {!currentBand.bannerUrl && <span className="home__banner-hint">Banner image</span>}
        {isOwner && (
          <>
            <input
              ref={bannerInput}
              type="file"
              accept="image/*"
              hidden
              onChange={onPick('banner')}
            />
            <input ref={logoInput} type="file" accept="image/*" hidden onChange={onPick('logo')} />
            <button
              type="button"
              className="home__banner-upload"
              onClick={guard(() => bannerInput.current?.click())}
            >
              배너 변경
            </button>
          </>
        )}
        <div className="home__band">
          {isOwner ? (
            <button
              type="button"
              className="home__band-avatar home__band-avatar--edit"
              onClick={guard(() => logoInput.current?.click())}
              title="로고 변경"
            >
              {logo}
              <span className="home__band-avatar-hint">변경</span>
            </button>
          ) : (
            <span className="home__band-avatar">{logo}</span>
          )}
          <span className="stack">
            <strong>{currentBand.name}</strong>
            <span className="home__band-sub">
              멤버 {currentBand.memberCount}명 · 합주곡 {confirmed.length} · 위시 {wishlist.length}
            </span>
          </span>
        </div>
      </div>

      {/* 통계 */}
      <div className="home__stats">
        <Link className="home__stat" to={`${base}/songs`}>
          <strong>{confirmed.length}</strong>
          <span className="muted">합주곡</span>
        </Link>
        <Link className="home__stat" to={`${base}/songs`}>
          <strong>{wishlist.length}</strong>
          <span className="muted">위시리스트</span>
        </Link>
        <Link className="home__stat" to={`${base}/media`}>
          <strong>{media.length}</strong>
          <span className="muted">영상</span>
        </Link>
        <Link className="home__stat home__stat--desktop" to={`${base}/members`}>
          <strong>{currentBand.memberCount}</strong>
          <span className="muted">멤버</span>
        </Link>
        <Link className="home__stat" to={isOwner ? `${base}/followers` : `${base}/members`}>
          <strong>{currentBand.followerCount}</strong>
          <span className="muted">팔로워</span>
        </Link>
      </div>

      {/* 모바일: 멤버/팔로워/설정은 하단탭에 없으니 홈에서 진입 */}
      <div className="home__manage">
        <Link className="home__manage-link" to={`${base}/members`}>
          멤버 관리
        </Link>
        {isOwner && (
          <Link className="home__manage-link" to={`${base}/followers`}>
            팔로워 관리
          </Link>
        )}
        {isOwner && (
          <Link className="home__manage-link" to={`${base}/settings`}>
            밴드 설정
          </Link>
        )}
      </div>

      <div className="home__cols">
        {/* 다가오는 일정 */}
        <section className="home__section">
          <div className="spread">
            <span className="kicker">다가오는 일정</span>
            <Link className="home__more" to={`${base}/schedule`}>
              전체보기
            </Link>
          </div>

          {upcoming ? (
            <div className="panel home__next">
              <div className="home__next-body">
                <div className="home__next-date">
                  <strong>{upcoming.day}</strong>
                  <span className="muted">
                    {upcoming.month + 1}월 {upcoming.dow}
                  </span>
                </div>
                <div className="stack" style={{ gap: 6, flex: 1 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                    <span
                      className={`tag ${upcoming.type === 'PERFORMANCE' ? 'tag--muted' : 'tag--accent'}`}
                    >
                      {KIND_LABEL[upcoming.type]}
                    </span>
                    <span className="tag">D-{dday}</span>
                  </div>
                  <strong style={{ fontFamily: 'var(--font-heading)', fontSize: 16 }}>
                    {upcoming.location || KIND_LABEL[upcoming.type]}
                  </strong>
                  <span className="muted" style={{ fontSize: 12 }}>
                    {upcoming.timeLabel}
                    {upcoming.location ? ` · ${upcoming.location}` : ''}
                  </span>
                </div>
              </div>
              <Link className="home__next-foot" to={`${base}/schedule`}>
                <span className="muted" style={{ fontSize: 12 }}>
                  참석 {going} / {currentBand.memberCount}
                </span>
                <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-accent-700)' }}>
                  출결 체크 →
                </span>
              </Link>
            </div>
          ) : (
            <div className="panel home__empty">예정된 일정이 없습니다.</div>
          )}
        </section>

        {/* 최근 등록된 곡 */}
        <section className="home__section">
          <div className="spread">
            <span className="kicker">최근 등록된 곡</span>
            <Link className="home__more" to={`${base}/songs`}>
              전체보기
            </Link>
          </div>

          {recentSongs.length > 0 ? (
            <div className="home__songlist">
              {recentSongs.map((s, i) => (
                <div key={s.id} className="home__songrow">
                  <span className="home__songno">{String(i + 1).padStart(2, '0')}</span>
                  <span className="stack" style={{ flex: 1 }}>
                    <strong style={{ fontSize: 14 }}>{s.title}</strong>
                    <span className="muted" style={{ fontSize: 11 }}>
                      {s.artist} · {s.proposer} 등록
                    </span>
                  </span>
                  <span className={`tag ${s.status === 'CONFIRMED' ? '' : 'tag--soft'}`}>
                    {s.status === 'CONFIRMED' ? '합주곡' : `위시 · ${s.votes}표`}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="panel home__empty">아직 등록된 곡이 없습니다.</div>
          )}
        </section>
      </div>

      {/* 최근 영상 */}
      <section className="home__section home__section--wide">
        <div className="spread">
          <span className="kicker">최근 영상</span>
          <Link className="home__more" to={`${base}/media`}>
            전체보기
          </Link>
        </div>

        {media.length > 0 ? (
          <div className="home__videos">
            {media.slice(0, 3).map((v, i) => {
              const [a, b] = STRIPE_SHADES[i % STRIPE_SHADES.length];
              return (
                <div key={v.id} className="home__video">
                  <div className="home__video-thumb" style={{ background: stripe(a, b) }}>
                    {v.thumbnailUrl && (
                      <img
                        className="home__video-thumb-img"
                        src={v.thumbnailUrl}
                        alt=""
                        loading="lazy"
                      />
                    )}
                    <span className="home__play" />
                  </div>
                  <div className="stack" style={{ padding: 9, gap: 4 }}>
                    <strong style={{ fontSize: 12, lineHeight: 1.3 }}>{v.title}</strong>
                    <span className="muted" style={{ fontSize: 10 }}>
                      {v.source} · {v.date}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="panel home__empty">등록된 영상이 없습니다.</div>
        )}
      </section>
    </div>
  );
}
