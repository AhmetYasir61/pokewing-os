import { useEffect, useState } from 'react';
import { getCustomItems, saveCustomItem, deleteCustomItem, canUseItemCreator, hasBridge, type CustomItem } from '../bridge';

interface Props { onBack: () => void; toast: (t: string) => void; }

const RARITIES = [
  { id: 'common', name: 'Sıradan', color: '#B0B0B0' },
  { id: 'uncommon', name: 'Nadir', color: '#30D158' },
  { id: 'rare', name: 'Ender', color: '#0A84FF' },
  { id: 'epic', name: 'Efsanevi', color: '#BF5AF2' },
  { id: 'legendary', name: 'Destansı', color: '#FF9F0A' },
];
const STAT_KEYS = [
  { k: 'damage', label: '⚔️ Hasar' },
  { k: 'health', label: '❤️ Can' },
  { k: 'speed', label: '💨 Hız' },
  { k: 'armor', label: '🛡️ Zırh' },
  { k: 'attack_speed', label: '⚡ Saldırı Hızı' },
];

const empty = (): Partial<CustomItem> => ({
  id: 0, name: '', material: 'minecraft:diamond_sword', lore: [], rarity: 'common',
  useCommand: '', stats: {}, price: 0, sellable: false,
});

export function ItemCreatorApp({ onBack, toast }: Props) {
  const [items, setItems] = useState<CustomItem[]>([]);
  const [editing, setEditing] = useState<Partial<CustomItem> | null>(null);
  const [allowed, setAllowed] = useState<boolean | null>(null);
  const [loreText, setLoreText] = useState('');

  useEffect(() => {
    canUseItemCreator().then(v => setAllowed(hasBridge() ? v : true));   // tarayıcıda önizleme serbest
    refresh();
  }, []);

  const refresh = async () => {
    const list = await getCustomItems();
    if (list) setItems(list);
    else if (!hasBridge()) setItems(MOCK);   // tarayıcı önizleme
  };

  const openEdit = (it?: CustomItem) => {
    const e = it ? { ...it, stats: { ...it.stats } } : empty();
    setEditing(e);
    setLoreText((e.lore ?? []).join('\n'));
  };

  const save = async () => {
    if (!editing) return;
    if (!editing.name?.trim()) { toast('Item adı gerekli'); return; }
    const payload: Partial<CustomItem> = {
      ...editing,
      lore: loreText.split('\n').map(s => s.trim()).filter(Boolean),
    };
    const r = await saveCustomItem(payload);
    if (r?.ok || !hasBridge()) {
      toast('Item kaydedildi ✓');
      setEditing(null);
      refresh();
    } else toast(r?.msg ?? 'Kayıt başarısız');
  };

  const remove = async (id: number) => {
    if (!confirm('Bu item silinsin mi?')) return;
    await deleteCustomItem(id);
    toast('Silindi');
    refresh();
  };

  const rarityColor = (r: string) => RARITIES.find(x => x.id === r)?.color ?? '#B0B0B0';
  const strip = (s: string) => s.replace(/§./g, '');

  if (allowed === false) {
    return (
      <div style={S.wrap}>
        <div style={S.header}><button onClick={onBack} style={S.back}>‹</button><span>Item Creator</span></div>
        <div style={S.locked}>
          🔒<br />Bu uygulama yalnız <b>geliştirici / owner</b> içindir.<br />
          <span style={{ opacity: 0.6, fontSize: 13 }}>Launcher → Kontrol Paneli → Yetkiler'den yetki alınır.</span>
        </div>
      </div>
    );
  }

  // -------- Düzenleyici --------
  if (editing) {
    return (
      <div style={S.wrap}>
        <div style={S.header}>
          <button onClick={() => setEditing(null)} style={S.back}>‹</button>
          <span>{editing.id ? 'Item Düzenle' : 'Yeni Item'}</span>
        </div>
        <div style={S.scroll}>
          {/* Önizleme */}
          <div style={{ ...S.preview, borderColor: rarityColor(editing.rarity ?? 'common') }}>
            <div style={{ fontSize: 34 }}>🧿</div>
            <div style={{ fontWeight: 700, color: rarityColor(editing.rarity ?? 'common') }}>
              {strip(editing.name || 'İsimsiz Item')}
            </div>
            {loreText.split('\n').filter(Boolean).map((l, i) => (
              <div key={i} style={{ fontSize: 12, opacity: 0.75 }}>{strip(l)}</div>
            ))}
          </div>

          <label style={S.lbl}>İsim (renk kodu: §a §6 vb.)</label>
          <input style={S.inp} value={editing.name ?? ''} onChange={e => setEditing({ ...editing, name: e.target.value })} placeholder="§6Ateş Kılıcı" />

          <label style={S.lbl}>Materyal (vanilla veya ItemsAdder id)</label>
          <input style={S.inp} value={editing.material ?? ''} onChange={e => setEditing({ ...editing, material: e.target.value })} placeholder="minecraft:diamond_sword  veya  ia:ruby_sword" />

          <label style={S.lbl}>Açıklama (her satır ayrı)</label>
          <textarea style={{ ...S.inp, height: 70, resize: 'vertical' }} value={loreText} onChange={e => setLoreText(e.target.value)} placeholder="§7Efsanevi bir kılıç&#10;§cYakıcı hasar" />

          <label style={S.lbl}>Nadirlik</label>
          <div style={S.rarRow}>
            {RARITIES.map(r => (
              <button key={r.id} onClick={() => setEditing({ ...editing, rarity: r.id })}
                style={{ ...S.rarBtn, background: editing.rarity === r.id ? r.color : '#1c1c22', color: editing.rarity === r.id ? '#000' : r.color }}>
                {r.name}
              </button>
            ))}
          </div>

          <label style={S.lbl}>RPG Statları</label>
          {STAT_KEYS.map(s => (
            <div key={s.k} style={S.statRow}>
              <span style={{ flex: 1, fontSize: 13 }}>{s.label}</span>
              <input type="number" style={S.statInp} value={editing.stats?.[s.k] ?? 0}
                onChange={e => setEditing({ ...editing, stats: { ...editing.stats, [s.k]: Number(e.target.value) } })} />
            </div>
          ))}

          <label style={S.lbl}>Kullanınca çalışacak komut ({'{player}'})</label>
          <input style={S.inp} value={editing.useCommand ?? ''} onChange={e => setEditing({ ...editing, useCommand: e.target.value })} placeholder="effect give {player} minecraft:strength 30 1" />

          <label style={S.lbl}>Market</label>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <label style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: 13 }}>
              <input type="checkbox" checked={!!editing.sellable} onChange={e => setEditing({ ...editing, sellable: e.target.checked })} />
              Satılabilir
            </label>
            <input type="number" style={{ ...S.inp, flex: 1, marginTop: 0 }} value={editing.price ?? 0}
              onChange={e => setEditing({ ...editing, price: Number(e.target.value) })} placeholder="Fiyat (coin)" />
          </div>

          <button onClick={save} style={S.saveBtn}>💾 Kaydet</button>
        </div>
      </div>
    );
  }

  // -------- Liste --------
  return (
    <div style={S.wrap}>
      <div style={S.header}>
        <button onClick={onBack} style={S.back}>‹</button>
        <span>Item Creator</span>
        <button onClick={() => openEdit()} style={S.addBtn}>+ Yeni</button>
      </div>
      <div style={S.scroll}>
        {items.length === 0 && <div style={{ opacity: 0.5, textAlign: 'center', marginTop: 40 }}>Henüz özel item yok.<br />+ Yeni ile başla.</div>}
        {items.map(it => (
          <div key={it.id} style={{ ...S.card, borderLeftColor: rarityColor(it.rarity) }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700, color: rarityColor(it.rarity) }}>{strip(it.name)}</div>
              <div style={{ fontSize: 11, opacity: 0.55 }}>{it.material}{it.sellable ? ` · ${it.price} 🪙` : ''}</div>
            </div>
            <button onClick={() => openEdit(it)} style={S.iconBtn}>✏️</button>
            <button onClick={() => remove(it.id)} style={S.iconBtn}>🗑️</button>
          </div>
        ))}
      </div>
    </div>
  );
}

