export type AppId =
  | 'home'
  | 'yayin'
  | 'market'
  | 'depo'
  | 'katalog'
  | 'news'
  | 'mesaj'
  | 'kamera'
  | 'vc'
  | 'galeri'
  | 'web'
  | 'tema'
  | 'ayar'
  | 'hesap'
  | 'muzik'
  | 'takvim'
  | 'notlar'
  | 'harita';

export interface AppMeta {
  id: AppId;
  name: string;
  gradient: string;
  iconKey: string;
  dockSlot?: boolean;
  badge?: number;
}

export interface Product {
  id: number;
  name: string;
  type: string;
  price: number;
  desc: string;
  imageUrl?: string;
}

export interface InventoryItem {
  id: number;
  name: string;
  type: string;
  slots?: number;
}

export interface Contact {
  name: string;
  number: string;
  online: boolean;
  avatar?: string;
}

export interface Message {
  who: 'me' | 'them';
  text: string;
  time: string;
}

export interface NewsItem {
  id: number;
  title: string;
  body: string;
  date: string;
  tag: string;
}

export interface Notification {
  id: string;
  app: AppId;
  title: string;
  body: string;
  time: string;
  read: boolean;
}

export interface OSState {
  activeApp: AppId | null;
  prevApp: AppId | null;
  drawerOpen: boolean;
  notifPanelOpen: boolean;
  quickSettingsOpen: boolean;
  lockScreen: boolean;
  theme: number;
  wallpaper: string;
  wallpaperUrl: string;
  wifi: boolean;
  bluetooth: boolean;
  dnd: boolean;
  darkMode: boolean;
  brightness: number;
  volume: number;
  coins: number;
  userName: string;
  chatContact: Contact | null;
  threads: Record<string, Message[]>;
  dialNumber: string;
  notifications: Notification[];
  toasts: { id: string; text: string }[];
  contextMenu: { x: number; y: number; app: AppId } | null;
  searchQuery: string;
  musicPlaying: boolean;
  musicTrack: number;
  calendarDate: Date;
  notes: { id: string; title: string; body: string; date: string }[];
  activeNote: string | null;
  webUrl: string;
  webHistory: string[];
  msgUnread: number;                                  // toplam okunmamış mesaj (app rozeti)
  banner: { from: string; text: string } | null;      // üstte bildirim baloncuğu
}
