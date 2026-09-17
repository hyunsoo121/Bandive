import { Link } from 'react-router-dom';
import { BrandMark } from '../components/BrandMark';
import './LandingPage.css';

interface Props {
  onLogin: () => void;
}

const FEATURES: {
  title: string;
  desc: string;
  mock: 'media' | 'schedule' | 'songs' | 'members';
}[] = [
  {
    title: '흩어진 합주 영상, 한곳에 아카이빙',
    desc: '유튜브·구글드라이브·구글포토 등 어디에 올렸든 링크만 붙이면 썸네일까지 자동으로 정리됩니다. 자체 저장 공간 없이도 밴드의 역사가 차곡차곡 쌓여요.',
    mock: 'media',
  },
  {
    title: '연습·공연 기록이 그대로 남는 캘린더',
    desc: '지난 일정과 그날의 출결, 연결된 영상까지 한 화면에서 다시 볼 수 있어요. 언제 무슨 곡으로 어떤 무대를 했는지 기억이 흐려져도 기록은 남습니다.',
    mock: 'schedule',
  },
  {
    title: '합주곡 히스토리 관리',
    desc: '위시리스트 투표부터 확정, 세션별 담당자 배정까지 — 우리 밴드가 다뤄온 곡들의 발자취를 정리해두세요.',
    mock: 'songs',
  },
  {
    title: '초대 링크로 멤버 모으기',
    desc: '가입은 초대 링크로만 — 검색 노출 없이 원하는 사람만 초대하세요. 세션·리더 표시, 게스트 연주자 관리까지 지원합니다.',
    mock: 'members',
  },
];

function SongsMock() {
  const rows = [
    { title: '라일락', votes: 8 },
    { title: 'Blueming', votes: 5 },
    { title: '좋은 날', votes: 3 },
  ];
  return (
    <div className="landing__mock landing__mock--songs">
      {rows.map((r) => (
        <div key={r.title} className="landing__mock-row">
          <span className="landing__mock-vote">▲ {r.votes}</span>
          <span className="landing__mock-title">{r.title}</span>
        </div>
      ))}
    </div>
  );
}

function ScheduleMock() {
  const days = Array.from({ length: 28 }, (_, i) => i + 1);
  const marked = new Set([5, 12, 19, 24]);
  return (
    <div className="landing__mock landing__mock--schedule">
      <div className="landing__mock-cal">
        {days.map((d) => (
          <span key={d} className={`landing__mock-day${marked.has(d) ? ' is-on' : ''}`}>
            {d}
          </span>
        ))}
      </div>
    </div>
  );
}

function MediaMock() {
  return (
    <div className="landing__mock landing__mock--media">
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="landing__mock-thumb" />
      ))}
    </div>
  );
}

function MembersMock() {
  const rows = ['보컬 · 리더', '기타', '베이스', '드럼'];
  return (
    <div className="landing__mock landing__mock--members">
      {rows.map((r) => (
        <div key={r} className="landing__mock-row">
          <span className="landing__mock-avatar" />
          <span className="landing__mock-title">{r}</span>
        </div>
      ))}
    </div>
  );
}

const MOCKS = { songs: SongsMock, schedule: ScheduleMock, media: MediaMock, members: MembersMock };

/** 비로그인 방문자용 랜딩페이지 — "/" 진입 시 로그인 안 한 상태면 이걸 보여준다. */
export function LandingPage({ onLogin }: Props) {
  return (
    <div className="landing">
      <header className="landing__hero">
        <BrandMark size={40} wordmark />
        <h1 className="landing__headline">우리 밴드의 활동을, 잊히지 않게</h1>
        <p className="landing__sub">
          단톡방에 흩어지던 합주 영상·일정·곡 기록을 밴디브 하나에 차곡차곡 쌓아 아카이빙하세요.
        </p>
        <div className="landing__cta">
          <button type="button" className="btn btn--primary" onClick={onLogin}>
            로그인 / 회원가입
          </button>
          <Link className="btn btn--ghost" to="/explore">
            다른 밴드 구경하기
          </Link>
        </div>
      </header>

      <section className="landing__features">
        {FEATURES.map((f, i) => {
          const Mock = MOCKS[f.mock];
          return (
            <div key={f.title} className={`landing__feature${i % 2 === 1 ? ' is-reverse' : ''}`}>
              <div className="landing__feature-text">
                <span className="kicker">0{i + 1}</span>
                <h2>{f.title}</h2>
                <p className="muted">{f.desc}</p>
              </div>
              <div className="landing__feature-visual panel">
                <Mock />
              </div>
            </div>
          );
        })}
      </section>

      <footer className="landing__footer">
        <strong>지인 밴드끼리, 가볍게 시작하세요</strong>
        <button type="button" className="btn btn--primary" onClick={onLogin}>
          지금 시작하기
        </button>
      </footer>
    </div>
  );
}
