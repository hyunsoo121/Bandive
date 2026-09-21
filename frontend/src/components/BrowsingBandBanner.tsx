import { Link } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import './BrowsingBandBanner.css';

/**
 * 탐색의 "이 밴드 페이지로 →" 등으로 들어온, 내 소속이 아닌 밴드를 구경 중일 때 뜨는 안내바.
 * 실제 밴드 페이지(AppLayout)에 들어오면 사이드바/탭바가 전부 이 밴드 기준으로 바뀌어버려서,
 * 탐색으로 돌아가거나 내 밴드로 복귀하는 길이 안 보이던 문제를 여기서 메운다.
 */
export function BrowsingBandBanner() {
  const { bands, currentBand } = useApp();
  if (!currentBand) return null;
  if (bands.some((b) => b.id === currentBand.id)) return null; // 내 밴드면 안 뜸

  const myBand = bands[0] ?? null;

  return (
    <div className="browsing-banner">
      <span className="browsing-banner__text">“{currentBand.name}” 밴드를 구경하는 중입니다</span>
      <span className="browsing-banner__actions">
        <Link className="browsing-banner__link" to="/explore">
          ← 탐색으로
        </Link>
        {myBand && (
          <Link className="browsing-banner__link" to={`/bands/${myBand.id}`}>
            내 밴드로 →
          </Link>
        )}
      </span>
    </div>
  );
}
