import React, { useEffect, useRef, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import type { MiniProps } from './MiniApps';

/* ============ ✂️ TAŞ KAĞIT MAKAS ============ */
function TasKagitMakas({ onBack }: MiniProps) {
  const OPT = [{ e: '✊', n: 'Taş' }, { e: '✋', n: 'Kağıt' }, { e: '✌️', n: 'Makas' }];
  const [me, setMe] = useState<number | null>(null);
  const [cpu, setCpu] = useState<number | null>(null);
  const [res, setRes] = useState('');
  const [score, setScore] = useState({ w: 0, l: 0, d: 0 });
  const play = (i: number) => {
    const c = Math.floor(Math.random() * 3);
    setMe(i); setCpu(c);
    if (i === c) { setRes('Berabere'); setScore(s => ({ ...s, d: s.d + 1 })); }
    else if ((i + 1) % 3 === c) { setRes('Kaybettin'); setScore(s => ({ ...s, l: s.l + 1 })); }
    else { setRes('Kazandın! 🎉'); setScore(s => ({ ...s, w: s.w + 1 })); }
  };
  return (
    <AppWindow title="Taş Kağıt Makas" accentColor="#5AC8FA" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-6 gap-5">
        <div className="flex items-center justify-around w-full px-8">
          <div className="flex flex-col items-center gap-1"><span style={{ fontSize: 54 }}>{cpu !== null ? OPT[cpu].e : '❔'}</span><span className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>Rakip</span></div>
          <span className="text-white text-2xl font-light">VS</span>
          <div className="flex flex-col items-center gap-1"><span style={{ fontSize: 54 }}>{me !== null ? OPT[me].e : '❔'}</span><span className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>Sen</span></div>
        </div>
        <div className="text-white font-bold text-xl h-7">{res}</div>
        <div className="flex gap-3">
          {OPT.map((o, i) => (
            <button key={i} onClick={() => play(i)} className="rounded-2xl flex flex-col items-center justify-center active:scale-90 transition-transform"
              style={{ width: 84, height: 84, background: '#1c1c1e' }}><span style={{ fontSize: 34 }}>{o.e}</span><span className="text-[11px] text-white mt-0.5">{o.n}</span></button>
          ))}
        </div>
        <div className="flex gap-4 text-sm mt-1">
          <span style={{ color: '#30D158' }}>G {score.w}</span><span style={{ color: '#8E8E93' }}>B {score.d}</span><span style={{ color: '#FF453A' }}>M {score.l}</span>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ ⚡ REAKSİYON TESTİ ============ */
function Reaksiyon({ onBack }: MiniProps) {
  const [state, setState] = useState<'idle' | 'wait' | 'now' | 'result'>('idle');
  const [ms, setMs] = useState(0);
  const [best, setBest] = useState<number | null>(null);
  const startAt = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const begin = () => {
    setState('wait');
    timer.current = setTimeout(() => { startAt.current = Date.now(); setState('now'); }, 1200 + Math.random() * 2500);
  };
  const tap = () => {
    if (state === 'idle' || state === 'result') { begin(); return; }
    if (state === 'wait') { if (timer.current) clearTimeout(timer.current); setState('idle'); setMs(-1); return; }
    if (state === 'now') {
      const d = Date.now() - startAt.current; setMs(d); setState('result');
      setBest(b => (b === null || d < b) ? d : b);
    }
  };
  useEffect(() => () => { if (timer.current) clearTimeout(timer.current); }, []);
  const bg = state === 'now' ? '#30D158' : state === 'wait' ? '#FF453A' : '#1c1c1e';
  return (
    <AppWindow title="Reaksiyon" accentColor="#FFD60A" onBack={onBack}>
      <div className="flex flex-col h-full">
        <button onClick={tap} className="flex-1 flex flex-col items-center justify-center gap-3 transition-colors" style={{ background: bg }}>
          {state === 'idle' && <><span style={{ fontSize: 44 }}>⚡</span><span className="text-white font-semibold text-lg">Başlamak için dokun</span></>}
          {state === 'wait' && <span className="text-white font-semibold text-lg">Yeşili bekle...</span>}
          {state === 'now' && <span className="text-white font-bold text-3xl">DOKUN!</span>}
          {state === 'result' && <><span className="text-white font-bold text-4xl">{ms < 0 ? 'Erken!' : ms + ' ms'}</span><span className="text-white/70">Tekrar için dokun</span></>}
        </button>
        <div className="text-center py-3 text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>En iyi: {best !== null ? best + ' ms' : '—'}</div>
      </div>
    </AppWindow>
  );
}

/* ============ 🔨 KÖSTEBEK ============ */
function Kostebek({ onBack, toast }: MiniProps) {
  const [holes, setHoles] = useState<boolean[]>(Array(9).fill(false));
  const [score, setScore] = useState(0);
  const [time, setTime] = useState(30);
  const [run, setRun] = useState(false);
  useEffect(() => {
    if (!run) return;
    const pop = setInterval(() => setHoles(() => { const h = Array(9).fill(false); h[Math.floor(Math.random() * 9)] = true; if (Math.random() < 0.4) h[Math.floor(Math.random() * 9)] = true; return h; }), 750);
    const clock = setInterval(() => setTime(t => { if (t <= 1) { setRun(false); return 0; } return t - 1; }), 1000);
    return () => { clearInterval(pop); clearInterval(clock); };
  }, [run]);
  useEffect(() => { if (time === 0) toast(`⏱️ Bitti! Skor: ${score}`); }, [time]);
  const hit = (i: number) => { if (run && holes[i]) { setScore(s => s + 1); setHoles(h => h.map((v, x) => x === i ? false : v)); } };
  const start = () => { setScore(0); setTime(30); setHoles(Array(9).fill(false)); setRun(true); };
  return (
    <AppWindow title="Köstebek" accentColor="#8B5A2B" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-4 gap-4">
        <div className="flex gap-6 text-white font-semibold"><span>⏱️ {time}s</span><span>🔨 {score}</span></div>
        <div className="grid grid-cols-3 gap-3">
          {holes.map((up, i) => (
            <button key={i} onClick={() => hit(i)} className="rounded-full flex items-center justify-center active:scale-90 transition-transform"
              style={{ width: 82, height: 82, background: 'radial-gradient(circle at 50% 70%, #3a2a1a, #14100a)', fontSize: 40 }}>
              <span style={{ transition: 'transform 0.1s', transform: up ? 'translateY(0)' : 'translateY(30px)', opacity: up ? 1 : 0 }}>{up ? '🐹' : ''}</span>
            </button>
          ))}
        </div>
        {!run && <button className="pill-btn gold" onClick={start}>{time === 0 ? 'Tekrar Oyna' : 'Başlat'}</button>}
      </div>
    </AppWindow>
  );
}

/* ============ 🧱 TUĞLA KIR (Breakout) ============ */
function TuglaKir({ onBack, toast }: MiniProps) {
  const cvs = useRef<HTMLCanvasElement | null>(null);
  const [over, setOver] = useState<'' | 'win' | 'lose'>('');
  const paddleX = useRef(150);
  useEffect(() => {
    const c = cvs.current; if (!c) return; const ctx = c.getContext('2d'); if (!ctx) return;
    const W = 300, H = 380; c.width = W; c.height = H;
    let ball = { x: W / 2, y: H - 40, dx: 2.4, dy: -2.4, r: 6 };
    const pw = 62, ph = 10;
    const cols = 6, rows = 4, bw = W / cols, bh = 18;
    const bricks: boolean[] = Array(cols * rows).fill(true);
    let raf = 0; let alive = true;
    const COLORS = ['#FF453A', '#FF9F0A', '#FFD60A', '#30D158'];
    const move = (e: MouseEvent) => { const r = c.getBoundingClientRect(); paddleX.current = Math.max(pw / 2, Math.min(W - pw / 2, e.clientX - r.left)); };
    const touch = (e: TouchEvent) => { const r = c.getBoundingClientRect(); paddleX.current = Math.max(pw / 2, Math.min(W - pw / 2, e.touches[0].clientX - r.left)); };
    c.addEventListener('mousemove', move); c.addEventListener('touchmove', touch);
    const loop = () => {
      if (!alive) return;
      ctx.clearRect(0, 0, W, H); ctx.fillStyle = '#0a0a0c'; ctx.fillRect(0, 0, W, H);
      bricks.forEach((b, i) => { if (!b) return; const bx = (i % cols) * bw, by = 30 + Math.floor(i / cols) * (bh + 4); ctx.fillStyle = COLORS[Math.floor(i / cols)]; ctx.fillRect(bx + 2, by, bw - 4, bh); });
      const px = paddleX.current;
      ctx.fillStyle = '#5AC8FA'; ctx.fillRect(px - pw / 2, H - 20, pw, ph);
      ctx.beginPath(); ctx.arc(ball.x, ball.y, ball.r, 0, Math.PI * 2); ctx.fillStyle = '#fff'; ctx.fill();
      ball.x += ball.dx; ball.y += ball.dy;
      if (ball.x < ball.r || ball.x > W - ball.r) ball.dx *= -1;
      if (ball.y < ball.r) ball.dy *= -1;
      if (ball.y > H - 20 - ball.r && ball.y < H - 20 + ph && ball.x > px - pw / 2 && ball.x < px + pw / 2 && ball.dy > 0) {
        ball.dy *= -1; ball.dx += ((ball.x - px) / (pw / 2)) * 1.2;
      }
      bricks.forEach((b, i) => {
        if (!b) return; const bx = (i % cols) * bw + 2, by = 30 + Math.floor(i / cols) * (bh + 4);
        if (ball.x > bx && ball.x < bx + bw - 4 && ball.y > by && ball.y < by + bh) { bricks[i] = false; ball.dy *= -1; }
      });
      if (bricks.every(b => !b)) { alive = false; setOver('win'); toast('🎉 Kazandın!'); return; }
      if (ball.y > H) { alive = false; setOver('lose'); return; }
      raf = requestAnimationFrame(loop);
    };
    loop();
    return () => { alive = false; cancelAnimationFrame(raf); c.removeEventListener('mousemove', move); c.removeEventListener('touchmove', touch); };
  }, [over === '' ? 0 : 1]);
  return (
    <AppWindow title="Tuğla Kır" accentColor="#5AC8FA" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-3 gap-3">
        <div className="text-xs" style={{ color: 'rgba(235,235,245,0.5)' }}>Fareyle/parmakla küreği kaydır</div>
        <div className="relative">
          <canvas ref={cvs} style={{ borderRadius: 12, border: '1px solid #1c1c1e' }} />
          {over && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3" style={{ background: 'rgba(0,0,0,0.6)', borderRadius: 12 }}>
              <span className="text-white font-bold text-2xl">{over === 'win' ? '🎉 Kazandın' : '💥 Bitti'}</span>
              <button className="pill-btn ghost" onClick={() => setOver('')}>Tekrar</button>
            </div>
          )}
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ 💣 MAYIN TARLASI ============ */
function MayinTarlasi({ onBack, toast }: MiniProps) {
  const N = 9, MINES = 12;
  type Cell = { mine: boolean; open: boolean; flag: boolean; n: number };
  const build = (): Cell[] => {
    const g: Cell[] = Array.from({ length: N * N }, () => ({ mine: false, open: false, flag: false, n: 0 }));
    let placed = 0; while (placed < MINES) { const i = Math.floor(Math.random() * N * N); if (!g[i].mine) { g[i].mine = true; placed++; } }
    for (let i = 0; i < N * N; i++) { if (g[i].mine) continue; let c = 0; neigh(i).forEach(j => { if (g[j].mine) c++; }); g[i].n = c; }
    return g;
  };
  const neigh = (i: number): number[] => {
    const x = i % N, y = Math.floor(i / N); const out: number[] = [];
    for (let dx = -1; dx <= 1; dx++) for (let dy = -1; dy <= 1; dy++) {
      if (!dx && !dy) continue; const nx = x + dx, ny = y + dy;
      if (nx >= 0 && nx < N && ny >= 0 && ny < N) out.push(ny * N + nx);
    }
    return out;
  };
  const [grid, setGrid] = useState<Cell[]>(build);
  const [dead, setDead] = useState(false);
  const [won, setWon] = useState(false);
  const flood = (g: Cell[], i: number) => {
    const stack = [i];
    while (stack.length) { const k = stack.pop()!; if (g[k].open || g[k].flag) continue; g[k].open = true; if (g[k].n === 0 && !g[k].mine) neigh(k).forEach(j => { if (!g[j].open) stack.push(j); }); }
  };
  const open = (i: number) => {
    if (dead || won || grid[i].open || grid[i].flag) return;
    const g = grid.map(c => ({ ...c }));
    if (g[i].mine) { g.forEach(c => { if (c.mine) c.open = true; }); setGrid(g); setDead(true); toast('💥 Mayına bastın!'); return; }
    flood(g, i); setGrid(g);
    if (g.every(c => c.mine || c.open)) { setWon(true); toast('🎉 Temizledin!'); }
  };
  const flag = (e: React.MouseEvent, i: number) => { e.preventDefault(); if (dead || won || grid[i].open) return; setGrid(grid.map((c, x) => x === i ? { ...c, flag: !c.flag } : c)); };
  const reset = () => { setGrid(build()); setDead(false); setWon(false); };
  const flags = grid.filter(c => c.flag).length;
  const NC = ['', '#5AC8FA', '#30D158', '#FF9F0A', '#FF453A', '#BF5AF2', '#FFD60A', '#FF375F', '#fff'];
  return (
    <AppWindow title="Mayın Tarlası" accentColor="#8E8E93" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-3 gap-3">
        <div className="flex gap-6 text-white font-semibold text-sm"><span>💣 {MINES - flags}</span><button className="pill-btn ghost text-xs py-1 px-3" onClick={reset}>Yeni</button></div>
        <div className="grid select-none" style={{ gridTemplateColumns: `repeat(${N},30px)`, gap: 2 }}>
          {grid.map((c, i) => (
            <button key={i} onClick={() => open(i)} onContextMenu={e => flag(e, i)}
              className="flex items-center justify-center font-bold rounded active:scale-95"
              style={{ width: 30, height: 30, fontSize: 14, background: c.open ? (c.mine ? '#5c1f1c' : '#242426') : '#3a3a3c', color: c.mine ? '#fff' : NC[c.n] }}>
              {c.open ? (c.mine ? '💣' : c.n || '') : c.flag ? '🚩' : ''}
            </button>
          ))}
        </div>
        <div className="text-[11px]" style={{ color: 'rgba(235,235,245,0.4)' }}>Sağ tık = bayrak</div>
      </div>
    </AppWindow>
  );
}

/* ============ 🟦 TETRIS ============ */
function Tetris({ onBack, toast }: MiniProps) {
  const COLS = 10, ROWS = 18;
  const SHAPES: number[][][] = [
    [[1, 1, 1, 1]], [[1, 1], [1, 1]], [[0, 1, 0], [1, 1, 1]],
    [[1, 0, 0], [1, 1, 1]], [[0, 0, 1], [1, 1, 1]], [[1, 1, 0], [0, 1, 1]], [[0, 1, 1], [1, 1, 0]],
  ];
  const COLORS = ['#5AC8FA', '#FFD60A', '#BF5AF2', '#0A84FF', '#FF9F0A', '#30D158', '#FF453A'];
  const [board, setBoard] = useState<number[]>(() => Array(COLS * ROWS).fill(0));
  const [score, setScore] = useState(0);
  const [over, setOver] = useState(false);
  const piece = useRef({ shape: SHAPES[0], color: 1, x: 3, y: 0 });
  const boardRef = useRef(board); boardRef.current = board;

  const collide = (sh: number[][], px: number, py: number, bd: number[]) => {
    for (let y = 0; y < sh.length; y++) for (let x = 0; x < sh[y].length; x++) {
      if (!sh[y][x]) continue; const nx = px + x, ny = py + y;
      if (nx < 0 || nx >= COLS || ny >= ROWS) return true;
      if (ny >= 0 && bd[ny * COLS + nx]) return true;
    }
    return false;
  };
  const spawn = () => { const i = Math.floor(Math.random() * SHAPES.length); piece.current = { shape: SHAPES[i], color: i + 1, x: 3, y: 0 }; };
  const merge = () => {
    const bd = [...boardRef.current]; const p = piece.current;
    p.shape.forEach((row, y) => row.forEach((v, x) => { if (v && p.y + y >= 0) bd[(p.y + y) * COLS + (p.x + x)] = p.color; }));
    let cleared = 0;
    for (let y = ROWS - 1; y >= 0; y--) { if (bd.slice(y * COLS, y * COLS + COLS).every(v => v)) { bd.splice(y * COLS, COLS); bd.unshift(...Array(COLS).fill(0)); cleared++; y++; } }
    if (cleared) setScore(s => s + [0, 40, 100, 300, 1200][cleared]);
    setBoard(bd);
    spawn();
    if (collide(piece.current.shape, piece.current.x, piece.current.y, bd)) { setOver(true); toast(`Oyun bitti! Skor: ${score}`); }
  };
  const tick = () => {
    const p = piece.current;
    if (!collide(p.shape, p.x, p.y + 1, boardRef.current)) { p.y++; setBoard(b => [...b]); }
    else merge();
  };
  const rotate = () => { const p = piece.current; const r = p.shape[0].map((_, i) => p.shape.map(row => row[i]).reverse()); if (!collide(r, p.x, p.y, boardRef.current)) { p.shape = r; setBoard(b => [...b]); } };
  const shift = (d: number) => { const p = piece.current; if (!collide(p.shape, p.x + d, p.y, boardRef.current)) { p.x += d; setBoard(b => [...b]); } };
  const drop = () => { const p = piece.current; while (!collide(p.shape, p.x, p.y + 1, boardRef.current)) p.y++; merge(); };

  useEffect(() => { spawn(); }, []);
  useEffect(() => { if (over) return; const t = setInterval(tick, 620); return () => clearInterval(t); }, [over]);
  useEffect(() => {
    const k = (e: KeyboardEvent) => {
      if (over) return;
      if (e.key === 'ArrowLeft') { e.preventDefault(); shift(-1); }
      else if (e.key === 'ArrowRight') { e.preventDefault(); shift(1); }
      else if (e.key === 'ArrowDown') { e.preventDefault(); tick(); }
      else if (e.key === 'ArrowUp') { e.preventDefault(); rotate(); }
      else if (e.key === ' ') { e.preventDefault(); drop(); }
    };
    window.addEventListener('keydown', k); return () => window.removeEventListener('keydown', k);
  }, [over]);

  const render = [...board]; const p = piece.current;
  p.shape.forEach((row, y) => row.forEach((v, x) => { if (v && p.y + y >= 0 && p.y + y < ROWS) render[(p.y + y) * COLS + (p.x + x)] = p.color; }));
  const reset = () => { setBoard(Array(COLS * ROWS).fill(0)); setScore(0); setOver(false); spawn(); };
  return (
    <AppWindow title="Tetris" accentColor="#5AC8FA" onBack={onBack}>
      <div className="flex flex-col items-center h-full pt-3 gap-2">
        <div className="text-white font-bold">Skor: {score}</div>
        <div className="relative grid" style={{ gridTemplateColumns: `repeat(${COLS},15px)`, gap: 1, background: '#0a0a0c', padding: 4, borderRadius: 8 }}>
          {render.map((v, i) => <div key={i} style={{ width: 15, height: 15, borderRadius: 2, background: v ? COLORS[v - 1] : '#161618' }} />)}
          {over && <div className="absolute inset-0 flex flex-col items-center justify-center gap-2" style={{ background: 'rgba(0,0,0,0.7)', borderRadius: 8 }}>
            <span className="text-white font-bold">Bitti</span><button className="pill-btn ghost text-xs" onClick={reset}>Tekrar</button></div>}
        </div>
        <div className="grid grid-cols-4 gap-2 mt-1">
          <button className="dial-key" style={{ width: 48, height: 48 }} onClick={() => shift(-1)}>◀</button>
          <button className="dial-key" style={{ width: 48, height: 48 }} onClick={rotate}>⟳</button>
          <button className="dial-key" style={{ width: 48, height: 48 }} onClick={() => shift(1)}>▶</button>
          <button className="dial-key" style={{ width: 48, height: 48 }} onClick={drop}>⬇</button>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ 📐 BİRİM ÇEVİRİCİ ============ */
function BirimCevirici({ onBack }: MiniProps) {
  const CATS: Record<string, { units: Record<string, number>; }> = {
    'Uzunluk': { units: { 'm': 1, 'km': 1000, 'cm': 0.01, 'mm': 0.001, 'mil': 1609.34, 'inç': 0.0254, 'ft': 0.3048 } },
    'Ağırlık': { units: { 'kg': 1, 'g': 0.001, 'ton': 1000, 'lb': 0.453592, 'ons': 0.0283495 } },
    'Sıcaklık': { units: { '°C': 1, '°F': 0, 'K': 0 } },
    'Alan': { units: { 'm²': 1, 'km²': 1e6, 'ha': 1e4, 'ft²': 0.092903, 'dönüm': 1000 } },
    'Hız': { units: { 'km/s': 1, 'm/s': 3.6, 'mph': 1.60934, 'knot': 1.852 } },
  };
  const [cat, setCat] = useState('Uzunluk');
  const [from, setFrom] = useState('m');
  const [to, setTo] = useState('km');
  const [val, setVal] = useState('1');
  const units = Object.keys(CATS[cat].units);
  const convert = (): string => {
    const v = parseFloat(val); if (isNaN(v)) return '';
    if (cat === 'Sıcaklık') {
      let cC = from === '°C' ? v : from === '°F' ? (v - 32) * 5 / 9 : v - 273.15;
      const out = to === '°C' ? cC : to === '°F' ? cC * 9 / 5 + 32 : cC + 273.15;
      return (Math.round(out * 100) / 100).toString();
    }
    const base = v * CATS[cat].units[from]; const out = base / CATS[cat].units[to];
    return (Math.round(out * 1e6) / 1e6).toString();
  };
  const pickCat = (c: string) => { setCat(c); const u = Object.keys(CATS[c].units); setFrom(u[0]); setTo(u[1] || u[0]); };
  return (
    <AppWindow title="Birim Çevirici" accentColor="#64D2FF" onBack={onBack}>
      <div className="flex flex-col h-full px-5 pt-4 gap-4">
        <div className="flex flex-wrap gap-2">
          {Object.keys(CATS).map(c => <button key={c} onClick={() => pickCat(c)} className="px-3 py-1.5 rounded-full text-sm" style={{ background: cat === c ? '#0A84FF' : '#2c2c2e', color: '#fff' }}>{c}</button>)}
        </div>
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <input value={val} onChange={e => setVal(e.target.value)} inputMode="decimal" style={{ flex: 1, fontSize: 22, textAlign: 'right' }} />
            <select value={from} onChange={e => setFrom(e.target.value)} style={{ background: '#2c2c2e', color: '#fff', borderRadius: 8, padding: '10px 8px' }}>{units.map(u => <option key={u}>{u}</option>)}</select>
          </div>
          <div className="text-center text-2xl" style={{ color: '#64D2FF' }}>↓</div>
          <div className="flex items-center gap-2">
            <div style={{ flex: 1, fontSize: 22, textAlign: 'right', color: '#fff', padding: '10px 12px', background: '#1c1c1e', borderRadius: 10, minHeight: 46 }}>{convert()}</div>
            <select value={to} onChange={e => setTo(e.target.value)} style={{ background: '#2c2c2e', color: '#fff', borderRadius: 8, padding: '10px 8px' }}>{units.map(u => <option key={u}>{u}</option>)}</select>
          </div>
        </div>
      </div>
    </AppWindow>
  );
}

/* ============ 🔦 FENER ============ */
function Fener({ onBack }: MiniProps) {
  const [on, setOn] = useState(false);
  return (
    <AppWindow title="Fener" accentColor="#FFD60A" onBack={onBack} transparent>
      <button onClick={() => setOn(!on)} className="w-full h-full flex flex-col items-center justify-center gap-6 transition-colors" style={{ background: on ? '#ffffff' : '#050505' }}>
        <span style={{ fontSize: 90, filter: on ? 'none' : 'grayscale(1) brightness(0.4)' }}>{on ? '🔦' : '🔦'}</span>
        <span className="font-semibold text-lg" style={{ color: on ? '#111' : '#666' }}>{on ? 'AÇIK — kapatmak için dokun' : 'Feneri açmak için dokun'}</span>
      </button>
    </AppWindow>
  );
}

/* ============ 🔮 SİHİRLİ KÜRE ============ */
function SihirliKure({ onBack }: MiniProps) {
  const ANS = ['Kesinlikle evet', 'Bence evet', 'Şüphesiz', 'Belki', 'Sonra tekrar sor', 'Pek sanmam', 'Hayır', 'Olmaz', 'Şansına küs', 'Şimdi olmaz', 'İşaretler evet diyor', 'Emin değilim'];
  const [ans, setAns] = useState('Bir soru sor ve küreye dokun');
  const [shake, setShake] = useState(false);
  const ask = () => { setShake(true); setTimeout(() => { setAns(ANS[Math.floor(Math.random() * ANS.length)]); setShake(false); }, 500); };
  return (
    <AppWindow title="Sihirli Küre" accentColor="#BF5AF2" onBack={onBack}>
      <div className="flex flex-col items-center justify-center h-full gap-8">
        <button onClick={ask} className="rounded-full flex items-center justify-center" style={{ width: 200, height: 200, background: 'radial-gradient(circle at 35% 30%, #4a3a6e, #14101e)', boxShadow: '0 10px 40px rgba(191,90,242,0.3)', transition: 'transform 0.1s', transform: shake ? 'rotate(-6deg) scale(0.97)' : 'none' }}>
          <div className="rounded-full flex items-center justify-center text-center px-4" style={{ width: 120, height: 120, background: 'radial-gradient(circle at 40% 35%, #2a2a3e, #08080e)' }}>
            <span style={{ color: '#c9a9ff', fontSize: shake ? 40 : 13, fontWeight: 600 }}>{shake ? '🔮' : ans}</span>
          </div>
        </button>
        <span className="text-sm" style={{ color: 'rgba(235,235,245,0.4)' }}>Küreye dokun, kaderini öğren</span>
      </div>
    </AppWindow>
  );
}

/* ============ 🎵 METRONOM ============ */
function Metronom({ onBack }: MiniProps) {
  const [bpm, setBpm] = useState(100);
  const [run, setRun] = useState(false);
  const [beat, setBeat] = useState(0);
  const ctxRef = useRef<AudioContext | null>(null);
  const tickSound = (accent: boolean) => {
    try {
      ctxRef.current = ctxRef.current || new AudioContext();
      const ctx = ctxRef.current; if (ctx.state === 'suspended') void ctx.resume();
      const o = ctx.createOscillator(); const g = ctx.createGain();
      o.frequency.value = accent ? 1200 : 800; g.gain.setValueAtTime(0.3, ctx.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.06);
      o.connect(g); g.connect(ctx.destination); o.start(); o.stop(ctx.currentTime + 0.07);
    } catch { /* ses yok */ }
  };
  useEffect(() => {
    if (!run) return;
    let b = 0;
    const t = setInterval(() => { tickSound(b % 4 === 0); setBeat(b % 4); b++; }, 60000 / bpm);
    return () => clearInterval(t);
  }, [run, bpm]);
  return (
    <AppWindow title="Metronom" accentColor="#FF9F0A" onBack={onBack}>
      <div className="flex flex-col items-center justify-center h-full gap-7">
        <div className="flex gap-3">
          {[0, 1, 2, 3].map(i => <div key={i} className="rounded-full" style={{ width: 18, height: 18, background: run && beat === i ? '#FF9F0A' : '#2c2c2e', transition: 'background 0.05s' }} />)}
        </div>
        <div className="text-white font-light" style={{ fontSize: 72 }}>{bpm}<span className="text-lg" style={{ color: 'rgba(235,235,245,0.4)' }}> BPM</span></div>
        <input type="range" min={40} max={220} value={bpm} onChange={e => setBpm(+e.target.value)} style={{ width: 220, accentColor: '#FF9F0A' }} />
        <button onClick={() => setRun(!run)} className="px-10 py-3 rounded-full font-semibold" style={{ background: run ? '#5c3a0a' : '#1d4a2a', color: run ? '#FF9F0A' : '#30D158' }}>{run ? 'Durdur' : 'Başlat'}</button>
      </div>
    </AppWindow>
  );
}

/* ============ KAYIT DEFTERİ 2 ============ */
export const MINI_APPS_2: Record<string, React.ComponentType<MiniProps>> = {
  tkm: TasKagitMakas,
  reaksiyon: Reaksiyon,
  kostebek: Kostebek,
  tugla: TuglaKir,
  mayin: MayinTarlasi,
  tetris: Tetris,
  birim: BirimCevirici,
  fener: Fener,
  kure: SihirliKure,
  metronom: Metronom,
};
