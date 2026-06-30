import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { User, Bell, Shield, Smartphone, Info, ChevronRight, Volume2, Wifi, Bluetooth, Lock } from 'lucide-react';
import { OSState } from '../types';

interface Props {
  state: OSState;
  onBack: () => void;
  onToggleWifi: () => void;
  onToggleBt: () => void;
  onToggleDnd: () => void;
  onBrightness: (v: number) => void;
  onVolume: (v: number) => void;
  onSetUsername: (name: string) => void;
  onToast: (msg: string) => void;
  onLock: () => void;
}

export function AyarlarApp({ state, onBack, onToggleWifi, onToggleBt, onToggleDnd, onBrightness, onVolume, onSetUsername, onToast, onLock }: Props) {
  const [section, setSection] = useState<string | null>(null);
  const [editName, setEditName] = useState(state.userName);

  if (section === 'profil') {
    return (
      <AppWindow title="Profil" accentColor="#8E8E93" onBack={() => setSection(null)}>
        <div className="scroll-area h-full px-4 py-4 flex flex-col gap-4">
          <div className="flex flex-col items-center gap-3 py-4">
            <div className="w-20 h-20 rounded-full flex items-center justify-center font-bold text-white text-3xl" style={{ background: '#0A84FF' }}>
              {state.userName[0]?.toUpperCase()}
            </div>
            <div className="text-white font-bold text-xl">{state.userName}</div>
            <div className="text-sm" style={{ color: '#FFD60A' }}>⬡ {state.coins.toLocaleString('tr-TR')} Coin</div>
          </div>
          <div>
            <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Kullanıcı Adı</div>
            <input
              type="text"
              value={editName}
              onChange={e => setEditName(e.target.value)}
              maxLength={20}
              style={{ width: '100%' }}
            />
            <button
              className="pill-btn primary mt-3 w-full"
              onClick={() => { if (editName.trim()) { onSetUsername(editName.trim()); onToast('Kullanıcı adı güncellendi!'); }}}
            >
              Kaydet
            </button>
          </div>
        </div>
      </AppWindow>
    );
  }

  if (section === 'bildirim') {
    return (
      <AppWindow title="Bildirimler" accentColor="#8E8E93" onBack={() => setSection(null)}>
        <div className="scroll-area h-full px-4 py-3 flex flex-col gap-3">
          {[
            { label: 'Rahatsız Etme', val: state.dnd, toggle: onToggleDnd },
          ].map((s, i) => (
            <div key={i} className="flex items-center justify-between p-4 rounded-2xl" style={{ background: 'rgba(28,28,30,0.9)' }}>
              <span className="text-white font-medium">{s.label}</span>
              <div className={`toggle ${s.val ? 'on' : ''}`} onClick={s.toggle}>
                <div className="toggle-knob" />
              </div>
            </div>
          ))}
        </div>
      </AppWindow>
    );
  }

  const SECTIONS = [
    { id: 'profil', icon: User, color: '#0A84FF', label: 'Profil', sub: state.userName },
    { id: 'bildirim', icon: Bell, color: '#FF9F0A', label: 'Bildirimler', sub: state.dnd ? 'Rahatsız Etme Açık' : 'Normal' },
    { id: 'guvenlik', icon: Shield, color: '#30D158', label: 'Gizlilik & Güvenlik', sub: 'Hesap ayarları' },
    { id: 'cihaz', icon: Smartphone, color: '#BF5AF2', label: 'Cihaz', sub: 'WebOS 17.0' },
    { id: 'hakkinda', icon: Info, color: '#8E8E93', label: 'Hakkında', sub: 'Sürüm 1.0.0' },
  ];

  return (
    <AppWindow title="Ayarlar" accentColor="#8E8E93" onBack={onBack}>
      <div className="scroll-area h-full px-4 py-3 flex flex-col gap-4">
        {/* Quick toggles */}
        <div className="flex flex-col gap-2">
          <div className="text-xs font-semibold px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Hızlı Ayarlar</div>

          {[
            { icon: Wifi, label: 'Wi-Fi', val: state.wifi, toggle: onToggleWifi, color: '#0A84FF' },
            { icon: Bluetooth, label: 'Bluetooth', val: state.bluetooth, toggle: onToggleBt, color: '#0A84FF' },
            { icon: Bell, label: 'Rahatsız Etme', val: state.dnd, toggle: onToggleDnd, color: '#FF9F0A' },
          ].map((item, i) => {
            const Icon = item.icon;
            return (
              <div key={i} className="flex items-center gap-3 p-4 rounded-2xl" style={{ background: 'rgba(28,28,30,0.9)' }}>
                <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0" style={{ background: `${item.color}22` }}>
                  <Icon size={20} color={item.color} />
                </div>
                <span className="text-white font-medium flex-1">{item.label}</span>
                <div className={`toggle ${item.val ? 'on' : ''}`} onClick={item.toggle}>
                  <div className="toggle-knob" />
                </div>
              </div>
            );
          })}

          {/* Brightness */}
          <div className="p-4 rounded-2xl flex flex-col gap-2" style={{ background: 'rgba(28,28,30,0.9)' }}>
            <div className="flex items-center justify-between">
              <span className="text-white font-medium">Parlaklık</span>
              <span className="text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>{state.brightness}%</span>
            </div>
            <input type="range" min={10} max={100} value={state.brightness} onChange={e => onBrightness(+e.target.value)} style={{ width: '100%', accentColor: '#0A84FF', cursor: 'pointer' }} />
          </div>

          {/* Volume */}
          <div className="p-4 rounded-2xl flex flex-col gap-2" style={{ background: 'rgba(28,28,30,0.9)' }}>
            <div className="flex items-center justify-between">
              <span className="text-white font-medium">Ses</span>
              <span className="text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>{state.volume}%</span>
            </div>
            <input type="range" min={0} max={100} value={state.volume} onChange={e => onVolume(+e.target.value)} style={{ width: '100%', accentColor: '#0A84FF', cursor: 'pointer' }} />
          </div>
        </div>

        {/* Sections */}
        <div className="list-group">
          {SECTIONS.map((s, i) => {
            const Icon = s.icon;
            return (
              <div key={i} className="list-cell" onClick={() => setSection(s.id)}>
                <div className="cell-icon" style={{ background: `${s.color}22` }}>
                  <Icon size={22} color={s.color} />
                </div>
                <div className="cell-content">
                  <div className="cell-title">{s.label}</div>
                  <div className="cell-sub">{s.sub}</div>
                </div>
                <ChevronRight size={16} color="rgba(235,235,245,0.3)" />
              </div>
            );
          })}
        </div>

        {/* Lock */}
        <button
          className="rounded-2xl p-4 flex items-center gap-3"
          style={{ background: 'rgba(255,69,58,0.1)', border: '0.5px solid rgba(255,69,58,0.2)' }}
          onClick={onLock}
        >
          <Lock size={20} color="#FF453A" />
          <span className="font-medium" style={{ color: '#FF453A' }}>Kilitle</span>
        </button>
      </div>
    </AppWindow>
  );
}
