import { useEffect, type ReactNode } from 'react';
import './Modal.css';

interface Props {
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
  /** 하단 시트 스타일 (모바일 밴드 전환 등) */
  variant?: 'center' | 'sheet';
  width?: number;
}

export function Modal({
  title,
  onClose,
  children,
  footer,
  variant = 'center',
  width = 380,
}: Props) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div
      className={`overlay ${variant === 'sheet' ? 'overlay--bottom' : 'overlay--center'}`}
      onClick={onClose}
    >
      <div
        className={`modal ${variant === 'sheet' ? 'modal--sheet' : ''}`}
        style={variant === 'center' ? { width } : undefined}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <div className="modal__head">
          <h3 className="modal__title">{title}</h3>
          <button type="button" className="modal__close" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </div>
        <div className="modal__body scr">{children}</div>
        {footer && <div className="modal__foot">{footer}</div>}
      </div>
    </div>
  );
}
