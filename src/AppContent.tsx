import React from 'react';
import { AppId, OSState } from './types';
import { MUSIC_TRACKS } from './data';
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
import { YayinApp } from './apps/YayinApp';
import { MINI_APPS } from './apps/MiniApps';
import { MINI_APPS_2 } from './apps/MiniApps2';
import { DevStudio, CustomAppViewer } from './apps/DevStudio';
import { PlayStoreApp } from './apps/StoreApp';
import { findApp } from './appstore';

interface Props {
  appId: AppId;
  state: OSState;
  dispatch: React.Dispatch<any>;
  toast: (t: string) => void;
  onClose: () => void;
  onOpenApp?: (id: string) => void;   // Play Store "Aç" (masaüstünde pencere, telefonda app)
}

/** Telefon uygulamalarını hem telefon hem masaüstü pencerelerinde render eder. */
export function AppContent({ appId, state, dispatch, toast, onClose, onOpenApp }: Props) {
  // ---- Play Store ekosistemi (dinamik uygulamalar) ----
  if (appId === 'store')
    return <PlayStoreApp onBack={onClose} toast={toast}
      onOpenApp={(id) => (onOpenApp ? onOpenApp(id) : dispatch({ type: 'OPEN_APP', app: id }))} />;
  if (appId === 'dev') return <DevStudio onBack={onClose} toast={toast} />;
  const Mini = MINI_APPS[appId] || MINI_APPS_2[appId];
  if (Mini) return <Mini onBack={onClose} toast={toast} />;
  if (typeof appId === 'string' && appId.startsWith('dev:')) {
    const capp = findApp(appId);
    if (capp) return <CustomAppViewer app={capp} onBack={onClose} />;
  }

  switch (appId) {
    case 'market':
      return <MarketApp coins={state.coins} onBack={onClose} onBuy={(p) => dispatch({ type: 'SPEND_COIN', amount: p })} onToast={toast} onCoins={(n) => dispatch({ type: 'SET_COINS', amount: n })} />;
    case 'depo':
      return <DepoApp onBack={onClose} coins={state.coins} onToast={toast} />;
    case 'katalog':
      return <KatalogApp onBack={onClose} />;
    case 'news':
      return <NewsApp onBack={onClose} />;
    case 'mesaj':
      return <MesajApp onBack={onClose} threads={state.threads}
        onSend={(n, t) => dispatch({ type: 'SEND_MSG', contactName: n, text: t })}
        onRecv={(n, t) => dispatch({ type: 'RECV_MSG', contactName: n, text: t })} onToast={toast} />;
    case 'vc':
      return <TelefonApp dialNumber={state.dialNumber} onBack={onClose}
        onDialAppend={(c) => dispatch({ type: 'DIAL_APPEND', char: c })}
        onDialDelete={() => dispatch({ type: 'DIAL_DELETE' })}
        onDialClear={() => dispatch({ type: 'DIAL_CLEAR' })} onToast={toast} />;
    case 'galeri':
      return <GaleriApp onBack={onClose} onToast={toast} />;
    case 'kamera':
      return <KameraApp onBack={onClose} onToast={toast} />;
    case 'web':
      return <WebApp onBack={onClose} webUrl={state.webUrl}
        onNavigate={(u) => dispatch({ type: 'NAVIGATE_WEB', url: u })} onToast={toast} />;
    case 'tema':
      return <TemaApp onBack={onClose} currentTheme={state.theme} wallpaperUrl={state.wallpaperUrl}
        onSetTheme={(i) => dispatch({ type: 'SET_THEME', idx: i })}
        onSetWallpaper={(u) => dispatch({ type: 'SET_WALLPAPER_URL', url: u })} onToast={toast} />;
    case 'muzik':
      return <MuzikApp onBack={onClose} playing={state.musicPlaying} trackIdx={state.musicTrack} volume={state.volume}
        onToggle={() => dispatch({ type: 'TOGGLE_MUSIC' })}
        onNext={() => dispatch({ type: 'NEXT_TRACK', total: MUSIC_TRACKS.length })}
        onPrev={() => dispatch({ type: 'PREV_TRACK', total: MUSIC_TRACKS.length })}
        onSetTrack={(i) => dispatch({ type: 'SET_TRACK', idx: i })} />;
    case 'takvim':
      return <TakvimApp onBack={onClose} date={state.calendarDate} onSetDate={(d) => dispatch({ type: 'SET_CALENDAR', date: d })} />;
    case 'notlar':
      return <NotlarApp notes={state.notes} activeNote={state.activeNote} onBack={onClose}
        onCreate={(t, b) => dispatch({ type: 'CREATE_NOTE', title: t, body: b })}
        onUpdate={(id, t, b) => dispatch({ type: 'UPDATE_NOTE', id, title: t, body: b })}
        onDelete={(id) => dispatch({ type: 'DELETE_NOTE', id })}
        onOpenNote={(id) => dispatch({ type: 'OPEN_NOTE', id })} onToast={toast} />;
    case 'yayin':
      return <YayinApp onBack={onClose} onToast={toast} />;
    case 'harita':
      return <HaritaApp onBack={onClose} onToast={toast} />;
    case 'hesap':
      return <HesapApp onBack={onClose} />;
    case 'ayar':
      return <AyarlarApp state={state} onBack={onClose}
        onToggleWifi={() => dispatch({ type: 'TOGGLE_WIFI' })}
        onToggleBt={() => dispatch({ type: 'TOGGLE_BT' })}
        onToggleDnd={() => dispatch({ type: 'TOGGLE_DND' })}
        onBrightness={(v) => dispatch({ type: 'SET_BRIGHTNESS', val: v })}
        onVolume={(v) => dispatch({ type: 'SET_VOLUME', val: v })}
        onSetUsername={(n) => dispatch({ type: 'SET_USERNAME', name: n })}
        onToast={toast} onLock={() => dispatch({ type: 'LOCK' })} />;
    default:
      return null;
  }
}
