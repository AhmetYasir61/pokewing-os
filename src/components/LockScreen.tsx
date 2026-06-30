import React, { useState, useEffect } from 'react';
import { Fingerprint } from 'lucide-react';

interface Props { wallpaper: string; wallpaperUrl: string; onUnlock: () => void; }

export function LockScreen({ wallpaper, wallpaperUrl, onUnlock }: Props) {
  const [time, setTime] = useState(new Date());
  const [unlocking, setUnlocking] = useState(false);

  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  const days = ['Pazar','Pazartesi','Salı','Çarşamba','Perşembe','Cuma','Cumartesi'];
  const months = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];

  const handleUnlock = () => {
    setUnlocking(true);
    setTimeout(onUnlock, 350);
  };

  return (
    <div
      className="absolute inset-0 z-50 flex flex-col items-center"
      style={{
        background: wallpaperUrl ? `url(${wallpaperUrl}) center/cover` : wallpaper,
      }}
    >
      <div className="absolute inset-0" style={{ background: 'rgba(0,0,0,0.35)' }} />
      <div className="relative z-10 flex flex-col items-center flex-1 justify-center gap-2 w-full" style={{ opacity: unlocking ? 0 : 1, transition: 'opacity 0.3s' }}>
        <div className="text-white font-thin" style={{ fontSize: 72, letterSpacing: -2, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
          {String(time.getHours()).padStart(2,'0')}:{String(time.getMinutes()).padStart(2,'0')}
        </div>
        <div className="text-white text-lg font-medium" style={{ textShadow: '0 1px 8px rgba(0,0,0,0.5)' }}>
          {days[time.getDay()]}, {time.getDate()} {months[time.getMonth()]}
        </div>
      </div>

      <div className="relative z-10 pb-14 flex flex-col items-center gap-3" style={{ opacity: unlocking ? 0 : 1, transition: 'opacity 0.3s' }}>
        <button
          className="flex flex-col items-center gap-2 focus:outline-none"
          onClick={handleUnlock}
        >
          <div
            className="w-16 h-16 rounded-full flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.15)', backdropFilter: 'blur(12px)', border: '1px solid rgba(255,255,255,0.3)' }}
          >
            <Fingerprint size={32} color="rgba(255,255,255,0.9)" />
          </div>
          <span className="text-white text-sm font-medium" style={{ textShadow: '0 1px 4px rgba(0,0,0,0.5)' }}>
            Kilidi Aç
          </span>
        </button>
      </div>
    </div>
  );
}
