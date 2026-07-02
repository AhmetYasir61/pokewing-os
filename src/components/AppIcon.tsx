import React from 'react';
import { AppMeta } from '../types';
import { AppIconGraphic } from './AppIconGraphic';

interface Props {
  app: AppMeta;
  size?: 'sm' | 'md' | 'lg';
  onPress: () => void;
  onLongPress?: () => void;
  wobble?: boolean;
}

export function AppIcon({ app, size = 'md', onPress, onLongPress, wobble }: Props) {
  const dims = size === 'lg' ? 'w-16 h-16' : size === 'sm' ? 'w-12 h-12' : 'w-14 h-14';
  const radius = size === 'sm' ? 'rounded-2xl' : 'rounded-[22px]';
  const fontSize = size === 'sm' ? 'text-[10px]' : 'text-[11px]';
  const nameWidth = size === 'sm' ? 'w-14' : 'w-16';
  const longRef = React.useRef<ReturnType<typeof setTimeout>>();

  const handleTouchStart = () => {
    longRef.current = setTimeout(() => {
      onLongPress?.();
    }, 500);
  };
  const handleTouchEnd = () => {
    clearTimeout(longRef.current);
  };

  return (
    <div className="flex flex-col items-center gap-1.5" style={{ animation: wobble ? 'wobble 0.5s ease infinite' : undefined }}>
      <button
        className={`${dims} ${radius} relative flex items-center justify-center overflow-hidden app-icon-shadow transition-transform active:scale-90 focus:outline-none select-none`}
        style={{ background: app.gradient }}
        onClick={onPress}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        onMouseDown={handleTouchStart}
        onMouseUp={handleTouchEnd}
        onMouseLeave={handleTouchEnd}
      >
        <AppIconGraphic id={app.id} size={size} />
        {!!app.badge && (
          <div className="notif-dot" style={{ top: -4, right: -4 }}>
            {app.badge > 999 ? '999+' : app.badge}
          </div>
        )}
      </button>
      <span
        className={`${fontSize} ${nameWidth} font-medium text-center leading-tight text-white`}
        style={{ textShadow: '0 1px 4px rgba(0,0,0,0.7)' }}
      >
        {app.name}
      </span>
    </div>
  );
}
