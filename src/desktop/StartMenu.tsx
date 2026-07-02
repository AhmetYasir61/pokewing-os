import { Search, Power, FolderOpen, FileText } from 'lucide-react';
import { APPS } from '../data';
import { AppIconGraphic } from '../components/AppIconGraphic';
import { AppId } from '../types';
import { installedAppsForPlatform } from '../appstore';

interface Props { userName: string; onOpen: (id: string) => void; onClose: () => void; }

export function StartMenu({ userName, onOpen, onClose }: Props) {
  const pinned = [
    ...APPS.map(a => ({ id: a.id as string, name: a.name, gradient: a.gradient, app: a.id as AppId, emoji: undefined as string | undefined })),
    // Play Store'dan kurulanlar (PC hedefli olanlar)
    ...installedAppsForPlatform().map(s => ({
      id: s.id, name: s.name, gradient: `linear-gradient(160deg, ${s.color}, ${s.color}88)`, app: null, emoji: s.emoji,
    })),
    { id: 'explorer', name: 'Dosyalar', gradient: 'linear-gradient(160deg,#FFD60A,#E8A020)', app: null, emoji: undefined },
    { id: 'notepad', name: 'Not Defteri', gradient: 'linear-gradient(160deg,#4FC3F7,#2b7fae)', app: null, emoji: undefined },
  ];
  return (
    <>
      <div className="fixed inset-0 z-[900]" onClick={onClose} />
      <div onMouseDown={(e) => e.stopPropagation()}
        className="absolute z-[901] left-1/2 -translate-x-1/2 bottom-[60px] rounded-xl overflow-hidden flex flex-col"
        style={{ width: 540, maxWidth: '92vw', height: 600, maxHeight: '78vh',
          background: 'rgba(36,36,38,0.86)', backdropFilter: 'blur(40px) saturate(1.5)',
          border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 30px 80px rgba(0,0,0,0.55)' }}>
        <div className="p-5 pb-2">
          <div className="flex items-center gap-2 px-3 h-9 rounded-full" style={{ background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.1)' }}>
            <Search size={16} className="text-gray-400" />
            <input placeholder="Uygulama, dosya ara" className="bg-transparent outline-none text-sm text-white flex-1" />
          </div>
        </div>
        <div className="px-6 text-[13px] font-semibold text-gray-300 mb-2">Sabitlenmiş</div>
        <div className="flex-1 overflow-auto px-5 pb-4" style={{ display: 'grid', gridTemplateColumns: 'repeat(6,1fr)', gap: 12, alignContent: 'start' }}>
          {pinned.map(p => (
            <button key={p.id} onClick={() => onOpen(p.id)} className="flex flex-col items-center gap-1.5 py-2 rounded-lg hover:bg-white/10">
              <div className="w-11 h-11 rounded-xl flex items-center justify-center" style={{ background: p.gradient, boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.25)' }}>
                {p.emoji ? <span style={{ fontSize: 22 }}>{p.emoji}</span>
                  : p.app ? <AppIconGraphic id={p.app} size="sm" />
                  : p.id === 'explorer' ? <FolderOpen size={22} color="#fff" /> : <FileText size={22} color="#fff" />}
              </div>
              <span className="text-[11px] text-gray-200 text-center leading-tight">{p.name}</span>
            </button>
          ))}
        </div>
        <div className="h-14 flex-none flex items-center justify-between px-6" style={{ background: 'rgba(0,0,0,0.25)', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold" style={{ background: 'linear-gradient(160deg,#0a84ff,#0a4cae)' }}>{(userName[0] || 'P').toUpperCase()}</div>
            <span className="text-sm text-white">{userName}</span>
          </div>
          <button onClick={() => onOpen('lock')} className="p-2 rounded-lg hover:bg-white/10 text-gray-200"><Power size={18} /></button>
        </div>
      </div>
    </>
  );
}
