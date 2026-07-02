import React, { useRef, useState } from 'react';
import { AppIcon } from './AppIcon';
import { APPS, DOCK_APPS } from '../data';
import { AppId, AppMeta, OSState } from '../types';
import { installedAppsForPlatform } from '../appstore';
import { Search } from 'lucide-react';

interface Props {
  state: OSState;
  onOpenApp: (id: AppId) => void;
  onOpenDrawer: () => void;
  onSearch: (q: string) => void;
  onLongPressApp: (id: AppId, x: number, y: number) => void;
}

const HOME_PAGE_APP_IDS: AppId[] = ['market', 'depo', 'katalog', 'news', 'mesaj', 'kamera', 'galeri', 'web', 'yayin', 'store', 'tema', 'muzik', 'takvim', 'notlar'];
const PER_PAGE = 12; // 4 sütun × 3 satır / sayfa

export function HomeScreen({ state, onOpenApp, onOpenDrawer, onSearch, onLongPressApp }: Props) {
  // Rozetler dinamik: Mesajlar = okunmamış mesaj; diğerleri genel rozet haritasından (news, store, ...)
  const withBadge = (a: AppMeta) => ({
    ...a,
    badge: a.id === 'mesaj' ? state.msgUnread : (state.badges[a.id] || 0),
  });
  const baseApps = APPS.filter(a => HOME_PAGE_APP_IDS.includes(a.id)).map(withBadge);
  // Play Store'dan kurulan uygulamalar (emoji ikonlu) sayfalara akar
  const storeApps: AppMeta[] = installedAppsForPlatform()
    .filter(s => !HOME_PAGE_APP_IDS.includes(s.id))
    .map(s => ({ id: s.id, name: s.name, gradient: `linear-gradient(160deg, ${s.color}, ${s.color}88)`, iconKey: s.id, emoji: s.emoji }));
  const allApps = [...baseApps, ...storeApps];

  const dockApps = APPS.filter(a => DOCK_APPS.includes(a.id)).map(withBadge);
  const [search, setSearch] = React.useState('');

  // ---- Kaydırmalı sayfalar (telefonlardaki gibi) ----
  const pages: AppMeta[][] = [];
  for (let i = 0; i < allApps.length; i += PER_PAGE) pages.push(allApps.slice(i, i + PER_PAGE));
  if (pages.length === 0) pages.push([]);
  const [page, setPage] = useState(0);
  const [dragX, setDragX] = useState(0);
  const swipe = useRef<{ sx: number; active: boolean } | null>(null);
  const areaRef = useRef<HTMLDivElement>(null);

  const swStart = (x: number) => { swipe.current = { sx: x, active: true }; };
  const swMove = (x: number) => { if (swipe.current?.active) setDragX(x - swipe.current.sx); };
  const swEnd = () => {
    if (!swipe.current?.active) return;
    const dx = dragX;
    swipe.current = null;
    setDragX(0);
    if (dx < -55 && page < pages.length - 1) setPage(page + 1);   // sola kaydır → sonraki sayfa
    else if (dx > 55 && page > 0) setPage(page - 1);               // sağa kaydır → önceki sayfa
  };

  const filtered = search.trim()
    ? allApps.filter(a => a.name.toLowerCase().includes(search.toLowerCase()))
    : null;

  const renderIcon = (app: AppMeta) => (
    <div key={app.id} className="flex justify-center">
      <AppIcon
        app={app}
        onPress={() => onOpenApp(app.id)}
        onLongPress={() => {
          const el = document.querySelector(`[data-appid="${app.id}"]`);
          const rect = el?.getBoundingClientRect();
          onLongPressApp(app.id, rect?.x ?? 100, rect?.y ?? 100);
        }}
      />
    </div>
  );

  return (
    <div className="absolute inset-0 flex flex-col overflow-hidden" style={{ paddingTop: 52 }}>
      {/* Search bar */}
      <div className="px-4 mb-3">
        <div className="flex items-center gap-2 px-4 rounded-2xl"
          style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(16px)', height: 40 }}>
          <Search size={15} color="rgba(235,235,245,0.5)" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); onSearch(e.target.value); }}
            placeholder="Ara..."
            style={{ background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 15, flex: 1, fontFamily: 'inherit', padding: 0 }}
          />
        </div>
      </div>

      {/* Widget area - date + greeting (sadece 1. sayfada) */}
      {!search && page === 0 && (
        <div className="px-5 mb-4">
          <div className="rounded-3xl p-4"
            style={{ background: 'rgba(255,255,255,0.08)', backdropFilter: 'blur(20px)', border: '0.5px solid rgba(255,255,255,0.1)' }}>
            <div className="text-xs font-semibold mb-0.5" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
              {new Date().toLocaleDateString('tr-TR', { weekday: 'long', month: 'long', day: 'numeric' })}
            </div>
            <div className="text-white font-bold text-lg">Hoş geldin, {state.userName}!</div>
            <div className="flex items-center gap-3 mt-2">
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{ background: 'rgba(255,214,10,0.15)', border: '0.5px solid rgba(255,214,10,0.3)' }}>
                <span style={{ color: '#FFD60A', fontWeight: 700, fontSize: 15 }}>⬡</span>
                <span style={{ color: '#FFD60A', fontWeight: 700, fontSize: 14 }}>{state.coins.toLocaleString('tr-TR')}</span>
                <span style={{ color: 'rgba(235,235,245,0.6)', fontSize: 12 }}>Coin</span>
              </div>
              <div className="flex-1 text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>
                {state.wifi ? '5G • Bağlı' : 'Çevrimdışı'}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* App alanı */}
      {filtered ? (
        <div className="flex-1 scroll-area px-4">
          <div className="grid grid-cols-4 gap-x-2 gap-y-5 pb-4">{filtered.map(renderIcon)}</div>
          {filtered.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 gap-2">
              <span style={{ fontSize: 36 }}>🔍</span>
              <span style={{ color: 'rgba(235,235,245,0.5)', fontSize: 15 }}>Sonuç bulunamadı</span>
            </div>
          )}
        </div>
      ) : (
        <>
          {/* Kaydırılabilir sayfalar */}
          <div ref={areaRef} className="flex-1 overflow-hidden"
            onMouseDown={e => swStart(e.clientX)}
            onMouseMove={e => swMove(e.clientX)}
            onMouseUp={swEnd}
            onMouseLeave={swEnd}
            onTouchStart={e => swStart(e.touches[0].clientX)}
            onTouchMove={e => swMove(e.touches[0].clientX)}
            onTouchEnd={swEnd}>
            <div className="flex h-full"
              style={{
                width: `${pages.length * 100}%`,
                transform: `translateX(calc(${-page * (100 / pages.length)}% + ${dragX}px))`,
                transition: dragX === 0 ? 'transform 0.32s cubic-bezier(0.25,0.8,0.3,1)' : 'none',
              }}>
              {pages.map((pg, pi) => (
                <div key={pi} className="px-4" style={{ width: `${100 / pages.length}%` }}>
                  <div className="grid grid-cols-4 gap-x-2 gap-y-5">{pg.map(renderIcon)}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Sayfa noktaları */}
          {pages.length > 1 && (
            <div className="flex justify-center gap-1.5 pb-1 pt-2">
              {pages.map((_, i) => (
                <button key={i} onClick={() => setPage(i)} className="focus:outline-none"
                  style={{ width: i === page ? 16 : 6, height: 6, borderRadius: 3, background: i === page ? '#fff' : 'rgba(255,255,255,0.35)', transition: 'all 0.25s' }} />
              ))}
            </div>
          )}
        </>
      )}

      {/* Swipe up hint */}
      <button onClick={onOpenDrawer} className="flex flex-col items-center gap-1 pb-2 pt-1 focus:outline-none">
        <div className="flex gap-1">
          {[0, 1, 2].map(i => (
            <div key={i} style={{ width: 5, height: 5, borderRadius: '50%', background: i === 0 ? '#fff' : 'rgba(255,255,255,0.35)' }} />
          ))}
        </div>
      </button>

      {/* Dock */}
      <div className="px-4 pb-6">
        <div className="flex justify-around items-center py-3 px-4 rounded-3xl"
          style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(28px)', border: '0.5px solid rgba(255,255,255,0.12)' }}>
          {dockApps.map(app => (
            <AppIcon key={app.id} app={app} size="sm" onPress={() => onOpenApp(app.id)} />
          ))}
        </div>
      </div>
    </div>
  );
}
