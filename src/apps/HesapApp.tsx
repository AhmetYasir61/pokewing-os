import React, { useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { Delete } from 'lucide-react';

interface Props { onBack: () => void; }

const KEYS = [
  ['7','8','9','÷'],
  ['4','5','6','×'],
  ['1','2','3','−'],
  ['0','.','=','+'],
];

export function HesapApp({ onBack }: Props) {
  const [display, setDisplay] = useState('0');
  const [prev, setPrev] = useState('');
  const [op, setOp] = useState('');
  const [reset, setReset] = useState(false);

  const press = (key: string) => {
    if (key === '=') {
      if (!prev || !op) return;
      const a = parseFloat(prev);
      const b = parseFloat(display);
      let result = 0;
      if (op === '+') result = a + b;
      if (op === '−') result = a - b;
      if (op === '×') result = a * b;
      if (op === '÷') result = b !== 0 ? a / b : 0;
      const str = Number.isInteger(result) ? String(result) : result.toFixed(8).replace(/\.?0+$/, '');
      setDisplay(str);
      setPrev('');
      setOp('');
      setReset(true);
      return;
    }
    if (['+','−','×','÷'].includes(key)) {
      setPrev(display);
      setOp(key);
      setReset(true);
      return;
    }
    if (key === '.') {
      if (reset) { setDisplay('0.'); setReset(false); return; }
      if (!display.includes('.')) setDisplay(d => d + '.');
      return;
    }
    if (reset) {
      setDisplay(key);
      setReset(false);
    } else {
      setDisplay(d => d === '0' ? key : d.length > 10 ? d : d + key);
    }
  };

  const clear = () => { setDisplay('0'); setPrev(''); setOp(''); setReset(false); };
  const backspace = () => setDisplay(d => d.length > 1 ? d.slice(0,-1) : '0');
  const negate = () => setDisplay(d => d.startsWith('-') ? d.slice(1) : d === '0' ? '0' : '-' + d);
  const pct = () => setDisplay(d => String(parseFloat(d) / 100));

  const BtnColor: Record<string,string> = { '÷':'rgba(10,132,255,0.25)', '×':'rgba(10,132,255,0.25)', '−':'rgba(10,132,255,0.25)', '+':'rgba(10,132,255,0.25)', '=':'#0A84FF' };

  return (
    <AppWindow title="Hesap Makinesi" accentColor="#636366" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* Display */}
        <div className="flex-1 flex flex-col justify-end px-6 pb-2">
          {prev && (
            <div className="text-right mb-1" style={{ color: 'rgba(235,235,245,0.4)', fontSize: 16 }}>
              {prev} {op}
            </div>
          )}
          <div className="text-right font-light" style={{ fontSize: Math.max(28, 52 - display.length * 2), color: '#fff', overflow: 'hidden', wordBreak: 'break-all' }}>
            {display}
          </div>
        </div>

        {/* Top extra row */}
        <div className="grid grid-cols-4 gap-2 px-4 mb-2">
          {[['AC','#FF453A','white'], ['+/-','rgba(58,58,60,0.9)','white'], ['%','rgba(58,58,60,0.9)','white']].map(([label, bg, fg]) => (
            <button
              key={label}
              className="h-16 rounded-full font-semibold focus:outline-none active:opacity-70 transition-opacity text-lg"
              style={{ background: bg, color: fg }}
              onClick={() => { if (label==='AC') clear(); if (label==='+/-') negate(); if (label==='%') pct(); }}
            >
              {label}
            </button>
          ))}
          <button className="h-16 rounded-full flex items-center justify-center focus:outline-none active:opacity-70 transition-opacity" style={{ background: 'rgba(58,58,60,0.9)' }} onClick={backspace}>
            <Delete size={22} color="#fff" />
          </button>
        </div>

        {/* Keys */}
        <div className="grid grid-cols-4 gap-2 px-4 pb-6">
          {KEYS.flatMap((row) =>
            row.map(key => (
              <button
                key={key}
                className="h-16 rounded-full font-semibold focus:outline-none active:opacity-70 transition-opacity text-xl"
                style={{
                  background: BtnColor[key] ?? 'rgba(58,58,60,0.9)',
                  color: BtnColor[key] && key !== '=' ? '#0A84FF' : '#fff',
                  border: op === key ? '2px solid #0A84FF' : '2px solid transparent',
                }}
                onClick={() => press(key)}
              >
                {key}
              </button>
            ))
          )}
        </div>
      </div>
    </AppWindow>
  );
}
