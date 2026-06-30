import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { Plus, Trash2, Check } from 'lucide-react';
import { OSState } from '../types';

interface Props {
  notes: OSState['notes'];
  activeNote: string | null;
  onBack: () => void;
  onCreate: (title: string, body: string) => void;
  onUpdate: (id: string, title: string, body: string) => void;
  onDelete: (id: string) => void;
  onOpenNote: (id: string | null) => void;
  onToast: (msg: string) => void;
}

export function NotlarApp({ notes, activeNote, onBack, onCreate, onUpdate, onDelete, onOpenNote, onToast }: Props) {
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');

  const openNote = notes.find(n => n.id === activeNote);

  if (creating || openNote) {
    const isEdit = !!openNote;
    const editTitle = isEdit ? openNote.title : title;
    const editBody = isEdit ? openNote.body : body;

    return (
      <AppWindow
        title={isEdit ? 'Düzenle' : 'Yeni Not'}
        accentColor="#FFD60A"
        onBack={() => { setCreating(false); setTitle(''); setBody(''); onOpenNote(null); }}
        headerRight={
          <div className="flex items-center gap-2">
            {isEdit && (
              <button
                onClick={() => { onDelete(openNote.id); onToast('Not silindi'); }}
                className="w-8 h-8 flex items-center justify-center rounded-full focus:outline-none"
                style={{ background: 'rgba(255,69,58,0.12)' }}
              >
                <Trash2 size={15} color="#FF453A" />
              </button>
            )}
            <button
              onClick={() => {
                if (isEdit) {
                  onUpdate(openNote.id, editTitle, editBody);
                  onToast('Not güncellendi');
                  onOpenNote(null);
                } else {
                  if (!title.trim()) return;
                  onCreate(title.trim(), body);
                  setCreating(false);
                  setTitle('');
                  setBody('');
                  onToast('Not oluşturuldu');
                }
              }}
              className="w-8 h-8 flex items-center justify-center rounded-full focus:outline-none"
              style={{ background: 'rgba(255,214,10,0.15)' }}
            >
              <Check size={16} color="#FFD60A" />
            </button>
          </div>
        }
      >
        <div className="flex flex-col h-full px-4 py-3">
          <input
            type="text"
            value={isEdit ? openNote.title : title}
            onChange={e => isEdit ? onUpdate(openNote.id, e.target.value, openNote.body) : setTitle(e.target.value)}
            placeholder="Başlık..."
            style={{ background: 'none', border: 'none', borderBottom: '0.5px solid rgba(255,255,255,0.1)', borderRadius: 0, fontSize: 22, fontWeight: 700, marginBottom: 12, padding: '8px 0' }}
          />
          <textarea
            value={isEdit ? openNote.body : body}
            onChange={e => isEdit ? onUpdate(openNote.id, openNote.title, e.target.value) : setBody(e.target.value)}
            placeholder="Notu buraya yaz..."
            style={{ flex: 1, background: 'none', border: 'none', outline: 'none', color: 'rgba(235,235,245,0.85)', fontSize: 15, fontFamily: 'inherit', lineHeight: 1.65, resize: 'none' }}
          />
        </div>
      </AppWindow>
    );
  }

  return (
    <AppWindow
      title="Notlar"
      accentColor="#FFD60A"
      onBack={onBack}
      headerRight={
        <button
          onClick={() => setCreating(true)}
          className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none"
          style={{ background: 'rgba(255,214,10,0.15)' }}
        >
          <Plus size={20} color="#FFD60A" />
        </button>
      }
    >
      <div className="scroll-area h-full px-4 py-3 flex flex-col gap-2">
        {notes.length === 0 ? (
          <div className="flex flex-col items-center py-16 gap-3">
            <span style={{ fontSize: 44 }}>📝</span>
            <span className="text-white font-semibold text-lg">Henüz not yok</span>
            <button className="pill-btn primary" onClick={() => setCreating(true)}>İlk Notunu Oluştur</button>
          </div>
        ) : (
          notes.map(note => (
            <button
              key={note.id}
              className="rounded-2xl p-4 text-left"
              style={{ background: 'rgba(255,214,10,0.06)', border: '0.5px solid rgba(255,214,10,0.12)' }}
              onClick={() => onOpenNote(note.id)}
            >
              <div className="flex items-start justify-between gap-2">
                <span className="text-white font-bold text-base">{note.title}</span>
                <span className="text-[11px] flex-shrink-0" style={{ color: 'rgba(235,235,245,0.4)' }}>{note.date}</span>
              </div>
              <div className="text-[13px] mt-1" style={{ color: 'rgba(235,235,245,0.55)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                {note.body}
              </div>
            </button>
          ))
        )}
      </div>
    </AppWindow>
  );
}
