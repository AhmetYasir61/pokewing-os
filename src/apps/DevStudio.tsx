import React, { useEffect, useState } from 'react';
import { AppWindow } from '../components/AppWindow';
import { hasBridge, bridge, publishAppBridge, deleteAppBridge, getServerApps } from '../bridge';
import { DEVELOPERS, Target, StoreApp, devApps, saveDevApp, deleteDevApp, install, isPcMode, cloudApps, setCloudApps } from '../appstore';
import { Code2, Rocket, Trash2, ShieldAlert, Smartphone, Monitor, Play } from 'lucide-react';

interface Props { onBack: () => void; toast: (t: string) => void; }

const TEMPLATE = `<!doctype html>
<html><head><meta charset="utf-8"><style>
  body{margin:0;background:#101014;color:#fff;font-family:system-ui;display:flex;
       flex-direction:column;align-items:center;justify-content:center;height:100vh;gap:12px}
  button{background:#0A84FF;color:#fff;border:none;border-radius:10px;padding:10px 22px;font-size:15px}
</style></head><body>
  <h2>Merhaba PokeWing! 👋</h2>
  <p id="s">0 tık</p>
  <button onclick="document.getElementById('s').textContent=(++n)+' tık'">Tıkla</button>
  <script>let n=0;</script>
</body></html>`;

