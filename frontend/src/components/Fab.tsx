import './Fab.css';

interface Props {
  label: string;
  onClick: () => void;
}

/** 화면별 기본 액션 버튼 (곡 추가 / 일정 등록 / 영상 첨부 등) */
export function Fab({ label, onClick }: Props) {
  return (
    <button type="button" className="fab" onClick={onClick}>
      {label}
    </button>
  );
}
