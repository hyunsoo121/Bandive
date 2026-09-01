import { useCallback } from 'react';
import { useApp } from '../store/AppContext';

/**
 * 액션(투표/출결/등록 등)을 감싸는 가드.
 * 비회원이면 로그인 모달을 띄우고, 아니면 원래 동작을 실행한다.
 *
 *   const guard = useGuard();
 *   <button onClick={guard(() => vote(song.id))}>투표</button>
 */
export function useGuard() {
  const { role, openLogin } = useApp();
  return useCallback(
    <A extends unknown[]>(fn: (...args: A) => void) =>
      (...args: A) => {
        if (role === 'guest') {
          openLogin();
          return;
        }
        fn(...args);
      },
    [role, openLogin],
  );
}
