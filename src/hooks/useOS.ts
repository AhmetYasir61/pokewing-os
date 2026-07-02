import { useCallback, useEffect, useReducer, useRef } from 'react';
import { OSState, AppId, Notification, Contact } from '../types';
import { THEMES } from '../data';
import { hasBridge, bridge, getMessages, ding, getServerApps, parseCloudApps } from '../bridge';
import { itemId, setCloudApps } from '../appstore';
import type { Message } from '../types';

// ---- Telefon verileri item'a bağlı saklanır (PC'deki gibi): tema, duvar kağıdı, notlar, ayarlar ----
const PHONE_KEY = () => 'pwphone:' + itemId();
function loadPhone(): Partial<OSState> {
  try { return JSON.parse(localStorage.getItem(PHONE_KEY()) || 'null') || {}; } catch { return {}; }
}

const initThreads: Record<string, import('../types').Message[]> = {
  Misty: [
    { who: 'them', text: 'Selam! Bugün nasılsın?', time: '10:22' },
    { who: 'me', text: 'İyiyim, teşekkürler! Sen nasılsın?', time: '10:23' },
    { who: 'them', text: 'Müthişim! PvP\'e girdin mi bu hafta?', time: '10:25' },
  ],
  Brock: [
    { who: 'them', text: 'Turnuva bileti aldın mı?', time: '09:15' },
    { who: 'me', text: 'Henüz almadım, bakacağım.', time: '09:16' },
  ],
};

const initNotes = [
  { id: 'n1', title: 'Sunucu Notları', body: 'Bakım saatleri: 03:00 - 05:00\nPvP turnuvası: Cumartesi 20:00\nSezon bitiş tarihi: 31 Temmuz', date: '30 Haz' },
  { id: 'n2', title: 'Alım Listesi', body: 'VIP Rütbesi\nEfsane Anahtarı x3\nUçuş Modu', date: '29 Haz' },
];

const initState: OSState = {
  activeApp: null,
  prevApp: null,
  drawerOpen: false,
  notifPanelOpen: false,
  quickSettingsOpen: false,
  lockScreen: false,
  theme: 0,
  wallpaper: THEMES[0].bg,
  wallpaperUrl: '',
  wifi: true,
  bluetooth: false,
  dnd: false,
  darkMode: true,
  brightness: 85,
  volume: 70,
  coins: 12500,
  userName: 'AshKetchum',
  chatContact: null,
  threads: initThreads,
  dialNumber: '',
  notifications: [
    { id: 'notif1', app: 'news', title: 'Duyurular', body: 'Sezon 1 Başladı! Giriş yap ve ödüllerini al.', time: '10:30', read: false },
    { id: 'notif2', app: 'mesaj', title: 'Misty', body: 'Müthişim! PvP\'e girdin mi bu hafta?', time: '10:25', read: false },
    { id: 'notif3', app: 'market', title: 'Market', body: 'Hafta sonu %50 indirim! Kaçırma!', time: '09:00', read: false },
  ],
  toasts: [],
  contextMenu: null,
  searchQuery: '',
  musicPlaying: false,
  musicTrack: 0,
  calendarDate: new Date(),
  notes: initNotes,
  activeNote: null,
  webUrl: 'https://www.google.com',
  webHistory: [],
  msgUnread: 0,
  banner: null,
};

