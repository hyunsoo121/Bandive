import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../store/AppContext';
import * as followApi from '../api/follow';
import type { NotificationItem, NotificationType } from '../types';
import { NavIcon } from './NavIcon';
import './NotificationBell.css';

const MESSAGE: Record<NotificationType, (n: NotificationItem) => string> = {
  SCHEDULE_CREATED: (n) =>
    `${n.actorNickname ?? '누군가'}님이 ${n.bandName}에 새 일정을 등록했어요.`,
  MEMBER_JOINED: (n) => `${n.actorNickname ?? '누군가'}님이 ${n.bandName}에 합류했어요.`,
  FOLLOW_REQUESTED: (n) => `${n.actorNickname ?? '누군가'}님이 ${n.bandName} 팔로우를 요청했어요.`,
  FOLLOW_AUTO_APPROVED: (n) =>
    `${n.actorNickname ?? '누군가'}님이 ${n.bandName}을(를) 팔로우했어요.`,
};

/** 알림이 가리키는 화면 — FOLLOW_REQUESTED 는 클릭이 아니라 승인/거절 버튼으로만 처리한다. */
function targetPath(n: NotificationItem): string | null {
  switch (n.type) {
    case 'SCHEDULE_CREATED':
      return `/bands/${n.bandId}/schedule`;
    case 'MEMBER_JOINED':
      return `/bands/${n.bandId}/members`;
    case 'FOLLOW_AUTO_APPROVED':
      return `/bands/${n.bandId}/followers`;
    case 'FOLLOW_REQUESTED':
      return null;
  }
}

/**
 * 오른쪽 위 알림 벨. 새로고침·재진입 시에만 갱신(폴링·웹소켓 없음) — 벨을 열 때마다 한 번 더 불러온다.
 * FOLLOW_REQUESTED 만 승인/거절 액션이 있고 나머지는 확인용(클릭하면 관련 화면으로 이동 + 읽음 처리).
 */
export function NotificationBell() {
  const {
    user,
    notifications,
    notificationsLoading,
    refreshNotifications,
    markNotificationRead,
    markAllNotificationsRead,
  } = useApp();
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const unreadCount = notifications.filter((n) => !n.readAt).length;

  useEffect(() => {
    if (!open) return;
    const onClickOutside = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, [open]);

  if (!user) return null;

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) void refreshNotifications();
  };

  const openRow = (n: NotificationItem) => {
    void markNotificationRead(n.id);
    const path = targetPath(n);
    if (path) {
      setOpen(false);
      navigate(path);
    }
  };

  const decide = async (n: NotificationItem, approve: boolean) => {
    if (!n.actorId || busyId) return;
    setBusyId(n.id);
    try {
      if (approve) await followApi.approveFollower(n.bandId, n.actorId);
      else await followApi.removeFollower(n.bandId, n.actorId);
    } catch {
      /* 다른 화면(팔로워 관리)에서 이미 처리됐거나 실패 — 어느 쪽이든 이 알림은 더 할 게 없으니 읽음 처리 */
    } finally {
      void markNotificationRead(n.id);
      setBusyId(null);
    }
  };

  return (
    <div className="notif" ref={rootRef}>
      <button type="button" className="notif__bell" onClick={toggle} aria-label="알림">
        <NavIcon
          d1="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"
          d2="M10 21a2 2 0 0 0 4 0"
          size={20}
        />
        {unreadCount > 0 && (
          <span className="notif__badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="notif__panel panel">
          <div className="notif__head spread">
            <span className="kicker">알림</span>
            {notifications.some((n) => !n.readAt) && (
              <button
                type="button"
                className="notif__markall"
                onClick={() => void markAllNotificationsRead()}
              >
                모두 읽음
              </button>
            )}
          </div>

          {notificationsLoading && notifications.length === 0 ? (
            <p className="muted notif__empty">불러오는 중…</p>
          ) : notifications.length === 0 ? (
            <p className="muted notif__empty">알림이 없습니다.</p>
          ) : (
            <div className="notif__list">
              {notifications.map((n) => (
                <div key={n.id} className={`notif__row${n.readAt ? '' : ' is-unread'}`}>
                  <button type="button" className="notif__row-main" onClick={() => openRow(n)}>
                    <span className="notif__dot" aria-hidden="true" />
                    <span className="notif__text">{MESSAGE[n.type](n)}</span>
                  </button>
                  {n.type === 'FOLLOW_REQUESTED' && !n.readAt && (
                    <div className="notif__actions">
                      <button
                        type="button"
                        className="notif__act notif__act--ok"
                        disabled={busyId === n.id}
                        onClick={() => decide(n, true)}
                      >
                        승인
                      </button>
                      <button
                        type="button"
                        className="notif__act"
                        disabled={busyId === n.id}
                        onClick={() => decide(n, false)}
                      >
                        거절
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
