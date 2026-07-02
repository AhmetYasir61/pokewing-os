import React, { useEffect, useRef, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { itemId } from '../appstore';
import { Lock, Trash2, Play, Pause, RotateCcw } from 'lucide-react';

export interface MiniProps { onBack: () => void; toast: (t: string) => void; }

/* ============ 🧮 HESAP MAKİNESİ ============ */
function HesapMakinesi({ onBack }: MiniProps) {
  const [disp, setDisp] = useState('0');
  const [acc, setAcc] = useState<number | null>(null);
  const [op, setOp] = useState<string | null>(null);
  const [fresh, setFresh] = useState(true);

  const num = (d: string) => { setDisp(fresh || disp === '0' ? d : disp + d); setFresh(false); };
  const dot = () => { if (!disp.includes('.')) { setDisp(fresh ? '0.' : disp + '.'); setFresh(false); } };
  const calc = (a: number, b: number, o: string) => o === '+' ? a + b : o === '-' ? a - b : o === '×' ? a * b : b === 0 ? 0 : a / b;
  const doOp = (o: string) => { const v = parseFloat(disp); setAcc(acc !== null && op ? calc(acc, v, op) : v); setOp(o); setFresh(true); if (acc !== null && op) setDisp(String(calc(acc, v, op))); };
  const eq = () => { if (acc !== null && op) { const r = calc(acc, parseFloat(disp), op); setDisp(String(Math.round(r * 1e10) / 1e10)); setAcc(null); setOp(null); setFresh(true); } };
  const clr = () => { setDisp('0'); setAcc(null); setOp(null); setFresh(true); };

  const B = ({ t, c, on }: { t: string; c?: string; on: () => void }) => (
    <button onClick={on} className="rounded-2xl text-[22px] font-semibold flex items-center justify-center active:scale-95 transition-transform"
      style={{ background: c || '#2c2c2e', color: c === '#FF9F0A' ? '#fff' : c === '#a5a5a5' ? '#000' : '#fff', aspectRatio: '1' }}>{t}</button>
  );
  return (
    <AppWindow title="Hesap Makinesi" accentColor="#FF9F0A" onBack={onBack}>
      <div className="flex flex-col h-full px-4 pb-5">
        <div className="flex-1 flex items-end justify-end px-2 pb-3">
          <span className="text-white font-light" style={{ fontSize: disp.length > 9 ? 38 : 56, wordBreak: 'break-all', textAlign: 'right' }}>{disp}</span>
        </div>
        <div className="grid grid-cols-4 gap-2.5">
          <B t="AC" c="#a5a5a5" on={clr} /><B t="±" c="#a5a5a5" on={() => setDisp(String(-parseFloat(disp)))} /><B t="%" c="#a5a5a5" on={() => setDisp(String(parseFloat(disp) / 100))} /><B t="÷" c="#FF9F0A" on={() => doOp('÷')} />
          <B t="7" on={() => num('7')} /><B t="8" on={() => num('8')} /><B t="9" on={() => num('9')} /><B t="×" c="#FF9F0A" on={() => doOp('×')} />
          <B t="4" on={() => num('4')} /><B t="5" on={() => num('5')} /><B t="6" on={() => num('6')} /><B t="-" c="#FF9F0A" on={() => doOp('-')} />
          <B t="1" on={() => num('1')} /><B t="2" on={() => num('2')} /><B t="3" on={() => num('3')} /><B t="+" c="#FF9F0A" on={() => doOp('+')} />
          <button onClick={() => num('0')} className="col-span-2 rounded-2xl text-[22px] font-semibold text-white text-left pl-7 active:scale-95 transition-transform" style={{ background: '#2c2c2e' }}>0</button>
          <B t="." on={dot} /><B t="=" c="#FF9F0A" on={eq} />
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ ⏱️ SAAT & KRONOMETRE ============ */
function SaatApp({ onBack }: MiniProps) {
  const [tab, setTab] = useState<'saat' | 'krono' | 'sayac'>('krono');
  const [now, setNow] = useState(new Date());
  const [ms, setMs] = useState(0);
  const [run, setRun] = useState(false);
  const [laps, setLaps] = useState<number[]>([]);
  const [cd, setCd] = useState(60);
  const [cdLeft, setCdLeft] = useState(0);
  const [cdRun, setCdRun] = useState(false);

  useEffect(() => { const t = setInterval(() => setNow(new Date()), 500); return () => clearInterval(t); }, []);
  useEffect(() => { if (!run) return; const s = Date.now() - ms; const t = setInterval(() => setMs(Date.now() - s), 47); return () => clearInterval(t); }, [run]);
  useEffect(() => {
    if (!cdRun || cdLeft <= 0) return;
    const t = setInterval(() => setCdLeft(l => { if (l <= 1) { setCdRun(false); return 0; } return l - 1; }), 1000);
    return () => clearInterval(t);
  }, [cdRun, cdLeft]);

  const fmt = (m: number) => `${String(Math.floor(m / 60000)).padStart(2, '0')}:${String(Math.floor(m / 1000) % 60).padStart(2, '0')}.${String(Math.floor(m / 10) % 100).padStart(2, '0')}`;
  return (
    <AppWindow title="Saat" accentColor="#0A84FF" onBack={onBack}>
      <div className="flex flex-col h-full">
        <div className="px-4 py-2"><div className="seg-ctrl">
          {(['saat', 'krono', 'sayac'] as const).map(t => (
            <button key={t} className={`seg-btn ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>{t === 'saat' ? 'Saat' : t === 'krono' ? 'Kronometre' : 'Sayaç'}</button>
          ))}
        </div></div>
        {tab === 'saat' && (
          <div className="flex-1 flex flex-col items-center justify-center gap-2">
            <div className="text-white font-light" style={{ fontSize: 64, fontVariantNumeric: 'tabular-nums' }}>
              {String(now.getHours()).padStart(2, '0')}:{String(now.getMinutes()).padStart(2, '0')}<span className="text-[28px]" style={{ color: 'rgba(235,235,245,0.4)' }}>:{String(now.getSeconds()).padStart(2, '0')}</span>
            </div>
            <div style={{ color: 'rgba(235,235,245,0.5)' }}>{now.toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</div>
          </div>
        )}
        {tab === 'krono' && (
          <div className="flex-1 flex flex-col items-center pt-10 gap-6">
            <div className="text-white font-light" style={{ fontSize: 58, fontVariantNumeric: 'tabular-nums' }}>{fmt(ms)}</div>
            <div className="flex gap-4">
              <button onClick={() => { setRun(false); setMs(0); setLaps([]); }} className="w-16 h-16 rounded-full flex items-center justify-center" style={{ background: '#2c2c2e' }}><RotateCcw size={22} color="#fff" /></button>
              <button onClick={() => setRun(!run)} className="w-16 h-16 rounded-full flex items-center justify-center" style={{ background: run ? '#5c1f1c' : '#1d4a2a' }}>
                {run ? <Pause size={24} color="#FF453A" /> : <Play size={24} color="#30D158" />}
              </button>
              <button onClick={() => run && setLaps([ms, ...laps])} className="w-16 h-16 rounded-full flex items-center justify-center text-white text-[13px] font-semibold" style={{ background: '#2c2c2e' }}>Tur</button>
            </div>
            <div className="w-full px-6 scroll-area" style={{ maxHeight: 200 }}>
              {laps.map((l, i) => (
                <div key={i} className="flex justify-between py-2" style={{ borderBottom: '0.5px solid rgba(255,255,255,0.08)', color: 'rgba(235,235,245,0.7)' }}>
                  <span>Tur {laps.length - i}</span><span style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(l)}</span>
                </div>
              ))}
            </div>
          </div>
        )}
        {tab === 'sayac' && (
          <div className="flex-1 flex flex-col items-center pt-10 gap-6">
            <div className="text-white font-light" style={{ fontSize: 58, fontVariantNumeric: 'tabular-nums' }}>
              {String(Math.floor((cdRun || cdLeft ? cdLeft : cd) / 60)).padStart(2, '0')}:{String((cdRun || cdLeft ? cdLeft : cd) % 60).padStart(2, '0')}
            </div>
            {!cdRun && (
              <div className="flex gap-2">
                {[60, 180, 300, 600].map(s => (
                  <button key={s} onClick={() => { setCd(s); setCdLeft(0); }} className="px-3 py-1.5 rounded-full text-sm" style={{ background: cd === s ? '#0A84FF' : '#2c2c2e', color: '#fff' }}>{s / 60} dk</button>
                ))}
              </div>
            )}
            <button onClick={() => { if (cdRun) { setCdRun(false); setCdLeft(0); } else { setCdLeft(cd); setCdRun(true); } }}
              className="px-8 py-3 rounded-full font-semibold" style={{ background: cdRun ? '#5c1f1c' : '#1d4a2a', color: cdRun ? '#FF453A' : '#30D158' }}>
              {cdRun ? 'Durdur' : 'Başlat'}
            </button>
          </div>
        )}
      </div>
    </AppWindow>
  );
}

/* ============ 🔢 2048 ============ */
function G2048({ onBack, toast }: MiniProps) {
  type Grid = number[][];
  const empty = (): Grid => Array.from({ length: 4 }, () => [0, 0, 0, 0]);
  const addTile = (g: Grid) => {
    const free: [number, number][] = [];
    g.forEach((r, y) => r.forEach((v, x) => { if (!v) free.push([y, x]); }));
    if (free.length) { const [y, x] = free[Math.floor(Math.random() * free.length)]; g[y][x] = Math.random() < 0.9 ? 2 : 4; }
    return g;
  };
  const [grid, setGrid] = useState<Grid>(() => addTile(addTile(empty())));
  const [score, setScore] = useState(0);

  const slide = (row: number[]) => {
    const a = row.filter(v => v); let s = 0;
    for (let i = 0; i < a.length - 1; i++) if (a[i] === a[i + 1]) { a[i] *= 2; s += a[i]; a.splice(i + 1, 1); }
    while (a.length < 4) a.push(0);
    return { a, s };
  };
  const move = (dir: 0 | 1 | 2 | 3) => { // 0=sol 1=sağ 2=yukarı 3=aşağı
    setGrid(g => {
      let ns = 0; let changed = false;
      const rot = (m: Grid): Grid => m[0].map((_, i) => m.map(r => r[i]));
      let m = g.map(r => [...r]);
      if (dir === 2) m = rot(m); if (dir === 3) m = rot(m).map(r => r.reverse());
      if (dir === 1) m = m.map(r => [...r].reverse());
      m = m.map(r => { const { a, s } = slide(r); ns += s; if (a.join() !== r.join()) changed = true; return a; });
      if (dir === 1) m = m.map(r => r.reverse());
      if (dir === 2) m = rot(rot(rot(m))); if (dir === 3) m = rot(rot(rot(m.map(r => r.reverse()))));
      if (!changed) return g;
      setScore(x => x + ns);
      if (ns >= 2048) toast('🎉 2048! Efsanesin!');
      return addTile(m);
    });
  };
  useEffect(() => {
    const k = (e: KeyboardEvent) => {
      const map: Record<string, 0 | 1 | 2 | 3> = { ArrowLeft: 0, ArrowRight: 1, ArrowUp: 2, ArrowDown: 3 };
      if (map[e.key] !== undefined) { e.preventDefault(); move(map[e.key]); }
    };
    window.addEventListener('keydown', k);
    return () => window.removeEventListener('keydown', k);
  }, []);
  const COLORS: Record<number, string> = { 2: '#3a3a3c', 4: '#48484a', 8: '#FF9F0A', 16: '#FF8008', 32: '#FF6B3A', 64: '#FF453A', 128: '#FFD60A', 256: '#FFC60A', 512: '#FFB60A', 1024: '#30D158', 2048: '#0A84FF' };
  return (
    <AppWindow title="2048" accentColor="#FFD60A" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-4 gap-4">
        <div className="flex items-center gap-4">
          <div className="px-4 py-2 rounded-xl text-center" style={{ background: '#2c2c2e' }}>
            <div className="text-[10px] uppercase" style={{ color: 'rgba(235,235,245,0.5)' }}>Skor</div>
            <div className="text-white font-bold text-lg">{score}</div>
          </div>
          <button className="pill-btn ghost" onClick={() => { setGrid(addTile(addTile(empty()))); setScore(0); }}>Yeni Oyun</button>
        </div>
        <div className="grid grid-cols-4 gap-2 p-2 rounded-2xl" style={{ background: '#1c1c1e' }}>
          {grid.flat().map((v, i) => (
            <div key={i} className="rounded-xl flex items-center justify-center font-bold"
              style={{ width: 68, height: 68, background: v ? COLORS[v] || '#0A84FF' : '#2c2c2e', color: v <= 4 && v > 0 ? '#eee' : '#fff', fontSize: v > 512 ? 20 : 26 }}>
              {v || ''}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-3 gap-2 mt-1">
          <div /><button className="dial-key" style={{ width: 56, height: 56 }} onClick={() => move(2)}>▲</button><div />
          <button className="dial-key" style={{ width: 56, height: 56 }} onClick={() => move(0)}>◀</button>
          <button className="dial-key" style={{ width: 56, height: 56 }} onClick={() => move(3)}>▼</button>
          <button className="dial-key" style={{ width: 56, height: 56 }} onClick={() => move(1)}>▶</button>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ 🐍 YILAN ============ */
function Yilan({ onBack }: MiniProps) {
  const N = 15;
  const [snake, setSnake] = useState<[number, number][]>([[7, 7], [7, 8]]);
  const [food, setFood] = useState<[number, number]>([3, 3]);
  const [dir, setDir] = useState<[number, number]>([0, -1]);
  const dirRef = useRef(dir); dirRef.current = dir;
  const [alive, setAlive] = useState(true);
  const [score, setScore] = useState(0);

  useEffect(() => {
    if (!alive) return;
    const t = setInterval(() => {
      setSnake(s => {
        const [dx, dy] = dirRef.current;
        const head: [number, number] = [(s[0][0] + dx + N) % N, (s[0][1] + dy + N) % N];
        if (s.some(([x, y]) => x === head[0] && y === head[1])) { setAlive(false); return s; }
        const ns: [number, number][] = [head, ...s];
        if (head[0] === food[0] && head[1] === food[1]) {
          setScore(v => v + 10);
          setFood([Math.floor(Math.random() * N), Math.floor(Math.random() * N)]);
        } else ns.pop();
        return ns;
      });
    }, 140);
    return () => clearInterval(t);
  }, [alive, food]);
  useEffect(() => {
    const k = (e: KeyboardEvent) => {
      const m: Record<string, [number, number]> = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] };
      const d = m[e.key]; if (!d) return;
      e.preventDefault();
      if (d[0] !== -dirRef.current[0] || d[1] !== -dirRef.current[1]) setDir(d);
    };
    window.addEventListener('keydown', k);
    return () => window.removeEventListener('keydown', k);
  }, []);
  const turn = (d: [number, number]) => { if (d[0] !== -dirRef.current[0] || d[1] !== -dirRef.current[1]) setDir(d); };
  const reset = () => { setSnake([[7, 7], [7, 8]]); setDir([0, -1]); setScore(0); setAlive(true); };
  return (
    <AppWindow title="Yılan" accentColor="#30D158" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-3 gap-3">
        <div className="text-white font-bold">Skor: {score} {!alive && <span style={{ color: '#FF453A' }}> — Oyun Bitti!</span>}</div>
        <div className="grid rounded-xl overflow-hidden" style={{ gridTemplateColumns: `repeat(${N},1fr)`, width: 300, height: 300, background: '#131313' }}>
          {Array.from({ length: N * N }, (_, i) => {
            const x = i % N, y = Math.floor(i / N);
            const isHead = snake[0][0] === x && snake[0][1] === y;
            const isBody = snake.some(([sx, sy]) => sx === x && sy === y);
            const isFood = food[0] === x && food[1] === y;
            return <div key={i} style={{ background: isHead ? '#7CFC9A' : isBody ? '#30D158' : isFood ? '#FF453A' : (x + y) % 2 ? '#161a16' : '#131713', borderRadius: isFood || isHead ? 4 : 0 }} />;
          })}
        </div>
        {!alive && <button className="pill-btn green" onClick={reset}>Tekrar Oyna</button>}
        <div className="grid grid-cols-3 gap-2">
          <div /><button className="dial-key" style={{ width: 54, height: 54 }} onClick={() => turn([0, -1])}>▲</button><div />
          <button className="dial-key" style={{ width: 54, height: 54 }} onClick={() => turn([-1, 0])}>◀</button>
          <button className="dial-key" style={{ width: 54, height: 54 }} onClick={() => turn([0, 1])}>▼</button>
          <button className="dial-key" style={{ width: 54, height: 54 }} onClick={() => turn([1, 0])}>▶</button>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ ⭕ XOX ============ */
function Xox({ onBack, toast }: MiniProps) {
  const [b, setB] = useState<(null | 'X' | 'O')[]>(Array(9).fill(null));
  const [over, setOver] = useState(false);
  const win = (a: (null | string)[]) => {
    const L = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]];
    for (const [i, j, k] of L) if (a[i] && a[i] === a[j] && a[i] === a[k]) return a[i];
    return a.every(v => v) ? 'draw' : null;
  };
  const play = (i: number) => {
    if (b[i] || over) return;
    const nb = [...b]; nb[i] = 'X';
    let w = win(nb);
    if (!w) {
      // basit AI: kazan > engelle > merkez > rastgele
      const me = 'O', you = 'X';
      const lines = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]];
      const pick = (sym: string) => {
        for (const l of lines) {
          const vals = l.map(x => nb[x]);
          if (vals.filter(v => v === sym).length === 2 && vals.includes(null)) return l[vals.indexOf(null)];
        }
        return -1;
      };
      let mv = pick(me); if (mv < 0) mv = pick(you);
      if (mv < 0 && !nb[4]) mv = 4;
      if (mv < 0) { const free = nb.map((v, x) => v ? -1 : x).filter(x => x >= 0); mv = free[Math.floor(Math.random() * free.length)]; }
      if (mv >= 0) nb[mv] = 'O';
      w = win(nb);
    }
    setB(nb);
    if (w) { setOver(true); toast(w === 'draw' ? 'Berabere!' : w === 'X' ? '🎉 Kazandın!' : '🤖 Bilgisayar kazandı!'); }
  };
  return (
    <AppWindow title="XOX" accentColor="#FF375F" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-8 gap-6">
        <div className="grid grid-cols-3 gap-2">
          {b.map((v, i) => (
            <button key={i} onClick={() => play(i)} className="rounded-2xl flex items-center justify-center font-bold active:scale-95 transition-transform"
              style={{ width: 88, height: 88, background: '#1c1c1e', fontSize: 40, color: v === 'X' ? '#0A84FF' : '#FF375F' }}>{v}</button>
          ))}
        </div>
        <button className="pill-btn ghost" onClick={() => { setB(Array(9).fill(null)); setOver(false); }}>Yeni Oyun</button>
        <div className="text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>Sen: <b style={{ color: '#0A84FF' }}>X</b> · Bilgisayar: <b style={{ color: '#FF375F' }}>O</b></div>
      </div>
    </AppWindow>
  );
}

/* ============ 🃏 HAFIZA ============ */
function Hafiza({ onBack, toast }: MiniProps) {
  const EMO = ['🍎', '⚡', '🔥', '🌊', '🌿', '⭐', '🌙', '💎'];
  const shuffle = () => [...EMO, ...EMO].sort(() => Math.random() - 0.5);
  const [cards, setCards] = useState(shuffle);
  const [open, setOpen] = useState<number[]>([]);
  const [done, setDone] = useState<Set<number>>(new Set());
  const [moves, setMoves] = useState(0);

  const flip = (i: number) => {
    if (open.length === 2 || open.includes(i) || done.has(i)) return;
    const no = [...open, i]; setOpen(no);
    if (no.length === 2) {
      setMoves(m => m + 1);
      if (cards[no[0]] === cards[no[1]]) {
        const nd = new Set(done); nd.add(no[0]); nd.add(no[1]); setDone(nd); setOpen([]);
        if (nd.size === cards.length) toast(`🎉 Bitti! ${moves + 1} hamle`);
      } else setTimeout(() => setOpen([]), 700);
    }
  };
  return (
    <AppWindow title="Hafıza" accentColor="#BF5AF2" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-4 gap-4">
        <div className="text-white font-semibold">Hamle: {moves}</div>
        <div className="grid grid-cols-4 gap-2.5">
          {cards.map((e, i) => {
            const show = open.includes(i) || done.has(i);
            return (
              <button key={i} onClick={() => flip(i)} className="rounded-xl flex items-center justify-center active:scale-95"
                style={{ width: 70, height: 70, fontSize: 30, background: show ? '#3a2a5e' : '#1c1c1e', transition: 'background 0.25s, transform 0.1s', opacity: done.has(i) ? 0.55 : 1 }}>
                {show ? e : '❓'}
              </button>
            );
          })}
        </div>
        <button className="pill-btn ghost" onClick={() => { setCards(shuffle()); setOpen([]); setDone(new Set()); setMoves(0); }}>Karıştır</button>
      </div>
    </AppWindow>
  );
}

/* ============ 🎹 PİYANO ============ */
function Piyano({ onBack }: MiniProps) {
  const ctxRef = useRef<AudioContext | null>(null);
  const play = (f: number) => {
    try {
      ctxRef.current = ctxRef.current || new AudioContext();
      const ctx = ctxRef.current;
      if (ctx.state === 'suspended') void ctx.resume();
      const o = ctx.createOscillator(); const g = ctx.createGain();
      o.type = 'triangle'; o.frequency.value = f;
      g.gain.setValueAtTime(0.25, ctx.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.1);
      o.connect(g); g.connect(ctx.destination); o.start(); o.stop(ctx.currentTime + 1.15);
    } catch { /* ses yok */ }
  };
  const WHITE = [{ n: 'Do', f: 261.6 }, { n: 'Re', f: 293.7 }, { n: 'Mi', f: 329.6 }, { n: 'Fa', f: 349.2 }, { n: 'Sol', f: 392 }, { n: 'La', f: 440 }, { n: 'Si', f: 493.9 }, { n: 'Do²', f: 523.3 }];
  const BLACK = [{ f: 277.2, off: 0 }, { f: 311.1, off: 1 }, { f: 370, off: 3 }, { f: 415.3, off: 4 }, { f: 466.2, off: 5 }];
  return (
    <AppWindow title="Piyano" accentColor="#40C8E0" onBack={onBack}>
      <div className="flex flex-col h-full items-center justify-center px-3">
        <div className="relative flex w-full" style={{ height: 260, maxWidth: 420 }}>
          {WHITE.map((k) => (
            <button key={k.n} onMouseDown={() => play(k.f)}
              className="flex-1 flex items-end justify-center pb-3 text-[11px] font-semibold rounded-b-lg active:bg-gray-200"
              style={{ background: '#f5f5f7', color: '#555', border: '1px solid #ccc', marginRight: 1 }}>
              {k.n}
            </button>
          ))}
          {BLACK.map((k, i) => (
            <button key={i} onMouseDown={() => play(k.f)}
              className="absolute rounded-b-md active:bg-gray-700"
              style={{ background: '#1c1c1e', width: `${100 / WHITE.length * 0.6}%`, height: '58%', left: `${(k.off + 1) * (100 / WHITE.length) - (100 / WHITE.length * 0.3)}%`, top: 0, zIndex: 2, border: '1px solid #000' }} />
          ))}
        </div>
        <div className="mt-4 text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>Tuşlara bas — gerçek ses 🎵</div>
      </div>
    </AppWindow>
  );
}

/* ============ 🎲 ZAR & YAZI TURA ============ */
function Zar({ onBack }: MiniProps) {
  const [die, setDie] = useState(6);
  const [coin, setCoin] = useState<'YAZI' | 'TURA' | null>(null);
  const [rolling, setRolling] = useState(false);
  const roll = (what: 'die' | 'coin') => {
    setRolling(true);
    let n = 0;
    const t = setInterval(() => {
      if (what === 'die') setDie(1 + Math.floor(Math.random() * 6));
      else setCoin(Math.random() < 0.5 ? 'YAZI' : 'TURA');
      if (++n >= 10) { clearInterval(t); setRolling(false); }
    }, 70);
  };
  const FACE = ['', '⚀', '⚁', '⚂', '⚃', '⚄', '⚅'];
  return (
    <AppWindow title="Zar & Yazı Tura" accentColor="#FF453A" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-10 gap-8">
        <div className="flex flex-col items-center gap-3">
          <div style={{ fontSize: 110, lineHeight: 1, filter: rolling ? 'blur(2px)' : 'none' }}>{FACE[die]}</div>
          <button className="pill-btn danger" onClick={() => roll('die')} disabled={rolling}>🎲 Zar At</button>
        </div>
        <div className="w-3/4 h-px" style={{ background: 'rgba(255,255,255,0.08)' }} />
        <div className="flex flex-col items-center gap-3">
          <div className="w-28 h-28 rounded-full flex items-center justify-center font-bold text-xl"
            style={{ background: 'linear-gradient(160deg,#FFD60A,#B8860B)', color: '#3a2a00', filter: rolling ? 'blur(2px)' : 'none' }}>
            {coin ?? '?'}
          </div>
          <button className="pill-btn gold" onClick={() => roll('coin')} disabled={rolling}>🪙 Yazı Tura</button>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ 🕶️ DARKCHAT (PIN korumalı gizli yazışma kasası) ============ */
function DarkChat({ onBack, toast }: MiniProps) {
  const PKEY = 'pwdark:' + itemId();
  const load = () => { try { return JSON.parse(localStorage.getItem(PKEY) || 'null') || { pin: '', logs: [] }; } catch { return { pin: '', logs: [] }; } };
  const [data, setData] = useState<{ pin: string; logs: { t: string; when: string }[] }>(load);
  const [locked, setLocked] = useState(!!load().pin);
  const [pinIn, setPinIn] = useState('');
  const [text, setText] = useState('');
  const save = (d: typeof data) => { setData(d); try { localStorage.setItem(PKEY, JSON.stringify(d)); } catch { /* kota */ } };

  if (locked) {
    return (
      <AppWindow title="DarkChat" accentColor="#8E8E93" onBack={onBack}>
        <div className="flex flex-col items-center justify-center h-full gap-4 px-8" style={{ background: '#0a0a0c' }}>
          <Lock size={36} color="#4a4a4e" />
          <div className="text-white font-semibold">PIN gir</div>
          <input type="password" value={pinIn} onChange={e => setPinIn(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
            onKeyDown={e => { if (e.key === 'Enter') { if (pinIn === data.pin) { setLocked(false); setPinIn(''); } else { toast('Yanlış PIN'); setPinIn(''); } } }}
            placeholder="••••" inputMode="numeric" style={{ textAlign: 'center', letterSpacing: 8, fontSize: 22, maxWidth: 180 }} autoFocus />
          <button className="pill-btn ghost" onClick={() => { if (pinIn === data.pin) { setLocked(false); setPinIn(''); } else { toast('Yanlış PIN'); setPinIn(''); } }}>Aç</button>
        </div>
      </AppWindow>
    );
  }
  return (
    <AppWindow title="DarkChat" accentColor="#8E8E93" onBack={onBack}
      headerRight={<button onClick={() => { save({ ...data, logs: [] }); toast('Tüm kayıtlar YOK EDİLDİ 🔥'); }}
        className="p-1.5 rounded-full" style={{ background: 'rgba(255,69,58,0.15)' }}><Trash2 size={14} color="#FF453A" /></button>}>
      <div className="flex flex-col h-full" style={{ background: '#0a0a0c' }}>
        {!data.pin && (
          <div className="px-4 py-2 text-[12px] flex items-center gap-2" style={{ background: 'rgba(255,159,10,0.08)', color: '#FF9F0A' }}>
            <Lock size={12} /> PIN belirle:
            <input value={pinIn} onChange={e => setPinIn(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))} placeholder="4-6 hane"
              style={{ width: 90, padding: '4px 8px', fontSize: 12 }} inputMode="numeric" />
            <button className="pill-btn ghost text-xs py-1 px-2" onClick={() => { if (pinIn.length >= 4) { save({ ...data, pin: pinIn }); setPinIn(''); toast('PIN ayarlandı 🔒'); } else toast('En az 4 hane'); }}>Kaydet</button>
          </div>
        )}
        <div className="flex-1 scroll-area px-4 py-3 flex flex-col gap-2">
          {data.logs.length === 0 && (
            <div className="text-center mt-10 text-[13px]" style={{ color: '#3a3a3e' }}>
              ~ kayıt dışı bölge ~<br />Buraya yazdıkların yalnız bu cihazda, şifreli kasada durur.<br />Çöp kutusu = kanıt yok etme.
            </div>
          )}
          {data.logs.map((l, i) => (
            <div key={i} className="px-3 py-2 rounded-lg text-[13.5px]" style={{ background: '#121216', color: '#9fe870', fontFamily: 'Consolas,monospace', border: '1px solid #1c2a1c' }}>
              <span style={{ color: '#3f5f3f', fontSize: 10, marginRight: 8 }}>{l.when}</span>{l.t}
            </div>
          ))}
        </div>
        <div className="px-3 pb-4 pt-2 flex items-center gap-2">
          <input value={text} onChange={e => setText(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && text.trim()) { save({ ...data, logs: [...data.logs, { t: text.trim(), when: new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' }) }] }); setText(''); } }}
            placeholder="gizli not / plan yaz..." style={{ flex: 1, background: '#121216', border: '1px solid #222', color: '#9fe870', fontFamily: 'Consolas,monospace' }} />
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ KAYIT DEFTERİ ============ */
export const MINI_APPS: Record<string, React.ComponentType<MiniProps>> = {
  hesapm: HesapMakinesi,
  saat: SaatApp,
  g2048: G2048,
  yilan: Yilan,
  xox: Xox,
  hafiza: Hafiza,
  piyano: Piyano,
  zar: Zar,
  darkchat: DarkChat,
};
