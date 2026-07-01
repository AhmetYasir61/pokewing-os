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

export const hasBridge = (): boolean =>
  typeof window !== 'undefined' && typeof window.cefQuery === 'function';

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
