import { useEffect } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import { BrandMark } from './BrandMark';
import { RestrictedBandView } from './RestrictedBandView';
import { FullscreenLoader } from '../pages/SystemPages';
import { AppChrome } from './AppChrome';
import './AppLayout.css';

export function AppLayout() {
  const { bandId } = useParams();
  const { currentBandId, currentBand, bandRestricted, bandLoading, bootLoading, setCurrentBandId } =
    useApp();

  // URL 의 밴드 → 컨텍스트 (뒤로가기 / 직접 URL 진입 / 밴드 전환 대응)
  useEffect(() => {
    if (bandId && bandId !== currentBandId) setCurrentBandId(bandId);
  }, [bandId, currentBandId, setCurrentBandId]);

  // 세션 복구가 끝나기 전에 밴드를 그려버리면 로그인 상태인데도 잠깐 비회원(guest)처럼 보인다 —
  // 새로고침/직접 URL 진입마다 "로그아웃된 것처럼" 깜빡이던 원인.
  if (bootLoading) return <FullscreenLoader label="세션 확인 중…" />;

  if (!currentBand) {
    if (bandLoading || bandId !== currentBandId)
      return <FullscreenLoader label="밴드 불러오는 중…" />;
    return (
      <div
        style={{
          minHeight: '100dvh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 14,
          textAlign: 'center',
        }}
      >
        <BrandMark size={32} />
        <p className="muted">밴드를 찾을 수 없거나 접근할 수 없습니다.</p>
        <Link className="btn" to="/">
          홈으로
        </Link>
      </div>
    );
  }

  if (bandRestricted) {
    return <RestrictedBandView />;
  }

  return (
    <AppChrome>
      <Outlet />
    </AppChrome>
  );
}
