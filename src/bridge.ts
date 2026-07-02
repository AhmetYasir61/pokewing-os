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
