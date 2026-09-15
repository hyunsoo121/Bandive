import { useEffect, useRef, useState } from 'react';
import { useApp } from '../store/AppContext';
import type { BandVisibility } from '../types';
import { CropModal } from './CropModal';
import { Modal } from './Modal';
import { VISIBILITY_LABEL, VISIBILITY_HINT, VISIBILITY_ORDER } from '../lib/bandVisibility';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

function imageError(file: File): string | null {
  if (!file.type.startsWith('image/')) return '이미지 파일만 올릴 수 있습니다.';
  if (file.size > MAX_IMAGE_BYTES) return '5MB 이하 이미지만 올릴 수 있습니다.';
  return null;
}

export function CreateBandModal() {
  const { createBand, closeCreate } = useApp();
  const [name, setName] = useState('');
  const [visibility, setVisibility] = useState<BandVisibility>('PUBLIC');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const canCreate = name.trim().length > 0 && !submitting;

  const logoInput = useRef<HTMLInputElement>(null);
  const bannerInput = useRef<HTMLInputElement>(null);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [bannerFile, setBannerFile] = useState<File | null>(null);
  const [cropTarget, setCropTarget] = useState<{ kind: 'logo' | 'banner'; file: File } | null>(
    null,
  );

  const logoPreview = usePreviewUrl(logoFile);
  const bannerPreview = usePreviewUrl(bannerFile);

  const pick = (kind: 'logo' | 'banner') => (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const err = imageError(file);
    if (err) {
      setError(err);
      return;
    }
    setError(null);
    setCropTarget({ kind, file });
  };

  const submitCrop = (cropped: File) => {
    if (cropTarget?.kind === 'logo') setLogoFile(cropped);
    if (cropTarget?.kind === 'banner') setBannerFile(cropped);
    setCropTarget(null);
  };

  const submit = async () => {
    if (!canCreate) return;
    setSubmitting(true);
    setError(null);
    try {
      // createBand 가 밴드 생성(+로고/배너 업로드) 후 해당 밴드로 이동시키고 모달을 닫는다
      await createBand(name, visibility, logoFile, bannerFile);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '밴드를 만들지 못했습니다.');
      setSubmitting(false);
    }
  };

  return (
    <>
      <Modal
        title="새 밴드 만들기"
        width={360}
        onClose={closeCreate}
        footer={
          <>
            <button
              type="button"
              className="btn btn--primary"
              style={{ flex: 1 }}
              disabled={!canCreate}
              onClick={submit}
            >
              {submitting ? '만드는 중…' : '밴드 만들기'}
            </button>
            <button type="button" className="btn" onClick={closeCreate}>
              취소
            </button>
          </>
        }
      >
        <div className="field">
          <label htmlFor="band-name">밴드 이름</label>
          <input
            id="band-name"
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="예: 목요일의 소음"
            autoFocus
          />
        </div>

        <div className="field">
          <label>공개범위</label>
          <div className="seg">
            {VISIBILITY_ORDER.map((v) => (
              <button
                key={v}
                type="button"
                className={`seg__opt${visibility === v ? ' seg__opt--on' : ''}`}
                onClick={() => setVisibility(v)}
              >
                {VISIBILITY_LABEL[v]}
              </button>
            ))}
          </div>
          <span className="muted" style={{ fontSize: 11 }}>
            {VISIBILITY_HINT[visibility]}
          </span>
        </div>

        <div style={{ display: 'flex', gap: 12 }}>
          <div className="field">
            <label>로고</label>
            <input ref={logoInput} type="file" accept="image/*" hidden onChange={pick('logo')} />
            <button
              type="button"
              className="upload-box upload-box--square"
              style={
                logoPreview
                  ? {
                      backgroundImage: `url(${logoPreview})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                      border: '2px solid var(--color-text)',
                      color: 'transparent',
                    }
                  : undefined
              }
              onClick={() => logoInput.current?.click()}
            >
              {logoPreview ? '변경' : '업로드'}
            </button>
          </div>
          <div className="field" style={{ flex: 1 }}>
            <label>배너</label>
            <input
              ref={bannerInput}
              type="file"
              accept="image/*"
              hidden
              onChange={pick('banner')}
            />
            <button
              type="button"
              className="upload-box"
              style={
                bannerPreview
                  ? {
                      backgroundImage: `url(${bannerPreview})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                      border: '2px solid var(--color-text)',
                      color: 'transparent',
                    }
                  : undefined
              }
              onClick={() => bannerInput.current?.click()}
            >
              {bannerPreview ? '변경' : '이미지 업로드'}
            </button>
          </div>
        </div>

        <p className="muted" style={{ fontSize: 11, margin: 0, lineHeight: 1.5 }}>
          만들면 내가 관리자가 됩니다. 로고·배너는 나중에도 설정 페이지에서 바꿀 수 있습니다.
        </p>
        {error && <p style={{ fontSize: 12, margin: 0, color: 'var(--color-accent)' }}>{error}</p>}
      </Modal>

      {cropTarget && (
        <CropModal
          file={cropTarget.file}
          aspect={cropTarget.kind === 'logo' ? 1 : 3}
          circle={cropTarget.kind === 'logo'}
          title={cropTarget.kind === 'logo' ? '로고 자르기' : '배너 자르기'}
          onCancel={() => setCropTarget(null)}
          onCropped={submitCrop}
        />
      )}
    </>
  );
}

/** File → object URL 미리보기. 파일이 바뀌거나 언마운트되면 이전 URL 을 정리한다. */
function usePreviewUrl(file: File | null): string | null {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    if (!file) {
      setUrl(null);
      return;
    }
    const next = URL.createObjectURL(file);
    setUrl(next);
    return () => URL.revokeObjectURL(next);
  }, [file]);
  return url;
}
