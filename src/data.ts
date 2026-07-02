import { AppMeta, Contact, NewsItem, Product, InventoryItem } from './types';

export const APPS: AppMeta[] = [
  { id: 'market', name: 'Market', gradient: 'linear-gradient(160deg,#FFD60A,#FF9F0A)', iconKey: 'market', badge: 0 },
  { id: 'depo', name: 'Depo', gradient: 'linear-gradient(160deg,#0A84FF,#0a4cae)', iconKey: 'depo' },
  { id: 'katalog', name: 'Katalog', gradient: 'linear-gradient(160deg,#5E5CE6,#3634A3)', iconKey: 'katalog' },
  { id: 'news', name: 'Duyurular', gradient: 'linear-gradient(160deg,#FF453A,#C5281C)', iconKey: 'news', badge: 3 },
  { id: 'mesaj', name: 'Mesajlar', gradient: 'linear-gradient(160deg,#30D158,#1a8f3c)', iconKey: 'mesaj', badge: 2 },
  { id: 'kamera', name: 'Kamera', gradient: 'linear-gradient(160deg,#3a3a3e,#1a1a1e)', iconKey: 'kamera' },
  { id: 'vc', name: 'Telefon', gradient: 'linear-gradient(160deg,#30D158,#178a3a)', iconKey: 'vc' },
  { id: 'galeri', name: 'Galeri', gradient: 'linear-gradient(160deg,#FF375F,#BF5AF2)', iconKey: 'galeri' },
  { id: 'web', name: 'Tarayıcı', gradient: 'linear-gradient(160deg,#40C8E0,#0A84FF)', iconKey: 'web' },
  { id: 'tema', name: 'Kişiselleştir', gradient: 'linear-gradient(160deg,#BF5AF2,#7A2EAE)', iconKey: 'tema' },
  { id: 'muzik', name: 'Müzik', gradient: 'linear-gradient(160deg,#FF375F,#C5281C)', iconKey: 'muzik' },
  { id: 'takvim', name: 'Takvim', gradient: 'linear-gradient(160deg,#FF9F0A,#FF453A)', iconKey: 'takvim' },
  { id: 'notlar', name: 'Notlar', gradient: 'linear-gradient(160deg,#FFD60A,#FF9F0A)', iconKey: 'notlar' },
  { id: 'yayin', name: 'Yayın', gradient: 'linear-gradient(160deg,#53FC18,#1a8f3c)', iconKey: 'yayin' },
  { id: 'store', name: 'Play Store', gradient: 'linear-gradient(160deg,#30D158,#0A84FF)', iconKey: 'store' },
  { id: 'harita', name: 'Harita', gradient: 'linear-gradient(160deg,#30D158,#40C8E0)', iconKey: 'harita' },
  { id: 'hesap', name: 'Hesap', gradient: 'linear-gradient(160deg,#636366,#48484A)', iconKey: 'hesap' },
  { id: 'ayar', name: 'Ayarlar', gradient: 'linear-gradient(160deg,#8E8E93,#48484A)', iconKey: 'ayar' },
];

export const DOCK_APPS: string[] = ['vc', 'mesaj', 'web', 'market'];

export const THEMES = [
  { name: 'Uzay', bg: 'radial-gradient(60% 50% at 25% 12%,#3a2a6e55,transparent 60%),radial-gradient(55% 45% at 85% 22%,#0a5d8a55,transparent 60%),radial-gradient(70% 60% at 60% 100%,#7a1f5a55,transparent 60%),linear-gradient(165deg,#10131f,#0a0a12 55%,#140a1c)' },
  { name: 'Okyanus', bg: 'radial-gradient(60% 50% at 30% 15%,#0a5d8a66,transparent),linear-gradient(165deg,#07172a,#0a1422 55%,#081c2a)' },
  { name: 'Orman', bg: 'radial-gradient(60% 50% at 30% 15%,#1a7a4a66,transparent),linear-gradient(165deg,#0a1b12,#0e2a18 55%,#0a1a10)' },
  { name: 'Gün Batımı', bg: 'radial-gradient(60% 50% at 30% 15%,#ff7a3a55,transparent),linear-gradient(165deg,#2a120a,#1c0a14 55%,#281014)' },
  { name: 'Mor', bg: 'radial-gradient(60% 50% at 30% 15%,#7a2eae66,transparent),linear-gradient(165deg,#160a26,#0a0a16 55%,#120820)' },
  { name: 'Kırmızı', bg: 'radial-gradient(60% 50% at 30% 15%,#8a0a1a66,transparent),linear-gradient(165deg,#1c0a0a,#0e0508 55%,#180a0a)' },
];

