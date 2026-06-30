import React, { useRef } from 'react';
import { X, Minus, Square } from 'lucide-react';

export interface WinData {
  id: string;
  kind: 'app' | 'explorer' | 'notepad';
  appId?: string;
  title: string;
  icon?: React.ReactNode;
  x: number; y: number; w: number; h: number; z: number;
  min: boolean; max: boolean;
  data?: any;
}

interface Props {
  win: WinData; focused: boolean;
  onFocus: () => void; onClose: () => void; onMin: () => void; onMax: () => void;
  onMove: (x: number, y: number) => void; onResize: (w: number, h: number) => void;
  children: React.ReactNode;
}

export function Win({ win, focused, onFocus, onClose, onMin, onMax, onMove, onResize, children }: Props) {
  const drag = useRef<{ sx: number; sy: number; ox: number; oy: number } | null>(null);
  const rez = useRef<{ sx: number; sy: number; ow: number; oh: number } | null>(null);

  const startDrag = (e: React.MouseEvent) => {
    if (win.max) return;
    onFocus();
    drag.current = { sx: e.clientX, sy: e.clientY, ox: win.x, oy: win.y };
    const mv = (ev: MouseEvent) => { const d = drag.current; if (!d) return; onMove(d.ox + ev.clientX - d.sx, Math.max(0, d.oy + ev.clientY - d.sy)); };
    const up = () => { drag.current = null; document.removeEventListener('mousemove', mv); document.removeEventListener('mouseup', up); };
    document.addEventListener('mousemove', mv); document.addEventListener('mouseup', up);
  };
  const startRez = (e: React.MouseEvent) => {
    e.stopPropagation(); onFocus();
    rez.current = { sx: e.clientX, sy: e.clientY, ow: win.w, oh: win.h };
    const mv = (ev: MouseEvent) => { const r = rez.current; if (!r) return; onResize(Math.max(380, r.ow + ev.clientX - r.sx), Math.max(260, r.oh + ev.clientY - r.sy)); };
    const up = () => { rez.current = null; document.removeEventListener('mousemove', mv); document.removeEventListener('mouseup', up); };
    document.addEventListener('mousemove', mv); document.addEventListener('mouseup', up);
  };

  const pos: React.CSSProperties = win.max
    ? { left: 0, top: 0, width: '100%', height: 'calc(100% - 48px)' }
    : { left: win.x, top: win.y, width: win.w, height: win.h };

  return (
    <div
      onMouseDown={onFocus}
      style={{
        position: 'absolute', zIndex: win.z, display: win.min ? 'none' : 'flex', flexDirection: 'column',
        borderRadius: win.max ? 0 : 9, overflow: 'hidden', background: '#202020',
        border: '1px solid ' + (focused ? 'rgba(255,255,255,0.18)' : 'rgba(255,255,255,0.08)'),
        boxShadow: focused ? '0 32px 80px rgba(0,0,0,0.55)' : '0 12px 32px rgba(0,0,0,0.4)', ...pos,
      }}
    >
      <div onMouseDown={startDrag} onDoubleClick={onMax}
        style={{ height: 34, display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 12, background: focused ? '#2b2b2b' : '#262626', userSelect: 'none' }}>
        <span style={{ display: 'flex', width: 16, height: 16 }}>{win.icon}</span>
        <span style={{ flex: 1, fontSize: 12.5, color: '#e8e8e8', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{win.title}</span>
        <button onClick={onMin} className="w-[40px] h-[34px] flex items-center justify-center text-gray-300 hover:bg-white/10"><Minus size={15} /></button>
        <button onClick={onMax} className="w-[40px] h-[34px] flex items-center justify-center text-gray-300 hover:bg-white/10"><Square size={11} /></button>
        <button onClick={onClose} className="w-[42px] h-[34px] flex items-center justify-center text-gray-300 hover:bg-[#c42b1c] hover:text-white"><X size={16} /></button>
      </div>
      <div style={{ flex: 1, overflow: 'hidden', background: '#1a1a1a', position: 'relative' }}>{children}</div>
      {!win.max && <div onMouseDown={startRez} style={{ position: 'absolute', right: 0, bottom: 0, width: 16, height: 16, cursor: 'nwse-resize' }} />}
    </div>
  );
}
