package com.pokewing.efmocap;

import java.util.Locale;

/**
 * Camera menu: lay out the shot. Stand where you want the camera, look at the
 * subject, drop a point — the camera glides through the points on playback.
 */
public class CameraScreen extends StudioScreen {
    private static int sel = -1;

    public CameraScreen() { super("Kamera"); }

    @Override protected String listTitle() { return "KAMERA NOKTALARI"; }
    @Override protected int railIndex() { return 2; }

    @Override
    protected void buildContent() {
        CameraDirector cam = CameraDirector.INSTANCE;
        int count = cam.path().size();
        if (sel >= count) sel = count - 1;

        int y = contentY + 20;
        for (int i = 0; i < count; i++) {
            if (y > tlY - 24) break;
            final int idx = i;
            CameraKeyframe k = cam.path().keys.get(i);
            String label = String.format(Locale.ROOT, "%2d  %.0f %.0f %.0f", i + 1, k.x, k.y, k.z);
            row(listX + 4, y, listW - 8, label, null, sel == i, () -> { sel = idx; rebuild(); });
            y += 20;
        }

        int px = propX + 8, py = contentY + 22;

        btn(px, py, 170, "+ Nokta ekle (durduğun yer)",
                "Konumun ve bakışın kamera noktası olur", () -> {
            cam.addKeyframeHere();
            sel = cam.keyCount() - 1;
            status = "§bnokta " + cam.keyCount() + " eklendi";
            rebuild();
        });
        btn(px + 176, py, 110, "Son noktayı sil", null, () -> {
            cam.removeLast(); rebuild();
        });
        danger(propX + propW - 96, py, 88, "Yolu temizle", () -> {
            cam.clearPath(); sel = -1; rebuild();
        });

        py += 26;
        btn(px, py, 170, "▶ Sinematiği oynat",
                "Sahneyi başa sarar ve kamerayı uçurur", () -> {
            if (cam.keyCount() < 2) { status = "§cen az 2 nokta gerek"; return; }
            ReplayDirector.INSTANCE.setPaused(false);
            if (ReplayDirector.INSTANCE.activeCount() == 0) ReplayDirector.INSTANCE.playScene(true);
            else ReplayDirector.INSTANCE.restart();
            cam.play();
            onClose();
        });
        btn(px + 176, py, 110, "Varsayılan geçiş: " + cam.segmentTicks + "t",
                "Yeni noktaların süresi", () -> {
            cam.segmentTicks = switch (cam.segmentTicks) {
                case 20 -> 40; case 40 -> 60; case 60 -> 100; case 100 -> 20; default -> 40;
            };
            rebuild();
        });

        py += 26;
        zones.add(new Zone(px, py, propW - 16, 18,
                "§8Toplam " + count + " nokta · " + Theme.time(cam.path().durationTicks()),
                null, () -> {}, Zone.ROW, false));

        if (sel < 0 || sel >= count) return;
        CameraKeyframe k = cam.path().keys.get(sel);

        py += 28;
        btn(px, py, 130, "Bu noktaya git", "Kendini bu noktaya ışınla", () -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.setPos(k.x, k.y - 1.62, k.z);
                minecraft.player.setYRot(k.yaw);
                minecraft.player.setXRot(k.pitch);
                status = "§bnokta " + (sel + 1) + "'e gidildi";
            }
        });
        btn(px + 136, py, 150, "Buradan güncelle",
                "Noktayı şu anki konum/bakışa taşı", () -> {
            if (minecraft != null && minecraft.player != null) {
                var p = minecraft.player;
                k.x = p.getX(); k.y = p.getEyeY(); k.z = p.getZ();
                k.yaw = p.getYRot(); k.pitch = p.getXRot();
                cam.save();
                status = "§bnokta " + (sel + 1) + " güncellendi";
                rebuild();
            }
        });

        py += 24;
        btn(px, py, 130, "Süre: " + k.travelTicks + "t",
                "Önceki noktadan buraya geçiş süresi", () -> {
            k.travelTicks = switch (k.travelTicks) {
                case 20 -> 40; case 40 -> 60; case 60 -> 100; case 100 -> 20; default -> 40;
            };
            cam.save(); rebuild();
        });
        btn(px + 136, py, 110, String.format(Locale.ROOT, "FOV: %.0f", k.fov),
                "Düşük FOV = yakınlaştırılmış görüntü", () -> {
            k.fov = k.fov >= 110 ? 30 : k.fov + 10;
            cam.save(); rebuild();
        });
        btn(px + 252, py, 110, String.format(Locale.ROOT, "Yatım: %.0f°", k.roll),
                "Kamerayı yana yatır", () -> {
            k.roll = k.roll >= 20 ? -20 : k.roll + 5;
            cam.save(); rebuild();
        });
    }
}
