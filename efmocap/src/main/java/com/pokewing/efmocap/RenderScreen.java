package com.pokewing.efmocap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Render menu: shoot the scene to disk, and the settings that affect a shoot. */
public class RenderScreen extends StudioScreen {
    private EditBox ffmpegBox;

    public RenderScreen() { super("Render & ayarlar"); }

    @Override protected String listTitle() { return ""; }
    @Override protected int pageIndex() { return 3; }
    @Override protected boolean usesList() { return false; }

    @Override
    protected void buildContent() {
        VideoRecorder vr = VideoRecorder.INSTANCE;
        int px = listX + 10, py = contentY + 24;

        section(px, py - 14, "ÇEKİM");
        toggle(px, py, 190, vr.isActive() ? "■ Kaydı durdur" : "● Video kaydını başlat",
                vr.isActive(), () -> {
            if (vr.isActive()) {
                double fps = vr.stop();
                status = "§a" + vr.frameCount() + " kare @ "
                        + String.format(Locale.ROOT, "%.1f", fps) + " fps — kodlanıyor";
            } else {
                status = vr.start() ? "§akayıt başladı (arayüz gizlendi)" : "§cbaşlatılamadı";
            }
            rebuild();
        });
        btn(px + 196, py, 200, "▶ Tam sinematik",
                "Sahneyi kurar, kamerayı uçurur ve videoyu kaydeder", () -> {
            if (CameraDirector.INSTANCE.keyCount() < 2) {
                status = "§cen az 2 kamera noktası gerek";
                return;
            }
            ReplayDirector.INSTANCE.setPaused(false);
            ReplayDirector.INSTANCE.playScene(true);
            VideoRecorder.INSTANCE.start();
            CameraDirector.INSTANCE.play();
            onClose();
        });

        py += 34;
        section(px, py - 14, "VİDEO");
        btn(px, py, 150, "FPS: " + (Settings.videoFps <= 0
                        ? (Settings.offlineRender ? "60 (varsayılan)" : "otomatik")
                        : String.valueOf((int) Settings.videoFps)),
                "Pürüzsüz renderde hedef hız; gerçek zamanlıda ölçülür", () -> {
            Settings.videoFps = switch ((int) Settings.videoFps) {
                case 0 -> 30; case 30 -> 60; case 60 -> 120; case 120 -> 24; default -> 0;
            };
            Settings.save(); rebuild();
        });
        btn(px + 156, py, 150, "Kalite: crf " + Settings.videoCrf,
                "Düşük = daha kaliteli, daha büyük dosya", () -> {
            Settings.videoCrf = switch (Settings.videoCrf) {
                case 18 -> 14; case 14 -> 23; case 23 -> 28; default -> 18;
            };
            Settings.save(); rebuild();
        });
        btn(px + 312, py, 130, "Çıktı klasörü", null,
                () -> net.minecraft.Util.getPlatform().openFile(
                        VideoRecorder.renderRoot().toFile()));

        py += 22;
        toggle(px, py, 306, Settings.offlineRender
                        ? "Pürüzsüz render (yavaş çeker, akıcı çıkar)"
                        : "Gerçek zamanlı çekim (hızlı ama takılabilir)",
                Settings.offlineRender, () -> {
            Settings.offlineRender = !Settings.offlineRender;
            Settings.save(); rebuild();
        });

        py += 34;
        section(px, py - 14, "FFMPEG");
        ffmpegBox = new EditBox(font, px, py, 306, 18, Component.literal("ffmpeg"));
        ffmpegBox.setMaxLength(512);
        ffmpegBox.setValue(Settings.ffmpegPath);
        ffmpegBox.setHint(Component.literal("ffmpeg yolu — exe ya da klasör (boş = otomatik ara)"));
        addRenderableWidget(ffmpegBox);
        btn(px + 312, py, 130, "Kaydet + test", null, () -> {
            Settings.ffmpegPath = ffmpegBox.getValue().trim();
            Settings.save();
            String found = VideoRecorder.INSTANCE.resolveFfmpeg();
            status = found != null ? "§affmpeg çalışıyor: " + found
                                   : "§cffmpeg bulunamadı — yolu kontrol et";
            if (found != null) ffmpegBox.setValue(Settings.ffmpegPath);
        });

        py += 34;
        section(px, py - 14, "SAHNE");
        btn(px, py, 190, "Çürüme: " + (Settings.decayTicks / 20) + " sn",
                "Ceset ne kadar sonra iskelete döner", () -> {
            Settings.decayTicks = switch (Settings.decayTicks) {
                case 200 -> 600; case 600 -> 1200; case 1200 -> 2400;
                case 2400 -> 100; default -> 200;
            };
            Settings.save(); rebuild();
        });
        btn(px + 196, py, 150, "Config klasörü", null,
                () -> net.minecraft.Util.getPlatform().openFile(
                        CharacterLibrary.root().toFile()));
    }

    // --- section captions ------------------------------------------------

    private final java.util.List<int[]> sections = new java.util.ArrayList<>();
    private final java.util.List<String> sectionLabels = new java.util.ArrayList<>();

    private void section(int x, int y, String label) {
        sections.add(new int[] {x, y});
        sectionLabels.add(label);
    }

    @Override
    protected void rebuild() {
        sections.clear();
        sectionLabels.clear();
        super.rebuild();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        for (int i = 0; i < sections.size(); i++) {
            int[] p = sections.get(i);
            g.drawString(font, "§8" + sectionLabels.get(i), p[0], p[1], Theme.TEXT_DIM, false);
        }
    }
}
