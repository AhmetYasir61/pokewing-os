import React from 'react';
import { APPS } from '../data';
import { AppIcon } from './AppIcon';
import { AppId } from '../types';
import { Search, X } from 'lucide-react';

interface Props {
  onOpenApp: (id: AppId) => void;
  onClose: () => void;
}

export function AppDrawer({ onOpenApp, onClose }: Props) {
  const [search, setSearch] = React.useState('');
  const filtered = search.trim()
    ? APPS.filter(a => a.name.toLowerCase().includes(search.toLowerCase()))
    : APPS;

  return (
    <div
      className="absolute inset-0 z-40 flex flex-col anim-slideup"
      style={{ background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(32px)', paddingTop: 60 }}
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      {/* Search */}
      <div className="px-4 mb-4">
        <div
          className="flex items-center gap-2 px-4 rounded-2xl"
          style={{ background: 'rgba(255,255,255,0.1)', height: 44 }}
        >
          <Search size={16} color="rgba(235,235,245,0.5)" />
          <input
            autoFocus
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Uygulama ara..."
            style={{ background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 16, flex: 1, fontFamily: 'inherit', padding: 0 }}
          />
          {search && (
            <button onClick={() => setSearch('')} className="focus:outline-none">
              <X size={16} color="rgba(235,235,245,0.5)" />
            </button>
          )}
        </div>
      </div>

      {/* Header */}
      <div className="px-5 mb-3">
        <span className="text-white font-bold text-xl">Tüm Uygulamalar</span>
      </div>

      {/* Grid */}
      <div className="flex-1 scroll-area px-4">
        <div className="grid grid-cols-4 gap-x-2 gap-y-5 pb-8">
          {filtered.map(app => (
            <div key={app.id} className="flex justify-center">
              <AppIcon app={app} onPress={() => { onOpenApp(app.id); onClose(); }} />
            </div>
          ))}
        </div>
        {filtered.length === 0 && (
          <div className="flex flex-col items-center py-12 gap-2">
            <span style={{ color: 'rgba(235,235,245,0.4)', fontSize: 15 }}>Uygulama bulunamadı</span>
          </div>
        )}
      </div>
    </div>
  );
}