const MOCK: CustomItem[] = [
  { id: 1, name: '§6Ateş Kılıcı', material: 'minecraft:diamond_sword', lore: ['§7Yakıcı hasar'], rarity: 'legendary', useCommand: '', stats: { damage: 12 }, price: 5000, sellable: true, author: 'demo' },
  { id: 2, name: '§bBuz Baltası', material: 'minecraft:iron_axe', lore: ['§7Donduran vuruş'], rarity: 'epic', useCommand: '', stats: { damage: 8, speed: 2 }, price: 3000, sellable: true, author: 'demo' },
];

const S: Record<string, React.CSSProperties> = {
  wrap: { display: 'flex', flexDirection: 'column', height: '100%', background: '#0d0d10', color: '#fff' },
  header: { display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', fontWeight: 700, fontSize: 17, borderBottom: '1px solid #1c1c22' },
  back: { background: 'none', border: 'none', color: '#0A84FF', fontSize: 26, cursor: 'pointer', lineHeight: 1 },
  addBtn: { marginLeft: 'auto', background: '#0A84FF', color: '#fff', border: 'none', borderRadius: 8, padding: '6px 12px', fontSize: 13, cursor: 'pointer' },
  scroll: { flex: 1, overflowY: 'auto', padding: 14 },
  card: { display: 'flex', alignItems: 'center', gap: 8, background: '#16161c', borderRadius: 10, padding: 12, marginBottom: 8, borderLeft: '4px solid' },
  iconBtn: { background: '#22222a', border: 'none', borderRadius: 8, padding: '6px 9px', cursor: 'pointer', fontSize: 14 },
  lbl: { display: 'block', fontSize: 12, opacity: 0.6, margin: '14px 0 4px' },
  inp: { width: '100%', boxSizing: 'border-box', background: '#16161c', border: '1px solid #2a2a33', borderRadius: 8, color: '#fff', padding: '9px 11px', fontSize: 14, marginTop: 2 },
  rarRow: { display: 'flex', flexWrap: 'wrap', gap: 6 },
  rarBtn: { border: 'none', borderRadius: 8, padding: '7px 11px', fontSize: 12, cursor: 'pointer', fontWeight: 600 },
  statRow: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
  statInp: { width: 70, background: '#16161c', border: '1px solid #2a2a33', borderRadius: 8, color: '#fff', padding: '6px 8px', fontSize: 13 },
  preview: { background: '#000', border: '2px solid', borderRadius: 12, padding: 16, textAlign: 'center', marginBottom: 8 },
  saveBtn: { width: '100%', background: 'linear-gradient(90deg,#0A84FF,#5E5CE6)', color: '#fff', border: 'none', borderRadius: 10, padding: 13, fontSize: 15, fontWeight: 700, marginTop: 20, cursor: 'pointer' },
  locked: { textAlign: 'center', marginTop: 60, lineHeight: 1.8, fontSize: 16 },
};
