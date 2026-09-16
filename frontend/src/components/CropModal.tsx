import { useEffect, useRef, useState } from 'react';
import { Modal } from './Modal';
import './CropModal.css';

interface Props {
  file: File;
  /** 가로/세로 비율. 1 = 정사각(아바타·로고), 3 = 와이드(배너) */
  aspect: number;
  title?: string;
  onCancel: () => void;
  onCropped: (file: File) => void;
}

interface Offset {
  x: number;
  y: number;
}

const FRAME_W = 320;
/** 결과 이미지 긴 쪽 픽셀 수 — 화면 표시용 축소본이 아니라 저장될 실제 크기. */
const OUTPUT_W = 960;

/** 프레임을 항상 이미지로 채우도록(letterbox 없이) offset 을 범위 안으로 눌러 넣는다. */
function clampOffset(
  offset: Offset,
  dispW: number,
  dispH: number,
  frameW: number,
  frameH: number,
): Offset {
  return {
    x: Math.min(0, Math.max(frameW - dispW, offset.x)),
    y: Math.min(0, Math.max(frameH - dispH, offset.y)),
  };
}

/**
 * 사진 자르기 공용 모달 — 프로필 사진 / 밴드 로고·배너에서 재사용. 순수 Canvas, 외부 라이브러리 없음.
 * 드래그로 위치 이동, 슬라이더로 확대. "적용"하면 고정 해상도 JPEG File 을 콜백으로 돌려준다 — 업로드는
 * 호출한 쪽이 기존 API(uploadAvatar 등)로 그대로 이어서 하면 된다.
 */
export function CropModal({ file, aspect, title = '사진 자르기', onCancel, onCropped }: Props) {
  const frameH = Math.round(FRAME_W / aspect);
  // ⚠️ 생성(useMemo)과 해제(useEffect cleanup)를 분리하면 안 된다 — StrictMode 개발 모드는 effect를
  // 마운트→정리→재마운트로 한 번 더 돌려보는데, <img> 가 blob 을 다 읽기도 전에 URL 이 revoke 돼서
  // 사진이 안 보이는 버그가 생겼다. 생성·해제를 같은 effect 안에 묶어 재마운트마다 새로 만든다.
  const [imgUrl, setImgUrl] = useState<string | null>(null);
  useEffect(() => {
    const url = URL.createObjectURL(file);
    setImgUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  const imgRef = useRef<HTMLImageElement>(null);
  const [natural, setNatural] = useState<{ w: number; h: number } | null>(null);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState<Offset>({ x: 0, y: 0 });
  const dragRef = useRef<{ startX: number; startY: number; offX: number; offY: number } | null>(
    null,
  );
  const [busy, setBusy] = useState(false);

  const baseScale = natural ? Math.max(FRAME_W / natural.w, frameH / natural.h) : 1;
  const scale = baseScale * zoom;
  const dispW = natural ? natural.w * scale : 0;
  const dispH = natural ? natural.h * scale : 0;

  const onImgLoad = () => {
    const img = imgRef.current;
    if (!img) return;
    const w = img.naturalWidth;
    const h = img.naturalHeight;
    const bs = Math.max(FRAME_W / w, frameH / h);
    setNatural({ w, h });
    setZoom(1);
    setOffset({ x: (FRAME_W - w * bs) / 2, y: (frameH - h * bs) / 2 });
  };

  const onZoom = (nextZoom: number) => {
    setZoom(nextZoom);
    if (!natural) return;
    const s = baseScale * nextZoom;
    setOffset((prev) => clampOffset(prev, natural.w * s, natural.h * s, FRAME_W, frameH));
  };

  const onPointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    dragRef.current = { startX: e.clientX, startY: e.clientY, offX: offset.x, offY: offset.y };
    e.currentTarget.setPointerCapture(e.pointerId);
  };

  const onPointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current || !natural) return;
    const dx = e.clientX - dragRef.current.startX;
    const dy = e.clientY - dragRef.current.startY;
    setOffset(
      clampOffset(
        { x: dragRef.current.offX + dx, y: dragRef.current.offY + dy },
        dispW,
        dispH,
        FRAME_W,
        frameH,
      ),
    );
  };

  const onPointerUp = () => {
    dragRef.current = null;
  };

  const confirm = () => {
    const img = imgRef.current;
    if (!img || !natural || busy) return;
    setBusy(true);

    const outW = OUTPUT_W;
    const outH = Math.round(OUTPUT_W / aspect);
    const canvas = document.createElement('canvas');
    canvas.width = outW;
    canvas.height = outH;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      setBusy(false);
      return;
    }
    const srcX = -offset.x / scale;
    const srcY = -offset.y / scale;
    const srcW = FRAME_W / scale;
    const srcH = frameH / scale;
    ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, outW, outH);

    canvas.toBlob(
      (blob) => {
        setBusy(false);
        if (!blob) return;
        const base = file.name.replace(/\.\w+$/, '') || 'image';
        onCropped(new File([blob], `${base}.jpg`, { type: 'image/jpeg' }));
      },
      'image/jpeg',
      0.92,
    );
  };

  return (
    <Modal
      title={title}
      width={FRAME_W + 40}
      onClose={onCancel}
      footer={
        <>
          <button
            type="button"
            className="btn btn--primary"
            style={{ flex: 1 }}
            disabled={!natural || busy}
            onClick={confirm}
          >
            {busy ? '처리 중…' : '적용'}
          </button>
          <button type="button" className="btn" onClick={onCancel}>
            취소
          </button>
        </>
      }
    >
      <div className="stack" style={{ gap: 12 }}>
        <div
          className="cropmodal__frame"
          style={{ width: FRAME_W, height: frameH }}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
        >
          <img
            ref={imgRef}
            src={imgUrl ?? undefined}
            alt=""
            onLoad={onImgLoad}
            draggable={false}
            style={
              natural
                ? { width: dispW, height: dispH, left: offset.x, top: offset.y }
                : { opacity: 0 }
            }
          />
        </div>
        <div className="cropmodal__zoom">
          <span className="muted" style={{ fontSize: 11 }}>
            확대
          </span>
          <input
            type="range"
            min={1}
            max={3}
            step={0.01}
            value={zoom}
            onChange={(e) => onZoom(Number(e.target.value))}
          />
        </div>
        <span className="muted" style={{ fontSize: 11 }}>
          드래그해서 위치를 옮기고, 슬라이더로 확대할 수 있어요.
        </span>
      </div>
    </Modal>
  );
}
