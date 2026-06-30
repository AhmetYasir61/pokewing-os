import React from 'react';
import { AppWindow } from '../components/AppWindow';
import { MUSIC_TRACKS } from '../data';
import { Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Volume2 } from 'lucide-react';

interface Props {
  onBack: () => void;
  playing: boolean;
  trackIdx: number;
  volume: number;
  onToggle: () => void;
  onNext: () => void;
  onPrev: () => void;
  onSetTrack: (i: number) => void;
}

export function MuzikApp({ onBack, playing, trackIdx, volume, onToggle, onNext, onPrev, onSetTrack }: Props) {
  const track = MUSIC_TRACKS[trackIdx];
  const [progress, setProgress] = React.useState(32);
  const [shuffle, setShuffle] = React.useState(false);
  const [repeat, setRepeat] = React.useState(false);

  React.useEffect(() => {
    if (!playing) return;
    const t = setInterval(() => setProgress(p => p >= 100 ? 0 : p + 0.5), 300);
    return () => clearInterval(t);
  }, [playing]);

  return (
    <AppWindow title="Müzik" accentColor="#FF375F" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Album art */}
        <div className="flex justify-center px-8 pt-4 pb-2">
          <div
            className="w-48 h-48 rounded-3xl flex items-center justify-center"
            style={{
              background: `linear-gradient(135deg,${track.color},${track.color}88)`,
              boxShadow: `0 24px 60px ${track.color}55`,
              animation: playing ? 'spin 8s linear infinite' : undefined,
            }}
          >
            <span style={{ fontSize: 64 }}>🎵</span>
          </div>
        </div>

        {/* Track info */}
        <div className="px-6 py-3 text-center">
          <div className="text-white font-bold text-xl">{track.title}</div>
          <div className="text-sm mt-1" style={{ color: 'rgba(235,235,245,0.55)' }}>{track.artist}</div>
        </div>

        {/* Progress */}
        <div className="px-6 mb-2">
          <div
            className="progress-bar mb-1.5 cursor-pointer"
            onClick={e => {
              const rect = e.currentTarget.getBoundingClientRect();
              setProgress(((e.clientX - rect.left) / rect.width) * 100);
            }}
          >
            <div className="progress-fill" style={{ width: `${progress}%`, background: track.color }} />
          </div>
          <div className="flex justify-between" style={{ color: 'rgba(235,235,245,0.45)', fontSize: 12 }}>
            <span>{Math.floor(progress / 100 * 3 * 60 + progress / 100 * 24)}:{String(Math.floor((progress / 100 * 204) % 60)).padStart(2, '0')}</span>
            <span>{track.duration}</span>
          </div>
        </div>

        {/* Controls */}
        <div className="flex items-center justify-between px-6 py-3">
          <button onClick={() => setShuffle(!shuffle)} className="focus:outline-none">
            <Shuffle size={22} color={shuffle ? '#FF375F' : 'rgba(235,235,245,0.55)'} />
          </button>
          <button onClick={onPrev} className="w-12 h-12 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.07)' }}>
            <SkipBack size={22} color="#fff" />
          </button>
          <button
            onClick={onToggle}
            className="w-16 h-16 rounded-full flex items-center justify-center focus:outline-none"
            style={{ background: track.color, boxShadow: `0 8px 28px ${track.color}77` }}
          >
            {playing ? <Pause size={28} color="#fff" /> : <Play size={28} color="#fff" />}
          </button>
          <button onClick={onNext} className="w-12 h-12 flex items-center justify-center rounded-full focus:outline-none" style={{ background: 'rgba(255,255,255,0.07)' }}>
            <SkipForward size={22} color="#fff" />
          </button>
          <button onClick={() => setRepeat(!repeat)} className="focus:outline-none">
            <Repeat size={22} color={repeat ? '#FF375F' : 'rgba(235,235,245,0.55)'} />
          </button>
        </div>

        {/* Volume */}
        <div className="flex items-center gap-3 px-6 py-2">
          <Volume2 size={16} color="rgba(235,235,245,0.4)" />
          <div className="flex-1 progress-bar" style={{ height: 3 }}>
            <div className="progress-fill" style={{ width: `${volume}%`, background: track.color }} />
          </div>
          <Volume2 size={20} color="rgba(235,235,245,0.7)" />
        </div>

        {/* Track list */}
        <div className="flex-1 scroll-area px-4 py-2">
          <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Çalma Listesi</div>
          {MUSIC_TRACKS.map((t, i) => (
            <button
              key={i}
              className="flex items-center gap-3 px-3 py-2.5 w-full text-left rounded-xl"
              style={{ background: i === trackIdx ? `${t.color}15` : 'transparent', border: i === trackIdx ? `0.5px solid ${t.color}44` : '0.5px solid transparent', marginBottom: 2 }}
              onClick={() => onSetTrack(i)}
            >
              <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style={{ background: `${t.color}33` }}>
                {i === trackIdx && playing ? (
                  <span style={{ fontSize: 14, animation: 'pulse 1s ease infinite' }}>♪</span>
                ) : (
                  <span className="text-xs font-bold" style={{ color: t.color }}>{i + 1}</span>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-white font-medium text-sm truncate">{t.title}</div>
                <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>{t.artist}</div>
              </div>
              <span className="text-xs" style={{ color: 'rgba(235,235,245,0.4)' }}>{t.duration}</span>
            </button>
          ))}
        </div>
      </div>
    </AppWindow>
  );
}
