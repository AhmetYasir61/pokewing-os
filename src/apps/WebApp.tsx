import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { hasBridge, browseInGame } from '../bridge';
import { Search, ArrowLeft, ArrowRight, RefreshCw, Home, Bookmark, X } from 'lucide-react';

interface Props { onBack: () => void; webUrl: string; onNavigate: (url: string) => void; onToast: (msg: string) => void; }

const BOOKMARKS = [
  { name: 'Google', url: 'https://google.com', icon: '🔍' },
  { name: 'YouTube', url: 'https://youtube.com', icon: '▶️' },
  { name: 'Twitter', url: 'https://twitter.com', icon: '🐦' },
  { name: 'GitHub', url: 'https://github.com', icon: '💻' },
];

const QUICK_SEARCH = [
  'Pokemon oyunu', 'Minecraft sunucusu', 'PvP taktikleri', 'En iyi rütbe',
];

const FEATURED_SITES = [
  { name: 'Sunucu Forumu', url: 'https://forum.server.com', icon: '💬', desc: 'Tartışmalar ve duyurular' },
  { name: 'Wiki', url: 'https://wiki.server.com', icon: '📚', desc: 'Oyun rehberi ve bilgi tabanı' },
  { name: 'Discord', url: 'https://discord.gg/server', icon: '🎮', desc: 'Topluluk kanalı' },
];

export function WebApp({ onBack, webUrl, onNavigate, onToast }: Props) {
  const [urlInput, setUrlInput] = useState(webUrl);
  const [loading, setLoading] = useState(false);
  const [showBar, setShowBar] = useState(true);
  const [page, setPage] = useState<'home' | 'page'>('home');

  const navigate = (url: string) => {
    let full = url;
    if (!url.startsWith('http')) {
      // URL değilse Google araması yap
      full = url.includes('.') && !url.includes(' ')
        ? `https://${url}`
        : `https://www.google.com/search?q=${encodeURIComponent(url)}`;
    }
    if (hasBridge()) {
      // OYUN İÇİ GERÇEK TARAYICI: Google/YouTube MCEF overlay'de açılır (ses dahil).
      onToast('Tarayıcı açılıyor...');
      void browseInGame(full);
      onNavigate(full);
      return;
    }
    setLoading(true);
    setUrlInput(full);
    onNavigate(full);
    setPage('page');
    setTimeout(() => setLoading(false), 1500);
  };

  const displayUrl = (url: string) => {
    try { return new URL(url).hostname.replace('www.', ''); } catch { return url; }
  };

  if (page === 'page') {
    return (
      <AppWindow title="" onBack={onBack} noHeader>
        <div className="absolute inset-0 flex flex-col" style={{ background: 'rgb(10,10,12)' }}>
          {/* Address bar */}
          <div className="flex items-center gap-2 px-3 py-2 flex-shrink-0" style={{ paddingTop: 10, borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}>
            <button onClick={onBack} className="w-8 h-8 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.06)' }}>
              <ArrowLeft size={17} color="rgba(235,235,245,0.7)" />
            </button>
            <div
              className="flex-1 flex items-center gap-2 px-3 py-2 rounded-xl"
              style={{ background: 'rgba(44,44,46,0.9)', border: '0.5px solid rgba(255,255,255,0.08)' }}
              onClick={() => setPage('home')}
            >
              <Search size={13} color="rgba(235,235,245,0.4)" />
              <span className="text-sm flex-1 truncate" style={{ color: 'rgba(235,235,245,0.7)' }}>{displayUrl(urlInput)}</span>
              {loading && <div className="spinner flex-shrink-0" style={{ width: 14, height: 14, borderWidth: 2 }} />}
            </div>
            <button onClick={() => { setLoading(true); setTimeout(() => setLoading(false), 1000); }} className="w-8 h-8 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.06)' }}>
              <RefreshCw size={15} color="rgba(235,235,245,0.7)" />
            </button>
          </div>

          {/* Simulated page */}
          <div className="flex-1 flex flex-col items-center justify-center gap-4 px-8">
            <div className="text-5xl">🌐</div>
            <div className="text-white font-bold text-xl text-center">{displayUrl(urlInput)}</div>
            <div className="text-center text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>
              Bu tarayıcı demo moddadır.{'\n'}Gerçek web içeriği gösterilmiyor.
            </div>
            <button className="pill-btn ghost" onClick={() => setPage('home')}>Ana Sayfaya Dön</button>
          </div>
        </div>
      </AppWindow>
    );
  }

  return (
    <AppWindow title="Tarayıcı" accentColor="#0A84FF" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Search bar */}
        <div className="px-4 pt-2 pb-3">
          <div className="flex items-center gap-2 px-4 py-2.5 rounded-xl" style={{ background: 'rgba(44,44,46,0.9)', border: '0.5px solid rgba(255,255,255,0.08)' }}>
            <Search size={15} color="rgba(235,235,245,0.4)" />
            <input
              value={urlInput}
              onChange={e => setUrlInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && navigate(urlInput)}
              placeholder="Ara veya URL gir..."
              style={{ flex: 1, background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 14, fontFamily: 'inherit', padding: 0 }}
            />
            {urlInput && (
              <button onClick={() => setUrlInput('')}>
                <X size={14} color="rgba(235,235,245,0.4)" />
              </button>
            )}
          </div>
        </div>

        <div className="flex-1 scroll-area px-4 pb-4 flex flex-col gap-4">
          {/* Quick search */}
          <div>
            <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Hızlı Ara</div>
            <div className="flex flex-wrap gap-2">
              {QUICK_SEARCH.map((q, i) => (
                <button
                  key={i}
                  className="px-3 py-1.5 rounded-full text-sm"
                  style={{ background: 'rgba(44,44,46,0.9)', color: '#0A84FF' }}
                  onClick={() => navigate(q)}
                >
                  {q}
                </button>
              ))}
            </div>
          </div>

          {/* Bookmarks */}
          <div>
            <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Yer İşaretleri</div>
            <div className="grid grid-cols-4 gap-2">
              {BOOKMARKS.map((b, i) => (
                <button
                  key={i}
                  className="flex flex-col items-center gap-1.5 p-2 rounded-xl"
                  style={{ background: 'rgba(44,44,46,0.7)' }}
                  onClick={() => navigate(b.url)}
                >
                  <span style={{ fontSize: 24 }}>{b.icon}</span>
                  <span className="text-[11px] text-white font-medium">{b.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Featured */}
          <div>
            <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Öne Çıkanlar</div>
            <div className="flex flex-col gap-2">
              {FEATURED_SITES.map((s, i) => (
                <button
                  key={i}
                  className="flex items-center gap-3 p-3 rounded-2xl text-left"
                  style={{ background: 'rgba(28,28,30,0.9)' }}
                  onClick={() => navigate(s.url)}
                >
                  <span style={{ fontSize: 28 }}>{s.icon}</span>
                  <div>
                    <div className="text-white font-semibold text-sm">{s.name}</div>
                    <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{s.desc}</div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AppWindow>
  );
}
