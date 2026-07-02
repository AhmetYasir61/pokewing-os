import React from 'react';
import { Wifi, WifiOff, Bluetooth, BellOff, Battery } from 'lucide-react';
import { OSState } from '../types';

interface Props {
  state: OSState;
  onNotifTap: () => void;
  onQSTap: () => void;
  time: string;
}

export function StatusBar({ state, onNotifTap, onQSTap, time }: Props) {
  const unread = state.notifications.filter(n => !n.read).length;

  return (
    <div
      className="absolute top-0 left-0 right-0 z-50 flex items-center px-4"
      style={{ height: 44, background: 'linear-gradient(to bottom, rgba(0,0,0,0.45), transparent)' }}
    >
      {/* Left: time */}
      <button
        className="text-white font-semibold text-[15px] mr-auto focus:outline-none"
        style={{ letterSpacing: 0.5, fontVariantNumeric: 'tabular-nums' }}
        onClick={onNotifTap}
      >
        {time}
      </button>

      {/* Right: icons */}
      <div className="flex items-center gap-2" onClick={onQSTap}>
        {state.dnd && <BellOff size={13} color="rgba(255,255,255,0.8)" />}
        {state.bluetooth && <Bluetooth size={13} color="rgba(255,255,255,0.8)" />}
        {state.wifi
          ? <Wifi size={14} color="rgba(255,255,255,0.9)" />
          : <WifiOff size={14} color="rgba(255,255,255,0.5)" />
        }
        {/* Signal bars */}
        <div className="flex items-end gap-px">
          {[4, 6, 8, 10].map((h, i) => (
            <div
              key={i}
              style={{ width: 3, height: h, borderRadius: 1.5, background: i < 3 ? 'rgba(255,255,255,0.9)' : 'rgba(255,255,255,0.3)' }}
            />
          ))}
        </div>
        {/* Battery — gerçek pil seviyesi (item'a bağlı; azalır/dolar) */}
        <div className="flex items-center gap-1">
          <span style={{ fontSize: 10.5, color: state.battery <= 15 ? '#FF453A' : 'rgba(255,255,255,0.85)', fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
            %{Math.round(state.battery)}
          </span>
          <div style={{ width: 22, height: 12, border: '1.5px solid rgba(255,255,255,0.7)', borderRadius: 3, position: 'relative', overflow: 'hidden' }}>
            <div style={{
              position: 'absolute', top: 1, left: 1, bottom: 1,
              width: `${Math.max(3, state.battery * 0.9)}%`,
              background: state.battery <= 15 ? '#FF453A' : state.battery <= 40 ? '#FFD60A' : '#30D158',
              borderRadius: 1.5, transition: 'width 0.5s, background 0.5s',
            }} />
          </div>
          <div style={{ width: 2, height: 5, background: 'rgba(255,255,255,0.5)', borderRadius: '0 1px 1px 0' }} />
        </div>
        {/* Notif count */}
        {unread > 0 && (
          <div
            className="flex items-center justify-center rounded-full text-white font-bold"
            style={{ minWidth: 16, height: 16, background: '#FF453A', fontSize: 10, padding: '0 3px' }}
          >
            {unread}
          </div>
        )}
      </div>
    </div>
  );
}
