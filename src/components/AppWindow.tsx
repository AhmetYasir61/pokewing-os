import React from 'react';
import { ChevronLeft } from 'lucide-react';

interface Props {
  title: string;
  accentColor?: string;
  onBack: () => void;
  children: React.ReactNode;
  noHeader?: boolean;
  headerRight?: React.ReactNode;
  transparent?: boolean;
}

export function AppWindow({ title, accentColor = '#0A84FF', onBack, children, noHeader, headerRight, transparent }: Props) {
  return (
    <div
      className="absolute inset-0 z-30 flex flex-col anim-winin"
      style={{ background: transparent ? 'transparent' : 'rgb(10,10,12)' }}
    >
      {!noHeader && (
        <div
          className="flex items-center gap-2 px-3 flex-shrink-0"
          style={{ paddingTop: 46, paddingBottom: 8, background: transparent ? 'rgba(10,10,12,0.6)' : 'rgb(10,10,12)', borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}
        >
          <button
            onClick={onBack}
            className="flex items-center gap-0.5 focus:outline-none active:opacity-60 transition-opacity"
            style={{ color: accentColor, fontWeight: 600, fontSize: 16 }}
          >
            <ChevronLeft size={22} color={accentColor} strokeWidth={2.2} />
            <span>Geri</span>
          </button>
          <span className="flex-1 text-center text-white font-semibold text-base truncate pr-10">{title}</span>
          {headerRight && <div className="flex-shrink-0">{headerRight}</div>}
        </div>
      )}
      <div className="flex-1 overflow-hidden relative">
        {children}
      </div>
    </div>
  );
}