type Action =
  | { type: 'OPEN_APP'; app: AppId }
  | { type: 'CLOSE_APP' }
  | { type: 'TOGGLE_DRAWER' }
  | { type: 'CLOSE_DRAWER' }
  | { type: 'TOGGLE_NOTIF_PANEL' }
  | { type: 'CLOSE_NOTIF_PANEL' }
  | { type: 'TOGGLE_QS' }
  | { type: 'CLOSE_QS' }
  | { type: 'TOGGLE_WIFI' }
  | { type: 'TOGGLE_BT' }
  | { type: 'TOGGLE_DND' }
  | { type: 'SET_BRIGHTNESS'; val: number }
  | { type: 'SET_VOLUME'; val: number }
  | { type: 'SET_THEME'; idx: number }
  | { type: 'SET_WALLPAPER_URL'; url: string }
  | { type: 'ADD_COIN'; amount: number }
  | { type: 'SPEND_COIN'; amount: number }
  | { type: 'SET_COINS'; amount: number }
  | { type: 'SET_THREADS'; threads: Record<string, Message[]> }
  | { type: 'SET_MSG_UNREAD'; count: number }
  | { type: 'BUMP_UNREAD' }
  | { type: 'SET_BANNER'; banner: { from: string; text: string } | null }
  | { type: 'OPEN_CHAT'; contact: Contact }
  | { type: 'CLOSE_CHAT' }
  | { type: 'SEND_MSG'; contactName: string; text: string }
  | { type: 'RECV_MSG'; contactName: string; text: string }
  | { type: 'DIAL_APPEND'; char: string }
  | { type: 'DIAL_DELETE' }
  | { type: 'DIAL_CLEAR' }
  | { type: 'MARK_NOTIFS_READ' }
  | { type: 'DISMISS_NOTIF'; id: string }
  | { type: 'PUSH_NOTIF'; notif: Notification }
  | { type: 'PUSH_TOAST'; text: string }
  | { type: 'REMOVE_TOAST'; id: string }
  | { type: 'SET_CONTEXT_MENU'; menu: OSState['contextMenu'] }
  | { type: 'SET_SEARCH'; q: string }
  | { type: 'TOGGLE_MUSIC' }
  | { type: 'SET_TRACK'; idx: number }
  | { type: 'NEXT_TRACK'; total: number }
  | { type: 'PREV_TRACK'; total: number }
  | { type: 'SET_CALENDAR'; date: Date }
  | { type: 'CREATE_NOTE'; title: string; body: string }
  | { type: 'UPDATE_NOTE'; id: string; title: string; body: string }
  | { type: 'DELETE_NOTE'; id: string }
  | { type: 'OPEN_NOTE'; id: string | null }
  | { type: 'NAVIGATE_WEB'; url: string }
  | { type: 'SET_USERNAME'; name: string }
  | { type: 'UNLOCK' }
  | { type: 'LOCK' };

