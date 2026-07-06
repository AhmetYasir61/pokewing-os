import { useEffect, useState } from 'react';
import { getShopListings, buyShopListing, sellHeldItem, getMyListings, cancelListing, hasBridge, type ShopListing } from '../bridge';

interface Props { onBack: () => void; toast: (t: string) => void; }

type Tab = 'browse' | 'sell' | 'mine';

export function PlayershopApp({ onBack, toast }: Props) {
  const [tab, setTab] = useState<Tab>('browse');
  const [listings, setListings] = useState<ShopListing[]>([]);
  const [mine, setMine] = useState<ShopListing[]>([]);
  const [coins, setCoins] = useState(0);
  const [sellPrice, setSellPrice] = useState('');

  useEffect(() => { refresh(); }, [tab]);

  const refresh = async () => {
    if (tab === 'browse') {
      const r = await getShopListings();
      if (r) { setListings(r.listings); setCoins(r.coins); }
      else if (!hasBridge()) { setListings(MOCK); setCoins(452275); }
    } else if (tab === 'mine') {
      const r = await getMyListings();
      if (r) setMine(r);
      else if (!hasBridge()) setMine(MOCK.slice(0, 1));
    }
  };

  const buy = async (l: ShopListing) => {
    const r = await buyShopListing(l.id);
    if (r?.ok || !hasBridge()) {
      toast(`✔ ${strip(l.display)} satın alındı`);
      if (r?.coins != null) setCoins(r.coins);
      refresh();
    } else toast(r?.msg ?? 'Satın alınamadı');
  };

  const sell = async () => {
    const price = parseInt(sellPrice);
    if (!price || price < 1) { toast('Geçerli fiyat gir'); return; }
    const r = await sellHeldItem(price);
    if (r?.ok || !hasBridge()) {
      toast('✔ Elindeki item satışa çıktı');
      setSellPrice('');
      setTab('mine');
    } else toast(r?.msg ?? 'Satışa konamadı');
  };

  const cancel = async (id: number) => {
    await cancelListing(id);
    toast('İlan geri çekildi');
    refresh();
  };

  const strip = (s: string) => s.replace(/§./g, '');

  return (
    <div style={S.wrap}>
      <div style={S.header}>
        <button onClick={onBack} style={S.back}>‹</button>
        <span>Oyuncu Marketi</span>
        <span style={S.coins}>💰 {coins.toLocaleString()}</span>
      </div>

      {/* sekmeler */}
      <div style={S.tabs}>
        {(['browse', 'sell', 'mine'] as Tab[]).map(t => (
          <button key={t} onClick={() => setTab(t)}
            style={{ ...S.tab, ...(tab === t ? S.tabActive : {}) }}>
            {t === 'browse' ? '🛒 Al' : t === 'sell' ? '🏷️ Sat' : '📦 İlanlarım'}
          </button>
        ))}
      </div>

      <div style={S.scroll}>
        {/* ---- ALIŞVERİŞ ---- */}
        {tab === 'browse' && (
          listings.length === 0
            ? <div style={S.empty}>Satışta ürün yok.<br />İlk satan sen ol!</div>
            : listings.map(l => (
              <div key={l.id} style={S.card}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600 }}>{strip(l.display)}</div>
                  <div style={{ fontSize: 11, opacity: 0.55 }}>Satıcı: {l.seller}{l.amount > 1 ? ` · x${l.amount}` : ''}</div>
                </div>
                <button onClick={() => buy(l)} style={S.buyBtn}>{l.price.toLocaleString()} 🪙</button>
              </div>
            ))
        )}

        {/* ---- SATIŞA KOY ---- */}
        {tab === 'sell' && (
          <div>
            <div style={S.sellBox}>
              <div style={{ fontSize: 40, textAlign: 'center' }}>🏷️</div>
              <div style={{ textAlign: 'center', opacity: 0.8, marginBottom: 12 }}>
                <b>Elindeki</b> item'ı satışa koy.<br />
                <span style={{ fontSize: 12, opacity: 0.6 }}>Eline aldığın item satışa çıkar, alıcı çıkınca coin sana gelir.</span>
              </div>
              <label style={S.lbl}>Fiyat (coin)</label>
              <input type="number" style={S.inp} value={sellPrice}
                onChange={e => setSellPrice(e.target.value)} placeholder="Örn: 5000" />
              <button onClick={sell} style={S.sellBtn}>🏷️ Elimdekini Sat</button>
            </div>
          </div>
        )}

        {/* ---- İLANLARIM ---- */}
        {tab === 'mine' && (
          mine.length === 0
            ? <div style={S.empty}>Aktif ilanın yok.</div>
            : mine.map(l => (
              <div key={l.id} style={S.card}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600 }}>{strip(l.display)}</div>
                  <div style={{ fontSize: 11, opacity: 0.55 }}>{l.price.toLocaleString()} 🪙{l.amount > 1 ? ` · x${l.amount}` : ''}</div>
                </div>
                <button onClick={() => cancel(l.id)} style={S.cancelBtn}>Geri Çek</button>
              </div>
            ))
        )}
      </div>
    </div>
  );
}

