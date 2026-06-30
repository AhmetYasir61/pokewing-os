import React from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_CONTACTS } from '../data';
import { Phone, PhoneIncoming, PhoneOff, Delete } from 'lucide-react';

interface Props {
  dialNumber: string;
  onBack: () => void;
  onDialAppend: (c: string) => void;
  onDialDelete: () => void;
  onDialClear: () => void;
  onToast: (msg: string) => void;
}

const KEYS = [
  ['1', ''], ['2', 'ABC'], ['3', 'DEF'],
  ['4', 'GHI'], ['5', 'JKL'], ['6', 'MNO'],
  ['7', 'PQRS'], ['8', 'TUV'], ['9', 'WXYZ'],
  ['*', ''], ['0', '+'], ['#', ''],
];

export function TelefonApp({ dialNumber, onBack, onDialAppend, onDialDelete, onDialClear, onToast }: Props) {
  const formatDisplay = (num: string) => {
    if (num.length <= 3) return num;
    if (num.length <= 6) return `${num.slice(0,3)} ${num.slice(3)}`;
    return `${num.slice(0,3)} ${num.slice(3,6)} ${num.slice(6)}`;
  };

  const handleCall = () => {
    if (dialNumber.length < 3) { onToast('Geçersiz numara'); return; }
    const contact = MOCK_CONTACTS.find(c => c.number === dialNumber || c.number.includes(dialNumber));
    onToast(contact ? `${contact.name} aranıyor...` : `${formatDisplay(dialNumber)} aranıyor...`);
    onDialClear();
  };

  return (
    <AppWindow title="Telefon" accentColor="#30D158" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Contacts */}
        <div className="px-4 pt-2 pb-2">
          <div className="text-xs font-semibold mb-2 px-1" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1, textTransform: 'uppercase' }}>Son Aramalar</div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {MOCK_CONTACTS.slice(0, 4).map((c, i) => (
              <button
                key={i}
                className="flex flex-col items-center gap-1.5 flex-shrink-0 focus:outline-none"
                onClick={() => { onDialClear(); c.number.split('').forEach(ch => onDialAppend(ch)); }}
              >
                <div
                  className="w-12 h-12 rounded-full flex items-center justify-center font-bold text-white"
                  style={{ background: `hsl(${c.name.charCodeAt(0) * 20},60%,40%)` }}
                >
                  {c.name[0]}
                </div>
                <span className="text-[11px] text-white">{c.name}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="h-px mx-4" style={{ background: 'rgba(255,255,255,0.06)' }} />

        {/* Display */}
        <div className="flex items-center justify-center px-6 py-4 min-h-[72px]">
          <div className="flex-1 text-center text-white font-light" style={{ fontSize: dialNumber ? 32 : 18, letterSpacing: 3, color: dialNumber ? '#fff' : 'rgba(235,235,245,0.3)' }}>
            {dialNumber ? formatDisplay(dialNumber) : 'Numara girin'}
          </div>
          {dialNumber.length > 0 && (
            <button onClick={onDialDelete} className="w-9 h-9 flex items-center justify-center rounded-full focus:outline-none active:bg-white/10" style={{ marginLeft: 8 }}>
              <Delete size={20} color="rgba(235,235,245,0.7)" />
            </button>
          )}
        </div>

        {/* Keypad */}
        <div className="flex-1 flex flex-col justify-center px-8">
          <div className="grid grid-cols-3 gap-3">
            {KEYS.map(([num, sub]) => (
              <button
                key={num + sub}
                className="dial-key flex-col mx-auto"
                onClick={() => onDialAppend(num)}
              >
                <span className="text-white font-medium text-xl leading-none">{num}</span>
                {sub && <span className="text-[10px] font-medium mt-0.5" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1.5 }}>{sub}</span>}
              </button>
            ))}
          </div>
        </div>

        {/* Call button */}
        <div className="flex justify-center gap-6 px-8 pb-10 pt-2">
          {dialNumber.length > 0 && (
            <button
              className="w-16 h-16 rounded-full flex items-center justify-center focus:outline-none active:scale-90 transition-transform"
              style={{ background: '#FF453A', boxShadow: '0 8px 24px rgba(255,69,58,0.5)' }}
              onClick={onDialClear}
            >
              <PhoneOff size={26} color="#fff" />
            </button>
          )}
          <button
            className="w-16 h-16 rounded-full flex items-center justify-center focus:outline-none active:scale-90 transition-transform"
            style={{ background: '#30D158', boxShadow: '0 8px 24px rgba(48,209,88,0.5)' }}
            onClick={handleCall}
          >
            <Phone size={26} color="#fff" />
          </button>
        </div>
      </div>
    </AppWindow>
  );
}