export function DevStudio({ onBack, toast }: Props) {
  const [user, setUser] = useState<string | null>(null);
  const [checked, setChecked] = useState(false);
  const [apps, setApps] = useState<StoreApp[]>(devApps);
  const [name, setName] = useState('');
  const [emoji, setEmoji] = useState('🚀');
  const [desc, setDesc] = useState('');
  const [html, setHtml] = useState(TEMPLATE);
  const [targets, setTargets] = useState<Target[]>(['phone', 'pc']);
  const [preview, setPreview] = useState(false);

  // Giriş: oyun içindeki KENDİ kullanıcı adınla (köprüden) — yetki listesiyle karşılaştırılır
  useEffect(() => {
    if (!hasBridge()) { setUser('Önizleme'); setChecked(true); return; }
    bridge<{ name?: string }>('me').then(m => { setUser(m?.name ?? null); setChecked(true); });
    // Sunucu kataloğunu tazele (uygulamalarım listesi güncel olsun)
    getServerApps().then(c => { if (c) { setCloudApps(c.apps); setApps(myApps()); } });
  }, []);

  // Uygulamalarım: oyun içinde = sunucudaki yayınlarım; önizlemede = yerel taslaklar
  function myApps(): StoreApp[] {
    if (!hasBridge()) return devApps();
    const me = (user ?? '').toLowerCase();
    return cloudApps().filter(a => (a.author ?? '').toLowerCase() === me || me === '');
  }

  const authorized = !hasBridge() || (user !== null && DEVELOPERS.some(d => d.toLowerCase() === user.toLowerCase()));

  if (!isPcMode()) {
    return (
      <AppWindow title="Dev Studio" accentColor="#5E5CE6" onBack={onBack}>
        <div className="flex flex-col items-center justify-center h-full gap-3 px-8">
          <Monitor size={40} color="rgba(235,235,245,0.25)" />
          <div className="text-white font-semibold text-lg">Sadece PC'de çalışır</div>
          <div className="text-center text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>
            Uygulama geliştirmek için Rotom PC'ni kullan.
          </div>
        </div>
      </AppWindow>
    );
  }

  if (checked && !authorized) {
    return (
      <AppWindow title="Dev Studio" accentColor="#5E5CE6" onBack={onBack}>
        <div className="flex flex-col items-center justify-center h-full gap-3 px-8">
          <ShieldAlert size={40} color="#FF453A" />
          <div className="text-white font-semibold text-lg">Erişim reddedildi</div>
          <div className="text-center text-sm" style={{ color: 'rgba(235,235,245,0.5)' }}>
            Bu uygulama yalnız <b>owner ve geliştiriciler</b> içindir.{'\n'}Giriş yapılan hesap: <b style={{ color: '#FF9F0A' }}>{user ?? 'bilinmiyor'}</b>
          </div>
        </div>
      </AppWindow>
    );
  }

  const publish = async () => {
    const n = name.trim();
    if (n.length < 2) { toast('Uygulama adı gir.'); return; }
    if (targets.length === 0) { toast('En az bir hedef seç (Telefon/PC).'); return; }
    if (html.trim().length < 20) { toast('Uygulama kodu çok kısa.'); return; }
    if (html.length > 120000) { toast('Kod çok büyük (sınır ~120 KB).'); return; }
    const id = 'dev:' + n.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 20);
    const app: StoreApp = {
      id, name: n, emoji: emoji || '🚀', color: '#5E5CE6',
      desc: desc.trim() || `${user} tarafından geliştirildi`,
      category: 'Geliştirici', targets: [...targets],
      size: (html.length / 1024).toFixed(1) + ' KB', custom: true, html, author: user ?? '',
    };
    if (hasBridge()) {
      // SUNUCUYA yayınla → tüm oyunculara dağıtılır (sunucu yetkiyi ayrıca doğrular)
      const r = await publishAppBridge(app);
      if (!r) { toast('Sunucuya ulaşılamadı.'); return; }
      if (r.error) { toast(r.error); return; }
      setCloudApps(r.apps);
      install(id);
      setApps(myApps());
      toast(`🚀 "${n}" yayınlandı — TÜM oyunculara dağıtıldı!`);
    } else {
      saveDevApp(app);
      install(id);
      setApps(devApps());
      toast(`🚀 "${n}" yayınlandı (önizleme — yerel).`);
    }
  };

  const tgl = (t: Target) => setTargets(x => x.includes(t) ? x.filter(v => v !== t) : [...x, t]);

  return (
    <AppWindow title="Dev Studio" accentColor="#5E5CE6" onBack={onBack}
      headerRight={<span className="text-[11px] px-2 py-1 rounded-full" style={{ background: 'rgba(94,92,230,0.18)', color: '#9a98ff' }}>👨‍💻 {user}</span>}>
      <div className="flex h-full">
        {/* Sol: editör */}
        <div className="flex-1 flex flex-col min-w-0 p-3 gap-2">
          <div className="flex gap-2">
            <input value={emoji} onChange={e => setEmoji(e.target.value.slice(0, 4))} placeholder="🚀" style={{ width: 56, textAlign: 'center' }} />
            <input value={name} onChange={e => setName(e.target.value)} placeholder="Uygulama adı" style={{ flex: 1 }} />
          </div>
          <input value={desc} onChange={e => setDesc(e.target.value)} placeholder="Kısa açıklama (Play Store'da görünür)" />
          {/* Hedef seçimi: PC / Telefon / ikisi */}
          <div className="flex items-center gap-2 text-[13px] text-white">
            <span style={{ color: 'rgba(235,235,245,0.5)' }}>Hedef:</span>
            <button onClick={() => tgl('phone')} className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
              style={{ background: targets.includes('phone') ? '#30D158' : '#2c2c2e', color: targets.includes('phone') ? '#003312' : '#999' }}>
              <Smartphone size={13} /> Telefon
            </button>
            <button onClick={() => tgl('pc')} className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
              style={{ background: targets.includes('pc') ? '#0A84FF' : '#2c2c2e', color: targets.includes('pc') ? '#fff' : '#999' }}>
              <Monitor size={13} /> PC
            </button>
          </div>
          {preview ? (
            <iframe title="önizleme" sandbox="allow-scripts" srcDoc={html}
              className="flex-1 rounded-xl" style={{ border: '1px solid rgba(255,255,255,0.1)', background: '#101014' }} />
          ) : (
            <textarea value={html} onChange={e => setHtml(e.target.value)} spellCheck={false}
              className="flex-1 rounded-xl p-3 outline-none"
              style={{ background: '#0d1117', color: '#9fd0ff', fontFamily: 'Consolas,monospace', fontSize: 12.5, border: '1px solid rgba(255,255,255,0.08)', resize: 'none', lineHeight: 1.5 }} />
          )}
          <div className="flex gap-2">
            <button className="pill-btn ghost flex-1 flex items-center justify-center gap-2" onClick={() => setPreview(p => !p)}>
              {preview ? <><Code2 size={15} /> Kod</> : <><Play size={15} /> Önizle</>}
            </button>
            <button className="pill-btn flex-1 flex items-center justify-center gap-2"
              style={{ background: '#5E5CE6', color: '#fff', fontWeight: 700 }} onClick={publish}>
              <Rocket size={15} /> Yayınla
            </button>
          </div>
        </div>
        {/* Sağ: yayınlanan uygulamalar */}
        <div className="w-48 flex-none p-3 flex flex-col gap-2" style={{ borderLeft: '0.5px solid rgba(255,255,255,0.08)' }}>
          <div className="text-[11px] font-semibold uppercase" style={{ color: 'rgba(235,235,245,0.45)', letterSpacing: 1 }}>Uygulamaların</div>
          <div className="flex-1 scroll-area flex flex-col gap-2">
            {apps.length === 0 && <div className="text-[12px] mt-4 text-center" style={{ color: 'rgba(235,235,245,0.3)' }}>Henüz yayın yok</div>}
            {apps.map(a => (
              <div key={a.id} className="rounded-xl p-2.5 flex items-center gap-2" style={{ background: 'rgba(28,28,30,0.9)' }}>
                <span style={{ fontSize: 20 }}>{a.emoji}</span>
                <div className="flex-1 min-w-0">
                  <div className="text-white text-[12.5px] font-semibold truncate">{a.name}</div>
                  <div className="text-[10px]" style={{ color: 'rgba(235,235,245,0.4)' }}>
                    {a.targets.includes('phone') && '📱'}{a.targets.includes('pc') && '🖥️'} {a.size}
                  </div>
                </div>
                <button onClick={async () => {
                  if (hasBridge()) {
                    const r = await deleteAppBridge(a.id);
                    if (r?.error) { toast(r.error); return; }
                    if (r) setCloudApps(r.apps);
                    setApps(myApps());
                  } else {
                    deleteDevApp(a.id);
                    setApps(devApps());
                  }
                  toast('Uygulama kaldırıldı.');
                }}
                  className="p-1 rounded hover:bg-red-600/30"><Trash2 size={13} color="#FF453A" /></button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </AppWindow>
  );
}

/** Play Store'dan kurulan geliştirici uygulamasını çalıştırır (sandbox iframe). */
export function CustomAppViewer({ app, onBack }: { app: StoreApp; onBack: () => void }) {
  return (
    <AppWindow title={`${app.emoji} ${app.name}`} accentColor="#5E5CE6" onBack={onBack}>
      <iframe title={app.name} sandbox="allow-scripts" srcDoc={app.html || '<p style="color:#fff">Boş uygulama</p>'}
        className="w-full h-full" style={{ border: 'none', background: '#101014' }} />
    </AppWindow>
  );
}
