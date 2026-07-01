import React from 'react';
import { OSState, AppId } from '../types';
import { X, Check } from 'lucide-react';
import { AppIconGraphic } from './AppIconGraphic';
import { APPS } from '../data';

interface Props {
  state: OSState;
  onClose: () => void;
  onMarkRead: () => void;
  onDismiss: (id: string) => void;
  onOpenApp: (id: AppId) => void;
}

export function NotificationPanel({ state, onClose, onMarkRead, onDismiss, onOpenApp }: Props) {
  const unread = state.notifications.filter(n => !n.read).length;

  return (
    <div
      className="absolute inset-0 z-40 anim-slidedown"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
      style={{ background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }}
    >
      <div
        className="absolute left-0 right-0 top-0 flex flex-col"
        style={{ maxHeight: '75%', background: 'rgba(20,20,24,0.92)', backdropFilter: 'blur(40px)', borderRadius: '0 0 28px 28px' }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 pt-12 pb-3">
          <span className="text-white font-bold text-xl">Bildirimler</span>
          <div className="flex gap-2">
            {unread > 0 && (
              <button onClick={onMarkRead} className="pill-btn ghost text-xs py-1.5 px-3">
                Tümünü Oku
              </button>
            )}
            <button onClick={onClose} className="w-8 h-8 rounded-full flex items-center justify-center" style={{ background: 'rgba(58,58,60,0.9)' }}>
              <X size={16} color="#fff" />
            </button>
          </div>
        </div>

        {/* Notif list */}
        <div className="scroll-area flex-1 px-4 pb-4 flex flex-col gap-2">
          {state.notifications.length === 0 ? (
            <div className="flex flex-col items-center py-10 gap-2">
              <Check size={32} color="rgba(235,235,245,0.3)" />
              <span style={{ color: 'rgba(235,235,245,0.4)', fontSize: 15 }}>Tüm bildirimler okundu</span>
            </div>
          ) : (
            state.notifications.map(notif => {
              const app = APPS.find(a => a.id === notif.app);
              return (
                <div
                  key={notif.id}
                  className="flex items-start gap-3 rounded-2xl p-3 relative"
                  style={{
                    background: notif.read ? 'rgba(44,44,46,0.7)' : 'rgba(10,84,255,0.12)',
                    border: notif.read ? '0.5px solid rgba(255,255,255,0.06)' : '0.5px solid rgba(10,132,255,0.3)',
                  }}
                  onClick={() => { onOpenApp(notif.app); onClose(); }}
                >
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ background: app?.gradient || '#333' }}
                  >
                    <AppIconGraphic id={notif.app} size="sm" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-white font-semibold text-sm">{notif.title}</span>
                      <span style={{ color: 'rgba(235,235,245,0.45)', fontSize: 11 }}>{notif.time}</span>
                    </div>
                    <span style={{ color: 'rgba(235,235,245,0.7)', fontSize: 13, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {notif.body}
                    </span>
                  </div>
                  <button
                    className="flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center"
                    style={{ background: 'rgba(255,255,255,0.08)' }}
                    onClick={e => { e.stopPropagation(); onDismiss(notif.id); }}
                  >
                    <X size={12} color="rgba(235,235,245,0.6)" />
                  </button>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
