import React, { useState } from 'react';
import { Folder, FileText, ChevronLeft, FolderPlus, FilePlus, Trash2, Monitor, HardDrive } from 'lucide-react';

export interface FsNode { id: string; name: string; type: 'folder' | 'txt'; parent: string; content?: string; }
export interface FsOps {
  create: (parent: string, type: 'folder' | 'txt') => void;
  rename: (id: string, name: string) => void;
  del: (id: string) => void;
  openTxt: (node: FsNode) => void;
}

export function FileExplorer({ fs, ops, start = 'root' }: { fs: Record<string, FsNode>; ops: FsOps; start?: string }) {
  const [cwd, setCwd] = useState(start);
  const children = Object.values(fs).filter(n => n.parent === cwd);
  const cur = fs[cwd];

  return (
    <div className="flex h-full text-[13px] text-gray-200" style={{ background: '#1c1c1c' }}>
      {/* sidebar */}
      <div className="w-44 flex-none p-2 space-y-1" style={{ background: '#202020', borderRight: '1px solid rgba(255,255,255,0.06)' }}>
        <Side icon={<Monitor size={16} />} label="Masaüstü" active={cwd === 'root'} onClick={() => setCwd('root')} />
        <Side icon={<HardDrive size={16} />} label="Bu Bilgisayar" active={false} onClick={() => setCwd('root')} />
      </div>
      {/* main */}
      <div className="flex-1 flex flex-col min-w-0">
        <div className="flex items-center gap-2 px-3 h-10 flex-none" style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
          <button disabled={cwd === 'root'} onClick={() => setCwd(cur?.parent || 'root')}
            className="p-1 rounded hover:bg-white/10 disabled:opacity-30"><ChevronLeft size={18} /></button>
          <span className="opacity-70">{cwd === 'root' ? 'Masaüstü' : cur?.name}</span>
          <div className="flex-1" />
          <button onClick={() => ops.create(cwd, 'folder')} className="flex items-center gap-1 px-2 py-1 rounded hover:bg-white/10 text-[12px]"><FolderPlus size={15} /> Klasör</button>
          <button onClick={() => ops.create(cwd, 'txt')} className="flex items-center gap-1 px-2 py-1 rounded hover:bg-white/10 text-[12px]"><FilePlus size={15} /> Metin</button>
        </div>
        <div className="flex-1 overflow-auto p-3" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,84px)', gap: 10, alignContent: 'start' }}>
          {children.length === 0 && <div className="col-span-full opacity-40 text-center mt-8">Bu klasör boş</div>}
          {children.map(n => (
            <button key={n.id}
              onDoubleClick={() => n.type === 'folder' ? setCwd(n.id) : ops.openTxt(n)}
              className="group relative flex flex-col items-center gap-1 p-2 rounded hover:bg-white/10 text-center">
              {n.type === 'folder' ? <Folder size={40} className="text-[#e8c069]" fill="#e8c069" /> : <FileText size={38} className="text-[#9fd0ff]" />}
              <span className="text-[11.5px] leading-tight break-words w-full">{n.name}</span>
              <span onClick={(e) => { e.stopPropagation(); ops.del(n.id); }}
                className="absolute top-0 right-0 opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-red-600/70"><Trash2 size={13} /></span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

function Side({ icon, label, active, onClick }: { icon: React.ReactNode; label: string; active: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} className={'flex items-center gap-2 w-full px-2 py-1.5 rounded ' + (active ? 'bg-white/15' : 'hover:bg-white/8')}>
      {icon}<span>{label}</span>
    </button>
  );
}
