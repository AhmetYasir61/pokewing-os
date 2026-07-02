import React, { useState, useEffect } from 'react';
import { Delete, Lock } from 'lucide-react';
import { itemId } from '../appstore';

interface Props { wallpaper: string; wallpaperUrl: string; onUnlock: () => void; }

// PIN item'a bağlıdır (telefon çalınırsa PIN de onunla gider). Varsayılan: 0000
export const PIN_KEY = () => 'pwpin:' + itemId();
export const getPin = (): string => { try { return localStorage.getItem(PIN_KEY()) || '0000'; } catch { return '0000'; } };
export const setPin = (p: string): void => { try { localStorage.setItem(PIN_KEY(), p); } catch { /* kota */ } };

export function LockScreen({ wallpaper, wallpaperUrl, onUnlock }: Props) {
  const [time, setTime] = useState(new Date());
  const [entry, setEntry] = useState('');
  const [shake, setShake] = useState(0);
  const [unlocking, setUnlocking] = useState(false);
  const pin = getPin();

  useEffect(() => {
    const t = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(t);
  }, []);

  // Fiziksel klavye desteği (MCEF klavye iletiyor)
  useEffect(() => {
    const k = (e: KeyboardEvent) => {
      if (/^[0-9]$/.test(e.key)) press(e.key);
      else if (e.key === 'Backspace') setEntry(v => v.slice(0, -1));
    };
    window.addEventListener('keydown', k);
    return () => window.removeEventListener('keydown', k);
  });

  const press = (d: string) => {
    setEntry(prev => {
      const next = (prev + d).slice(0, pin.length);
      if (next.length === pin.length) {
        if (next === pin) {
          setUnlocking(true);
          setTimeout(onUnlock, 300);
        } else {
          setShake(s => s + 1);
          setTimeout(() => setEntry(''), 350);
        }
      }
      return next;
    });
  };

  const days = ['Pazar', 'Pazartesi', 'Salı', 'Çarşamba', 'Perşembe', 'Cuma', 'Cumartesi'];
  const months = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran', 'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];

  const KEYS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', '⌫'];

  return (
    <div className="absolute inset-0 z-50 flex flex-col items-center"
      style={{ background: wallpaperUrl ? `url(${wallpaperUrl}) center/cover` : wallpaper }}>
      <div className="absolute inset-0" style={{ background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(6px)' }} />

      <div className="relative z-10 flex flex-col items-center w-full pt-14 gap-1"
        style={{ opacity: unlocking ? 0 : 1, transition: 'opacity 0.3s' }}>
        <div className="text-white font-thin" style={{ fontSize: 54, letterSpacing: -2, lineHeight: 1, fontVariantNumeric: 'tabular-nums' }}>
          {String(time.getHours()).padStart(2, '0')}:{String(time.getMinutes()).padStart(2, '0')}
        </div>
        <div className="text-white text-[15px] font-medium" style={{ textShadow: '0 1px 8px rgba(0,0,0,0.5)' }}>
          {days[time.getDay()]}, {time.getDate()} {months[time.getMonth()]}
        </div>
      </div>

      {/* PIN alanı */}
      <div className="relative z-10 flex-1 flex flex-col items-center justify-center gap-5 w-full"
        style={{ opacity: unlocking ? 0 : 1, transition: 'opacity 0.3s' }}>
        <div className="flex flex-col items-center gap-2">
          <Lock size={16} color="rgba(255,255,255,0.7)" />
          <span className="text-white text-[14px] font-medium">PIN Kodunu Gir</span>
        </div>
        {/* noktalar */}
        <div key={shake} className="flex gap-3" style={{ animation: shake ? 'pinshake 0.35s' : undefined }}>
          {Array.from({ length: pin.length }, (_, i) => (
            <div key={i} className="rounded-full"
              style={{
                width: 13, height: 13,
                background: i < entry.length ? '#fff' : 'rgba(255,255,255,0.25)',
                border: '1px solid rgba(255,255,255,0.5)', transition: 'background 0.15s',
              }} />
          ))}
        </div>
        {/* tuş takımı */}
        <div className="grid grid-cols-3 gap-3 mt-2">
          {KEYS.map((k, i) => k === '' ? <div key={i} /> : (
            <button key={i}
              onClick={() => k === '⌫' ? setEntry(v => v.slice(0, -1)) : press(k)}
              className="rounded-full flex items-center justify-center text-white font-medium active:scale-90 transition-transform focus:outline-none"
              style={{
                width: 64, height: 64, fontSize: 24,
                background: 'rgba(255,255,255,0.12)', backdropFilter: 'blur(10px)',
                border: '0.5px solid rgba(255,255,255,0.18)',
              }}>
              {k === '⌫' ? <Delete size={22} /> : k}
            </button>
          ))}
        </div>
        {pin === '0000' && (
          <div className="text-[11.5px] px-3 py-1 rounded-full"
            style={{ background: 'rgba(255,214,10,0.15)', color: '#FFD60A' }}>
            Varsayılan PIN: 0000 — Ayarlar'dan değiştir!
          </div>
        )}
      </div>
    </div>
  );
}