export const MOCK_PRODUCTS: Product[] = [
  { id: 1, name: 'VIP Rütbesi', type: 'Rütbe', price: 5000, desc: 'Renkli isim, /fly komutu, özel kit ve daha fazlası. Sunucu genelinde özel ayrıcalıklar sağlar.' },
  { id: 2, name: 'Başlangıç Kiti', type: 'Kit', price: 1500, desc: 'Yeni başlayanlar için tam ekipman seti. Silahlar, zırh ve yiyecek içerir.' },
  { id: 3, name: 'Efsane Anahtarı', type: 'Anahtar', price: 3200, desc: 'Efsanevi sandığı açar. Nadir ödüller şansı yüksek!' },
  { id: 4, name: 'Zapdos Petty', type: 'Kozmetik', price: 3500, desc: 'Seni takip eden sevimli Zapdos evcil hayvanı.' },
  { id: 5, name: 'PvP Kiti', type: 'Kit', price: 4000, desc: 'Savaş için tam donanım seti. Efsanevi silahlar dahil.' },
  { id: 6, name: 'Uçuş Modu', type: 'Özellik', price: 2500, desc: 'Sunucuda özgürce uçabilme. 30 günlük.' },
  { id: 7, name: 'Rank Up Paketi', type: 'Paket', price: 8000, desc: 'Bir üst rütbeye anında yükselt! Tüm ayrıcalıklar dahil.' },
];

export const MOCK_INVENTORY: InventoryItem[] = [
  { id: 9, name: 'VIP Rütbesi', type: 'Rütbe', slots: 1 },
  { id: 10, name: 'Başlangıç Kiti', type: 'Kit', slots: 3 },
  { id: 11, name: 'Efsane Anahtarı', type: 'Anahtar', slots: 2 },
];

export const MOCK_CONTACTS: Contact[] = [
  { name: 'Misty', number: '5654321', online: true },
  { name: 'Brock', number: '5998877', online: true },
  { name: 'Gary', number: '5111222', online: false },
  { name: 'Nurse Joy', number: '5333444', online: true },
  { name: 'Officer Jenny', number: '5555666', online: false },
  { name: 'Prof. Oak', number: '5777888', online: false },
];

export const MOCK_NEWS: NewsItem[] = [
  { id: 1, title: 'Sezon 1 Başladı!', body: 'Yeni ödüller, görevler ve sınırlı içerikler seni bekliyor. Hemen giriş yap, günlük ödüllerini topla!', date: '30 Haz', tag: 'Güncelleme' },
  { id: 2, title: 'Hafta Sonu %50 İndirim', body: 'Tüm rütbelerde bu hafta sonu geçerli %50 indirim. Fırsatı kaçırma!', date: '29 Haz', tag: 'Kampanya' },
  { id: 3, title: 'PvP Turnuvası', body: 'Cumartesi saat 20:00\'de büyük PvP turnuvası başlıyor. Ödül havuzu: 50.000 Coin!', date: '28 Haz', tag: 'Etkinlik' },
  { id: 4, title: 'Sunucu Bakımı', body: 'Yarın 03:00-05:00 arasında planlı bakım yapılacak. Bu süre zarfında sunucu erişilemez olabilir.', date: '27 Haz', tag: 'Bilgi' },
  { id: 5, title: 'Yeni Harita: Volcano Isle', body: 'Tamamen yeni bir harita eklendi. Volkanik ada teması ve özel boss\'ları keşfet!', date: '25 Haz', tag: 'Yeni' },
];

export const MUSIC_TRACKS = [
  { title: 'Pallet Town', artist: 'Game Freak', duration: '3:24', color: '#0A84FF' },
  { title: 'Lavender Town', artist: 'Game Freak', duration: '2:58', color: '#BF5AF2' },
  { title: 'Battle! Trainer', artist: 'Game Freak', duration: '1:42', color: '#FF453A' },
  { title: 'Viridian Forest', artist: 'Game Freak', duration: '4:12', color: '#30D158' },
  { title: 'Pokemon Center', artist: 'Game Freak', duration: '2:15', color: '#FFD60A' },
];

export const GALLERY_PHOTOS = [
  'https://images.pexels.com/photos/1169754/pexels-photo-1169754.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/1366909/pexels-photo-1366909.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/3408744/pexels-photo-3408744.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/1323550/pexels-photo-1323550.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/3586966/pexels-photo-3586966.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/2310713/pexels-photo-2310713.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/1366957/pexels-photo-1366957.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/3358707/pexels-photo-3358707.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/1631677/pexels-photo-1631677.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/3220366/pexels-photo-3220366.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/3225517/pexels-photo-3225517.jpeg?auto=compress&cs=tinysrgb&w=400',
  'https://images.pexels.com/photos/1556665/pexels-photo-1556665.jpeg?auto=compress&cs=tinysrgb&w=400',
];
