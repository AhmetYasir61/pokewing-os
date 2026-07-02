import React, { useState, useRef, useEffect } from 'react';
import { MOCK_CONTACTS } from '../data';
import { Message } from '../types';
import { hasBridge, sendMsgBridge, markMsgRead, getPhoneDirectory } from '../bridge';
import { Send, ChevronLeft, Phone, Video, MessageCircle, UserPlus, X, Search } from 'lucide-react';

interface Props {
  onBack: () => void;
  threads: Record<string, Message[]>;
  onSend: (name: string, text: string) => void;
  onRecv: (name: string, text: string) => void;
  onToast: (msg: string) => void;
}

// WhatsApp koyu tema renkleri
const WA = {
  bg: '#0B141A', panel: '#111B21', header: '#202C33', accent: '#00A884',
  sent: '#005C4B', recv: '#202C33', text: '#E9EDEF', sub: '#8696A0',
};

interface SavedContact { name: string; number: string; }

const itemId = () => new URLSearchParams(window.location.search).get('item') || 'default';
const CKEY = () => 'pwcontacts:' + itemId();

function loadContacts(): SavedContact[] {
  try { return JSON.parse(localStorage.getItem(CKEY()) || 'null') || []; } catch { return []; }
}
function saveContacts(list: SavedContact[]) {
  try { localStorage.setItem(CKEY(), JSON.stringify(list)); } catch { /* kota */ }
}

const REPLIES = [
  'Harika!', 'Tamam, anlıyorum.', 'Haha evet biliyorum!', 'Şimdi gidebilirim.',
  'Bekle biraz araştırayım.', 'Evet, benimle takıl!', 'Güzel fikir!', '👍',
];

