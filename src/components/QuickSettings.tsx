import React from 'react';
import { Wifi, WifiOff, Bluetooth, Moon, BellOff, Sun, Volume2, Battery } from 'lucide-react';
import { OSState } from '../types';

interface Props {
  state: OSState;
  onClose: () => void;
  onToggleWifi: () => void;
  onToggleBt: () => void;
  onToggleDnd: () => void;
  onBrightness: (v: number) => void;
  onVolume: (v: number) => void;
}

export function QuickSettings({ state, onClose, onToggleWifi, onToggleBt, onToggleDnd, onBrightness, onVolume }: Props) {
  return (
    <div
      className="absolute inset-0 z-40"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
      style={{ background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)' }}
    >
      <div
        className="absolute right-3 top-12 anim-winin rounded-3xl overflow-hidden"
        style={{ width: 320, background: 'rgba(24,24,28,0.95)', backdropFilter: 'blur(40px)', border: '0.5px solid rgba(255,255,255,0.1)' }}
      >
        {/* Top row: tiles */}
        <div className="grid grid-cols-2 gap-2 p-4">
          <button
            className={`qs-tile ${state.wifi ? 'on' : 'off'}`}
            onClick={onToggleWifi}
          >
            {state.wifi ? <Wifi size={20} color="#0A84FF" /> : <WifiOff size={20} color="rgba(235,235,245,0.5)" />}
            <div>
              <div className="text-sm font-semibold text-white">{state.wifi ? 'Wi-Fi' : 'Wi-Fi Kapalı'}</div>
              <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{state.wifi ? 'GameNet_5G' : 'Bağlantı yok'}</div>
            </div>
          </button>

          <button
            className={`qs-tile ${state.bluetooth ? 'on' : 'off'}`}
            onClick={onToggleBt}
          >
            <Bluetooth size={20} color={state.bluetooth ? '#0A84FF' : 'rgba(235,235,245,0.5)'} />
            <div>
              <div className="text-sm font-semibold text-white">Bluetooth</div>
              <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{state.bluetooth ? 'Açık' : 'Kapalı'}</div>
            </div>
          </button>

          <button
            className={`qs-tile ${state.dnd ? 'on' : 'off'}`}
            onClick={onToggleDnd}
          >
            <BellOff size={20} color={state.dnd ? '#0A84FF' : 'rgba(235,235,245,0.5)'} />
            <div>
              <div className="text-sm font-semibold text-white">Rahatsız Etme</div>
              <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{state.dnd ? 'Açık' : 'Kapalı'}</div>
            </div>
          </button>

          <div className="qs-tile off">
            <Battery size={20} color="#30D158" />
            <div>
              <div className="text-sm font-semibold text-white">Pil</div>
              <div className="text-xs" style={{ color: '#30D158' }}>%72 · Şarj Oluyor</div>
            </div>
          </div>
        </div>

        {/* Brightness */}
        <div className="px-4 pb-3">
          <div className="flex items-center gap-3">
            <Moon size={15} color="rgba(235,235,245,0.5)" />
            <input
              type="range" min={10} max={100} value={state.brightness}
              onChange={e => onBrightness(+e.target.value)}
              className="flex-1 accent-white"
              style={{ height: 4, accentColor: '#0A84FF', cursor: 'pointer' }}
            />
            <Sun size={18} color="rgba(235,235,245,0.8)" />
            <span style={{ color: 'rgba(235,235,245,0.5)', fontSize: 13, minWidth: 26 }}>{state.brightness}%</span>
          </div>
        </div>

        {/* Volume */}
        <div className="px-4 pb-4">
          <div className="flex items-center gap-3">
            <Volume2 size={15} color="rgba(235,235,245,0.5)" />
            <input
              type="range" min={0} max={100} value={state.volume}
              onChange={e => onVolume(+e.target.value)}
              className="flex-1"
              style={{ height: 4, accentColor: '#0A84FF', cursor: 'pointer' }}
            />
            <Volume2 size={18} color="rgba(235,235,245,0.8)" />
            <span style={{ color: 'rgba(235,235,245,0.5)', fontSize: 13, minWidth: 26 }}>{state.volume}%</span>
          </div>
        </div>

        <div className="h-px mx-4" style={{ background: 'rgba(255,255,255,0.08)' }} />

        {/* User row */}
        <div className="flex items-center gap-3 px-4 py-3">
          <div className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-white text-sm" style={{ background: '#0A84FF' }}>
            {state.userName[0]?.toUpperCase()}
          </div>
          <div className="flex-1">
            <div className="text-sm font-semibold text-white">{state.userName}</div>
            <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>
              {state.coins.toLocaleString('tr-TR')} Coin
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
