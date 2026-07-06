// Play Store altyapısı: uygulama kataloğu, kurulum durumu (item'a bağlı),
// platform hedefi (telefon/pc) ve geliştirici yetkileri.
import { hasBridge } from './bridge';

export type Target = 'phone' | 'pc';

export interface StoreApp {
  id: string;          // app id (AppId olarak da kullanılır)
  name: string;
  emoji: string;       // ikon
  color: string;       // ikon gradyanı için ana renk
  desc: string;
  category: 'Oyun' | 'Araç' | 'Sosyal' | 'Geliştirici';
  targets: Target[];   // hangi cihazlarda kullanılabilir
  size: string;        // süs: "2.1 MB" gibi
  custom?: boolean;    // geliştirici stüdyosundan yayınlanan
  html?: string;       // custom uygulamanın kaynak kodu (iframe'de çalışır)
  author?: string;
  paidUntil?: number;  // aylık kira bitişi (ms) — D fazı
  expired?: boolean;   // kirası dolmuş (yalnız sahibi görür)
}

// ---- GELİŞTİRİCİLER: Dev Studio'ya giriş yetkisi olan Minecraft kullanıcı adları ----
// (Owner + geliştirici ekibin. Buraya isim ekleyerek yetki verirsin.)
export const DEVELOPERS: string[] = ['YasirTasarim', 'AhmetYasir61'];

export const isPcMode = (): boolean =>
  new URLSearchParams(window.location.search).get('mode') === 'pc';

export const currentTarget = (): Target => (isPcMode() ? 'pc' : 'phone');

export const itemId = (): string =>
  new URLSearchParams(window.location.search).get('item') || 'default';

// ---- Yerleşik katalog (İLK 10 — devamı "devam et" ile) ----
export const STORE_CATALOG: StoreApp[] = [
  { id: 'hesapm', name: 'Hesap Makinesi', emoji: '🧮', color: '#FF9F0A', desc: 'Dört işlem, yüzde ve daha fazlası. Sade ve hızlı.', category: 'Araç', targets: ['phone', 'pc'], size: '1.2 MB' },
  { id: 'saat', name: 'Saat & Kronometre', emoji: '⏱️', color: '#0A84FF', desc: 'Kronometre, geri sayım sayacı ve saat.', category: 'Araç', targets: ['phone', 'pc'], size: '0.9 MB' },
  { id: 'g2048', name: '2048', emoji: '🔢', color: '#FFD60A', desc: 'Efsane bulmaca! Karoları birleştir, 2048\'e ulaş.', category: 'Oyun', targets: ['phone', 'pc'], size: '3.4 MB' },
  { id: 'yilan', name: 'Yılan', emoji: '🐍', color: '#30D158', desc: 'Klasik yılan oyunu. Ne kadar uzayabilirsin?', category: 'Oyun', targets: ['phone', 'pc'], size: '2.1 MB' },
  { id: 'xox', name: 'XOX', emoji: '⭕', color: '#FF375F', desc: 'Bilgisayara karşı klasik üçtaş.', category: 'Oyun', targets: ['phone', 'pc'], size: '1.0 MB' },
  { id: 'hafiza', name: 'Hafıza', emoji: '🃏', color: '#BF5AF2', desc: 'Kart eşleştirme — hafızanı test et.', category: 'Oyun', targets: ['phone', 'pc'], size: '1.8 MB' },
  { id: 'piyano', name: 'Piyano', emoji: '🎹', color: '#40C8E0', desc: 'Gerçek sesli mini piyano. Melodi çal!', category: 'Araç', targets: ['phone', 'pc'], size: '4.2 MB' },
  { id: 'zar', name: 'Zar & Yazı Tura', emoji: '🎲', color: '#FF453A', desc: 'Karar veremedin mi? Zar at ya da yazı tura.', category: 'Araç', targets: ['phone'], size: '0.6 MB' },
  { id: 'darkchat', name: 'DarkChat', emoji: '🕶️', color: '#1c1c1e', desc: 'Kayıt dışı, PIN korumalı gizli yazışma kasası. Kötüler için.', category: 'Sosyal', targets: ['phone'], size: '2.7 MB' },
  { id: 'dev', name: 'Dev Studio', emoji: '👨‍💻', color: '#5E5CE6', desc: 'Kendi uygulamanı yaz ve yayınla. Sadece yetkili geliştiriciler.', category: 'Geliştirici', targets: ['pc'], size: '12.5 MB' },
  { id: 'itemcreator', name: 'Item Creator', emoji: '🧿', color: '#FF9F0A', desc: 'Özel item tasarla: ad, materyal, RPG stat, komut, market. Sadece geliştirici/owner.', category: 'Geliştirici', targets: ['phone', 'pc'], size: '4.8 MB' },
  // ---- 2. GRUP (11-20) ----
  { id: 'tkm', name: 'Taş Kağıt Makas', emoji: '✂️', color: '#5AC8FA', desc: 'Bilgisayara karşı klasik taş-kağıt-makas. Skor tut.', category: 'Oyun', targets: ['phone', 'pc'], size: '0.8 MB' },
  { id: 'reaksiyon', name: 'Reaksiyon Testi', emoji: '⚡', color: '#FFD60A', desc: 'Refleksin ne kadar hızlı? Yeşili gör, dokun!', category: 'Oyun', targets: ['phone', 'pc'], size: '0.7 MB' },
  { id: 'kostebek', name: 'Köstebek', emoji: '🔨', color: '#8B5A2B', desc: 'Köstebekleri yakala! 30 saniyede kaç tane?', category: 'Oyun', targets: ['phone', 'pc'], size: '1.5 MB' },
  { id: 'tugla', name: 'Tuğla Kır', emoji: '🧱', color: '#5AC8FA', desc: 'Klasik breakout — topu sektir, tüm tuğlaları kır.', category: 'Oyun', targets: ['phone', 'pc'], size: '2.3 MB' },
  { id: 'mayin', name: 'Mayın Tarlası', emoji: '💣', color: '#8E8E93', desc: 'Mayınlara basmadan tüm kareleri aç. Klasik!', category: 'Oyun', targets: ['phone', 'pc'], size: '1.9 MB' },
  { id: 'tetris', name: 'Tetris', emoji: '🟦', color: '#5AC8FA', desc: 'Efsane blok oyunu. Satırları doldur, temizle.', category: 'Oyun', targets: ['phone', 'pc'], size: '3.1 MB' },
  { id: 'birim', name: 'Birim Çevirici', emoji: '📐', color: '#64D2FF', desc: 'Uzunluk, ağırlık, sıcaklık, alan, hız — anında çevir.', category: 'Araç', targets: ['phone', 'pc'], size: '0.9 MB' },
  { id: 'fener', name: 'Fener', emoji: '🔦', color: '#FFD60A', desc: 'Ekranı bembeyaz yakan pratik fener.', category: 'Araç', targets: ['phone'], size: '0.3 MB' },
  { id: 'kure', name: 'Sihirli Küre', emoji: '🔮', color: '#BF5AF2', desc: 'Bir soru sor, kader küresi cevaplasın.', category: 'Araç', targets: ['phone', 'pc'], size: '0.6 MB' },
  { id: 'metronom', name: 'Metronom', emoji: '🎼', color: '#FF9F0A', desc: 'Gerçek sesli metronom — 40-220 BPM, vuruş göstergesi.', category: 'Araç', targets: ['phone', 'pc'], size: '1.1 MB' },
];

