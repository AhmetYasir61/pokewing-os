import React, { useEffect, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_INVENTORY } from '../data';
import { hasBridge, getDepo, claimItem } from '../bridge';
import { Package, Trash2, ArrowDownToLine } from 'lucide-react';

interface Props { onBack: () => void; coins: number; onToast: (msg: string) => void; }

const TYPE_COLORS: Record<string, string> = {
  'Rütbe': '#FFD60A', 'Kit': '#0A84FF', 'Kozmetik': '#BF5AF2',
  'Özellik': '#30D158', 'Anahtar': '#FF9F0A', 'Paket': '#FF375F',
};

export function DepoApp({ onBack, coins, onToast }: Props) {
  const [items, setItems] = useState(MOCK_INVENTORY);
  const [selected, setSelected] = useState<number | null>(null);
  const [tab, setTab] = useState<'envanter' | 'islem'>('envanter');

  // Oyun içi köprü varsa gerçek depo eşyalarını çek; yoksa mock kalır.
  useEffect(() => {
    if (!hasBridge()) return;
    let alive = true;
    getDepo().then(d => { if (alive && d) setItems(d); });
    return () => { alive = false; };
  }, []);

  const handleUse = (id: number) => {
    const item = items.find(i => i.id === id);
    if (hasBridge()) {
      // Gerçek: sunucu eşyayı envantere teslim eder (komutları çalıştırır)
      claimItem(id).then(r => {
        if (r && r.ok) { setItems(prev => prev.filter(i => i.id !== id)); onToast(r.msg || `${item?.name} teslim alındı!`); }
        else onToast(r?.msg || 'Teslim alınamadı (envanterde yer yok?).');
        setSelected(null);
      });
    } else {
      onToast(`${item?.name} teslim alındı!`);
      setItems(prev => prev.filter(i => i.id !== id));
      setSelected(null);
    }
  };

  const handleDelete = (id: number) => {
    setItems(prev => prev.filter(i => i.id !== id));
    setSelected(null);
    onToast('Eşya silindi.');
  };

  const HISTORY = [
    { type: 'Satın Alma', item: 'VIP Rütbesi', date: '30 Haz', amount: -5000, color: '#FF453A' },
    { type: 'Kazanım', item: 'Haftalık Ödül', date: '28 Haz', amount: 2500, color: '#30D158' },
    { type: 'Satın Alma', item: 'Efsane Anahtarı', date: '25 Haz', amount: -3200, color: '#FF453A' },
    { type: 'Kazanım', item: 'PvP Ödülü', date: '22 Haz', amount: 5000, color: '#30D158' },
  ];

  return (
    <AppWindow title="Depo" accentColor="#0A84FF" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Balance */}
        <div className="px-4 py-3 flex items-center gap-3" style={{ borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}>
          <div className="flex-1">
            <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>Toplam Coin</div>
            <div className="text-2xl font-bold" style={{ color: '#FFD60A' }}>⬡ {coins.toLocaleString('tr-TR')}</div>
          </div>
          <div className="flex flex-col items-end">
            <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>Eşya Sayısı</div>
            <div className="text-xl font-bold text-white">{items.length}</div>
          </div>
        </div>

        {/* Tabs */}
        <div className="px-4 py-2">
          <div className="seg-ctrl">
            <button className={`seg-btn ${tab === 'envanter' ? 'active' : ''}`} onClick={() => setTab('envanter')}>Envanter</button>
            <button className={`seg-btn ${tab === 'islem' ? 'active' : ''}`} onClick={() => setTab('islem')}>İşlem Geçmişi</button>
          </div>
        </div>

        {tab === 'envanter' ? (
          <div className="flex-1 scroll-area px-4 pb-4 flex flex-col gap-2 mt-1">
            {items.length === 0 ? (
              <div className="flex flex-col items-center py-16 gap-3">
                <Package size={40} color="rgba(235,235,245,0.2)" />
                <span style={{ color: 'rgba(235,235,245,0.4)', fontSize: 15 }}>Envanter boş</span>
              </div>
            ) : (
              items.map(item => (
                <div
                  key={item.id}
                  className="rounded-2xl p-4 flex items-center gap-3"
                  style={{ background: 'rgba(28,28,30,0.9)', border: `0.5px solid ${selected === item.id ? '#0A84FF44' : 'rgba(255,255,255,0.06)'}` }}
                  onClick={() => setSelected(selected === item.id ? null : item.id)}
                >
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ background: `${TYPE_COLORS[item.type] ?? '#888'}22` }}
                  >
                    <Package size={22} color={TYPE_COLORS[item.type] ?? '#888'} />
                  </div>
                  <div className="flex-1">
                    <div className="text-white font-semibold text-[15px]">{item.name}</div>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-[11px] px-1.5 py-0.5 rounded-full" style={{ background: `${TYPE_COLORS[item.type]}22`, color: TYPE_COLORS[item.type] }}>
                        {item.type}
                      </span>
                      {item.slots && (
                        <span className="text-[12px]" style={{ color: 'rgba(235,235,245,0.5)' }}>x{item.slots}</span>
                      )}
                    </div>
                  </div>
                  {selected === item.id && (
                    <div className="flex gap-2">
                      <button className="pill-btn primary text-xs py-1.5 px-3 flex items-center gap-1" onClick={e => { e.stopPropagation(); handleUse(item.id); }}>
                        <ArrowDownToLine size={13} /> Teslim Al
                      </button>
                      <button className="pill-btn danger text-xs py-1.5 px-3" onClick={e => { e.stopPropagation(); handleDelete(item.id); }}>
                        <Trash2 size={13} />
                      </button>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        ) : (
          <div className="flex-1 scroll-area px-4 pb-4 flex flex-col gap-2 mt-1">
            {HISTORY.map((h, i) => (
              <div key={i} className="rounded-2xl p-4 flex items-center gap-3" style={{ background: 'rgba(28,28,30,0.9)' }}>
                <div className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0" style={{ background: `${h.color}22` }}>
                  <span style={{ fontSize: 18 }}>{h.amount > 0 ? '↑' : '↓'}</span>
                </div>
                <div className="flex-1">
                  <div className="text-white font-semibold text-sm">{h.item}</div>
                  <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{h.type} · {h.date}</div>
                </div>
                <div className="font-bold text-base" style={{ color: h.color }}>
                  {h.amount > 0 ? '+' : ''}{h.amount.toLocaleString('tr-TR')} ⬡
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </AppWindow>
  );
}
