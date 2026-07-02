import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Monitor, Folder, FileText, FolderOpen, Image as ImageIcon, RefreshCw, Pencil, Trash2 } from 'lucide-react';
import { OSState } from '../types';
import { APPS } from '../data';
import { AppIconGraphic } from '../components/AppIconGraphic';
import { AppContent } from '../AppContent';
import { Win, WinData } from './Win';
import { findApp } from '../appstore';
import { Taskbar } from './Taskbar';
import { StartMenu } from './StartMenu';
import { FileExplorer, FsNode, FsOps } from './FileExplorer';
import { Notepad } from './Notepad';

const DEFAULT_WALL =
  'radial-gradient(120% 90% at 50% 0%, #1b6fb0 0%, #124a86 35%, #0a2a52 70%, #06122a 100%)';

interface Props { os: OSState; dispatch: React.Dispatch<any>; toast: (t: string) => void; time: string; }

let uid = 1;
const nid = () => 'n' + (uid++) + Date.now().toString(36);

export function Desktop({ os, dispatch, toast, time }: Props) {
  const itemId = useMemo(() => new URLSearchParams(location.search).get('item') || 'default', []);
  const KEY = 'pwdesk:' + itemId;

  const [fs, setFs] = useState<Record<string, FsNode>>(() => load(KEY)?.fs || {});
  const [iconPos, setIconPos] = useState<Record<string, { x: number; y: number }>>(() => load(KEY)?.iconPos || {});
  const [wallpaper, setWallpaper] = useState<string>(() => load(KEY)?.wallpaper || '');
  const [wins, setWins] = useState<WinData[]>([]);
  const [zTop, setZTop] = useState(10);
  const [start, setStart] = useState(false);
  const [ctx, setCtx] = useState<{ x: number; y: number; target?: string } | null>(null);
  const [iconCtx, setIconCtx] = useState<{ x: number; y: number; id: string } | null>(null);
  const [renaming, setRenaming] = useState<{ id: string; name: string } | null>(null);
  const clickRef = useRef<{ id: string; t: number }>({ id: '', t: 0 });
  const fileInput = useRef<HTMLInputElement>(null);

  // MCEF çift-tık göndermediği için tek-tık zamanlamasıyla çift-tık algıla.
  function iconClick(id: string, open: () => void) {
    const now = Date.now();
    if (clickRef.current.id === id && now - clickRef.current.t < 450) {
      clickRef.current = { id: '', t: 0 };
      open();
    } else {
      clickRef.current = { id, t: now };
    }
  }

  useEffect(() => {
    try { localStorage.setItem(KEY, JSON.stringify({ fs, iconPos, wallpaper })); } catch { /* kota */ }
  }, [fs, iconPos, wallpaper, KEY]);

  const dateStr = new Date().toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' });

  // ---- pencere yöneticisi ----
  function appIcon(appId: string) {
    const a = APPS.find(x => x.id === appId);
    return <div style={{ width: 16, height: 16, borderRadius: 4, background: a?.gradient || '#0a84ff' }} />;
  }
  function open(kind: WinData['kind'], opts: Partial<WinData> = {}) {
    setStart(false); setCtx(null);
    // app ise zaten açıksa odakla
    if (kind === 'app' && opts.appId) {
      const ex = wins.find(w => w.kind === 'app' && w.appId === opts.appId);
      if (ex) { focus(ex.id); return; }
    }
    const z = zTop + 1; setZTop(z);
    const id = nid();
    const base: WinData = {
      id, kind, z, min: false, max: false,
      x: 120 + (wins.length % 6) * 36, y: 70 + (wins.length % 6) * 30,
      w: kind === 'explorer' ? 760 : 820, h: kind === 'explorer' ? 480 : 560,
      title: 'Pencere', ...opts,
    };
    setWins(w => [...w, base]);
  }
  const focus = (id: string) => { const z = zTop + 1; setZTop(z); setWins(w => w.map(x => x.id === id ? { ...x, z, min: false } : x)); };
  const close = (id: string) => setWins(w => w.filter(x => x.id !== id));
  const setWin = (id: string, p: Partial<WinData>) => setWins(w => w.map(x => x.id === id ? { ...x, ...p } : x));
  const toggleMin = (id: string) => setWins(w => w.map(x => x.id === id ? { ...x, min: !x.min } : x));

  function openApp(appId: string) {
    const a = APPS.find(x => x.id === appId);
    const s = findApp(appId); // Play Store / geliştirici uygulaması
    open('app', {
      appId,
      title: a?.name || s?.name || appId,
      icon: s?.emoji ? <span style={{ fontSize: 13 }}>{s.emoji}</span> : appIcon(appId),
    });
  }
  function launch(id: string) {
    if (id === 'explorer') open('explorer', { title: 'Dosya Gezgini', icon: <FolderOpen size={14} color="#e8c069" />, data: { start: 'root' } });
    else if (id === 'notepad') { const n = createNode('root', 'txt'); openTxt(n); }
    else if (id === 'lock') { setWins([]); setStart(false); toast('Oturum kapatıldı'); }
    else openApp(id);
  }

  // ---- dosya sistemi ----
  const fsRef = useRef(fs); fsRef.current = fs;
  function createNode(parent: string, type: 'folder' | 'txt'): string {
    const base = type === 'folder' ? 'Yeni Klasör' : 'Yeni Metin Belgesi';
    const siblings = Object.values(fsRef.current).filter(n => n.parent === parent && n.name.startsWith(base));
    const name = base + (siblings.length ? ' (' + (siblings.length + 1) + ')' : '') + (type === 'txt' ? '.txt' : '');
    const id = nid();
    const node: FsNode = { id, name, type, parent, content: '' };
    fsRef.current = { ...fsRef.current, [id]: node }; // senkron: hemen sonraki openTxt(id) çalışsın
    setFs(f => ({ ...f, [id]: node }));
    return id;
  }
  const ops: FsOps = {
    create: (parent, type) => createNode(parent, type),
    rename: (id, name) => setFs(f => ({ ...f, [id]: { ...f[id], name } })),
    del: (id) => setFs(f => {
      const out = { ...f }; const stack = [id];
      while (stack.length) { const cur = stack.pop()!; Object.values(out).forEach(n => { if (n.parent === cur) stack.push(n.id); }); delete out[cur]; }
      return out;
    }),
    openTxt: (n) => openTxt(n.id),
  };
  function openTxt(nodeId: string) {
    const n = fsRef.current[nodeId]; if (!n) return;
    const ex = wins.find(w => w.kind === 'notepad' && w.data?.nodeId === nodeId);
    if (ex) { focus(ex.id); return; }
    open('notepad', { title: n.name + ' — Not Defteri', icon: <FileText size={14} color="#9fd0ff" />, data: { nodeId }, w: 560, h: 460 });
  }

  // ---- masaüstü ikonları ----
  const deskItems = [
    { id: 'mycomputer', name: 'Bu Bilgisayar', icon: <Monitor size={34} className="text-[#cfe6ff]" />, onOpen: () => launch('explorer') },
    { id: 'app:market', name: 'Market', icon: <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ background: APPS[0].gradient }}><AppIconGraphic id="market" size="sm" /></div>, onOpen: () => openApp('market') },
    { id: 'app:web', name: 'Tarayıcı', icon: <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ background: 'linear-gradient(160deg,#40C8E0,#0A84FF)' }}><AppIconGraphic id="web" size="sm" /></div>, onOpen: () => openApp('web') },
    ...Object.values(fs).filter(n => n.parent === 'root').map(n => ({
      id: n.id, name: n.name,
      icon: n.type === 'folder' ? <Folder size={34} className="text-[#e8c069]" fill="#e8c069" /> : <FileText size={32} className="text-[#9fd0ff]" />,
      onOpen: () => n.type === 'folder' ? open('explorer', { title: n.name, icon: <FolderOpen size={14} color="#e8c069" />, data: { start: n.id } }) : openTxt(n.id),
    })),
  ];
  function defaultPos(i: number) { return { x: 16, y: 16 + i * 92 }; }
  const dragRef = useRef<{ id: string; sx: number; sy: number; ox: number; oy: number } | null>(null);
  const rafRef = useRef(0);
  function startIconDrag(e: React.MouseEvent, id: string, cur: { x: number; y: number }) {
    e.stopPropagation();
    dragRef.current = { id, sx: e.clientX, sy: e.clientY, ox: cur.x, oy: cur.y };
    let lx = e.clientX, ly = e.clientY;
    // rAF ile sınırla: her mousemove'da değil, kare başına EN FAZLA 1 render (akıcı sürükleme)
    const mv = (ev: MouseEvent) => {
      lx = ev.clientX; ly = ev.clientY;
      if (rafRef.current) return;
      rafRef.current = requestAnimationFrame(() => {
        rafRef.current = 0;
        const d = dragRef.current; if (!d) return;
        setIconPos(p => ({ ...p, [d.id]: { x: Math.max(0, d.ox + lx - d.sx), y: Math.max(0, d.oy + ly - d.sy) } }));
      });
    };
    const up = () => { dragRef.current = null; document.removeEventListener('mousemove', mv); document.removeEventListener('mouseup', up); };
    document.addEventListener('mousemove', mv); document.addEventListener('mouseup', up);
  }

  function onWallFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]; if (!f) return;
    const r = new FileReader();
    r.onload = () => { setWallpaper(String(r.result)); toast('Masaüstü arka planı değişti'); };
    r.readAsDataURL(f);
  }

  const bg = wallpaper ? `url(${wallpaper}) center/cover no-repeat` : DEFAULT_WALL;

  return (
    <div className="w-full h-full relative overflow-hidden" style={{ background: bg, fontFamily: '"Segoe UI",system-ui,sans-serif' }}
      onMouseDown={() => { setCtx(null); setIconCtx(null); setStart(false); }}
      onContextMenu={(e) => { e.preventDefault(); setIconCtx(null); setCtx({ x: e.clientX, y: e.clientY }); }}>

      {/* masaüstü ikonları */}
      {deskItems.map((it, i) => {
        const p = iconPos[it.id] || defaultPos(i);
        const isRenaming = renaming?.id === it.id;
        return (
          <button key={it.id}
            onMouseDown={(e) => { if (e.button === 0 && !isRenaming) startIconDrag(e, it.id, p); }}
            onClick={(e) => { e.stopPropagation(); if (!isRenaming) iconClick(it.id, it.onOpen); }}
            onContextMenu={(e) => { e.preventDefault(); e.stopPropagation(); setCtx(null); setIconCtx({ x: e.clientX, y: e.clientY, id: it.id }); }}
            className="absolute flex flex-col items-center gap-1 w-[76px] p-1.5 rounded hover:bg-white/10 focus:bg-white/15"
            style={{ left: p.x, top: p.y }}>
            {it.icon}
            {isRenaming ? (
              <input autoFocus value={renaming!.name}
                onChange={(e) => setRenaming({ id: it.id, name: e.target.value })}
                onClick={(e) => e.stopPropagation()}
                onMouseDown={(e) => e.stopPropagation()}
                onBlur={() => { ops.rename(it.id, renaming!.name.trim() || it.name); setRenaming(null); }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') { ops.rename(it.id, renaming!.name.trim() || it.name); setRenaming(null); }
                  else if (e.key === 'Escape') setRenaming(null);
                }}
                className="text-[11.5px] text-center w-full rounded px-0.5 outline-none"
                style={{ background: '#fff', color: '#000', border: '1px solid #4cc2ff' }} />
            ) : (
              <span className="text-[11.5px] text-white text-center leading-tight" style={{ textShadow: '0 1px 3px rgba(0,0,0,0.8)' }}>{it.name}</span>
            )}
          </button>
        );
      })}

      {/* pencereler */}
      {wins.map(w => (
        <Win key={w.id} win={w} focused={w.z === Math.max(...wins.map(x => x.z))}
          onFocus={() => focus(w.id)} onClose={() => close(w.id)} onMin={() => toggleMin(w.id)} onMax={() => setWin(w.id, { max: !w.max })}
          onMove={(x, y) => setWin(w.id, { x, y })} onResize={(ww, hh) => setWin(w.id, { w: ww, h: hh })}>
          {w.kind === 'app' && <AppContent appId={w.appId as any} state={os} dispatch={dispatch} toast={toast} onClose={() => close(w.id)} onOpenApp={openApp} />}
          {w.kind === 'explorer' && <FileExplorer fs={fs} ops={ops} start={w.data?.start || 'root'} />}
          {w.kind === 'notepad' && fs[w.data?.nodeId] &&
            <Notepad name={fs[w.data.nodeId].name} content={fs[w.data.nodeId].content || ''}
              onChange={(v) => setFs(f => ({ ...f, [w.data.nodeId]: { ...f[w.data.nodeId], content: v } }))} />}
        </Win>
      ))}

      {/* sağ-tık menü */}
      {ctx && (
        <div className="absolute z-[950] py-1 rounded-lg text-[13px] text-gray-100"
          style={{ left: Math.min(ctx.x, window.innerWidth - 200), top: Math.min(ctx.y, window.innerHeight - 220), width: 190, background: 'rgba(43,43,45,0.95)', backdropFilter: 'blur(30px)', border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 12px 32px rgba(0,0,0,0.5)' }}
          onMouseDown={(e) => e.stopPropagation()}>
          <Ctx label="Yeni Klasör" icon={<Folder size={15} />} onClick={() => { createNode('root', 'folder'); setCtx(null); }} />
          <Ctx label="Yeni Metin Belgesi" icon={<FileText size={15} />} onClick={() => { createNode('root', 'txt'); setCtx(null); }} />
          <div className="my-1 h-px bg-white/10" />
          <Ctx label="Arka Planı Değiştir" icon={<ImageIcon size={15} />} onClick={() => { fileInput.current?.click(); setCtx(null); }} />
          <Ctx label="Yenile" icon={<RefreshCw size={15} />} onClick={() => setCtx(null)} />
        </div>
      )}

      {/* per-ikon sağ-tık menü (Aç / Yeniden Adlandır / Sil) */}
      {iconCtx && (() => {
        const isFs = !!fs[iconCtx.id];
        const target = deskItems.find(d => d.id === iconCtx.id);
        return (
          <div className="absolute z-[960] py-1 rounded-lg text-[13px] text-gray-100"
            style={{ left: Math.min(iconCtx.x, window.innerWidth - 190), top: Math.min(iconCtx.y, window.innerHeight - 160), width: 180, background: 'rgba(43,43,45,0.97)', backdropFilter: 'blur(24px)', border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 12px 32px rgba(0,0,0,0.5)' }}
            onMouseDown={(e) => e.stopPropagation()}>
            <Ctx label="Aç" icon={<FolderOpen size={15} />} onClick={() => { target?.onOpen(); setIconCtx(null); }} />
            {isFs && <Ctx label="Yeniden Adlandır" icon={<Pencil size={15} />} onClick={() => { setRenaming({ id: iconCtx.id, name: fs[iconCtx.id].name }); setIconCtx(null); }} />}
            {isFs && <Ctx label="Sil" icon={<Trash2 size={15} />} onClick={() => { ops.del(iconCtx.id); setIconCtx(null); }} />}
          </div>
        );
      })()}

      <input ref={fileInput} type="file" accept="image/png,image/jpeg" className="hidden" onChange={onWallFile} />

      {/* Toast bildirimleri (Windows tarzı sağ alt) */}
      {os.toasts.map((t, i) => (
        <div key={t.id} className="absolute right-4 z-[970] px-4 py-3 rounded-xl text-[13px] text-white anim-slideup"
          style={{ bottom: 60 + i * 52, background: 'rgba(36,36,40,0.96)', backdropFilter: 'blur(20px)', border: '1px solid rgba(255,255,255,0.12)', boxShadow: '0 10px 30px rgba(0,0,0,0.5)' }}>
          {t.text}
        </div>
      ))}

      {start && <StartMenu userName={os.userName} onOpen={launch} onClose={() => setStart(false)} />}

      <Taskbar time={time} dateStr={dateStr} startOpen={start} windows={wins}
        onStart={() => setStart(s => !s)} onOpen={launch} onTask={(id) => focus(id)} />
    </div>
  );
}

function Ctx({ label, icon, onClick }: { label: string; icon: React.ReactNode; onClick: () => void }) {
  return <button onClick={onClick} className="flex items-center gap-2.5 w-full px-3 py-1.5 hover:bg-white/12 text-left">{icon}<span>{label}</span></button>;
}

function load(key: string): any { try { return JSON.parse(localStorage.getItem(key) || 'null'); } catch { return null; } }
