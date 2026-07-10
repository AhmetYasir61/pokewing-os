// MCEF (oyun içi) JS ↔ Java köprüsü.
// Tarayıcıda window.cefQuery yoksa (normal önizleme) köprü pasiftir → uygulama mock veriyle çalışır.

interface CefQuery {
  request: string;
  onSuccess: (response: string) => void;
  onFailure: (code: number, message: string) => void;
  persistent?: boolean;
}

declare global {
  interface Window {
    cefQuery?: (q: CefQuery) => void;
    PWNotify?: (type: string, data: unknown) => void;
  }
}

import type { Product, InventoryItem, NewsItem } from './types';
import type { StoreApp } from './appstore';

export const hasBridge = (): boolean =>
  typeof window !== 'undefined' && typeof window.cefQuery === 'function';

// Backend ürün tipi (İngilizce) → web etiketi (Türkçe)
const TYPE_MAP: Record<string, string> = {
  rank: 'Rütbe', kit: 'Kit', cosmetic: 'Kozmetik', key: 'Anahtar', coin: 'Paket', item: 'Özellik',
};

export function mapProduct(p: any): Product {
  return {
    id: Number(p?.id) || 0,
    name: String(p?.name ?? ''),
    type: TYPE_MAP[String(p?.type ?? 'item')] ?? 'Özellik',
    price: Number(p?.price) || 0,
    desc: String(p?.description ?? p?.desc ?? ''),
    imageUrl: p?.imageUrl || undefined,
  };
}

export interface MarketData { coins: number; products: Product[]; }

/** Mod'dan gerçek market verisini çeker (coin + ürünler). Köprü/veri yoksa null. */
export async function getMarket(): Promise<MarketData | null> {
  const r = await bridge<{ coins?: number; products?: any[] }>('market');
  if (!r || !Array.isArray(r.products)) return null;
  return {
    coins: typeof r.coins === 'number' ? r.coins : -1,
    products: r.products.map(mapProduct),
  };
}

/** Ürün satın al. Dönüş: {ok, coins, msg}. Köprü yoksa null. */
export async function buyProduct(id: number): Promise<{ ok: boolean; coins?: number; msg?: string } | null> {
  return bridge<{ ok: boolean; coins?: number; msg?: string }>('buy', { id });
}

// ---- Depo ----
/** Depodaki (teslim bekleyen) eşyaları çeker. Köprü/veri yoksa null. */
export async function getDepo(): Promise<InventoryItem[] | null> {
  const r = await bridge<{ items?: any[] }>('depo');
  if (!r || !Array.isArray(r.items)) return null;
  return r.items.map((it): InventoryItem => ({
    id: Number(it?.depoId) || 0,
    name: String(it?.name ?? ''),
    type: TYPE_MAP[String(it?.type ?? 'item')] ?? 'Özellik',
    slots: Number(it?.slots ?? it?.qty ?? 1) || 1,
  }));
}

/** Depodan bir eşyayı teslim al (envantere). Dönüş: {ok, depoId, msg}. */
export async function claimItem(id: number): Promise<{ ok: boolean; depoId?: number; msg?: string } | null> {
  return bridge<{ ok: boolean; depoId?: number; msg?: string }>('claim', { id });
}

// ---- Mesajlar ----
export interface BridgeMsg { who: 'me' | 'them' | 'sys'; text: string; }

/** Mod'daki sohbet deposunu çeker: {threads, unread}. Köprü yoksa null. */
export async function getMessages(): Promise<{ threads: Record<string, BridgeMsg[]>; unread: Record<string, number> } | null> {
  const r = await bridge<{ threads?: Record<string, BridgeMsg[]>; unread?: Record<string, number> }>('messages');
  if (!r || typeof r.threads !== 'object') return null;
  return { threads: r.threads || {}, unread: r.unread || {} };
}

/** Gerçek oyuncuya mesaj gönder (sunucu iletir). */
export function sendMsgBridge(to: string, text: string): void {
  void bridge('sendMsg', { to, text });
}

