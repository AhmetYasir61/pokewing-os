import React, { useState, useRef, useEffect } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_CONTACTS } from '../data';
import { Contact, Message } from '../types';
import { hasBridge, sendMsgBridge, markMsgRead } from '../bridge';
import { Send, ChevronLeft, Phone, Video } from 'lucide-react';

interface Props {
  onBack: () => void;
  threads: Record<string, Message[]>;
  onSend: (name: string, text: string) => void;
  onRecv: (name: string, text: string) => void;
  onToast: (msg: string) => void;
}

const REPLIES = [
  'Harika!', 'Tamam, anlıyorum.', 'Haha evet biliyorum!', 'Şimdi gidebilirim.',
  'Bekle biraz araştırayım.', 'Evet, benimle takıl!', 'Güzel fikir!', '👍',
];

export function MesajApp({ onBack, threads, onSend, onRecv, onToast }: Props) {
  const [selected, setSelected] = useState<Contact | null>(null);
  const [input, setInput] = useState('');
  const msgEnd = useRef<HTMLDivElement>(null);

  const messages: Message[] = selected ? (threads[selected.name] ?? []) : [];

  useEffect(() => {
    msgEnd.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  // Sohbet açılınca mod tarafında "okundu" işaretle (rozet + bildirim bastırma)
  useEffect(() => {
    if (hasBridge()) markMsgRead(selected ? selected.name : '');
  }, [selected]);

  const send = () => {
    if (!selected || !input.trim()) return;
    const text = input.trim();
    onSend(selected.name, text);
    setInput('');
    if (hasBridge()) {
      // Gerçek mesaj: sunucu üzerinden hedef oyuncuya iletilir
      sendMsgBridge(selected.name, text);
    } else {
      // Önizleme (köprü yok) → sahte otomatik yanıt
      setTimeout(() => {
        const reply = REPLIES[Math.floor(Math.random() * REPLIES.length)];
        onRecv(selected.name, reply);
      }, 1200 + Math.random() * 1000);
    }
  };

  if (selected) {
    return (
      <div className="absolute inset-0 z-10 flex flex-col" style={{ background: 'rgb(10,10,12)' }}>
        {/* Chat header */}
        <div className="flex items-center gap-3 px-3 py-2 flex-shrink-0" style={{ borderBottom: '0.5px solid rgba(255,255,255,0.06)', paddingTop: 8 }}>
          <button onClick={() => setSelected(null)} className="flex items-center gap-0.5 focus:outline-none" style={{ color: '#0A84FF', fontWeight: 600 }}>
            <ChevronLeft size={22} color="#0A84FF" strokeWidth={2.2} />
          </button>
          <div
            className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
            style={{ background: `hsl(${selected.name.charCodeAt(0) * 20},60%,40%)` }}
          >
            {selected.name[0]}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-white font-semibold text-[15px]">{selected.name}</div>
            <div className="text-xs" style={{ color: selected.online ? '#30D158' : 'rgba(235,235,245,0.4)' }}>
              {selected.online ? 'Çevrimiçi' : 'Çevrimdışı'}
            </div>
          </div>
          <button className="w-8 h-8 rounded-full flex items-center justify-center focus:outline-none" style={{ background: 'rgba(10,132,255,0.12)' }} onClick={() => onToast('Arama başlatıldı...')}>
            <Phone size={16} color="#0A84FF" />
          </button>
          <button className="w-8 h-8 rounded-full flex items-center justify-center focus:outline-none" style={{ background: 'rgba(10,132,255,0.12)' }} onClick={() => onToast('Görüntülü arama...')}>
            <Video size={16} color="#0A84FF" />
          </button>
        </div>

        {/* Messages */}
        <div className="flex-1 scroll-area flex flex-col gap-2 px-4 py-3">
          {messages.length === 0 && (
            <div className="flex flex-col items-center py-8 gap-2">
              <div className="w-16 h-16 rounded-full flex items-center justify-center font-bold text-white text-2xl" style={{ background: `hsl(${selected.name.charCodeAt(0) * 20},60%,40%)` }}>
                {selected.name[0]}
              </div>
              <div className="text-white font-semibold mt-1">{selected.name}</div>
              <div className="text-sm" style={{ color: 'rgba(235,235,245,0.45)' }}>Sohbet başlatmak için mesaj gönder</div>
            </div>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`flex ${msg.who === 'me' ? 'justify-end' : 'justify-start'}`}>
              <div className="flex flex-col gap-0.5" style={{ alignItems: msg.who === 'me' ? 'flex-end' : 'flex-start' }}>
                <div className={`bubble ${msg.who}`}>{msg.text}</div>
                <div style={{ fontSize: 10, color: 'rgba(235,235,245,0.3)', paddingLeft: 4, paddingRight: 4 }}>{msg.time}</div>
              </div>
            </div>
          ))}
          <div ref={msgEnd} />
        </div>

        {/* Input */}
        <div className="px-3 pb-5 pt-2 flex items-center gap-2 flex-shrink-0" style={{ borderTop: '0.5px solid rgba(255,255,255,0.06)' }}>
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && send()}
            placeholder="Mesaj..."
            style={{ flex: 1, background: 'rgba(44,44,46,0.9)', border: '0.5px solid rgba(255,255,255,0.1)', borderRadius: 22, padding: '10px 16px', color: '#fff', fontSize: 15, fontFamily: 'inherit', outline: 'none' }}
          />
          <button
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 focus:outline-none transition-all"
            style={{ background: input.trim() ? '#0A84FF' : 'rgba(44,44,46,0.9)' }}
            onClick={send}
          >
            <Send size={18} color={input.trim() ? '#fff' : 'rgba(235,235,245,0.4)'} />
          </button>
        </div>
      </div>
    );
  }

  return (
    <AppWindow title="Mesajlar" accentColor="#0A84FF" onBack={onBack}>
      <div className="scroll-area h-full">
        {MOCK_CONTACTS.map((contact, i) => {
          const lastMsg = threads[contact.name]?.slice(-1)[0];
          return (
            <button
              key={i}
              className="flex items-center gap-3 px-4 py-3 w-full text-left relative focus:outline-none active:bg-white/5"
              style={{ borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}
              onClick={() => setSelected(contact)}
            >
              <div className="relative">
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-white text-lg flex-shrink-0"
                  style={{ background: `hsl(${contact.name.charCodeAt(0) * 20},60%,40%)` }}
                >
                  {contact.name[0]}
                </div>
                {contact.online && (
                  <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2" style={{ background: '#30D158', borderColor: 'rgb(10,10,12)' }} />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <span className="text-white font-semibold text-[15px]">{contact.name}</span>
                  {lastMsg && <span style={{ fontSize: 11, color: 'rgba(235,235,245,0.4)' }}>{lastMsg.time}</span>}
                </div>
                <div className="text-[13px] truncate mt-0.5" style={{ color: 'rgba(235,235,245,0.55)' }}>
                  {lastMsg ? (lastMsg.who === 'me' ? `Sen: ${lastMsg.text}` : lastMsg.text) : contact.number}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </AppWindow>
  );
}
