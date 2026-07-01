import React from 'react';
import { AppWindow } from '../components/AppWindow';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface Props { onBack: () => void; date: Date; onSetDate: (d: Date) => void; }

const MONTHS = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];
const DAYS = ['Pt','Sa','Ça','Pe','Cu','Ct','Pz'];

const EVENTS: Record<string, { title: string; color: string }[]> = {
  '2026-06-30': [{ title: 'PvP Turnuvası', color: '#FF453A' }],
  '2026-07-01': [{ title: 'Yeni Sezon', color: '#30D158' }],
  '2026-07-04': [{ title: 'Bakım', color: '#FF9F0A' }],
  '2026-07-10': [{ title: 'Etkinlik', color: '#BF5AF2' }],
  '2026-07-15': [{ title: 'İndirim Günü', color: '#FFD60A' }],
};

export function TakvimApp({ onBack, date, onSetDate }: Props) {
  const y = date.getFullYear();
  const m = date.getMonth();
  const today = new Date();

  const firstDay = new Date(y, m, 1).getDay();
  const daysInMonth = new Date(y, m + 1, 0).getDate();
  const startOffset = (firstDay + 6) % 7;

  const cells: (number | null)[] = [
    ...Array(startOffset).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  const prevMonth = () => onSetDate(new Date(y, m - 1, 1));
  const nextMonth = () => onSetDate(new Date(y, m + 1, 1));

  const today_str = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;

  const upcomingEvents = Object.entries(EVENTS)
    .filter(([d]) => d >= today_str)
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(0, 5);

  return (
    <AppWindow title="Takvim" accentColor="#FF9F0A" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Month header */}
        <div className="flex items-center justify-between px-5 py-3">
          <button onClick={prevMonth} className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.08)' }}>
            <ChevronLeft size={20} color="#fff" />
          </button>
          <span className="text-white font-bold text-lg">{MONTHS[m]} {y}</span>
          <button onClick={nextMonth} className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.08)' }}>
            <ChevronRight size={20} color="#fff" />
          </button>
        </div>

        {/* Day labels */}
        <div className="grid grid-cols-7 px-3 mb-1">
          {DAYS.map(d => (
            <div key={d} className="text-center text-[11px] font-semibold py-1" style={{ color: 'rgba(235,235,245,0.4)' }}>{d}</div>
          ))}
        </div>

        {/* Calendar grid */}
        <div className="grid grid-cols-7 px-3 gap-y-1">
          {cells.map((day, i) => {
            if (!day) return <div key={i} />;
            const dateStr = `${y}-${String(m + 1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
            const isToday = dateStr === today_str;
            const events = EVENTS[dateStr] ?? [];
            return (
              <button
                key={i}
                className="flex flex-col items-center py-1 rounded-xl focus:outline-none"
                style={{ background: isToday ? '#FF9F0A' : 'transparent' }}
                onClick={() => onSetDate(new Date(y, m, day))}
              >
                <span className="text-sm font-semibold" style={{ color: isToday ? '#fff' : 'rgba(235,235,245,0.9)' }}>{day}</span>
                <div className="flex gap-0.5 mt-0.5">
                  {events.slice(0, 2).map((e, j) => (
                    <div key={j} style={{ width: 4, height: 4, borderRadius: '50%', background: isToday ? '#fff' : e.color }} />
                  ))}
                </div>
              </button>
            );
          })}
        </div>

        <div className="h-px mx-4 mt-3" style={{ background: 'rgba(255,255,255,0.08)' }} />

        {/* Upcoming events */}
        <div className="flex-1 scroll-area px-4 py-3 flex flex-col gap-2">
          <div className="text-xs font-semibold px-1 mb-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
            Yaklaşan Etkinlikler
          </div>
          {upcomingEvents.length === 0 ? (
            <div className="text-center py-6 text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>Etkinlik yok</div>
          ) : (
            upcomingEvents.map(([d, evs]) => (
              evs.map((ev, i) => (
                <div key={`${d}-${i}`} className="flex items-center gap-3 p-3 rounded-xl" style={{ background: 'rgba(28,28,30,0.9)' }}>
                  <div style={{ width: 4, height: 36, borderRadius: 2, background: ev.color, flexShrink: 0 }} />
                  <div>
                    <div className="text-white font-semibold text-sm">{ev.title}</div>
                    <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{d.split('-').reverse().join(' ').replace(/-/g,'/')}</div>
                  </div>
                </div>
              ))
            ))
          )}
        </div>
      </div>
    </AppWindow>
  );
}