function reducer(state: OSState, action: Action): OSState {
  switch (action.type) {
    case 'OPEN_APP':
      return { ...state, activeApp: action.app, prevApp: state.activeApp, drawerOpen: false, notifPanelOpen: false, quickSettingsOpen: false, contextMenu: null, msgUnread: action.app === 'mesaj' ? 0 : state.msgUnread };
    case 'CLOSE_APP':
      return { ...state, activeApp: null, chatContact: null };
    case 'TOGGLE_DRAWER':
      return { ...state, drawerOpen: !state.drawerOpen, notifPanelOpen: false, quickSettingsOpen: false };
    case 'CLOSE_DRAWER':
      return { ...state, drawerOpen: false };
    case 'TOGGLE_NOTIF_PANEL':
      return { ...state, notifPanelOpen: !state.notifPanelOpen, quickSettingsOpen: false, drawerOpen: false };
    case 'CLOSE_NOTIF_PANEL':
      return { ...state, notifPanelOpen: false };
    case 'TOGGLE_QS':
      return { ...state, quickSettingsOpen: !state.quickSettingsOpen, notifPanelOpen: false, drawerOpen: false };
    case 'CLOSE_QS':
      return { ...state, quickSettingsOpen: false };
    case 'TOGGLE_WIFI':
      return { ...state, wifi: !state.wifi };
    case 'TOGGLE_BT':
      return { ...state, bluetooth: !state.bluetooth };
    case 'TOGGLE_DND':
      return { ...state, dnd: !state.dnd };
    case 'SET_BRIGHTNESS':
      return { ...state, brightness: action.val };
    case 'SET_VOLUME':
      return { ...state, volume: action.val };
    case 'SET_THEME':
      return { ...state, theme: action.idx, wallpaper: THEMES[action.idx].bg, wallpaperUrl: '' };
    case 'SET_WALLPAPER_URL':
      return { ...state, wallpaperUrl: action.url };
    case 'ADD_COIN':
      return { ...state, coins: state.coins + action.amount };
    case 'SPEND_COIN':
      return { ...state, coins: Math.max(0, state.coins - action.amount) };
    case 'SET_COINS':
      return { ...state, coins: Math.max(0, action.amount) };
    case 'SET_THREADS':
      return { ...state, threads: action.threads };
    case 'SET_MSG_UNREAD':
      return { ...state, msgUnread: Math.max(0, action.count) };
    case 'BUMP_UNREAD':
      return { ...state, msgUnread: state.activeApp === 'mesaj' ? state.msgUnread : state.msgUnread + 1 };
    case 'SET_BANNER':
      return { ...state, banner: action.banner };
    case 'OPEN_CHAT':
      return { ...state, chatContact: action.contact };
    case 'CLOSE_CHAT':
      return { ...state, chatContact: null };
    case 'SEND_MSG': {
      const now = new Date();
      const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`;
      const thread = state.threads[action.contactName] ?? [];
      return { ...state, threads: { ...state.threads, [action.contactName]: [...thread, { who: 'me', text: action.text, time }] } };
    }
    case 'RECV_MSG': {
      const now = new Date();
      const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`;
      const thread = state.threads[action.contactName] ?? [];
      return { ...state, threads: { ...state.threads, [action.contactName]: [...thread, { who: 'them', text: action.text, time }] } };
    }
    case 'DIAL_APPEND':
      return { ...state, dialNumber: state.dialNumber.length < 12 ? state.dialNumber + action.char : state.dialNumber };
    case 'DIAL_DELETE':
      return { ...state, dialNumber: state.dialNumber.slice(0, -1) };
    case 'DIAL_CLEAR':
      return { ...state, dialNumber: '' };
    case 'MARK_NOTIFS_READ':
      return { ...state, notifications: state.notifications.map(n => ({ ...n, read: true })) };
    case 'DISMISS_NOTIF':
      return { ...state, notifications: state.notifications.filter(n => n.id !== action.id) };
    case 'PUSH_NOTIF':
      return { ...state, notifications: [action.notif, ...state.notifications] };
    case 'PUSH_TOAST':
      return { ...state, toasts: [...state.toasts, { id: String(Date.now()), text: action.text }] };
    case 'REMOVE_TOAST':
      return { ...state, toasts: state.toasts.filter(t => t.id !== action.id) };
    case 'SET_CONTEXT_MENU':
      return { ...state, contextMenu: action.menu };
    case 'SET_SEARCH':
      return { ...state, searchQuery: action.q };
    case 'TOGGLE_MUSIC':
      return { ...state, musicPlaying: !state.musicPlaying };
    case 'SET_TRACK':
      return { ...state, musicTrack: action.idx, musicPlaying: true };
    case 'NEXT_TRACK':
      return { ...state, musicTrack: (state.musicTrack + 1) % action.total, musicPlaying: true };
    case 'PREV_TRACK':
      return { ...state, musicTrack: (state.musicTrack - 1 + action.total) % action.total, musicPlaying: true };
    case 'SET_CALENDAR':
      return { ...state, calendarDate: action.date };
    case 'CREATE_NOTE': {
      const now = new Date();
      const date = `${now.getDate()} ${['Oca','Şub','Mar','Nis','May','Haz','Tem','Ağu','Eyl','Eki','Kas','Ara'][now.getMonth()]}`;
      const note = { id: `n${Date.now()}`, title: action.title, body: action.body, date };
      return { ...state, notes: [note, ...state.notes], activeNote: note.id };
    }
    case 'UPDATE_NOTE':
      return { ...state, notes: state.notes.map(n => n.id === action.id ? { ...n, title: action.title, body: action.body } : n) };
    case 'DELETE_NOTE':
      return { ...state, notes: state.notes.filter(n => n.id !== action.id), activeNote: null };
    case 'OPEN_NOTE':
      return { ...state, activeNote: action.id };
    case 'NAVIGATE_WEB':
      return { ...state, webUrl: action.url, webHistory: [...state.webHistory, action.url] };
    case 'SET_USERNAME':
      return { ...state, userName: action.name };
    case 'UNLOCK':
      return { ...state, lockScreen: false };
    case 'LOCK':
      return { ...state, lockScreen: true, activeApp: null };
    default:
      return state;
  }
}

