import React from 'react';
import { AppId } from '../types';
import { Info, Share2, X } from 'lucide-react';

interface Props {
  menu: { x: number; y: number; app: AppId };
  onClose: () => void;
  onToast: (msg: string) => void;
}

export function ContextMenu({ menu, onClose, onToast }: Props) {
  return (
    <div className="absolute inset-0 z-50" onClick={onClose}>
      <div
        className="ctx-menu absolute anim-winin"
        style={{ left: Math.min(menu.x, window.innerWidth - 200), top: Math.min(menu.y, window.innerHeight - 200) }}
        onClick={e => e.stopPropagation()}
      >
        <div className="ctx-item" onClick={() => { onToast('Uygulama bilgisi'); onClose(); }}>
          <Info size={16} color="#0A84FF" />
          <span>Uygulama Bilgisi</span>
        </div>
        <div className="ctx-item" onClick={() => { onToast('Paylaşım menüsü'); onClose(); }}>
          <Share2 size={16} color="#0A84FF" />
          <span>Paylaş</span>
        </div>
        <div className="ctx-item danger" onClick={() => { onToast('Uygulama kaldırıldı (demo)'); onClose(); }}>
          <X size={16} color="#FF453A" />
          <span>Kaldır</span>
        </div>
      </div>
    </div>
  );
}
