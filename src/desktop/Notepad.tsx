interface Props { name: string; content: string; onChange: (v: string) => void; }

/** Basit Not Defteri — .txt dosyalarını düzenler (içerik dosya sistemine kaydedilir). */
export function Notepad({ name, content, onChange }: Props) {
  return (
    <div className="flex flex-col h-full" style={{ background: '#1e1e1e' }}>
      <div className="h-8 flex items-center px-3 text-[12px] text-gray-400 flex-none" style={{ borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        Dosya &nbsp; Düzen &nbsp; Görünüm &nbsp;·&nbsp; <span className="text-gray-300">{name}</span>
      </div>
      <textarea
        value={content}
        onChange={(e) => onChange(e.target.value)}
        spellCheck={false}
        className="flex-1 w-full resize-none outline-none p-3 text-[14px] leading-relaxed text-gray-100"
        style={{ background: '#1e1e1e', fontFamily: 'Consolas,Menlo,monospace' }}
        placeholder="Buraya yaz..."
      />
    </div>
  );
}
