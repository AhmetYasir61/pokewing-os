import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { GALLERY_PHOTOS } from '../data';
import { X, Share2, Trash2, Heart, ChevronLeft, ChevronRight } from 'lucide-react';

interface Props { onBack: () => void; onToast: (msg: string) => void; }

export function GaleriApp({ onBack, onToast }: Props) {
  const [fullscreen, setFullscreen] = useState<number | null>(null);
  const [liked, setLiked] = useState<Set<number>>(new Set());

  const goNext = () => fullscreen !== null && setFullscreen((fullscreen + 1) % GALLERY_PHOTOS.length);
  const goPrev = () => fullscreen !== null && setFullscreen((fullscreen - 1 + GALLERY_PHOTOS.length) % GALLERY_PHOTOS.length);

  return (
    <AppWindow title="Galeri" accentColor="#FF375F" onBack={onBack}>
      <div className="scroll-area h-full">
        {/* Albums */}
        <div className="px-4 py-3">
          <div className="text-xs font-semibold mb-3 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>
            Son Fotoğraflar · {GALLERY_PHOTOS.length}
          </div>
          <div className="gallery-grid">
            {GALLERY_PHOTOS.map((url, i) => (
              <div
                key={i}
                className="relative cursor-pointer overflow-hidden"
                style={{ aspectRatio: '1', background: '#1c1c1e' }}
                onClick={() => setFullscreen(i)}
              >
                <img
                  src={url}
                  alt=""
                  className="w-full h-full object-cover transition-transform duration-300 hover:scale-105"
                  loading="lazy"
                />
                {liked.has(i) && (
                  <div className="absolute top-1.5 right-1.5">
                    <Heart size={14} fill="#FF375F" color="#FF375F" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Fullscreen viewer */}
      {fullscreen !== null && (
        <div
          className="absolute inset-0 z-20 flex flex-col anim-fadein"
          style={{ background: '#000' }}
        >
          <div className="flex items-center justify-between px-4 py-3 flex-shrink-0" style={{ paddingTop: 10 }}>
            <button onClick={() => setFullscreen(null)} className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.1)' }}>
              <X size={18} color="#fff" />
            </button>
            <span className="text-white font-medium text-sm">{fullscreen + 1} / {GALLERY_PHOTOS.length}</span>
            <div className="flex gap-2">
              <button
                className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none"
                style={{ background: liked.has(fullscreen) ? 'rgba(255,55,95,0.2)' : 'rgba(255,255,255,0.1)' }}
                onClick={() => {
                  setLiked(prev => { const n = new Set(prev); n.has(fullscreen) ? n.delete(fullscreen) : n.add(fullscreen); return n; });
                }}
              >
                <Heart size={18} fill={liked.has(fullscreen) ? '#FF375F' : 'none'} color={liked.has(fullscreen) ? '#FF375F' : '#fff'} />
              </button>
              <button className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.1)' }} onClick={() => onToast('Paylaşım menüsü açıldı')}>
                <Share2 size={16} color="#fff" />
              </button>
              <button className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,69,58,0.15)' }} onClick={() => onToast('Fotoğraf silindi (demo)')}>
                <Trash2 size={16} color="#FF453A" />
              </button>
            </div>
          </div>

          {/* Image */}
          <div className="flex-1 relative flex items-center">
            <img
              src={GALLERY_PHOTOS[fullscreen]}
              alt=""
              className="w-full object-contain"
              style={{ maxHeight: '75vh' }}
            />
            <button
              className="absolute left-3 w-10 h-10 flex items-center justify-center rounded-full focus:outline-none"
              style={{ background: 'rgba(0,0,0,0.5)' }}
              onClick={goPrev}
            >
              <ChevronLeft size={22} color="#fff" />
            </button>
            <button
              className="absolute right-3 w-10 h-10 flex items-center justify-center rounded-full focus:outline-none"
              style={{ background: 'rgba(0,0,0,0.5)' }}
              onClick={goNext}
            >
              <ChevronRight size={22} color="#fff" />
            </button>
          </div>

          {/* Thumbnails */}
          <div className="flex gap-1 px-3 pb-6 pt-2 overflow-x-auto flex-shrink-0">
            {GALLERY_PHOTOS.map((url, i) => (
              <div
                key={i}
                className="flex-shrink-0 rounded-lg overflow-hidden cursor-pointer"
                style={{ width: 48, height: 48, border: i === fullscreen ? '2px solid #fff' : '2px solid transparent' }}
                onClick={() => setFullscreen(i)}
              >
                <img src={url} alt="" className="w-full h-full object-cover" />
              </div>
            ))}
          </div>
        </div>
      )}
    </AppWindow>
  );
}