/** Sohbet açıldı — mod tarafında okunmamışı temizle + bildirimi bastır. */
export function markMsgRead(peer: string): void {
  void bridge('msgRead', { peer });
}

/** Oyun içi gerçek tarayıcıyı aç (MCEF overlay — Google/YouTube, sesli). */
export async function browseInGame(url: string): Promise<boolean> {
  const r = await bridge<{ ok?: boolean }>('browse', { url });
  return !!r?.ok;
}

// ---- Uygulama mağazası (geliştirici yayınları — SUNUCUDA saklanır, herkese dağıtılır) ----
export interface CloudCatalog { apps: StoreApp[]; error?: string; canDev?: boolean; }

export function parseCloudApps(r: any): CloudCatalog | null {
  if (!r || !Array.isArray(r.apps)) return null;
  const apps: StoreApp[] = r.apps.map((a: any): StoreApp => ({
    id: String(a?.id ?? ''), name: String(a?.name ?? ''), emoji: String(a?.emoji ?? '🚀'),
    color: String(a?.color ?? '#5E5CE6'), desc: String(a?.desc ?? ''), category: 'Geliştirici',
    targets: Array.isArray(a?.targets) ? a.targets.filter((t: any) => t === 'phone' || t === 'pc') : ['phone', 'pc'],
    size: String(a?.size ?? ''), custom: true, html: String(a?.html ?? ''), author: String(a?.author ?? ''),
    paidUntil: Number(a?.paidUntil) || 0, expired: a?.expired === true,
  })).filter((a: StoreApp) => a.id.startsWith('dev:'));
  return { apps, error: r.error ? String(r.error) : undefined, canDev: r.canDev === true };
}

// ---- Item Creator (özel item tanımları — dev/owner) ----
export interface CustomItem {
  id: number;
  name: string;         // renk kodlu ad (§a gibi Minecraft kodları)
  material: string;     // vanilla 'minecraft:diamond_sword' veya ItemsAdder 'ia:ruby_sword'
  lore: string[];       // açıklama satırları
  rarity: string;       // common | uncommon | rare | epic | legendary
  useCommand: string;   // sağ tıkta çalışacak komut ({player})
  stats: Record<string, number>;  // {damage, speed, health, ...} RPG
  price: number;        // coin (market için)
  sellable: boolean;    // markette satılabilir mi
  author?: string;
}

export async function getCustomItems(): Promise<CustomItem[] | null> {
  const r = await bridge<{ items?: CustomItem[] }>('itemList');
  return r && Array.isArray(r.items) ? r.items : null;
}
export async function saveCustomItem(item: Partial<CustomItem>): Promise<{ ok: boolean; id?: number; msg?: string } | null> {
  const r = await bridge<{ ok?: boolean; id?: number; msg?: string }>('itemSave', item as Record<string, unknown>);
  return r ? { ok: !!r.ok, id: r.id, msg: r.msg } : null;
}
export async function deleteCustomItem(id: number): Promise<boolean> {
  const r = await bridge<{ ok?: boolean }>('itemDelete', { id });
  return !!r?.ok;
}
export async function canUseItemCreator(): Promise<boolean> {
  const r = await bridge<{ canDev?: boolean }>('itemPerm');
  return !!r?.canDev;
}

// ---- Oyuncu Marketi (Playershop) ----
export interface ShopListing {
  id: number;
  seller: string;
  display: string;
  price: number;
  amount: number;
}

