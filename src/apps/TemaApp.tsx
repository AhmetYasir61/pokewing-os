import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { THEMES } from '../data';
import { Check, Image, Palette } from 'lucide-react';

interface Props {
  onBack: () => void;
  currentTheme: number;
  wallpaperUrl: string;
  onSetTheme: (idx: number) => void;
  onSetWallpaper: (url: string) => void;
  onToast: (msg: string) => void;
}

const WALLPAPER_PRESETS = [
  { url: 'https://images.pexels.com/photos/1169754/pexels-photo-1169754.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Gökyüzü' },
  { url: 'https://images.pexels.com/photos/1366909/pexels-photo-1366909.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Orman' },
  { url: 'https://images.pexels.com/photos/3408744/pexels-photo-3408744.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Dağ' },
  { url: 'https://images.pexels.com/photos/1323550/pexels-photo-1323550.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Şelale' },
  { url: 'https://images.pexels.com/photos/3586966/pexels-photo-3586966.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Okyanus' },
  { url: 'https://images.pexels.com/photos/2310713/pexels-photo-2310713.jpeg?auto=compress&cs=tinysrgb&w=800', label: 'Gün Batımı' },
];

export function TemaApp({ onBack, currentTheme, wallpaperUrl, onSetTheme, onSetWallpaper, onToast }: Props) {
  const [tab, setTab] = useState<'tema' | 'duvar'>('tema');
  const [customUrl, setCustomUrl] = useState('');

  return (
    <AppWindow title="Kişiselleştir" accentColor="#BF5AF2" onBack={onBack}>
      <div className="flex flex-col h-full">
        <div className="px-4 py-2">
          <div className="seg-ctrl">
            <button className={`seg-btn ${tab === 'tema' ? 'active' : ''}`} onClick={() => setTab('tema')}>
              <Palette size={13} className="inline mr-1" /> Tema
            </button>
            <button className={`seg-btn ${tab === 'duvar' ? 'active' : ''}`} onClick={() => setTab('duvar')}>
              <Image size={13} className="inline mr-1" /> Duvar Kağıdı
            </button>
          </div>
        </div>

        {tab === 'tema' ? (
          <div className="flex-1 scroll-area px-4 py-2 flex flex-col gap-3">
            <div className="text-xs font-semibold px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
              Renk Teması
            </div>
            {THEMES.map((theme, i) => (
              <button
                key={i}
                className="flex items-center gap-4 rounded-2xl p-4 text-left"
                style={{
                  background: 'rgba(28,28,30,0.9)',
                  border: `0.5px solid ${currentTheme === i ? '#BF5AF2' : 'rgba(255,255,255,0.06)'}`,
                }}
                onClick={() => { onSetTheme(i); onToast(`${theme.name} teması uygulandı!`); }}
              >
                <div
                  className="w-12 h-12 rounded-xl flex-shrink-0"
                  style={{ background: theme.bg }}
                />
                <div className="flex-1">
                  <div className="text-white font-semibold">{theme.name}</div>
                  <div className="text-xs mt-0.5" style={{ color: 'rgba(235,235,245,0.5)' }}>
                    {currentTheme === i ? 'Aktif tema' : 'Uygulamak için dokun'}
                  </div>
                </div>
                {currentTheme === i && (
                  <div className="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0" style={{ background: '#BF5AF2' }}>
                    <Check size={14} color="#fff" />
                  </div>
                )}
              </button>
            ))}
          </div>
        ) : (
          <div className="flex-1 scroll-area px-4 py-2 flex flex-col gap-3">
            <div className="text-xs font-semibold px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
              Hazır Duvar Kağıtları
            </div>
            <div className="grid grid-cols-3 gap-2">
              {WALLPAPER_PRESETS.map((w, i) => (
                <button
                  key={i}
                  className="rounded-xl overflow-hidden relative focus:outline-none"
                  style={{ aspectRatio: '9/16', border: wallpaperUrl === w.url ? '2.5px solid #BF5AF2' : '2px solid transparent' }}
                  onClick={() => { onSetWallpaper(w.url); onToast(`${w.label} duvar kağıdı uygulandı!`); }}
                >
                  <img src={w.url} alt={w.label} className="w-full h-full object-cover" />
                  <div className="absolute bottom-0 left-0 right-0 py-1 px-1.5" style={{ background: 'linear-gradient(to top,rgba(0,0,0,0.6),transparent)' }}>
                    <span className="text-white text-[10px] font-medium">{w.label}</span>
                  </div>
                  {wallpaperUrl === w.url && (
                    <div className="absolute top-1.5 right-1.5 w-5 h-5 rounded-full flex items-center justify-center" style={{ background: '#BF5AF2' }}>
                      <Check size={11} color="#fff" />
                    </div>
                  )}
                </button>
              ))}
            </div>

            <div className="text-xs font-semibold px-1 mt-2" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
              Özel URL
            </div>
            <div className="flex gap-2">
              <input
                type="url"
                value={customUrl}
                onChange={e => setCustomUrl(e.target.value)}
                placeholder="https://..."
                style={{ flex: 1 }}
              />
              <button
                className="pill-btn primary"
                onClick={() => { if (customUrl) { onSetWallpaper(customUrl); onToast('Duvar kağıdı uygulandı!'); }}}
              >
                Uygula
              </button>
            </div>
          </div>
        )}
      </div>
    </AppWindow>
  );
}
