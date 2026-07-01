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

import type { Product } from './types';

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
