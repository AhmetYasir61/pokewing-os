import React, { useEffect, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { hasBridge, getStreams, setStream, browseInGame, PwStream,
  getBroadcasters, spectate, PwBroadcaster } from '../bridge';
import { Radio, Play, Plus, X, Eye, Clock, Video } from 'lucide-react';

interface Props { onBack: () => void; onToast: (msg: string) => void; }

const KICK = '#53FC18';

// Önizleme (köprü yok) için örnek yayınlar
const MOCK_STREAMS: PwStream[] = [
  { name: 'AshKetchum', title: 'Shiny avı — gece boyu!', url: 'https://youtube.com', mins: 42 },
  { name: 'Misty', title: 'Gym savaşları canlı', url: 'https://youtube.com', mins: 15 },
];
const MOCK_BC: PwBroadcaster[] = [
  { name: 'Brock', activity: 'Oto sinematik', droneId: 1 },
  { name: 'Gary', activity: 'Pilot drone', droneId: 2 },
];

export function YayinApp({ onBack, onToast }: Props) {
  const [streams, setStreams] = useState<PwStream[]>(hasBridge() ? [] : MOCK_STREAMS);
  const [me, setMe] = useState('');
  const [live, setLive] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(hasBridge());
  const [broadcasters, setBroadcasters] = useState<PwBroadcaster[]>(hasBridge() ? [] : MOCK_BC);

  const refresh = async () => {
    if (!hasBridge()) return;
    const s = await getStreams();
    if (s) { setStreams(s.streams); setMe(s.me); setLive(s.live); }
    setBroadcasters(await getBroadcasters());   // drone yayıncıları (oyun içi canlı)
    setLoading(false);
  };

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 10000); // 10 sn'de bir liste tazele
    return () => clearInterval(t);
  }, []);

  const startStream = async () => {
    const u = url.trim();
    if (!u.startsWith('http')) { onToast('Geçerli bir yayın URL\'i gir (YouTube canlı vb.)'); return; }
    if (hasBridge()) {
      const s = await setStream(true, title.trim(), u);
      if (s) { setStreams(s.streams); setLive(s.live); }
      onToast('🔴 Yayının açıldı! Herkese duyuruldu.');
    } else {
      onToast('Önizleme modunda yayın açılamaz.');
    }
    setShowForm(false); setTitle(''); setUrl('');
  };

  const stopStream = async () => {
    if (hasBridge()) {
      const s = await setStream(false);
      if (s) { setStreams(s.streams); setLive(s.live); }
    }
    onToast('Yayın kapatıldı.');
  };

  const watch = (s: PwStream) => {
    if (hasBridge()) { onToast(`${s.name} izleniyor...`); void browseInGame(s.url); }
    else onToast('Önizlemede izleme yok (oyun içinde çalışır).');
  };

  const watchDrone = (b: PwBroadcaster) => {
    if (hasBridge()) { onToast(`🎥 ${b.name} yayınına giriliyor...`); spectate(b.droneId, b.name); }
    else onToast('Önizlemede izleme yok (oyun içinde çalışır).');
  };

  return (
    <AppWindow title="Yayın" accentColor={KICK} onBack={onBack}
      headerRight={live ? (
        <button onClick={stopStream} className="flex items-center gap-1.5 px-2.5 py-1 rounded-full"
          style={{ background: 'rgba(255,69,58,0.15)', color: '#FF453A', fontWeight: 700, fontSize: 12 }}>
          <X size={13} /> Yayını Kapat
        </button>
      ) : undefined}
    >
      <div className="flex flex-col h-full">
        {/* Canlı sayacı */}
        <div className="px-4 py-3 flex items-center gap-3" style={{ background: 'linear-gradient(90deg,rgba(83,252,24,0.10),transparent)' }}>
          <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'rgba(83,252,24,0.15)' }}>
            <Radio size={20} color={KICK} />
          </div>
          <div className="flex-1">
            <div className="text-white font-bold text-[16px]">Canlı Yayınlar</div>
            <div className="text-[12px]" style={{ color: 'rgba(235,235,245,0.5)' }}>
              {streams.length > 0 ? `${streams.length} yayın aktif` : 'Şu an kimse yayında değil'}
            </div>
          </div>
          {live && (
            <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold"
              style={{ background: '#FF453A', color: '#fff' }}>
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" /> YAYINDASIN
            </span>
          )}
        </div>

        {/* 🎥 Drone yayınları — oyun içi canlı kamera; İzle = spectate (kamera yayıncının drone'una bağlanır) */}
        {broadcasters.length > 0 && (
          <div className="px-4 pt-2 pb-1">
            <div className="flex items-center gap-2 mb-2">
              <Video size={14} color={KICK} />
              <span className="text-[12px] font-bold uppercase tracking-wide" style={{ color: KICK }}>
                Drone Yayınları · {broadcasters.length} canlı
              </span>
            </div>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {broadcasters.map((b, i) => (
                <button key={i} onClick={() => watchDrone(b)}
                  className="flex-shrink-0 rounded-xl px-3 py-2 flex items-center gap-2 text-left"
                  style={{ background: 'rgba(28,28,30,0.9)', border: `0.5px solid ${KICK}44`, minWidth: 150 }}>
                  <div className="w-8 h-8 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
                    style={{ background: `hsl(${b.name.charCodeAt(0) * 20},60%,40%)`, border: `2px solid ${KICK}` }}>
                    {b.name[0]}
                  </div>
                  <div className="min-w-0">
                    <div className="text-white font-semibold text-[13px] truncate">{b.name}</div>
                    <div className="text-[10px] flex items-center gap-1" style={{ color: '#FF453A' }}>
                      <span className="w-1.5 h-1.5 rounded-full animate-pulse" style={{ background: '#FF453A' }} />
                      {b.activity}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Yayın listesi */}
        <div className="flex-1 scroll-area px-4 py-2 flex flex-col gap-3">
          {loading && <div className="flex justify-center py-10"><div className="spinner" /></div>}
          {!loading && streams.length === 0 && (
            <div className="flex flex-col items-center py-14 gap-3">
              <Radio size={40} color="rgba(235,235,245,0.2)" />
              <div className="text-white font-semibold">Henüz yayın yok</div>
              <div className="text-center text-sm px-6" style={{ color: 'rgba(235,235,245,0.45)' }}>
                İlk yayını sen aç! YouTube canlı linkini paylaş, herkes oyun içinden izlesin.
              </div>
            </div>
          )}
          {streams.map((s, i) => (
            <button key={i} onClick={() => watch(s)}
              className="rounded-2xl overflow-hidden text-left"
              style={{ background: 'rgba(28,28,30,0.9)', border: '0.5px solid rgba(83,252,24,0.15)' }}>
              {/* küçük "thumbnail" şeridi */}
              <div className="h-16 flex items-center justify-center relative"
                style={{ background: `linear-gradient(135deg, hsl(${s.name.charCodeAt(0) * 25},50%,18%), rgba(83,252,24,0.12))` }}>
                <Play size={26} color="rgba(255,255,255,0.85)" fill="rgba(255,255,255,0.85)" />
                <span className="absolute top-2 left-2 flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold"
                  style={{ background: '#FF453A', color: '#fff' }}>
                  <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" /> CANLI
                </span>
                <span className="absolute bottom-2 right-2 flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded"
                  style={{ background: 'rgba(0,0,0,0.6)', color: '#fff' }}>
                  <Clock size={10} /> {s.mins} dk
                </span>
              </div>
              <div className="p-3 flex items-center gap-3">
                <div className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
                  style={{ background: `hsl(${s.name.charCodeAt(0) * 20},60%,40%)`, border: `2px solid ${KICK}` }}>
                  {s.name[0]}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-white font-semibold text-[14px] truncate">{s.title}</div>
                  <div className="text-[12px]" style={{ color: KICK }}>{s.name}</div>
                </div>
                <Eye size={16} color="rgba(235,235,245,0.4)" />
              </div>
            </button>
          ))}
        </div>

        {/* Yayın Aç butonu */}
        {!live && (
          <div className="px-4 pb-5">
            <button onClick={() => setShowForm(true)}
              className="w-full flex items-center justify-center gap-2 py-3 rounded-2xl font-bold text-[15px]"
              style={{ background: KICK, color: '#000' }}>
              <Plus size={18} strokeWidth={2.5} /> Yayın Aç
            </button>
          </div>
        )}
      </div>

      {/* Yayın açma formu */}
      {showForm && (
        <div className="absolute inset-0 z-10 flex items-end anim-fadein"
          style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)' }}
          onClick={() => setShowForm(false)}>
          <div className="w-full anim-slideup rounded-t-3xl p-6" style={{ background: 'rgb(18,18,22)' }}
            onClick={e => e.stopPropagation()}>
            <div className="w-10 h-1 rounded-full mx-auto mb-5" style={{ background: 'rgba(255,255,255,0.2)' }} />
            <div className="text-white font-bold text-xl mb-1">🔴 Yayın Aç</div>
            <div className="text-sm mb-4" style={{ color: 'rgba(235,235,245,0.55)' }}>
              YouTube canlı (veya video) linkini paylaş — herkes oyun içi tarayıcıdan izler.
            </div>
            <input value={title} onChange={e => setTitle(e.target.value)} placeholder="Yayın başlığı (ör. Shiny avı!)"
              style={{ marginBottom: 10 }} />
            <input value={url} onChange={e => setUrl(e.target.value)} placeholder="https://youtube.com/watch?v=..." />
            <div className="flex gap-3 mt-5">
              <button className="pill-btn ghost flex-1" onClick={() => setShowForm(false)}>İptal</button>
              <button className="pill-btn flex-1" style={{ background: KICK, color: '#000', fontWeight: 700 }}
                onClick={startStream}>Yayını Başlat</button>
            </div>
          </div>
        </div>
      )}
    </AppWindow>
  );
}
