import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MapPin, Navigation, Search } from 'lucide-react';

interface Props { onBack: () => void; onToast: (msg: string) => void; }

const LOCATIONS = [
  { name: 'Pallet Town', x: 25, y: 70, color: '#0A84FF', desc: 'Başlangıç noktası' },
  { name: 'Viridian City', x: 25, y: 45, color: '#30D158', desc: 'Gym 1 ve Pokemon Merkezi' },
  { name: 'Pewter City', x: 25, y: 22, color: '#8E8E93', desc: 'Gym 2 ve Müze' },
  { name: 'Cerulean City', x: 58, y: 15, color: '#40C8E0', desc: 'Gym 3 ve Bisiklet Mağazası' },
  { name: 'Lavender Town', x: 72, y: 35, color: '#BF5AF2', desc: 'Pokemon Kulesi' },
  { name: 'Vermilion City', x: 58, y: 58, color: '#FFD60A', desc: 'Gym 4 ve Liman' },
  { name: 'Fuchsia City', x: 42, y: 75, color: '#FF375F', desc: 'Safari Zone ve Gym 6' },
];

export function HaritaApp({ onBack, onToast }: Props) {
  const [selected, setSelected] = useState<typeof LOCATIONS[0] | null>(null);
  const [search, setSearch] = useState('');

  const filtered = LOCATIONS.filter(l => l.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <AppWindow title="Harita" accentColor="#30D158" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Search */}
        <div className="px-4 py-2">
          <div className="flex items-center gap-2 px-3 py-2.5 rounded-xl" style={{ background: 'rgba(44,44,46,0.9)' }}>
            <Search size={14} color="rgba(235,235,245,0.4)" />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Konum ara..."
              style={{ flex: 1, background: 'none', border: 'none', outline: 'none', color: '#fff', fontSize: 14, fontFamily: 'inherit', padding: 0 }}
            />
          </div>
        </div>

        {/* Map area */}
        <div
          className="mx-4 rounded-2xl relative overflow-hidden flex-shrink-0"
          style={{ height: 220, background: 'linear-gradient(135deg,#0a2a0a,#0a1a2a,#1a0a2a)', border: '0.5px solid rgba(255,255,255,0.1)' }}
        >
          {/* Grid */}
          <div className="absolute inset-0 opacity-10">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="absolute" style={{ left: `${(i + 1) * 16.67}%`, top: 0, bottom: 0, width: '0.5px', background: '#fff' }} />
            ))}
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="absolute" style={{ top: `${(i + 1) * 20}%`, left: 0, right: 0, height: '0.5px', background: '#fff' }} />
            ))}
          </div>

          {/* Paths */}
          <svg className="absolute inset-0 w-full h-full" style={{ opacity: 0.3 }}>
            <line x1="25%" y1="70%" x2="25%" y2="22%" stroke="#30D158" strokeWidth="2" strokeDasharray="4,4" />
            <line x1="25%" y1="22%" x2="58%" y2="15%" stroke="#30D158" strokeWidth="2" strokeDasharray="4,4" />
            <line x1="58%" y1="15%" x2="72%" y2="35%" stroke="#30D158" strokeWidth="2" strokeDasharray="4,4" />
            <line x1="58%" y1="15%" x2="58%" y2="58%" stroke="#30D158" strokeWidth="2" strokeDasharray="4,4" />
            <line x1="58%" y1="58%" x2="42%" y2="75%" stroke="#30D158" strokeWidth="2" strokeDasharray="4,4" />
          </svg>

          {/* Locations */}
          {LOCATIONS.map((loc, i) => (
            <button
              key={i}
              className="absolute flex flex-col items-center focus:outline-none"
              style={{ left: `${loc.x}%`, top: `${loc.y}%`, transform: 'translate(-50%,-50%)' }}
              onClick={() => setSelected(loc)}
            >
              <div
                style={{
                  width: 10, height: 10, borderRadius: '50%', background: loc.color,
                  boxShadow: `0 0 8px ${loc.color}`, border: '2px solid rgba(255,255,255,0.6)',
                  transform: selected?.name === loc.name ? 'scale(1.6)' : 'scale(1)',
                  transition: 'transform 0.2s',
                }}
              />
            </button>
          ))}

          {/* User location */}
          <div className="absolute" style={{ left: '38%', top: '55%', transform: 'translate(-50%,-50%)' }}>
            <div style={{ width: 14, height: 14, borderRadius: '50%', background: '#0A84FF', border: '3px solid #fff', boxShadow: '0 0 12px #0A84FF' }} />
          </div>

          <div className="absolute bottom-2 right-2 px-2 py-1 rounded-lg" style={{ background: 'rgba(0,0,0,0.5)', fontSize: 10, color: 'rgba(255,255,255,0.5)' }}>
            Kanto Bölgesi
          </div>
        </div>

        {/* Selected popup */}
        {selected && (
          <div className="mx-4 mt-2 p-3 rounded-xl flex items-center gap-3" style={{ background: `${selected.color}15`, border: `0.5px solid ${selected.color}44` }}>
            <MapPin size={20} color={selected.color} />
            <div className="flex-1">
              <div className="text-white font-semibold">{selected.name}</div>
              <div className="text-xs" style={{ color: 'rgba(235,235,245,0.55)' }}>{selected.desc}</div>
            </div>
            <button
              className="pill-btn green text-xs py-1.5 px-3"
              onClick={() => onToast(`${selected.name} için yol tarifi başlatıldı`)}
            >
              Git
            </button>
          </div>
        )}

        {/* Location list */}
        <div className="flex-1 scroll-area px-4 py-3 flex flex-col gap-2">
          <div className="text-xs font-semibold px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
            Konumlar
          </div>
          {filtered.map((loc, i) => (
            <button
              key={i}
              className="flex items-center gap-3 p-3 rounded-xl text-left"
              style={{ background: selected?.name === loc.name ? `${loc.color}15` : 'rgba(28,28,30,0.9)', border: `0.5px solid ${selected?.name === loc.name ? `${loc.color}44` : 'rgba(255,255,255,0.06)'}` }}
              onClick={() => setSelected(loc)}
            >
              <div className="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0" style={{ background: `${loc.color}22` }}>
                <MapPin size={16} color={loc.color} />
              </div>
              <div>
                <div className="text-white font-medium text-sm">{loc.name}</div>
                <div className="text-xs" style={{ color: 'rgba(235,235,245,0.45)' }}>{loc.desc}</div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </AppWindow>
  );
}
