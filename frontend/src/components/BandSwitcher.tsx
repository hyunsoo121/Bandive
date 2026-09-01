import { useApp } from '../store/AppContext';
import { Avatar } from './Avatar';
import { Modal } from './Modal';
import './BandSwitcher.css';

interface Props {
  onNavigate: (bandId: string) => void;
}

export function BandSwitcher({ onNavigate }: Props) {
  const { bands, currentBand, setCurrentBandId, closeSwitcher, openCreate } = useApp();

  const pick = (id: string) => {
    setCurrentBandId(id);
    onNavigate(id);
  };

  return (
    <Modal title="밴드 전환" variant="sheet" onClose={closeSwitcher}>
      <div className="switcher__list">
        {bands.map((b) => {
          const active = b.id === currentBand.id;
          return (
            <button
              key={b.id}
              type="button"
              className={`switcher__row${active ? ' is-active' : ''}`}
              onClick={() => pick(b.id)}
            >
              <Avatar
                label={b.initial}
                size={40}
                heading
                color={active ? 'var(--color-accent)' : 'var(--color-text)'}
              />
              <span className="switcher__row-text">
                <strong>{b.name}</strong>
                <span className="muted">
                  멤버 {b.memberCount}명 · {b.myRole === 'owner' ? '밴드장' : '사용자'}
                </span>
              </span>
              {active && <span className="switcher__mark">●</span>}
            </button>
          );
        })}
      </div>

      <button type="button" className="switcher__create" onClick={openCreate}>
        <span className="switcher__plus">＋</span>
        <span className="switcher__row-text">
          <strong>새 밴드 만들기</strong>
          <span className="muted">이름·로고·배너를 설정하고 초대 코드를 발급합니다</span>
        </span>
      </button>
    </Modal>
  );
}