export function useOS() {
  const [state, dispatch] = useReducer(reducer, initState, (s) => {
    // Item'a kayıtlı telefon verilerini yükle (çalınan telefonda veriler yeni sahibine görünür)
    const p = loadPhone();
    return {
      ...s,
      ...(p.theme !== undefined ? { theme: p.theme, wallpaper: THEMES[p.theme]?.bg ?? s.wallpaper } : {}),
      ...(p.wallpaperUrl ? { wallpaperUrl: p.wallpaperUrl } : {}),
      ...(p.brightness !== undefined ? { brightness: p.brightness } : {}),
      ...(p.volume !== undefined ? { volume: p.volume } : {}),
      ...(p.wifi !== undefined ? { wifi: p.wifi } : {}),
      ...(p.bluetooth !== undefined ? { bluetooth: p.bluetooth } : {}),
      ...(p.dnd !== undefined ? { dnd: p.dnd } : {}),
      ...(Array.isArray(p.notes) && p.notes.length ? { notes: p.notes } : {}),
    };
  });
  const toastTimer = useRef<Record<string, ReturnType<typeof setTimeout>>>({});

  // Değişen telefon verilerini item'a yaz
  useEffect(() => {
    try {
      localStorage.setItem(PHONE_KEY(), JSON.stringify({
        theme: state.theme, wallpaperUrl: state.wallpaperUrl, brightness: state.brightness,
        volume: state.volume, wifi: state.wifi, bluetooth: state.bluetooth, dnd: state.dnd, notes: state.notes,
      }));
    } catch { /* kota */ }
  }, [state.theme, state.wallpaperUrl, state.brightness, state.volume, state.wifi, state.bluetooth, state.dnd, state.notes]);

  // Oyun içi (MCEF) köprü varsa gerçek veriyi çek; yoksa mock kalır (tarayıcı önizleme).
  useEffect(() => {
    if (!hasBridge()) return;
    let alive = true;
    (async () => {
      const me = await bridge<{ name?: string }>('me');
      if (alive && me?.name) dispatch({ type: 'SET_USERNAME', name: me.name });
      const mk = await bridge<{ coins?: number }>('market');
      if (alive && mk && typeof mk.coins === 'number' && mk.coins >= 0)
        dispatch({ type: 'SET_COINS', amount: mk.coins });
      // Gerçek sohbet geçmişi + okunmamış sayısı
      const ms = await getMessages();
      if (alive && ms) {
        const threads: Record<string, Message[]> = {};
        for (const [peer, list] of Object.entries(ms.threads))
          threads[peer] = list.filter(m => m.who !== 'sys').map(m => ({ who: m.who as 'me' | 'them', text: m.text, time: '' }));
        if (Object.keys(threads).length) dispatch({ type: 'SET_THREADS', threads });
        dispatch({ type: 'SET_MSG_UNREAD', count: Object.values(ms.unread).reduce((a, b) => a + b, 0) });
      }
      // Sunucudaki uygulama mağazası kataloğunu çek (geliştirici yayınları)
      const ca = await getServerApps();
      if (alive && ca) setCloudApps(ca.apps);
    })();
    return () => { alive = false; };
  }, []);

  // Mod'dan push bildirimleri (PWNotify): yeni mesaj → sohbete ekle + rozet + baloncuk + ding
  const bannerTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    (window as any).PWNotify = (type: string, data: any) => {
      if (type === 'apps') {
        // Biri uygulama yayınladı/kaldırdı → katalog önbelleğini tazele
        const c = parseCloudApps(data);
        if (c) setCloudApps(c.apps);
        return;
      }
      if (type === 'msg' && data && data.from) {
        dispatch({ type: 'RECV_MSG', contactName: String(data.peer || data.from), text: String(data.text || '') });
        dispatch({ type: 'BUMP_UNREAD' });
        dispatch({ type: 'SET_BANNER', banner: { from: String(data.from), text: String(data.text || '') } });
        ding();
        if (bannerTimer.current) clearTimeout(bannerTimer.current);
        bannerTimer.current = setTimeout(() => dispatch({ type: 'SET_BANNER', banner: null }), 4000);
      }
    };
    return () => { (window as any).PWNotify = undefined; };
  }, []);

  const toast = useCallback((text: string) => {
    const id = String(Date.now());
    dispatch({ type: 'PUSH_TOAST', text });
    toastTimer.current[id] = setTimeout(() => {
      dispatch({ type: 'REMOVE_TOAST', id });
    }, 2500);
  }, []);

  return { state, dispatch, toast };
}
