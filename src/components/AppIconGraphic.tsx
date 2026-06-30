import {
  ShoppingBag, Package, BookOpen, Newspaper, MessageCircle, Camera, Phone, Image,
  Globe, Palette, Music, Calendar, FileText, Map, Calculator, Settings, LucideIcon
} from 'lucide-react';
import { AppId } from '../types';

interface Props { id: AppId; size?: 'sm' | 'md' | 'lg'; }

const ICON_MAP: Record<AppId, LucideIcon> = {
  market: ShoppingBag,
  depo: Package,
  katalog: BookOpen,
  news: Newspaper,
  mesaj: MessageCircle,
  kamera: Camera,
  vc: Phone,
  galeri: Image,
  web: Globe,
  tema: Palette,
  muzik: Music,
  takvim: Calendar,
  notlar: FileText,
  harita: Map,
  hesap: Calculator,
  ayar: Settings,
  home: Settings,
};

export function AppIconGraphic({ id, size = 'md' }: Props) {
  const Icon = ICON_MAP[id] ?? Settings;
  const iconSize = size === 'sm' ? 22 : size === 'lg' ? 32 : 28;
  return <Icon size={iconSize} strokeWidth={1.6} color="#fff" />;
}
