import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_PRODUCTS } from '../data';
import { Product } from '../types';
import { ShoppingCart, Star, Zap, Check, X } from 'lucide-react';

interface Props {
  coins: number;
  onBack: () => void;
  onBuy: (price: number) => void;
  onToast: (msg: string) => void;
}

export function MarketApp({ coins, onBack, onBuy, onToast }: Props) {
  const [tab, setTab] = useState<'tumu' | 'rutbe' | 'kit' | 'kozmetik'>('tumu');
  const [selected, setSelected] = useState<Product | null>(null);
  const [buying, setBuying] = useState(false);
  const [bought, setBought] = useState<Set<number>>(new Set());

  const TABS = [
    { id: 'tumu', label: 'Tümü' },
    { id: 'rutbe', label: 'Rütbe' },
    { id: 'kit', label: 'Kit' },
    { id: 'kozmetik', label: 'Özel' },
  ] as const;

  const filtered = tab === 'tumu'
    ? MOCK_PRODUCTS
    : MOCK_PRODUCTS.filter(p => {
        if (tab === 'rutbe') return p.type === 'Rütbe';
        if (tab === 'kit') return p.type === 'Kit';
        return ['Kozmetik', 'Özellik', 'Anahtar', 'Paket'].includes(p.type);
      });

  const handleBuy = () => {
    if (!selected) return;
    if (coins < selected.price) { onToast('Yetersiz Coin!'); setSelected(null); return; }
    setBuying(true);
    setTimeout(() => {
      onBuy(selected.price);
      setBought(prev => new Set([...prev, selected.id]));
      setBuying(false);
      onToast(`${selected.name} satın alındı!`);
      setSelected(null);
    }, 1200);
  };

  const TYPE_COLORS: Record<string, string> = {
    'Rütbe': '#FFD60A', 'Kit': '#0A84FF', 'Kozmetik': '#BF5AF2',
    'Özellik': '#30D158', 'Anahtar': '#FF9F0A', 'Paket': '#FF375F',
  };

  return (
    <AppWindow title="Market" accentColor="#FFD60A" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Balance banner */}
        <div className="px-4 py-3" style={{ background: 'linear-gradient(90deg,rgba(255,214,10,0.12),rgba(255,159,10,0.08))' }}>
          <div className="flex items-center justify-between">
            <div>
              <div className="text-xs font-medium" style={{ color: 'rgba(235,235,245,0.5)' }}>Bakiyen</div>
              <div className="font-bold text-2xl" style={{ color: '#FFD60A' }}>
                ⬡ {coins.toLocaleString('tr-TR')}
                <span className="text-sm font-medium ml-1" style={{ color: 'rgba(235,235,245,0.6)' }}>Coin</span>
              </div>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-2 rounded-xl" style={{ background: 'rgba(255,214,10,0.15)' }}>
              <Zap size={16} color="#FFD60A" />
              <span className="text-sm font-semibold" style={{ color: '#FFD60A' }}>Coin Al</span>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="px-4 py-2">
          <div className="seg-ctrl">
            {TABS.map(t => (
              <button key={t.id} className={`seg-btn ${tab === t.id ? 'active' : ''}`} onClick={() => setTab(t.id as typeof tab)}>
                {t.label}
              </button>
            ))}
          </div>
        </div>

        {/* Product list */}
        <div className="flex-1 scroll-area px-4 py-2 flex flex-col gap-3">
          {filtered.map(product => {
            const isBought = bought.has(product.id);
            const canAfford = coins >= product.price;
            return (
              <div
                key={product.id}
                className="rounded-2xl p-4 flex items-center gap-3"
                style={{ background: 'rgba(28,28,30,0.9)', border: '0.5px solid rgba(255,255,255,0.06)' }}
                onClick={() => setSelected(product)}
              >
                <div
                  className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
                  style={{ background: `${TYPE_COLORS[product.type]}22`, border: `0.5px solid ${TYPE_COLORS[product.type]}44` }}
                >
                  <ShoppingCart size={22} color={TYPE_COLORS[product.type]} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <span className="text-white font-semibold text-[15px]">{product.name}</span>
                    <span
                      className="text-[10px] font-bold px-1.5 py-0.5 rounded-full"
                      style={{ background: `${TYPE_COLORS[product.type]}22`, color: TYPE_COLORS[product.type] }}
                    >
                      {product.type}
                    </span>
                  </div>
                  <div className="text-[12px] truncate" style={{ color: 'rgba(235,235,245,0.5)' }}>{product.desc.slice(0, 50)}...</div>
                </div>
                <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                  {isBought ? (
                    <div className="flex items-center gap-1 px-2.5 py-1 rounded-full" style={{ background: 'rgba(48,209,88,0.15)' }}>
                      <Check size={12} color="#30D158" />
                      <span className="text-xs font-semibold" style={{ color: '#30D158' }}>Sahip</span>
                    </div>
                  ) : (
                    <div className="text-sm font-bold" style={{ color: canAfford ? '#FFD60A' : 'rgba(235,235,245,0.4)' }}>
                      ⬡ {product.price.toLocaleString('tr-TR')}
                    </div>
                  )}
                  <div className="flex">
                    {[0,1,2,3,4].map(i => <Star key={i} size={10} fill={i < 4 ? '#FFD60A' : 'none'} color="#FFD60A" />)}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Purchase modal */}
      {selected && (
        <div
          className="absolute inset-0 z-10 flex items-end anim-fadein"
          style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)' }}
          onClick={() => !buying && setSelected(null)}
        >
          <div
            className="w-full anim-slideup rounded-t-3xl p-6"
            style={{ background: 'rgb(18,18,22)' }}
            onClick={e => e.stopPropagation()}
          >
            <div className="w-10 h-1 rounded-full mx-auto mb-5" style={{ background: 'rgba(255,255,255,0.2)' }} />
            <div className="text-white font-bold text-xl mb-1">{selected.name}</div>
            <div className="text-sm mb-4" style={{ color: 'rgba(235,235,245,0.6)' }}>{selected.desc}</div>
            <div className="flex items-center justify-between mb-6 p-3 rounded-xl" style={{ background: 'rgba(255,255,255,0.05)' }}>
              <span style={{ color: 'rgba(235,235,245,0.6)', fontSize: 14 }}>Fiyat</span>
              <span className="font-bold text-lg" style={{ color: '#FFD60A' }}>⬡ {selected.price.toLocaleString('tr-TR')}</span>
            </div>
            <div className="flex gap-3">
              <button className="pill-btn ghost flex-1" onClick={() => setSelected(null)}>İptal</button>
              <button
                className="pill-btn gold flex-1 flex items-center justify-center gap-2"
                disabled={coins < selected.price || buying}
                onClick={handleBuy}
              >
                {buying ? <div className="spinner" style={{ width: 18, height: 18, borderWidth: 2 }} /> : `⬡ Satın Al`}
              </button>
            </div>
            {coins < selected.price && (
              <div className="text-center mt-2 text-sm" style={{ color: '#FF453A' }}>Yetersiz bakiye!</div>
            )}
          </div>
        </div>
      )}
    </AppWindow>
  );
}