const MOCK: ShopListing[] = [
  { id: 1, seller: 'Ametio', display: '§6Ateş Kılıcı', price: 12000, amount: 1 },
  { id: 2, seller: 'Efe', display: '§bBuz Baltası', price: 8500, amount: 1 },
  { id: 3, seller: 'Kaya', display: '§aElmas', price: 500, amount: 16 },
];

const S: Record<string, React.CSSProperties> = {
  wrap: { display: 'flex', flexDirection: 'column', height: '100%', background: '#0d0d10', color: '#fff' },
  header: { display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', fontWeight: 700, fontSize: 17, borderBottom: '1px solid #1c1c22' },
  back: { background: 'none', border: 'none', color: '#0A84FF', fontSize: 26, cursor: 'pointer', lineHeight: 1 },
  coins: { marginLeft: 'auto', fontSize: 13, color: '#FFD60A', fontWeight: 700 },
  tabs: { display: 'flex', gap: 6, padding: '10px 14px' },
  tab: { flex: 1, background: '#16161c', border: 'none', borderRadius: 9, color: '#8a8a95', padding: '9px 0', fontSize: 13, cursor: 'pointer', fontWeight: 600 },
  tabActive: { background: '#0A84FF', color: '#fff' },
  scroll: { flex: 1, overflowY: 'auto', padding: '0 14px 14px' },
  card: { display: 'flex', alignItems: 'center', gap: 8, background: '#16161c', borderRadius: 10, padding: 12, marginBottom: 8 },
  buyBtn: { background: 'linear-gradient(90deg,#30D158,#28a745)', color: '#fff', border: 'none', borderRadius: 8, padding: '9px 13px', fontSize: 13, fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' },
  cancelBtn: { background: '#3a2222', color: '#ff6b6b', border: 'none', borderRadius: 8, padding: '9px 13px', fontSize: 12, cursor: 'pointer' },
  empty: { opacity: 0.5, textAlign: 'center', marginTop: 50, lineHeight: 1.8 },
  sellBox: { background: '#16161c', borderRadius: 14, padding: 20, marginTop: 8 },
  lbl: { display: 'block', fontSize: 12, opacity: 0.6, margin: '10px 0 4px' },
  inp: { width: '100%', boxSizing: 'border-box', background: '#0d0d10', border: '1px solid #2a2a33', borderRadius: 8, color: '#fff', padding: '11px', fontSize: 15 },
  sellBtn: { width: '100%', background: 'linear-gradient(90deg,#FF9F0A,#FF6B00)', color: '#fff', border: 'none', borderRadius: 10, padding: 13, fontSize: 15, fontWeight: 700, marginTop: 16, cursor: 'pointer' },
};
