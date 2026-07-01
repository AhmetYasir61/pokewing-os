import React from 'react';
import { AppIcon } from './AppIcon';
import { APPS, DOCK_APPS } from '../data';
import { AppId, OSState } from '../types';
import { Search } from 'lucide-react';

interface Props {
  state: OSState;
  onOpenApp: (id: AppId) => void;
  onOpenDrawer: () => void;
  onSearch: (q: string) => void;
  onLongPressApp: (id: AppId, x: number, y: number) => void;
}

const HOME_PAGE_APP_IDS: AppId[] = ['market', 'depo', 'katalog', 'news', 'mesaj', 'kamera', 'galeri', 'web', 'tema', 'muzik', 'takvim', 'notlar'];

export function HomeScreen({ state, onOpenApp, onOpenDrawer, onSearch, onLongPressApp }: Props) {
  const homeApps = APPS.filter(a => HOME_PAGE_APP_IDS.includes(a.id));
  const dockApps = APPS.filter(a => DOCK_APPS.includes(a.id));
  const [search, setSearch] = React.useState('');
  const filtered = search.trim()
    ? APPS.filter(a => a.name.toLowerCase().includes(search.toLowerCase()))
    : homeApps;

  return (
    <div className="absolute inset-0 flex flex-col overflow-hidden" style={{ paddingTop: 52 }}>
      {/* Search bar */}
      <div className="px-4 mb-3">
        <div
          className="flex items-center gap-2 px-4 rounded-2xl"
          style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(16px)', height: 40 }}
        >
          <Search size={15} color="rgba(235,235,245,0.5)" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); onSearch(e.target.value); }}
            placeholder="Ara..."
            style={{ background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 15, flex: 1, fontFamily: 'inherit', padding: 0 }}
          />
        </div>
      </div>

      {/* Widget area - date + greeting */}
      {!search && (
        <div className="px-5 mb-4">
          <div
            className="rounded-3xl p-4"
            style={{ background: 'rgba(255,255,255,0.08)', backdropFilter: 'blur(20px)', border: '0.5px solid rgba(255,255,255,0.1)' }}
          >
            <div className="text-xs font-semibold mb-0.5" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
              {new Date().toLocaleDateString('tr-TR', { weekday: 'long', month: 'long', day: 'numeric' })}
            </div>
            <div className="text-white font-bold text-lg">Hoş geldin, {state.userName}!</div>
            <div className="flex items-center gap-3 mt-2">
              <div
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{ background: 'rgba(255,214,10,0.15)', border: '0.5px solid rgba(255,214,10,0.3)' }}
              >
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

      {/* App grid */}
      <div className="flex-1 scroll-area px-4">
        <div className="grid grid-cols-4 gap-x-2 gap-y-5 pb-4">
          {filtered.map(app => (
            <div key={app.id} className="flex justify-center">
              <AppIcon
                app={app}
                onPress={() => onOpenApp(app.id)}
                onLongPress={(e) => {
                  const el = document.querySelector(`[data-appid="${app.id}"]`);
                  const rect = el?.getBoundingClientRect();
                  onLongPressApp(app.id, rect?.x ?? 100, rect?.y ?? 100);
                }}
              />
            </div>
          ))}
        </div>
        {search && filtered.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 gap-2">
            <span style={{ fontSize: 36 }}>🔍</span>
            <span style={{ color: 'rgba(235,235,245,0.5)', fontSize: 15 }}>Sonuç bulunamadı</span>
          </div>
        )}
      </div>

      {/* Swipe up hint */}
      <button
        onClick={onOpenDrawer}
        className="flex flex-col items-center gap-1 pb-2 pt-1 focus:outline-none"
      >
        <div className="flex gap-1">
          {[0, 1, 2].map(i => (
            <div key={i} style={{ width: 5, height: 5, borderRadius: '50%', background: i === 0 ? '#fff' : 'rgba(255,255,255,0.35)' }} />
          ))}
        </div>
      </button>

      {/* Dock */}
      <div className="px-4 pb-6">
        <div
          className="flex justify-around items-center py-3 px-4 rounded-3xl"
          style={{ background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(28px)', border: '0.5px solid rgba(255,255,255,0.12)' }}
        >
          {dockApps.map(app => (
            <AppIcon key={app.id} app={app} size="sm" onPress={() => onOpenApp(app.id)} />
          ))}
        </div>
      </div>
    </div>
  );
}
