import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { Camera, Zap, ZapOff, RotateCcw, Image } from 'lucide-react';
import { GALLERY_PHOTOS } from '../data';

interface Props { onBack: () => void; onToast: (msg: string) => void; }

export function KameraApp({ onBack, onToast }: Props) {
  const [flash, setFlash] = useState(false);
  const [mode, setMode] = useState<'foto' | 'video' | 'portre'>('foto');
  const [lastShot, setLastShot] = useState<string | null>(null);
  const [shutter, setShutter] = useState(false);

  const handleShoot = () => {
    setShutter(true);
    setTimeout(() => setShutter(false), 180);
    const photo = GALLERY_PHOTOS[Math.floor(Math.random() * GALLERY_PHOTOS.length)];
    setLastShot(photo);
    onToast('Fotoğraf çekildi!');
  };

  return (
    <AppWindow title="" onBack={onBack} noHeader transparent>
      <div
        className="absolute inset-0 flex flex-col"
        style={{ background: '#000' }}
      >
        {/* Viewfinder */}
        <div className="flex-1 relative overflow-hidden">
          <img
            src="https://images.pexels.com/photos/3408744/pexels-photo-3408744.jpeg?auto=compress&cs=tinysrgb&w=800"
            alt="viewfinder"
            className="w-full h-full object-cover"
          />
          {/* Shutter flash */}
          {shutter && <div className="absolute inset-0 bg-white" style={{ opacity: 0.8 }} />}

          {/* Top controls */}
          <div className="absolute top-0 left-0 right-0 flex items-center justify-between px-5 pt-12 pb-4"
            style={{ background: 'linear-gradient(to bottom,rgba(0,0,0,0.6),transparent)' }}>
            <button onClick={onBack} className="text-yellow-400 font-semibold text-base focus:outline-none">İptal</button>
            <button
              className="w-9 h-9 rounded-full flex items-center justify-center focus:outline-none"
              style={{ background: 'rgba(0,0,0,0.4)' }}
              onClick={() => setFlash(!flash)}
            >
              {flash ? <Zap size={18} fill="#FFD60A" color="#FFD60A" /> : <ZapOff size={18} color="rgba(255,255,255,0.8)" />}
            </button>
          </div>

          {/* Grid overlay */}
          <div className="absolute inset-0 pointer-events-none" style={{ opacity: 0.2 }}>
            <div className="absolute left-1/3 top-0 bottom-0 w-px bg-white" />
            <div className="absolute left-2/3 top-0 bottom-0 w-px bg-white" />
            <div className="absolute top-1/3 left-0 right-0 h-px bg-white" />
            <div className="absolute top-2/3 left-0 right-0 h-px bg-white" />
          </div>

          {/* Focus indicator */}
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div style={{ width: 72, height: 72, border: '1.5px solid rgba(255,214,10,0.7)', borderRadius: 4 }} />
          </div>
        </div>

        {/* Bottom controls */}
        <div style={{ background: '#000', paddingBottom: 32 }}>
          {/* Mode selector */}
          <div className="flex justify-center gap-6 py-3">
            {(['video', 'foto', 'portre'] as const).map(m => (
              <button
                key={m}
                className="focus:outline-none uppercase text-xs font-bold tracking-widest"
                style={{ color: mode === m ? '#FFD60A' : 'rgba(255,255,255,0.5)' }}
                onClick={() => setMode(m)}
              >
                {m === 'foto' ? 'FOTOĞRAF' : m === 'video' ? 'VİDEO' : 'PORTRESİ'}
              </button>
            ))}
          </div>

          {/* Shutter row */}
          <div className="flex items-center justify-between px-8 py-2">
            {/* Last photo thumbnail */}
            <button className="w-14 h-14 rounded-xl overflow-hidden flex-shrink-0 focus:outline-none" style={{ border: '2px solid rgba(255,255,255,0.3)' }}>
              {lastShot ? (
                <img src={lastShot} alt="" className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center" style={{ background: 'rgba(255,255,255,0.08)' }}>
                  <Image size={20} color="rgba(255,255,255,0.4)" />
                </div>
              )}
            </button>

            {/* Shutter button */}
            <button
              className="focus:outline-none active:scale-90 transition-transform"
              onClick={handleShoot}
            >
              <div
                style={{ width: 72, height: 72, borderRadius: '50%', border: '3px solid #fff', padding: 3 }}
              >
                <div style={{ width: '100%', height: '100%', borderRadius: '50%', background: mode === 'video' ? '#FF453A' : '#fff' }} />
              </div>
            </button>

            {/* Flip camera */}
            <button className="w-14 h-14 rounded-full flex items-center justify-center focus:outline-none" style={{ background: 'rgba(255,255,255,0.12)' }} onClick={() => onToast('Kamera döndürüldü')}>
              <RotateCcw size={22} color="#fff" />
            </button>
          </div>
        </div>
      </div>
    </AppWindow>
  );
}