// ---- Kurulum durumu (item'a bağlı — telefon çalınırsa uygulamalar da gider) ----
const IKEY = () => 'pwapps:' + itemId();
const DKEY = () => 'pwdevapps:' + itemId();

export function installedIds(): string[] {
  try { return JSON.parse(localStorage.getItem(IKEY()) || 'null') || []; } catch { return []; }
}
export function isInstalled(id: string): boolean { return installedIds().includes(id); }
export function install(id: string): void {
  const l = installedIds();
  if (!l.includes(id)) { l.push(id); try { localStorage.setItem(IKEY(), JSON.stringify(l)); } catch { /* kota */ } }
}
export function uninstall(id: string): void {
  try { localStorage.setItem(IKEY(), JSON.stringify(installedIds().filter(x => x !== id))); } catch { /* kota */ }
}

// ---- Geliştirici uygulamaları (Dev Studio'dan yayınlanan; bu cihazda saklanır) ----
export function devApps(): StoreApp[] {
  try { return JSON.parse(localStorage.getItem(DKEY()) || 'null') || []; } catch { return []; }
}
export function saveDevApp(app: StoreApp): void {
  const l = devApps().filter(a => a.id !== app.id);
  l.push(app);
  try { localStorage.setItem(DKEY(), JSON.stringify(l)); } catch { /* kota */ }
}
export function deleteDevApp(id: string): void {
  try { localStorage.setItem(DKEY(), JSON.stringify(devApps().filter(a => a.id !== id))); } catch { /* kota */ }
}

// ---- Bulut kataloğu (SUNUCUDAN gelen geliştirici yayınları — herkes görür) ----
// Katalog evrenseldir (item'a değil sunucuya aittir); yerelde önbelleklenir.
const CLOUD_KEY = 'pwcloudapps';
export function cloudApps(): StoreApp[] {
  try { return JSON.parse(localStorage.getItem(CLOUD_KEY) || 'null') || []; } catch { return []; }
}
export function setCloudApps(list: StoreApp[]): void {
  try { localStorage.setItem(CLOUD_KEY, JSON.stringify(list)); } catch { /* kota */ }
}

/** Tam katalog: yerleşik + geliştirici yayınları.
 *  Oyun içinde (köprü var) → SUNUCU kataloğu (herkese dağıtılan);
 *  önizlemede → yerel taslaklar. */
export function fullCatalog(): StoreApp[] {
  return [...STORE_CATALOG, ...(hasBridge() ? cloudApps() : devApps())];
}

/** Bu cihazda (platforma göre) kullanılabilir + kurulu uygulamalar. */
export function installedAppsForPlatform(): StoreApp[] {
  const t = currentTarget();
  return fullCatalog().filter(a => installedIds().includes(a.id) && a.targets.includes(t));
}

export function findApp(id: string): StoreApp | undefined {
  return fullCatalog().find(a => a.id === id);
}
