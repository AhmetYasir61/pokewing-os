import React, { useEffect, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_NEWS } from '../data';
import { NewsItem } from '../types';
import { hasBridge, getNews } from '../bridge';
import { Calendar, Tag, X } from 'lucide-react';

interface Props { onBack: () => void; }

const TAG_COLORS: Record<string, string> = {
  'Güncelleme': '#0A84FF',
  'Kampanya': '#FF9F0A',
  'Etkinlik': '#BF5AF2',
  'Bilgi': '#30D158',
  'Yeni': '#FF375F',
};

export function NewsApp({ onBack }: Props) {
  const [reading, setReading] = useState<NewsItem | null>(null);
  const [news, setNews] = useState<NewsItem[]>(MOCK_NEWS);

  // Oyun içi köprü varsa gerçek duyuruları çek; yoksa mock kalır.
  useEffect(() => {
    if (!hasBridge()) return;
    let alive = true;
    getNews().then(n => { if (alive && n && n.length) setNews(n); });
    return () => { alive = false; };
  }, []);

  return (
    <AppWindow
      title="Duyurular"
      accentColor="#FF453A"
      onBack={onBack}
      headerRight={reading ? (
        <button onClick={() => setReading(null)} className="w-8 h-8 rounded-full flex items-center justify-center" style={{ background: 'rgba(255,69,58,0.15)' }}>
          <X size={15} color="#FF453A" />
        </button>
      ) : undefined}
    >
      {!reading ? (
        <div className="scroll-area h-full px-4 py-3 flex flex-col gap-3">
          {news.map(news => {
            const tagColor = TAG_COLORS[news.tag] ?? '#8E8E93';
            return (
              <button
                key={news.id}
                className="rounded-2xl p-4 text-left w-full"
                style={{ background: 'rgba(28,28,30,0.9)', border: '0.5px solid rgba(255,255,255,0.06)' }}
                onClick={() => setReading(news)}
              >
                <div className="flex items-start gap-2 mb-2">
                  <span
                    className="text-[10px] font-bold px-2 py-0.5 rounded-full flex-shrink-0"
                    style={{ background: `${tagColor}22`, color: tagColor }}
                  >
                    {news.tag}
                  </span>
                  <div className="flex items-center gap-1 ml-auto flex-shrink-0" style={{ color: 'rgba(235,235,245,0.45)', fontSize: 11 }}>
                    <Calendar size={11} />
                    <span>{news.date}</span>
                  </div>
                </div>
                <div className="text-white font-bold text-base mb-1.5">{news.title}</div>
                <div className="text-[13px] leading-relaxed" style={{ color: 'rgba(235,235,245,0.6)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {news.body}
                </div>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="scroll-area h-full px-5 py-4">
          <div className="flex items-center gap-2 mb-3">
            <span
              className="text-[11px] font-bold px-2.5 py-1 rounded-full"
              style={{ background: `${TAG_COLORS[reading.tag] ?? '#8E8E93'}22`, color: TAG_COLORS[reading.tag] ?? '#8E8E93' }}
            >
              {reading.tag}
            </span>
            <div className="flex items-center gap-1 ml-auto" style={{ color: 'rgba(235,235,245,0.45)', fontSize: 12 }}>
              <Calendar size={13} />
              <span>{reading.date}</span>
            </div>
          </div>
          <div className="text-white font-bold text-2xl leading-tight mb-4">{reading.title}</div>
          <div className="text-base leading-relaxed" style={{ color: 'rgba(235,235,245,0.8)', lineHeight: 1.65 }}>{reading.body}</div>
          <div className="mt-6 h-px" style={{ background: 'rgba(255,255,255,0.08)' }} />
          <div className="flex items-center gap-2 mt-4">
            <Tag size={14} color="rgba(235,235,245,0.4)" />
            <span className="text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>Resmi Sunucu Duyurusu</span>
          </div>
        </div>
      )}
    </AppWindow>
  );
}
