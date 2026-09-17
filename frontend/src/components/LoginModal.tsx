import { useState } from 'react';
import { useApp } from '../store/AppContext';
import { ApiError } from '../api/types';
import { BrandMark } from './BrandMark';
import './LoginModal.css';

type Mode = 'login' | 'signup';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PW_RE = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

function messageFor(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.code === 'EMAIL_TAKEN') return '이미 가입된 이메일입니다. 로그인해 주세요.';
    if (err.code === 'LOGIN_FAILED') return '이메일 또는 비밀번호가 올바르지 않습니다.';
    if (err.code === 'VALIDATION_ERROR') return err.message;
  }
  return '문제가 발생했습니다. 잠시 후 다시 시도해 주세요.';
}

export function LoginModal() {
  const { login, emailLogin, signup, closeLogin } = useApp();

  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [nickname, setNickname] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const emailOk = EMAIL_RE.test(email.trim());
  const pwOk = PW_RE.test(password);
  const canSubmit =
    !submitting &&
    emailOk &&
    (mode === 'login'
      ? password.length > 0
      : pwOk && password === password2 && nickname.trim().length > 0);

  const submit = async () => {
    if (!canSubmit) return;
    setSubmitting(true);
    setError(null);
    try {
      if (mode === 'login') await emailLogin(email, password);
      else await signup(email, password, nickname);
      // 성공 시 finishAuth 가 모달을 닫는다
    } catch (e) {
      setError(messageFor(e));
      setSubmitting(false);
    }
  };

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) void submit();
  };

  return (
    <div className="overlay overlay--center" onClick={closeLogin}>
      <div
        className="login-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <BrandMark size={36} />
        <h3>{mode === 'login' ? '밴디브 로그인' : '밴디브 회원가입'}</h3>
        <p className="muted">열람은 누구나 가능하고, 투표·출결·등록은 로그인이 필요합니다.</p>

        <div className="field">
          <label htmlFor="lm-email">이메일</label>
          <input
            id="lm-email"
            className="input"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={onKey}
            placeholder="you@example.com"
          />
        </div>

        {mode === 'signup' && (
          <div className="field">
            <label htmlFor="lm-nick">닉네임</label>
            <input
              id="lm-nick"
              className="input"
              value={nickname}
              maxLength={50}
              onChange={(e) => setNickname(e.target.value)}
              onKeyDown={onKey}
              placeholder="밴드에서 보일 이름"
            />
          </div>
        )}

        <div className="field">
          <label htmlFor="lm-pw">비밀번호</label>
          <input
            id="lm-pw"
            className="input"
            type="password"
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={onKey}
            placeholder={mode === 'signup' ? '8자 이상, 영문+숫자' : ''}
          />
          {mode === 'signup' && password.length > 0 && !pwOk && (
            <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>
              8자 이상이고 영문과 숫자를 모두 포함해야 합니다.
            </span>
          )}
        </div>

        {mode === 'signup' && (
          <div className="field">
            <label htmlFor="lm-pw2">비밀번호 확인</label>
            <input
              id="lm-pw2"
              className="input"
              type="password"
              autoComplete="new-password"
              value={password2}
              onChange={(e) => setPassword2(e.target.value)}
              onKeyDown={onKey}
            />
            {password2.length > 0 && password !== password2 && (
              <span style={{ fontSize: 11, color: 'var(--color-accent)' }}>
                비밀번호가 일치하지 않습니다.
              </span>
            )}
          </div>
        )}

        {error && <span style={{ fontSize: 12, color: 'var(--color-accent)' }}>{error}</span>}

        <button type="button" className="btn btn--primary" disabled={!canSubmit} onClick={submit}>
          {submitting ? '처리 중…' : mode === 'login' ? '로그인' : '가입하고 시작하기'}
        </button>

        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={() => {
            setMode(mode === 'login' ? 'signup' : 'login');
            setError(null);
          }}
        >
          {mode === 'login' ? '이메일로 회원가입' : '이미 계정이 있어요 · 로그인'}
        </button>

        <div className="login-modal__or">또는</div>

        <button type="button" className="login-modal__kakao" onClick={login}>
          카카오로 계속하기
        </button>

        <button type="button" className="btn btn--ghost btn--sm" onClick={closeLogin}>
          둘러보기 계속
        </button>
      </div>
    </div>
  );
}
