package com.pokewing.efmocap;

import java.util.List;

/** Scene menu: the recorded takes, how they're cast, and when each actor dies. */
public class TakesScreen extends StudioScreen {
    private static String sel = "";

    public TakesScreen() { super("Çekimler & sahne"); }

    @Override protected String listTitle() { return "ÇEKİMLER"; }
    @Override protected int pageIndex() { return 0; }
    @Override protected String highlightedTake() { return sel; }

    @Override
    protected void buildContent() {
        TakeLibrary lib = TakeLibrary.INSTANCE;
        List<String> names = lib.names();
        if ((sel.isEmpty() || lib.get(sel) == null) && !names.isEmpty()) sel = names.get(0);

        int y = contentY + 20;
        for (String n : names) {
            if (y > tlY - 24) break;
            final String name = n;
            row(listX + 4, y, listW - 8, n, null, sel.equals(n), () -> { sel = name; rebuild(); });
            y += 20;
        }

        int px = propX + 8, py = contentY + 22;

        btn(px, py, 130, "▶ Sahneyi kur", "Bütün çekimleri birlikte oynat", () -> {
            ReplayDirector.INSTANCE.setPaused(false);
            int n = ReplayDirector.INSTANCE.playScene(true);
            status = n > 0 ? "§a" + n + " oyuncu sahnede" : "§cçekim yok";
        });
        btn(px + 136, py, 110, "Klonları kaldır", null, () -> {
            ReplayDirector.INSTANCE.clearAll(); status = "§etemizlendi";
        });

        MocapRecording r = lib.get(sel);
        if (r == null) {
            py += 30;
            zones.add(new Zone(px, py, propW - 16, 18,
                    "§8Kayıt için oyunda K tuşuna bas", null, () -> {}, Zone.ROW, false));
            return;
        }

        py += 30;
        zones.add(new Zone(px, py, propW - 16, 18,
                "§7" + r.name + "  §8· " + r.length() + " kare · " + Theme.time(r.length()),
                null, () -> {}, Zone.ROW, false));

        py += 26;
        btn(px, py, 130, "Yalnız bunu oynat", null, () -> {
            ReplayDirector.INSTANCE.setPaused(false);
            ReplayDirector.INSTANCE.play(r, true);
            status = "§a'" + r.name + "' oynuyor";
        });
        btn(px + 136, py, 150, "Karakter: "
                + (r.character == null || r.character.isEmpty() ? "varsayılan" : r.character),
                "Bu çekimi başka bir karaktere aktar", () -> {
            List<String> cs = CharacterLibrary.INSTANCE.names();
            if (cs.isEmpty()) { status = "§cönce karakter oluştur"; return; }
            int i = cs.indexOf(r.character) + 1;
            r.character = i >= cs.size() ? "" : cs.get(i);
            lib.save(r);
            rebuild();
        });

        py += 26;
        boolean hasDeath = r.deathTick >= 0;
        toggle(px, py, 150, hasDeath ? "☠ Ölüm: " + Theme.time(r.deathTick) : "Ölüm işaretle",
                hasDeath, () -> {
            if (r.deathTick >= 0) { r.deathTick = -1; status = "§7ölüm kaldırıldı"; }
            else {
                int t = ReplayDirector.INSTANCE.currentTickOf(r);
                r.deathTick = t >= 0 ? t : r.length() - 1;
                status = "§c" + Theme.time(r.deathTick) + " anında ölüyor";
            }
            lib.save(r);
            rebuild();
        });
        btn(px + 156, py, 130, "Kafanın olduğu ana",
                "Ölümü zaman çizgisindeki oynatma kafasına taşı", () -> {
            // deathTick is measured from the take's own start, not the scene's.
            r.deathTick = Math.max(0, ReplayDirector.INSTANCE.sceneTick() - r.startOffset);
            lib.save(r);
            status = "§cölüm " + Theme.time(r.deathTick);
            rebuild();
        });

        py += 26;
        // Slot a separately-recorded take into the scene without redoing it.
        btn(px, py, 150, "Giriş: " + Theme.time(r.startOffset),
                "Bu aktör sahneye ne kadar sonra girsin", () -> {});
        btn(px + 156, py, 22, "−", null, () -> {
            r.startOffset = Math.max(0, r.startOffset - 10); lib.save(r); rebuild();
        });
        btn(px + 180, py, 22, "+", null, () -> {
            r.startOffset += 10; lib.save(r); rebuild();
        });
        btn(px + 206, py, 130, "Kafanın olduğu ana",
                "Girişi oynatma kafasına taşı", () -> {
            r.startOffset = Math.max(0, ReplayDirector.INSTANCE.sceneTick());
            lib.save(r); status = "§bgiriş " + Theme.time(r.startOffset); rebuild();
        });

        py += 30;
        danger(px, py, 130, "Çekimi sil", () -> {
            lib.remove(sel); sel = ""; status = "§esilindi"; rebuild();
        });
    }
}
