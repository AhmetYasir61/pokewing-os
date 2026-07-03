import React, { useEffect, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { MOCK_CONTACTS } from '../data';
import {
  hasBridge, getPhoneDirectory, vcDial, vcAnswer, vcHangup,
  getVcSettings, setVcSetting, VcSettingsData,
} from '../bridge';
import { itemId } from '../appstore';
import { Phone, PhoneOff, PhoneIncoming, Delete, Mic, Volume2, Settings2, Users, Grid3x3 } from 'lucide-react';

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

interface CallState { state: number; peerName: string; peerNumber: string; msg: string; }

function savedContacts(): { name: string; number: string }[] {
  if (!hasBridge()) return MOCK_CONTACTS.map(c => ({ name: c.name, number: c.number }));
  try { return JSON.parse(localStorage.getItem('pwcontacts:' + itemId()) || 'null') || []; } catch { return []; }
}

export function TelefonApp({ dialNumber, onBack, onDialAppend, onDialDelete, onDialClear, onToast }: Props) {
  const [tab, setTab] = useState<'tus' | 'rehber' | 'ayar'>('tus');
  const [myNumber, setMyNumber] = useState('');
  const [call, setCall] = useState<CallState | null>(null);
  const [vc, setVc] = useState<VcSettingsData | null>(null);
  const [picker, setPicker] = useState<'mic' | 'spk' | null>(null);   // özel cihaz seçici (native select MCEF'te minicik kalıyor)
  const contacts = savedContacts();

  // Numaram (sunucudan) + ses ayarları
  useEffect(() => {
    if (!hasBridge()) { setMyNumber('5550001'); return; }
    getPhoneDirectory().then(d => { if (d?.myNumber) setMyNumber(d.myNumber); });
    getVcSettings().then(s => { if (s) setVc(s); });
  }, []);

  // Arama durumu (mod'dan push): 1=aranıyor 2=gelen 3=görüşmede 4=bitti
  useEffect(() => {
    const h = (e: Event) => {
      const d = (e as CustomEvent).detail as CallState;
      if (!d) return;
      if (d.state === 4) {
        setCall(d);
        if (d.msg) onToast(d.msg);
        setTimeout(() => setCall(c => (c && c.state === 4 ? null : c)), 1800);
      } else setCall(d);
    };
    window.addEventListener('pwcall', h);
    return () => window.removeEventListener('pwcall', h);
  }, []);

  const fmt = (num: string) => {
    if (num.length <= 3) return num;
    if (num.length <= 6) return `${num.slice(0, 3)} ${num.slice(3)}`;
    return `${num.slice(0, 3)} ${num.slice(3, 6)} ${num.slice(6)}`;
  };

  const doCall = (number: string) => {
    if (number.length < 3) { onToast('Geçersiz numara'); return; }
    if (hasBridge()) {
      vcDial(number);   // GERÇEK arama — sunucu VC sistemi
      setCall({ state: 1, peerName: '', peerNumber: number, msg: 'Aranıyor...' });
    } else {
      onToast(`${fmt(number)} aranıyor... (önizleme)`);
    }
    onDialClear();
  };

  const applyVc = async (key: string, val: number | boolean) => {
    if (!hasBridge()) { onToast('Önizlemede ayar kaydedilmez.'); return; }
    const s = await setVcSetting(key, val);
    if (s) setVc(s);
  };

  // ---------- ARAMA EKRANI (overlay) ----------
  if (call && call.state !== 4) {
    const incoming = call.state === 2;
    return (
      <AppWindow title="" onBack={onBack} noHeader>
        <div className="absolute inset-0 flex flex-col items-center justify-between py-14"
          style={{ background: 'linear-gradient(180deg,#0a2a1a,#050a08)' }}>
          <div className="flex flex-col items-center gap-3 mt-6">
            <div className="w-24 h-24 rounded-full flex items-center justify-center font-bold text-white text-4xl"
              style={{ background: `hsl(${(call.peerName || '?').charCodeAt(0) * 20},55%,40%)`, boxShadow: '0 0 0 10px rgba(48,209,88,0.12)' }}>
              {(call.peerName || call.peerNumber || '?')[0]}
            </div>
            <div className="text-white font-bold text-2xl">{call.peerName || fmt(call.peerNumber)}</div>
            <div className="text-[14px]" style={{ color: '#30D158' }}>
              {incoming ? '📞 Gelen arama...' : call.state === 3 ? 'Görüşme sürüyor' : 'Aranıyor...'}
            </div>
          </div>
          <div className="flex gap-10">
            {incoming && (
              <button onClick={() => vcAnswer(true)}
                className="rounded-full flex items-center justify-center animate-pulse"
                style={{ width: 72, height: 72, background: '#30D158' }}>
                <PhoneIncoming size={30} color="#fff" />
              </button>
            )}
            <button onClick={() => { if (incoming) vcAnswer(false); else vcHangup(); setCall(null); }}
              className="rounded-full flex items-center justify-center"
              style={{ width: 72, height: 72, background: '#FF453A' }}>
              <PhoneOff size={30} color="#fff" />
            </button>
          </div>
        </div>
      </AppWindow>
    );
  }

  return (
    <AppWindow title="Telefon" accentColor="#30D158" onBack={onBack}>
      <div className="flex flex-col h-full">
        {/* NUMARAM — herkes numaranı buradan öğrenir */}
        <div className="mx-4 mt-2 mb-1 rounded-2xl px-4 py-3 flex items-center gap-3"
          style={{ background: 'linear-gradient(90deg,rgba(48,209,88,0.12),rgba(48,209,88,0.04))', border: '0.5px solid rgba(48,209,88,0.25)' }}>
          <Phone size={18} color="#30D158" />
          <div className="flex-1">
            <div className="text-[10.5px] uppercase font-semibold" style={{ color: 'rgba(235,235,245,0.5)', letterSpacing: 1 }}>Numaram</div>
            <div className="text-white font-bold text-[19px]" style={{ fontVariantNumeric: 'tabular-nums', letterSpacing: 1 }}>
              {myNumber ? fmt(myNumber) : '...'}
            </div>
          </div>
          <span className="text-[10.5px] px-2 py-1 rounded-full" style={{ background: 'rgba(48,209,88,0.15)', color: '#30D158' }}>
            arkadaşlarına ver
          </span>
        </div>

        {/* Sekmeler */}
        <div className="px-4 py-2">
          <div className="seg-ctrl">
            <button className={`seg-btn ${tab === 'tus' ? 'active' : ''}`} onClick={() => setTab('tus')}><Grid3x3 size={13} style={{ display: 'inline', marginRight: 4 }} />Tuşlar</button>
            <button className={`seg-btn ${tab === 'rehber' ? 'active' : ''}`} onClick={() => setTab('rehber')}><Users size={13} style={{ display: 'inline', marginRight: 4 }} />Rehber</button>
            <button className={`seg-btn ${tab === 'ayar' ? 'active' : ''}`} onClick={() => setTab('ayar')}><Settings2 size={13} style={{ display: 'inline', marginRight: 4 }} />Ayarlar</button>
          </div>
        </div>

        {/* ---- TUŞ TAKIMI ---- */}
        {tab === 'tus' && (
          <div className="flex-1 flex flex-col justify-end pb-6">
            <div className="text-center text-white font-light mb-4" style={{ fontSize: 34, minHeight: 44, fontVariantNumeric: 'tabular-nums' }}>
              {fmt(dialNumber) || <span style={{ color: 'rgba(235,235,245,0.25)', fontSize: 17 }}>Numara gir</span>}
            </div>
            <div className="grid grid-cols-3 gap-3 px-10 justify-items-center">
              {KEYS.map(([d, sub]) => (
                <button key={d} className="dial-key" onClick={() => onDialAppend(d)}>
                  <span className="text-white text-[26px] font-medium leading-none">{d}</span>
                  {sub && <span className="text-[9px] mt-0.5" style={{ color: 'rgba(235,235,245,0.45)', letterSpacing: 1 }}>{sub}</span>}
                </button>
              ))}
            </div>
            <div className="flex items-center justify-center gap-8 mt-4">
              <div style={{ width: 44 }} />
              <button onClick={() => doCall(dialNumber)}
                className="w-16 h-16 rounded-full flex items-center justify-center active:scale-90 transition-transform"
                style={{ background: '#30D158', boxShadow: '0 8px 24px rgba(48,209,88,0.4)' }}>
                <Phone size={26} color="#fff" />
              </button>
              <button onClick={onDialDelete} className="w-11 h-11 flex items-center justify-center">
                <Delete size={22} color="rgba(235,235,245,0.6)" />
              </button>
            </div>
          </div>
        )}

        {/* ---- REHBER (kayıtlı kişiler — PokeChat ile ortak) ---- */}
        {tab === 'rehber' && (
          <div className="flex-1 scroll-area px-4 pb-4">
            {contacts.length === 0 && (
              <div className="text-center mt-12 text-sm px-6" style={{ color: 'rgba(235,235,245,0.45)' }}>
                Rehber boş. Kişi eklemek için <b>Mesajlar</b> uygulamasındaki <b>+</b> butonunu kullan (numarayla eklenir).
              </div>
            )}
            {contacts.map(c => (
              <div key={c.number || c.name} className="flex items-center gap-3 py-2.5"
                style={{ borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}>
                <div className="w-11 h-11 rounded-full flex items-center justify-center font-bold text-white flex-shrink-0"
                  style={{ background: `hsl(${c.name.charCodeAt(0) * 20},55%,40%)` }}>{c.name[0]}</div>
                <div className="flex-1 min-w-0">
                  <div className="text-white font-semibold text-[15px]">{c.name}</div>
                  <div className="text-[12.5px]" style={{ color: 'rgba(235,235,245,0.5)', fontVariantNumeric: 'tabular-nums' }}>{fmt(c.number)}</div>
                </div>
                <button onClick={() => doCall(c.number)}
                  className="w-10 h-10 rounded-full flex items-center justify-center active:scale-90"
                  style={{ background: 'rgba(48,209,88,0.15)' }}>
                  <Phone size={17} color="#30D158" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* ---- AYARLAR (mikrofon/kulaklık/cızırtı önleyici) ---- */}
        {tab === 'ayar' && (
          <div className="flex-1 scroll-area px-4 pb-5 flex flex-col gap-3">
            {!vc && hasBridge() && <div className="flex justify-center py-8"><div className="spinner" /></div>}
            {!hasBridge() && (
              <div className="text-center text-[12px] py-2 rounded-xl" style={{ background: 'rgba(255,159,10,0.1)', color: '#FF9F0A' }}>
                Önizleme modu — ayarlar oyun içinde kaydedilir
              </div>
            )}
            {(vc || !hasBridge()) && (() => {
              const s: VcSettingsData = vc ?? { micVol: 100, spkVol: 100, noiseGate: true, echoCancel: false, proximity: true, micIdx: 0, spkIdx: 0, inputs: ['Varsayılan'], outputs: ['Varsayılan'] };
              return (
                <>
                  {/* Cihazlar — özel seçici (native <select> MCEF'te minicik kalıyor) */}
                  <div className="rounded-2xl p-4 flex flex-col gap-3" style={{ background: 'rgba(28,28,30,0.9)' }}>
                    <div className="flex items-center gap-2 text-white font-semibold text-[14px]"><Mic size={15} color="#30D158" /> Mikrofon</div>
                    <button onClick={() => setPicker('mic')}
                      className="flex items-center gap-2 rounded-xl px-3.5 py-3 text-left"
                      style={{ background: '#2c2c2e' }}>
                      <span className="flex-1 text-white text-[14px] truncate">{s.inputs[s.micIdx] ?? 'Varsayılan'}</span>
                      <span style={{ color: 'rgba(235,235,245,0.4)', fontSize: 12 }}>değiştir ›</span>
                    </button>
                    <div className="flex items-center gap-3">
                      <span className="text-[12.5px] w-16" style={{ color: 'rgba(235,235,245,0.55)' }}>Ses %{s.micVol}</span>
                      <input type="range" min={0} max={100} value={s.micVol} className="flex-1" style={{ height: 26 }}
                        onChange={e => setVc(v => v ? { ...v, micVol: +e.target.value } : v)}
                        onMouseUp={e => applyVc('micVol', +(e.target as HTMLInputElement).value)}
                        onTouchEnd={e => applyVc('micVol', +(e.target as HTMLInputElement).value)} />
                    </div>
                    <div className="flex items-center gap-2 text-white font-semibold text-[14px] mt-1"><Volume2 size={15} color="#0A84FF" /> Kulaklık / Hoparlör</div>
                    <button onClick={() => setPicker('spk')}
                      className="flex items-center gap-2 rounded-xl px-3.5 py-3 text-left"
                      style={{ background: '#2c2c2e' }}>
                      <span className="flex-1 text-white text-[14px] truncate">{s.outputs[s.spkIdx] ?? 'Varsayılan'}</span>
                      <span style={{ color: 'rgba(235,235,245,0.4)', fontSize: 12 }}>değiştir ›</span>
                    </button>
                    <div className="flex items-center gap-3">
                      <span className="text-[12.5px] w-16" style={{ color: 'rgba(235,235,245,0.55)' }}>Ses %{s.spkVol}</span>
                      <input type="range" min={0} max={100} value={s.spkVol} className="flex-1" style={{ height: 26 }}
                        onChange={e => setVc(v => v ? { ...v, spkVol: +e.target.value } : v)}
                        onMouseUp={e => applyVc('spkVol', +(e.target as HTMLInputElement).value)}
                        onTouchEnd={e => applyVc('spkVol', +(e.target as HTMLInputElement).value)} />
                    </div>
                  </div>

                  {/* Büyük cihaz seçici sayfası (alttan açılır — okunaklı) */}
                  {picker && (
                    <div className="absolute inset-0 z-20 flex items-end anim-fadein"
                      style={{ background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)' }}
                      onClick={() => setPicker(null)}>
                      <div className="w-full anim-slideup rounded-t-3xl p-5 pb-7" style={{ background: 'rgb(18,18,22)', maxHeight: '80%' }}
                        onClick={e => e.stopPropagation()}>
                        <div className="w-10 h-1 rounded-full mx-auto mb-4" style={{ background: 'rgba(255,255,255,0.2)' }} />
                        <div className="text-white font-bold text-lg mb-3 flex items-center gap-2">
                          {picker === 'mic' ? <><Mic size={18} color="#30D158" /> Mikrofon Seç</> : <><Volume2 size={18} color="#0A84FF" /> Çıkış Cihazı Seç</>}
                        </div>
                        <div className="scroll-area flex flex-col gap-1.5" style={{ maxHeight: 340 }}>
                          {(picker === 'mic' ? s.inputs : s.outputs).map((n, i) => {
                            const sel = (picker === 'mic' ? s.micIdx : s.spkIdx) === i;
                            return (
                              <button key={i}
                                onClick={() => { applyVc(picker === 'mic' ? 'micIdx' : 'spkIdx', i); setPicker(null); }}
                                className="flex items-center gap-3 rounded-xl px-4 py-3.5 text-left"
                                style={{ background: sel ? (picker === 'mic' ? 'rgba(48,209,88,0.18)' : 'rgba(10,132,255,0.18)') : 'rgba(44,44,46,0.8)' }}>
                                <span className="flex-1 text-white text-[14.5px]">{n}</span>
                                {sel && <span style={{ color: picker === 'mic' ? '#30D158' : '#0A84FF', fontSize: 18 }}>✓</span>}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  )}
                  {/* Anahtarlar */}
                  <div className="rounded-2xl overflow-hidden" style={{ background: 'rgba(28,28,30,0.9)' }}>
                    {[
                      { k: 'noiseGate', t: 'Cızırtı Önleyici', d: 'Arka plan gürültüsünü temizler', v: s.noiseGate },
                      { k: 'echoCancel', t: 'Yankı Engelleme', d: 'Hoparlör yankısını keser', v: s.echoCancel },
                      { k: 'proximity', t: 'Yakınlık Sesi', d: 'Yakındaki oyuncuları duy', v: s.proximity },
                    ].map(row => (
                      <div key={row.k} className="flex items-center gap-3 px-4 py-3" style={{ borderBottom: '0.5px solid rgba(255,255,255,0.06)' }}>
                        <div className="flex-1">
                          <div className="text-white text-[14px] font-medium">{row.t}</div>
                          <div className="text-[11.5px]" style={{ color: 'rgba(235,235,245,0.45)' }}>{row.d}</div>
                        </div>
                        <div className={`toggle ${row.v ? 'on' : ''}`} onClick={() => applyVc(row.k, !row.v)}>
                          <div className="toggle-knob" />
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="text-center text-[11px]" style={{ color: 'rgba(235,235,245,0.35)' }}>
                    Bas-konuş (yakınlık): oyunda <b>V</b> tuşu · Ayarlar oyun dosyana kaydedilir
                  </div>
                </>
              );
            })()}
          </div>
        )}
      </div>
    </AppWindow>
  );
}
