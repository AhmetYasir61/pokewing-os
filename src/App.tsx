import React, { useEffect, useState } from 'react';
import { useOS } from './hooks/useOS';
import { MUSIC_TRACKS } from './data';
import { AppId } from './types';

// Components
import { StatusBar } from './components/StatusBar';
import { HomeScreen } from './components/HomeScreen';
import { AppDrawer } from './components/AppDrawer';
import { NotificationPanel } from './components/NotificationPanel';
import { QuickSettings } from './components/QuickSettings';
import { LockScreen } from './components/LockScreen';
import { ContextMenu } from './components/ContextMenu';
import { Desktop } from './desktop/Desktop';
import { AppContent } from './AppContent';

function useTime() {
  const [time, setTime] = useState(new Date());
  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(t);
  }, []);
  const h = String(time.getHours()).padStart(2, '0');
  const m = String(time.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
}

function PhoneFrame({ children }: { children: React.ReactNode }) {
  // Telefon 390x844 sabit; dar görüntü alanlarında (MCEF telefon viewport'u ~235x470)
  // kırpılmaması için viewport'a sığacak şekilde ölçeklenir.
  const [scale, setScale] = useState(1);
  useEffect(() => {
    const fit = () => {
      const margin = 4; // kenar payı (küçük — MCEF viewport'unu doldursun)
      // Üst sınır YOK: büyük viewport'ta (MCEF yüksek çözünürlük) telefon büyüyerek
      // ekranı doldurur → supersampling ile keskin görüntü.
      const s = Math.min(
        (window.innerWidth - margin) / 390,
        (window.innerHeight - margin) / 844
      );
      setScale(s > 0.2 ? s : 1);
    };
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, []);

  return (
    <div className="flex items-center justify-center w-full h-full" style={{ background: 'transparent' }}>
      <div style={{ transform: `scale(${scale})`, transformOrigin: 'center', flexShrink: 0 }}>
        <div
          className="anim-phonein"
          style={{
            width: 390,
            height: 844,
            borderRadius: 34,
            overflow: 'hidden',
            position: 'relative',
            boxShadow: '0 0 0 2px #333, 0 0 0 3px #555, 0 0 0 13px #1c1c1e, 0 40px 80px rgba(0,0,0,0.85)',
            flexShrink: 0,
          }}
        >
          {children}
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const { state, dispatch, toast } = useOS();
  const time = useTime();

  // MCEF/derin bağlantı: ?app=<id> ile belirli bir uygulamayı açarak başla
  useEffect(() => {
    const a = new URLSearchParams(window.location.search).get('app');
    const valid: AppId[] = ['market','depo','katalog','news','mesaj','vc','galeri','kamera','web','tema','muzik','takvim','notlar','harita','hesap','ayar'];
    if (a && valid.includes(a as AppId)) dispatch({ type: 'OPEN_APP', app: a as AppId });
  }, [dispatch]);

  // PC modu → Windows 11 masaüstü; aksi halde telefon arayüzü
  const isPc = new URLSearchParams(window.location.search).get('mode') === 'pc';
  if (isPc) return <Desktop os={state} dispatch={dispatch} toast={toast} time={time} />;

  const closeApp = () => dispatch({ type: 'CLOSE_APP' });

  // Tüm uygulamalar tek yerden (AppContent) — masaüstü pencereleriyle aynı kaynak.
  const renderApp = () => {
    if (!state.activeApp) return null;
    return (
      <AppContent
        appId={state.activeApp}
        state={state}
        dispatch={dispatch}
        toast={toast}
        onClose={closeApp}
        onOpenApp={(id) => dispatch({ type: 'OPEN_APP', app: id as AppId })}
      />
    );
  };

  const bg = state.wallpaperUrl
    ? `url(${state.wallpaperUrl}) center/cover no-repeat`
    : state.wallpaper;

  // 🪫 Pil %5'in altında → telefon açılmaz (kapalıyken kendi kendine yavaşça dolar: 1%/3dk)
  if (state.battery < 5) {
    return (
      <PhoneFrame>
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-4" style={{ background: '#000' }}>
          <span style={{ fontSize: 56 }} className="anim-appin">🪫</span>
          <div className="text-white font-semibold text-lg">Şarj bitti</div>
          <div className="text-[13px]" style={{ color: '#FF453A', fontVariantNumeric: 'tabular-nums' }}>
            %{Math.max(0, state.battery).toFixed(1)} — açılmak için %5 gerekli
          </div>
          <div className="text-center text-sm px-10" style={{ color: 'rgba(235,235,245,0.45)', lineHeight: 1.6 }}>
            Kapalıyken kendi kendine yavaşça şarj olur, birazdan tekrar dene.
            <br />⚡ Şarj istasyonu yakında geliyor!
          </div>
        </div>
      </PhoneFrame>
    );
  }

  return (
    <PhoneFrame>
      {/* Wallpaper */}
      <div
        className="absolute inset-0"
        style={{ background: bg }}
      />

      {/* Brightness overlay */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{ background: `rgba(0,0,0,${1 - state.brightness / 100 * 0.95})`, zIndex: 999 }}
      />

      {/* Lock screen */}
      {state.lockScreen && (
        <LockScreen
          wallpaper={state.wallpaper}
          wallpaperUrl={state.wallpaperUrl}
          onUnlock={() => dispatch({ type: 'UNLOCK' })}
        />
      )}

      {!state.lockScreen && (
        <>
          {/* Status bar */}
          <StatusBar
            state={state}
            time={time}
            onNotifTap={() => dispatch({ type: 'TOGGLE_NOTIF_PANEL' })}
            onQSTap={() => dispatch({ type: 'TOGGLE_QS' })}
          />

          {/* Home */}
          {!state.activeApp && (
            <HomeScreen
              state={state}
              onOpenApp={(id) => dispatch({ type: 'OPEN_APP', app: id })}
              onOpenDrawer={() => dispatch({ type: 'TOGGLE_DRAWER' })}
              onSearch={(q) => dispatch({ type: 'SET_SEARCH', q })}
              onLongPressApp={(id, x, y) => dispatch({ type: 'SET_CONTEXT_MENU', menu: { x, y, app: id } })}
            />
          )}

          {/* Active App — üstteki durum çubuğuyla (44px) çakışmasın diye yükseklik sınırı */}
          {state.activeApp && (
            <div className="absolute inset-x-0 bottom-0" style={{ top: 44 }}>
              {renderApp()}
            </div>
          )}

          {/* Notification panel */}
          {state.notifPanelOpen && (
            <NotificationPanel
              state={state}
              onClose={() => dispatch({ type: 'CLOSE_NOTIF_PANEL' })}
              onMarkRead={() => dispatch({ type: 'MARK_NOTIFS_READ' })}
              onDismiss={(id) => dispatch({ type: 'DISMISS_NOTIF', id })}
              onOpenApp={(app) => dispatch({ type: 'OPEN_APP', app })}
            />
          )}

          {/* Quick settings */}
          {state.quickSettingsOpen && (
            <QuickSettings
              state={state}
              onClose={() => dispatch({ type: 'CLOSE_QS' })}
              onToggleWifi={() => dispatch({ type: 'TOGGLE_WIFI' })}
              onToggleBt={() => dispatch({ type: 'TOGGLE_BT' })}
              onToggleDnd={() => dispatch({ type: 'TOGGLE_DND' })}
              onBrightness={(v) => dispatch({ type: 'SET_BRIGHTNESS', val: v })}
              onVolume={(v) => dispatch({ type: 'SET_VOLUME', val: v })}
            />
          )}

          {/* App drawer */}
          {state.drawerOpen && (
            <AppDrawer
              onOpenApp={(id) => dispatch({ type: 'OPEN_APP', app: id })}
              onClose={() => dispatch({ type: 'CLOSE_DRAWER' })}
            />
          )}

          {/* Context menu */}
          {state.contextMenu && (
            <ContextMenu
              menu={state.contextMenu}
              onClose={() => dispatch({ type: 'SET_CONTEXT_MENU', menu: null })}
              onToast={toast}
            />
          )}

          {/* Navigation bar */}
          {!state.activeApp && (
            <div
              className="absolute bottom-0 left-0 right-0 flex items-center justify-center pb-2"
              style={{ height: 34 }}
            >
              <div style={{ width: 120, height: 5, borderRadius: 2.5, background: 'rgba(255,255,255,0.35)' }} />
            </div>
          )}

          {/* Back gesture pill when app is open */}
          {state.activeApp && (
            <div
              className="absolute bottom-0 left-0 right-0 flex items-center justify-center pb-2"
              style={{ height: 34, zIndex: 50 }}
            >
              <button
                onClick={closeApp}
                style={{ width: 120, height: 5, borderRadius: 2.5, background: 'rgba(255,255,255,0.35)', border: 'none', cursor: 'pointer' }}
              />
            </div>
          )}

          {/* Yeni mesaj baloncuğu (üstte) */}
          {state.banner && (
            <div className="msg-banner">
              <div className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
                style={{ background: `hsl(${state.banner.from.charCodeAt(0) * 20},60%,40%)` }}>
                {state.banner.from[0]}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-white font-semibold text-[13px]">{state.banner.from}</div>
                <div className="text-[12px] truncate" style={{ color: 'rgba(235,235,245,0.65)' }}>{state.banner.text}</div>
              </div>
              <span style={{ fontSize: 16 }}>💬</span>
            </div>
          )}

          {/* Toasts */}
          {state.toasts.map(t => (
            <div key={t.id} className="toast-notif">{t.text}</div>
          ))}

          {/* Mini music player (when playing and not in music app) */}
          {state.musicPlaying && state.activeApp !== 'muzik' && (
            <div
              className="absolute left-4 right-4 flex items-center gap-3 px-4 rounded-2xl"
              style={{
                bottom: state.activeApp ? 44 : 90,
                height: 56,
                background: 'rgba(28,28,30,0.92)',
                backdropFilter: 'blur(20px)',
                border: '0.5px solid rgba(255,255,255,0.1)',
                zIndex: 45,
              }}
            >
              <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style={{ background: MUSIC_TRACKS[state.musicTrack].color }}>
                <span style={{ fontSize: 16 }}>🎵</span>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-white font-semibold text-sm truncate">{MUSIC_TRACKS[state.musicTrack].title}</div>
                <div className="text-[11px]" style={{ color: 'rgba(235,235,245,0.5)' }}>{MUSIC_TRACKS[state.musicTrack].artist}</div>
              </div>
              <button
                onClick={() => dispatch({ type: 'TOGGLE_MUSIC' })}
                className="w-8 h-8 rounded-full flex items-center justify-center focus:outline-none"
                style={{ background: 'rgba(255,255,255,0.1)' }}
              >
                <span style={{ fontSize: 16 }}>⏸</span>
              </button>
              <button
                onClick={() => dispatch({ type: 'NEXT_TRACK', total: MUSIC_TRACKS.length })}
                className="w-8 h-8 rounded-full flex items-center justify-center focus:outline-none"
                style={{ background: 'rgba(255,255,255,0.1)' }}
              >
                <span style={{ fontSize: 16 }}>⏭</span>
              </button>
            </div>
          )}
        </>
      )}
    </PhoneFrame>
  );
}
