import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { fullCatalog, installedIds, install, uninstall, currentTarget, StoreApp as SApp } from '../appstore';
import { Download, Check, Trash2, Star, Smartphone, Monitor, Search } from 'lucide-react';

interface Props { onBack: () => void; toast: (t: string) => void; onOpenApp: (id: string) => void; }

const CATS = ['Tümü', 'Oyun', 'Araç', 'Sosyal', 'Geliştirici'] as const;

export function PlayStoreApp({ onBack, toast, onOpenApp }: Props) {
  const [cat, setCat] = useState<(typeof CATS)[number]>('Tümü');
  const [q, setQ] = useState('');
  const [, force] = useState(0);
  const [busy, setBusy] = useState<string | null>(null);

  const t = currentTarget();
  const inst = installedIds();
  const list = fullCatalog().filter(a =>
    (cat === 'Tümü' || a.category === cat) &&
    (!q.trim() || a.name.toLowerCase().includes(q.toLowerCase())));

  const doInstall = (a: SApp) => {
    if (!a.targets.includes(t)) { toast(`Bu uygulama ${a.targets.includes('pc') ? 'sadece PC' : 'sadece telefon'} için.`); return; }
    setBusy(a.id);
    setTimeout(() => {                      // "indiriliyor" hissi
      install(a.id);
      setBusy(null); force(x => x + 1);
      toast(`${a.emoji} ${a.name} kuruldu! Ana ekranda.`);
    }, 900);
  };
  const doUninstall = (a: SApp) => { uninstall(a.id); force(x => x + 1); toast(`${a.name} kaldırıldı.`); };

  return (
    <AppWindow title="Play Store" accentColor="#30D158" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Arama */}
        <div className="px-4 pt-2 pb-1">
          <div className="flex items-center gap-2 px-3 py-2 rounded-xl" style={{ background: 'rgba(44,44,46,0.9)' }}>
            <Search size={14} color="rgba(235,235,245,0.4)" />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Uygulama ara..."
              style={{ flex: 1, background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 14, fontFamily: 'inherit', padding: 0 }} />
          </div>
        </div>
        {/* Kategoriler */}
        <div className="px-4 py-2 flex gap-2 overflow-x-auto scroll-area" style={{ flexShrink: 0 }}>
          {CATS.map(c => (
            <button key={c} onClick={() => setCat(c)} className="px-3 py-1.5 rounded-full text-[13px] whitespace-nowrap"
              style={{ background: cat === c ? '#30D158' : 'rgba(44,44,46,0.9)', color: cat === c ? '#003312' : '#ccc', fontWeight: cat === c ? 700 : 400 }}>
              {c}
            </button>
          ))}
        </div>
        {/* Uygulama listesi */}
        <div className="flex-1 scroll-area px-4 pb-4 flex flex-col gap-2.5">
          {list.map(a => {
            const installed = inst.includes(a.id);
            const compatible = a.targets.includes(t);
            return (
              <div key={a.id} className="rounded-2xl p-3.5 flex items-center gap-3"
                style={{ background: 'rgba(28,28,30,0.9)', border: '0.5px solid rgba(255,255,255,0.06)', opacity: compatible ? 1 : 0.55 }}>
                <div className="w-13 h-13 rounded-2xl flex items-center justify-center flex-shrink-0"
                  style={{ width: 52, height: 52, background: `linear-gradient(160deg, ${a.color}, ${a.color}88)`, fontSize: 26 }}>
                  {a.emoji}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-white font-semibold text-[14.5px] flex items-center gap-1.5">
                    {a.name}
                    {a.custom && <span className="text-[9px] px-1.5 py-0.5 rounded-full" style={{ background: 'rgba(94,92,230,0.2)', color: '#9a98ff' }}>TOPLULUK</span>}
                  </div>
                  <div className="text-[11.5px] truncate" style={{ color: 'rgba(235,235,245,0.5)' }}>{a.desc}</div>
                  <div className="flex items-center gap-2 mt-1 text-[10.5px]" style={{ color: 'rgba(235,235,245,0.4)' }}>
                    <span className="flex items-center gap-0.5"><Star size={9} fill="#FFD60A" color="#FFD60A" /> {(4 + (a.id.length % 10) / 10).toFixed(1)}</span>
                    <span>{a.size}</span>
                    <span className="flex items-center gap-0.5">
                      {a.targets.includes('phone') && <Smartphone size={10} />}
                      {a.targets.includes('pc') && <Monitor size={10} />}
                    </span>
                    {a.author && <span>· {a.author}</span>}
                  </div>
                </div>
                <div className="flex flex-col gap-1.5 items-end flex-shrink-0">
                  {installed ? (
                    <>
                      <button onClick={() => onOpenApp(a.id)} className="pill-btn green text-xs py-1.5 px-4 flex items-center gap-1">
                        <Check size={12} /> Aç
                      </button>
                      <button onClick={() => doUninstall(a)} className="text-[10.5px] flex items-center gap-1" style={{ color: '#FF453A' }}>
                        <Trash2 size={10} /> Kaldır
                      </button>
                    </>
                  ) : busy === a.id ? (
                    <div className="spinner" style={{ width: 20, height: 20 }} />
                  ) : (
                    <button onClick={() => doInstall(a)} className="pill-btn primary text-xs py-1.5 px-4 flex items-center gap-1" disabled={!compatible}>
                      <Download size={12} /> Yükle
                    </button>
                  )}
                </div>
              </div>
            );
          })}
          {list.length === 0 && (
            <div className="text-center py-14" style={{ color: 'rgba(235,235,245,0.4)' }}>Sonuç yok</div>
          )}
        </div>
      </div>
    </AppWindow>
  );
}
