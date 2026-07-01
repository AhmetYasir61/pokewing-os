import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { BookOpen, ChevronRight, Sword, Shield, Zap, Star, Trophy, Map } from 'lucide-react';

interface Props { onBack: () => void; }

const CATEGORIES = [
  { id: 'rutbeler', name: 'Rütbeler', icon: Trophy, count: 8, color: '#FFD60A', desc: 'Sunucu rütbe sistemi ve ayrıcalıklar' },
  { id: 'silahlar', name: 'Silahlar', icon: Sword, count: 24, color: '#FF453A', desc: 'Tüm silah ve ekipman listesi' },
  { id: 'zirhlar', name: 'Zırhlar', icon: Shield, count: 16, color: '#0A84FF', desc: 'Zırh türleri ve özellikleri' },
  { id: 'yetenekler', name: 'Yetenekler', icon: Zap, count: 12, color: '#BF5AF2', desc: 'Özel yetenek ve büyüler' },
  { id: 'petler', name: 'Evcil Hayvanlar', icon: Star, count: 30, color: '#30D158', desc: 'Tüm evcil hayvan kataloğu' },
  { id: 'haritalar', name: 'Haritalar', icon: Map, count: 7, color: '#40C8E0', desc: 'Sunucu harita listesi ve rehberi' },
];

const RANKS = [
  { name: 'Üye', color: '#8E8E93', perks: ['Temel komutlar', '/home komutu', 'Standart kit'] },
  { name: 'VIP', color: '#30D158', perks: ['Renkli isim', '/fly komutu', 'VIP kit', '2x XP', 'Özel şapka'] },
  { name: 'MVP', color: '#0A84FF', perks: ['Tüm VIP', '/speed komutu', '3x XP', 'MVP kit', 'Özel efekt'] },
  { name: 'Elite', color: '#BF5AF2', perks: ['Tüm MVP', '/god komutu', '5x XP', 'Elite kit', 'Özel parçacık'] },
  { name: 'Legend', color: '#FFD60A', perks: ['Tüm Elite', 'Tüm komutlar', '10x XP', 'Legend kit', 'Özel prefix'] },
];

export function KatalogApp({ onBack }: Props) {
  const [selectedCat, setSelectedCat] = useState<string | null>(null);

  return (
    <AppWindow title="Katalog" accentColor="#5E5CE6" onBack={onBack}
      headerRight={selectedCat ? (
        <button onClick={() => setSelectedCat(null)} style={{ color: '#5E5CE6', fontWeight: 600, fontSize: 14 }}>Geri</button>
      ) : undefined}
    >
      {!selectedCat ? (
        <div className="scroll-area h-full px-4 py-3 flex flex-col gap-3">
          <div className="text-xs font-semibold mb-1 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
            Kategoriler
          </div>
          {CATEGORIES.map(cat => {
            const Icon = cat.icon;
            return (
              <button
                key={cat.id}
                className="rounded-2xl p-4 flex items-center gap-4 text-left"
                style={{ background: 'rgba(28,28,30,0.9)', border: '0.5px solid rgba(255,255,255,0.06)' }}
                onClick={() => setSelectedCat(cat.id)}
              >
                <div className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style={{ background: `${cat.color}22` }}>
                  <Icon size={24} color={cat.color} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-white font-semibold text-[15px]">{cat.name}</div>
                  <div className="text-[12px] mt-0.5" style={{ color: 'rgba(235,235,245,0.5)' }}>{cat.desc}</div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold" style={{ color: cat.color }}>{cat.count}</span>
                  <ChevronRight size={16} color="rgba(235,235,245,0.3)" />
                </div>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="scroll-area h-full px-4 py-3 flex flex-col gap-3">
          {selectedCat === 'rutbeler' ? (
            <>
              <div className="text-xs font-semibold mb-1 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
                Rütbe Sistemi
              </div>
              {RANKS.map((rank, i) => (
                <div key={i} className="rounded-2xl p-4" style={{ background: 'rgba(28,28,30,0.9)', border: `0.5px solid ${rank.color}33` }}>
                  <div className="flex items-center gap-3 mb-3">
                    <div className="w-10 h-10 rounded-full flex items-center justify-center" style={{ background: `${rank.color}22`, border: `1.5px solid ${rank.color}` }}>
                      <Trophy size={18} color={rank.color} />
                    </div>
                    <span className="font-bold text-lg" style={{ color: rank.color }}>{rank.name}</span>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    {rank.perks.map((perk, j) => (
                      <div key={j} className="flex items-center gap-2">
                        <div className="w-1.5 h-1.5 rounded-full" style={{ background: rank.color }} />
                        <span className="text-sm" style={{ color: 'rgba(235,235,245,0.8)' }}>{perk}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </>
          ) : (
            <div className="flex flex-col items-center py-16 gap-3">
              <BookOpen size={40} color="rgba(235,235,245,0.2)" />
              <div className="text-white font-semibold text-lg">{CATEGORIES.find(c => c.id === selectedCat)?.name}</div>
              <div className="text-center text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>
                Bu kategori yakında güncellenecek.
              </div>
            </div>
          )}
        </div>
      )}
    </AppWindow>
  );
}