export function MesajApp({ onBack, threads, onSend, onRecv, onToast }: Props) {
  // Kayıtlı kişiler: oyun içinde localStorage (item'a bağlı); önizlemede mock rehber
  const [contacts, setContacts] = useState<SavedContact[]>(() =>
    hasBridge() ? loadContacts() : MOCK_CONTACTS.map(c => ({ name: c.name, number: c.number })));
  const [onlineNames, setOnlineNames] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<SavedContact | null>(null);
  const [input, setInput] = useState('');
  const [showAdd, setShowAdd] = useState(false);
  const [addNum, setAddNum] = useState('');
  const [addBusy, setAddBusy] = useState(false);
  const [search, setSearch] = useState('');
  const msgEnd = useRef<HTMLDivElement>(null);

  const messages: Message[] = selected ? (threads[selected.name] ?? []) : [];

  useEffect(() => { msgEnd.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages.length]);

  // Çevrimiçi durumu tazele (WhatsApp'taki "çevrimiçi" göstergesi)
  useEffect(() => {
    if (!hasBridge()) { setOnlineNames(new Set(MOCK_CONTACTS.filter(c => c.online).map(c => c.name))); return; }
    let alive = true;
    const tick = async () => {
      const d = await getPhoneDirectory();
      if (alive && d) setOnlineNames(new Set(d.online.map(o => o.name)));
    };
    tick();
    const t = setInterval(tick, 15000);
    return () => { alive = false; clearInterval(t); };
  }, []);

  // Sohbet açılınca okundu işaretle
  useEffect(() => { if (hasBridge()) markMsgRead(selected ? selected.name : ''); }, [selected]);

  // Numara ile kişi ekle — numara sunucuda kayıtlı bir oyuncuya ait olmalı (WhatsApp mantığı)
  const addContact = async () => {
    const num = addNum.trim();
    if (num.length < 4) { onToast('Geçerli bir numara gir.'); return; }
    if (contacts.some(c => c.number === num)) { onToast('Bu numara zaten rehberinde.'); return; }
    setAddBusy(true);
    if (hasBridge()) {
      const d = await getPhoneDirectory();
      setAddBusy(false);
      if (d?.myNumber === num) { onToast('Bu senin numaran 🙂'); return; }
      const found = d?.online.find(o => o.number === num);
      if (!found) { onToast('Numara bulunamadı. (Oyuncu çevrimiçi olmalı)'); return; }
      const next = [...contacts, { name: found.name, number: num }];
      setContacts(next); saveContacts(next);
      onToast(`${found.name} rehbere eklendi ✓`);
    } else {
      setAddBusy(false);
      onToast('Önizleme modunda kişi eklenemez.');
    }
    setShowAdd(false); setAddNum('');
  };

  const removeContact = (number: string) => {
    const next = contacts.filter(c => c.number !== number);
    setContacts(next); if (hasBridge()) saveContacts(next);
    onToast('Kişi silindi.');
  };

  const send = () => {
    if (!selected || !input.trim()) return;
    const text = input.trim();
    onSend(selected.name, text);
    setInput('');
    if (hasBridge()) {
      sendMsgBridge(selected.name, text);
    } else {
      setTimeout(() => onRecv(selected.name, REPLIES[Math.floor(Math.random() * REPLIES.length)]), 1200 + Math.random() * 1000);
    }
  };

  // Liste: kayıtlı kişiler + rehberde OLMAYAN ama mesaj atmış kişiler (bilinmeyen numara gibi)
  const unknownPeers = Object.keys(threads).filter(p => !contacts.some(c => c.name === p));
  const filteredContacts = contacts.filter(c =>
    !search.trim() || c.name.toLowerCase().includes(search.toLowerCase()) || c.number.includes(search));

  // ---------- SOHBET EKRANI ----------
  if (selected) {
    const online = onlineNames.has(selected.name);
    return (
      <div className="absolute inset-0 z-10 flex flex-col" style={{ background: WA.bg }}>
        {/* WA sohbet başlığı */}
        <div className="flex items-center gap-2.5 px-2.5 py-2 flex-shrink-0" style={{ background: WA.header, paddingTop: 10 }}>
          <button onClick={() => setSelected(null)} className="focus:outline-none p-1">
            <ChevronLeft size={22} color={WA.text} strokeWidth={2.2} />
          </button>
          <div className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
            style={{ background: `hsl(${selected.name.charCodeAt(0) * 20},55%,40%)` }}>
            {selected.name[0]}
          </div>
          <div className="flex-1 min-w-0">
            <div className="font-semibold text-[15px]" style={{ color: WA.text }}>{selected.name}</div>
            <div className="text-[11.5px]" style={{ color: online ? WA.accent : WA.sub }}>
              {online ? 'çevrimiçi' : 'çevrimdışı'}
            </div>
          </div>
          <button className="p-2 focus:outline-none" onClick={() => onToast(online ? 'Aramak için Telefon uygulamasını kullan' : 'Kişi çevrimdışı')}>
            <Phone size={18} color={WA.accent} />
          </button>
          <button className="p-2 focus:outline-none" onClick={() => onToast('Görüntülü arama yakında')}>
            <Video size={19} color={WA.accent} />
          </button>
        </div>

        {/* Mesajlar — WA duvar deseni hissi */}
        <div className="flex-1 scroll-area flex flex-col gap-1.5 px-3 py-3"
          style={{ background: `${WA.bg} url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='30' cy='30' r='1' fill='%23182229'/%3E%3C/svg%3E")` }}>
          {messages.length === 0 && (
            <div className="flex flex-col items-center py-10 gap-2">
              <div className="px-3 py-1.5 rounded-lg text-[12px]" style={{ background: '#182229', color: '#FFD279' }}>
                🔒 Mesajlar oyun sunucusu üzerinden iletilir
              </div>
            </div>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`flex ${msg.who === 'me' ? 'justify-end' : 'justify-start'}`}>
              <div className="relative max-w-[78%] px-3 py-1.5 text-[14.5px] leading-snug"
                style={{
                  background: msg.who === 'me' ? WA.sent : WA.recv,
                  color: WA.text,
                  borderRadius: 10,
                  borderTopRightRadius: msg.who === 'me' ? 2 : 10,
                  borderTopLeftRadius: msg.who === 'me' ? 10 : 2,
                  boxShadow: '0 1px 1px rgba(0,0,0,0.25)',
                }}>
                {msg.text}
                <span className="ml-2 align-bottom" style={{ fontSize: 10, color: 'rgba(233,237,239,0.55)' }}>
                  {msg.time}{msg.who === 'me' ? ' ✓✓' : ''}
                </span>
              </div>
            </div>
          ))}
          <div ref={msgEnd} />
        </div>

        {/* Giriş */}
        <div className="px-2.5 pb-4 pt-2 flex items-center gap-2 flex-shrink-0" style={{ background: WA.header }}>
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && send()}
            placeholder="Mesaj yaz..."
            style={{ flex: 1, background: '#2A3942', border: 'none', borderRadius: 22, padding: '10px 16px', color: WA.text, fontSize: 15, fontFamily: 'inherit', outline: 'none' }}
          />
          <button
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 focus:outline-none"
            style={{ background: WA.accent }}
            onClick={send}>
            <Send size={18} color="#fff" />
          </button>
        </div>
      </div>
    );
  }

  // ---------- KİŞİ LİSTESİ (WA ana ekran) ----------
  return (
    <div className="absolute inset-0 z-10 flex flex-col anim-winin" style={{ background: WA.panel }}>
      {/* WA başlık */}
      <div className="flex items-center gap-2 px-4 py-3 flex-shrink-0" style={{ background: WA.header, paddingTop: 12 }}>
        <button onClick={onBack} className="focus:outline-none p-0.5 -ml-1">
          <ChevronLeft size={22} color={WA.text} strokeWidth={2.2} />
        </button>
        <span className="flex-1 font-bold text-[19px]" style={{ color: WA.accent }}>PokeChat</span>
        <MessageCircle size={20} color={WA.sub} />
      </div>

      {/* Arama */}
      <div className="px-3 py-2" style={{ background: WA.panel }}>
        <div className="flex items-center gap-2 px-3 py-2 rounded-xl" style={{ background: WA.header }}>
          <Search size={14} color={WA.sub} />
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Ara veya numara gir"
            style={{ flex: 1, background: 'none', border: 'none', outline: 'none', color: WA.text, fontSize: 14, fontFamily: 'inherit', padding: 0 }} />
        </div>
      </div>

      <div className="flex-1 scroll-area">
        {/* Rehberde olmayan (bilinmeyen) mesajlar */}
        {unknownPeers.map(peer => {
          const last = threads[peer]?.slice(-1)[0];
          return (
            <button key={peer} onClick={() => setSelected({ name: peer, number: '' })}
              className="flex items-center gap-3 px-4 py-3 w-full text-left focus:outline-none active:bg-white/5"
              style={{ borderBottom: '0.5px solid rgba(134,150,160,0.12)' }}>
              <div className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-white text-lg flex-shrink-0"
                style={{ background: `hsl(${peer.charCodeAt(0) * 20},55%,40%)` }}>{peer[0]}</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-[15px]" style={{ color: WA.text }}>{peer}</span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded-full" style={{ background: 'rgba(255,159,10,0.15)', color: '#FF9F0A' }}>rehberde yok</span>
                </div>
                <div className="text-[13px] truncate mt-0.5" style={{ color: WA.sub }}>
                  {last ? (last.who === 'me' ? `Sen: ${last.text}` : last.text) : ''}
                </div>
              </div>
              {last && <span style={{ fontSize: 11, color: WA.sub }}>{last.time}</span>}
            </button>
          );
        })}

        {/* Kayıtlı kişiler */}
        {filteredContacts.map(contact => {
          const last = threads[contact.name]?.slice(-1)[0];
          const online = onlineNames.has(contact.name);
          return (
            <button key={contact.number || contact.name} onClick={() => setSelected(contact)}
              onContextMenu={(e) => { e.preventDefault(); removeContact(contact.number); }}
              className="flex items-center gap-3 px-4 py-3 w-full text-left focus:outline-none active:bg-white/5"
              style={{ borderBottom: '0.5px solid rgba(134,150,160,0.12)' }}>
              <div className="relative flex-shrink-0">
                <div className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-white text-lg"
                  style={{ background: `hsl(${contact.name.charCodeAt(0) * 20},55%,40%)` }}>{contact.name[0]}</div>
                {online && <div className="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2"
                  style={{ background: WA.accent, borderColor: WA.panel }} />}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-[15px]" style={{ color: WA.text }}>{contact.name}</span>
                  {last && <span style={{ fontSize: 11, color: WA.sub }}>{last.time}</span>}
                </div>
                <div className="text-[13px] truncate mt-0.5" style={{ color: WA.sub }}>
                  {last ? (last.who === 'me' ? `Sen: ${last.text}` : last.text) : `~${contact.number}`}
                </div>
              </div>
            </button>
          );
        })}

        {contacts.length === 0 && unknownPeers.length === 0 && (
          <div className="flex flex-col items-center py-16 gap-3 px-8">
            <MessageCircle size={40} color="rgba(134,150,160,0.3)" />
            <div className="font-semibold" style={{ color: WA.text }}>Rehberin boş</div>
            <div className="text-center text-sm" style={{ color: WA.sub }}>
              Mesajlaşmak için kişinin telefon numarasını öğren ve + ile ekle. (Numara: Telefon uygulamasında görünür)
            </div>
          </div>
        )}
      </div>

      {/* Yeni kişi FAB */}
      <button onClick={() => setShowAdd(true)}
        className="absolute bottom-6 right-5 w-14 h-14 rounded-2xl flex items-center justify-center focus:outline-none"
        style={{ background: WA.accent, boxShadow: '0 8px 24px rgba(0,168,132,0.4)' }}>
        <UserPlus size={22} color="#fff" />
      </button>

      {/* Kişi ekleme modalı */}
      {showAdd && (
        <div className="absolute inset-0 z-20 flex items-end anim-fadein"
          style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)' }}
          onClick={() => setShowAdd(false)}>
          <div className="w-full anim-slideup rounded-t-3xl p-6" style={{ background: WA.header }}
            onClick={e => e.stopPropagation()}>
            <div className="w-10 h-1 rounded-full mx-auto mb-5" style={{ background: 'rgba(255,255,255,0.2)' }} />
            <div className="font-bold text-xl mb-1" style={{ color: WA.text }}>Kişi Ekle</div>
            <div className="text-sm mb-4" style={{ color: WA.sub }}>
              Kişinin telefon numarasını gir — numara sunucuda kayıtlı bir oyuncuya ait olmalı.
            </div>
            <input value={addNum} onChange={e => setAddNum(e.target.value.replace(/[^0-9]/g, ''))}
              onKeyDown={e => e.key === 'Enter' && addContact()}
              placeholder="Telefon numarası (ör. 5111222)" inputMode="numeric" />
            <div className="flex gap-3 mt-5">
              <button className="pill-btn ghost flex-1" onClick={() => setShowAdd(false)}>İptal</button>
              <button className="pill-btn flex-1 flex items-center justify-center gap-2"
                style={{ background: WA.accent, color: '#fff', fontWeight: 700 }}
                disabled={addBusy} onClick={addContact}>
                {addBusy ? <div className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} /> : <><UserPlus size={15} /> Ekle</>}
              </button>
            </div>
            <div className="text-center mt-3 text-[11px]" style={{ color: WA.sub }}>
              İpucu: Kişiye uzun bas / sağ tıkla → rehberden sil
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
