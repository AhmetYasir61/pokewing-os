import { Search, Wifi, Volume2, FolderOpen } from 'lucide-react';
import { APPS } from '../data';
import { AppIconGraphic } from '../components/AppIconGraphic';
import { AppId } from '../types';
import { WinData } from './Win';

interface Props {
  time: string; dateStr: string; startOpen: boolean;
  windows: WinData[];
  onStart: () => void; onOpen: (id: string) => void; onTask: (id: string) => void;
}

const PINNED: string[] = ['explorer', 'market', 'web', 'mesaj', 'galeri'];

export function Taskbar({ time, dateStr, startOpen, windows, onStart, onOpen, onTask }: Props) {
  return (
    <div className="absolute bottom-0 left-0 right-0 z-[800] flex items-center justify-center"
      style={{ height: 48, background: 'rgba(32,32,34,0.78)', backdropFilter: 'blur(40px) saturate(1.6)', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
      {/* sol köşe boş bırakıldı; orta küme */}
      <div className="flex items-center gap-1.5">
        {/* Başlat */}
        <button onClick={onStart} title="Başlat"
          className={'w-10 h-10 rounded-lg flex items-center justify-center ' + (startOpen ? 'bg-white/15' : 'hover:bg-white/10')}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            {[0, 1, 2, 3].map(i => <span key={i} style={{ width: 8, height: 8, borderRadius: 1.5, background: '#4cc2ff' }} />)}
          </div>
        </button>
        {/* Arama */}
        <button className="h-10 px-3 rounded-lg flex items-center gap-2 hover:bg-white/10 text-gray-300">
          <Search size={17} /><span className="text-[12px] hidden sm:inline">Ara</span>
        </button>
        {/* Sabit + açık pencereler */}
        {PINNED.map(id => {
          const a = APPS.find(x => x.id === id);
          const running = windows.some(w => (w.kind === 'app' && w.appId === id) || (w.kind === 'explorer' && id === 'explorer'));
          return (
            <button key={id} onClick={() => onOpen(id)} title={a?.name || 'Dosyalar'}
              className="relative w-10 h-10 rounded-lg flex items-center justify-center hover:bg-white/10">
              <div className="w-7 h-7 rounded-lg flex items-center justify-center" style={{ background: a?.gradient || 'linear-gradient(160deg,#FFD60A,#E8A020)' }}>
                {a ? <AppIconGraphic id={a.id as AppId} size="sm" /> : <FolderOpen size={18} color="#fff" />}
              </div>
              {running && <span className="absolute bottom-0.5 left-1/2 -translate-x-1/2" style={{ width: 14, height: 3, borderRadius: 2, background: '#4cc2ff' }} />}
            </button>
          );
        })}
        {/* sabitli olmayan açık pencereler */}
        {windows.filter(w => !(w.kind === 'app' && PINNED.includes(w.appId || '')) && !(w.kind === 'explorer')).map(w => (
          <button key={w.id} onClick={() => onTask(w.id)} title={w.title}
            className="relative w-10 h-10 rounded-lg flex items-center justify-center hover:bg-white/10">
            <span className="w-7 h-7 rounded-lg flex items-center justify-center bg-white/5">{w.icon}</span>
            <span className="absolute bottom-0.5 left-1/2 -translate-x-1/2" style={{ width: 14, height: 3, borderRadius: 2, background: '#4cc2ff' }} />
          </button>
        ))}
      </div>
      {/* sağ: tepsi + saat */}
      <div className="absolute right-2 flex items-center gap-1">
        <div className="flex items-center gap-2 px-2 py-1 rounded-lg hover:bg-white/10 text-gray-200"><Wifi size={16} /><Volume2 size={16} /></div>
        <div className="px-2 py-1 rounded-lg hover:bg-white/10 text-right leading-tight text-gray-100">
          <div className="text-[12px]">{time}</div><div className="text-[11px] opacity-80">{dateStr}</div>
        </div>
      </div>
    </div>
  );
}