export async function getShopListings(): Promise<{ listings: ShopListing[]; coins: number } | null> {
  const r = await bridge<{ listings?: ShopListing[]; coins?: number }>('shopList');
  return r ? { listings: r.listings ?? [], coins: r.coins ?? 0 } : null;
}
export async function buyShopListing(id: number): Promise<{ ok: boolean; msg?: string; coins?: number } | null> {
  const r = await bridge<{ ok?: boolean; msg?: string; coins?: number }>('shopBuy', { id });
  return r ? { ok: !!r.ok, msg: r.msg, coins: r.coins } : null;
}
/** Elindeki item'ı satışa koy (mod eldeki item'ı alır). */
export async function sellHeldItem(price: number): Promise<{ ok: boolean; msg?: string } | null> {
  const r = await bridge<{ ok?: boolean; msg?: string }>('shopSell', { price });
  return r ? { ok: !!r.ok, msg: r.msg } : null;
}
export async function getMyListings(): Promise<ShopListing[] | null> {
  const r = await bridge<{ listings?: ShopListing[] }>('shopMine');
  return r && Array.isArray(r.listings) ? r.listings : null;
}
export async function cancelListing(id: number): Promise<boolean> {
  const r = await bridge<{ ok?: boolean }>('shopCancel', { id });
  return !!r?.ok;
}

// ---- Şarj (istasyondan alınan bekleyen şarjı çek — powerbank mantığı) ----
export async function chargeTake(item: string, need: number): Promise<number> {
  if (need <= 0) return 0;
  const r = await bridge<{ granted?: number }>('chargeTake', { item, need: Math.ceil(need) });
  return r && typeof r.granted === 'number' ? Math.max(0, r.granted) : 0;
}

export async function getServerApps(): Promise<CloudCatalog | null> {
  return parseCloudApps(await bridge('appList'));
}
export async function publishAppBridge(app: StoreApp): Promise<CloudCatalog | null> {
  return parseCloudApps(await bridge('publishApp', app as unknown as Record<string, unknown>));
}
export async function deleteAppBridge(id: string): Promise<CloudCatalog | null> {
  return parseCloudApps(await bridge('deleteApp', { id }));
}
/** Aylık kirayı yenile (+30 gün; sunucu 500 coin tahsil eder, OP muaf). */
export async function renewAppBridge(id: string): Promise<CloudCatalog | null> {
  return parseCloudApps(await bridge('renewApp', { id }));
}

// ---- VC: gerçek arama + ses ayarları (mikrofon/kulaklık/cızırtı önleyici) ----
export interface VcSettingsData {
  micVol: number; spkVol: number;
  noiseGate: boolean; echoCancel: boolean; proximity: boolean;
  micIdx: number; spkIdx: number;
  inputs: string[]; outputs: string[];
}

export async function getVcSettings(): Promise<VcSettingsData | null> {
  const r = await bridge<any>('vcSettings');
  return r && Array.isArray(r.inputs) ? (r as VcSettingsData) : null;
}
export async function setVcSetting(key: string, val: number | boolean): Promise<VcSettingsData | null> {
  const r = await bridge<any>('vcSet', { key, val });
  return r && Array.isArray(r.inputs) ? (r as VcSettingsData) : null;
}
export function vcDial(number: string): void { void bridge('dial', { number }); }
export function vcAnswer(accept: boolean): void { void bridge('answer', { accept }); }
export function vcHangup(): void { void bridge('hangup'); }

// ---- Rehber (WhatsApp mantığı: numarayla kişi ekleme) ----
export interface PwContact { name: string; number: string; online: boolean; }

/** Numaram + çevrimiçi oyuncular (isim+numara). Kişi eklerken numara doğrulamada kullanılır. */
export async function getPhoneDirectory(): Promise<{ myNumber: string; online: { name: string; number: string }[] } | null> {
  const r = await bridge<{ myNumber?: string; contacts?: { name?: string; number?: string }[] }>('contacts');
  if (!r) return null;
  return {
    myNumber: String(r.myNumber ?? ''),
    online: (Array.isArray(r.contacts) ? r.contacts : []).map(c => ({ name: String(c?.name ?? ''), number: String(c?.number ?? '') })),
  };
}

// ---- Yayın (Kick-tarzı canlı yayın dizini) ----
export interface PwStream { name: string; title: string; url: string; mins: number; }
export interface StreamState { streams: PwStream[]; me: string; live: boolean; }

