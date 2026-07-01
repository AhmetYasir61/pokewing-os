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

// Apps
import { MarketApp } from './apps/MarketApp';
import { DepoApp } from './apps/DepoApp';
import { KatalogApp } from './apps/KatalogApp';
import { NewsApp } from './apps/NewsApp';
import { MesajApp } from './apps/MesajApp';
import { TelefonApp } from './apps/TelefonApp';
import { GaleriApp } from './apps/GaleriApp';
import { KameraApp } from './apps/KameraApp';
import { WebApp } from './apps/WebApp';
import { TemaApp } from './apps/TemaApp';
import { MuzikApp } from './apps/MuzikApp';
import { TakvimApp } from './apps/TakvimApp';
import { NotlarApp } from './apps/NotlarApp';
import { HaritaApp } from './apps/HaritaApp';
import { HesapApp } from './apps/HesapApp';
import { AyarlarApp } from './apps/AyarlarApp';

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
      const margin = 10; // kenar payı
      const s = Math.min(
        (window.innerWidth - margin) / 390,
        (window.innerHeight - margin) / 844,
        1
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
          {/* Dynamic island */}
          <div
            style={{
              position: 'absolute', top: 12, left: '50%', transform: 'translateX(-50%)',
              width: 120, height: 34, background: '#000', borderRadius: 17, zIndex: 100,
            }}
          />
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

  const renderApp = () => {
    if (!state.activeApp) return null;
    switch (state.activeApp) {
      case 'market':
        return (
          <MarketApp
            coins={state.coins}
            onBack={closeApp}
            onBuy={(price) => dispatch({ type: 'SPEND_COIN', amount: price })}
            onToast={toast}
          />
        );
      case 'depo':
        return (
          <DepoApp
            onBack={closeApp}
            coins={state.coins}
            onToast={toast}
          />
        );
      case 'katalog':
        return <KatalogApp onBack={closeApp} />;
      case 'news':
        return <NewsApp onBack={closeApp} />;
      case 'mesaj':
        return (
          <MesajApp
            onBack={closeApp}
            threads={state.threads}
            onSend={(name, text) => dispatch({ type: 'SEND_MSG', contactName: name, text })}
            onRecv={(name, text) => dispatch({ type: 'RECV_MSG', contactName: name, text })}
            onToast={toast}
          />
        );
      case 'vc':
        return (
          <TelefonApp
            dialNumber={state.dialNumber}
            onBack={closeApp}
            onDialAppend={(c) => dispatch({ type: 'DIAL_APPEND', char: c })}
            onDialDelete={() => dispatch({ type: 'DIAL_DELETE' })}
            onDialClear={() => dispatch({ type: 'DIAL_CLEAR' })}
            onToast={toast}
          />
        );
      case 'galeri':
        return <GaleriApp onBack={closeApp} onToast={toast} />;
      case 'kamera':
        return <KameraApp onBack={closeApp} onToast={toast} />;
      case 'web':
        return (
          <WebApp
            onBack={closeApp}
            webUrl={state.webUrl}
            onNavigate={(url) => dispatch({ type: 'NAVIGATE_WEB', url })}
            onToast={toast}
          />
        );
      case 'tema':
        return (
          <TemaApp
            onBack={closeApp}
            currentTheme={state.theme}
            wallpaperUrl={state.wallpaperUrl}
            onSetTheme={(idx) => dispatch({ type: 'SET_THEME', idx })}
            onSetWallpaper={(url) => dispatch({ type: 'SET_WALLPAPER_URL', url })}
            onToast={toast}
          />
        );
      case 'muzik':
        return (
          <MuzikApp
            onBack={closeApp}
            playing={state.musicPlaying}
            trackIdx={state.musicTrack}
            volume={state.volume}
            onToggle={() => dispatch({ type: 'TOGGLE_MUSIC' })}
            onNext={() => dispatch({ type: 'NEXT_TRACK', total: MUSIC_TRACKS.length })}
            onPrev={() => dispatch({ type: 'PREV_TRACK', total: MUSIC_TRACKS.length })}
            onSetTrack={(i) => dispatch({ type: 'SET_TRACK', idx: i })}
          />
        );
      case 'takvim':
        return (
          <TakvimApp
            onBack={closeApp}
            date={state.calendarDate}
            onSetDate={(d) => dispatch({ type: 'SET_CALENDAR', date: d })}
          />
        );
      case 'notlar':
        return (
          <NotlarApp
            notes={state.notes}
            activeNote={state.activeNote}
            onBack={closeApp}
            onCreate={(title, body) => dispatch({ type: 'CREATE_NOTE', title, body })}
            onUpdate={(id, title, body) => dispatch({ type: 'UPDATE_NOTE', id, title, body })}
            onDelete={(id) => dispatch({ type: 'DELETE_NOTE', id })}
            onOpenNote={(id) => dispatch({ type: 'OPEN_NOTE', id })}
            onToast={toast}
          />
        );
      case 'harita':
        return <HaritaApp onBack={closeApp} onToast={toast} />;
      case 'hesap':
        return <HesapApp onBack={closeApp} />;
      case 'ayar':
        return (
          <AyarlarApp
            state={state}
            onBack={closeApp}
            onToggleWifi={() => dispatch({ type: 'TOGGLE_WIFI' })}
            onToggleBt={() => dispatch({ type: 'TOGGLE_BT' })}
            onToggleDnd={() => dispatch({ type: 'TOGGLE_DND' })}
            onBrightness={(v) => dispatch({ type: 'SET_BRIGHTNESS', val: v })}
            onVolume={(v) => dispatch({ type: 'SET_VOLUME', val: v })}
            onSetUsername={(name) => dispatch({ type: 'SET_USERNAME', name })}
            onToast={toast}
            onLock={() => dispatch({ type: 'LOCK' })}
          />
        );
      default:
        return null;
    }
  };

  const bg = state.wallpaperUrl
    ? `url(${state.wallpaperUrl}) center/cover no-repeat`
    : state.wallpaper;

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

          {/* Active App */}
          {renderApp()}

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