function parseStreams(r: any): StreamState | null {
  if (!r || !Array.isArray(r.streams)) return null;
  return {
    streams: r.streams.map((s: any): PwStream => ({
      name: String(s?.name ?? ''), title: String(s?.title ?? ''), url: String(s?.url ?? ''), mins: Number(s?.mins) || 0,
    })),
    me: String(r.me ?? ''), live: !!r.live,
  };
}

export async function getStreams(): Promise<StreamState | null> {
  return parseStreams(await bridge('streams'));
}

/** Yayını başlat (live=true, title+url) veya durdur (live=false). Güncel liste döner. */
export async function setStream(live: boolean, title = '', url = ''): Promise<StreamState | null> {
  return parseStreams(await bridge('streamSet', { live, title, url }));
}

// ---- Drone yayıncıları (Kick app: canlı oyuncu drone yayınları + İzle/spectate) ----
export interface PwBroadcaster { name: string; activity: string; droneId: number; }

export async function getBroadcasters(): Promise<PwBroadcaster[]> {
  const r = await bridge('broadcasters');
  if (!Array.isArray(r)) return [];
  return r.map((b: any): PwBroadcaster => ({
    name: String(b?.name ?? ''), activity: String(b?.activity ?? ''), droneId: Number(b?.droneId) || -1,
  }));
}
/** Yayıncının drone'unu izlemeye başla (oyun-içi kamerayı ona bağlar; web ekranı kapanır). */
export function spectate(droneId: number, name: string): void { void bridge('spectate', { droneId, name }); }
export function spectateStop(): void { void bridge('spectateStop'); }

// ---- Bildirim sesi (minik ding — WebAudio, dosya gerekmez) ----
let _actx: AudioContext | null = null;
export function ding(): void {
  try {
    _actx = _actx || new AudioContext();
    const ctx = _actx;
    if (ctx.state === 'suspended') void ctx.resume();
    const t = ctx.currentTime;
    [1318.5, 1760].forEach((f, i) => {   // E6 → A6: iki tonlu "ding"
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = 'sine'; o.frequency.value = f;
      g.gain.setValueAtTime(0.0001, t + i * 0.09);
      g.gain.exponentialRampToValueAtTime(0.12, t + i * 0.09 + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + i * 0.09 + 0.35);
      o.connect(g); g.connect(ctx.destination);
      o.start(t + i * 0.09); o.stop(t + i * 0.09 + 0.4);
    });
  } catch { /* ses yoksa sessiz geç */ }
}

// ---- Duyurular ----
const NEWS_TAG: Record<string, string> = { update: 'Güncelleme', event: 'Etkinlik', reward: 'Kampanya', info: 'Bilgi' };

/** Duyuruları çeker. Köprü/veri yoksa null. */
export async function getNews(): Promise<NewsItem[] | null> {
  const r = await bridge<any>('news');
  const arr = Array.isArray(r) ? r : r?.announcements;
  if (!Array.isArray(arr)) return null;
  return arr.map((a: any, i: number): NewsItem => ({
    id: i + 1,
    tag: NEWS_TAG[String(a?.type ?? '')] ?? 'Bilgi',
    date: String(a?.date ?? ''),
    title: String(a?.title ?? ''),
    body: String(a?.message ?? a?.body ?? ''),
  }));
}

/** Mod'a bir istek gönderir, JSON yanıtı çözer. Köprü yoksa null döner. */
export function bridge<T = any>(method: string, args: Record<string, unknown> = {}): Promise<T | null> {
  return new Promise((resolve) => {
    if (!hasBridge()) { resolve(null); return; }
    try {
      window.cefQuery!({
        request: JSON.stringify({ method, args }),
        onSuccess: (r: string) => {
          try { resolve(r ? (JSON.parse(r) as T) : null); } catch { resolve(null); }
        },
        onFailure: () => resolve(null),
      });
    } catch { resolve(null); }
  });
}
