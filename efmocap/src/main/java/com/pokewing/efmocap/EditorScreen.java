package com.pokewing.efmocap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * The EFMocap studio: one screen, four tabs — Characters, Takes, Camera, Render.
 * Cast a character, record with it, layer takes into a scene, lay out the
 * camera, and export, without leaving the game.
 */
public class EditorScreen extends Screen {
    private enum Tab { CHARACTERS, TAKES, CAMERA, RENDER }

    private Tab tab = Tab.CHARACTERS;
    private int scroll = 0;
    private EditBox nameBox;
    private String status = "";

    public EditorScreen() {
        super(Component.literal("EFMocap"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int x = 10, y = 26;
        // Tabs
        addTab("Karakterler", Tab.CHARACTERS, x, y, 84);
        addTab("Çekimler", Tab.TAKES, x + 88, y, 84);
        addTab("Kamera", Tab.CAMERA, x + 176, y, 84);
        addTab("Render", Tab.RENDER, x + 264, y, 84);

        switch (tab) {
            case CHARACTERS -> initCharacters();
            case TAKES -> initTakes();
            case CAMERA -> initCamera();
            case RENDER -> initRender();
        }
    }

    private void addTab(String label, Tab t, int x, int y, int w) {
        Button b = Button.builder(Component.literal(label), btn -> {
            tab = t; scroll = 0; status = ""; rebuild();
        }).bounds(x, y, w, 20).build();
        b.active = tab != t;
        addRenderableWidget(b);
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    // --- Characters ------------------------------------------------------

    private void initCharacters() {
        CharacterLibrary lib = CharacterLibrary.INSTANCE;
        int top = 56;

        nameBox = new EditBox(font, 10, top, 160, 20, Component.literal("isim"));
        nameBox.setHint(Component.literal("yeni karakter adı"));
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.literal("Oluştur"), b -> {
            String n = nameBox.getValue().trim();
            if (n.isEmpty()) { status = "§cisim gir"; return; }
            lib.create(n);
            status = "§a'" + n + "' oluşturuldu";
            rebuild();
        }).bounds(176, top, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Skin klasörü"), b -> {
            status = "§7" + CharacterLibrary.skinsDir();
            net.minecraft.Util.getPlatform().openFile(CharacterLibrary.skinsDir().toFile());
        }).bounds(252, top, 96, 20).build());

        List<String> names = lib.names();
        int y = top + 28;
        for (int i = scroll; i < names.size() && y < height - 60; i++) {
            String n = names.get(i);
            Character c = lib.get(n);
            boolean isActive = n.equals(lib.active);

            addRenderableWidget(Button.builder(
                    Component.literal((isActive ? "§a● " : "§7○ ") + c.describe()), b -> {
                lib.active = n; lib.save();
                status = "§a'" + n + "' seçildi — K ile bu karakterle kaydet";
                rebuild();
            }).bounds(10, y, 220, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Skin»"), b -> {
                cycleSkin(c);
                status = "§bskin: " + (c.skin.isEmpty() ? "varsayılan" : c.skin);
                rebuild();
            }).bounds(234, y, 46, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal(c.slim ? "ince" : "kalın"), b -> {
                c.slim = !c.slim; lib.put(c); rebuild();
            }).bounds(284, y, 40, 20).build());

            addRenderableWidget(Button.builder(Component.literal("+kuyruk"), b -> {
                c.attachments.add(new Attachment("tail", "Torso"));
                lib.put(c); status = "§bkuyruk eklendi"; rebuild();
            }).bounds(328, y, 52, 20).build());

            addRenderableWidget(Button.builder(Component.literal("+kulak"), b -> {
                c.attachments.add(new Attachment("ears", "Head"));
                lib.put(c); status = "§bkulak eklendi"; rebuild();
            }).bounds(384, y, 52, 20).build());

            addRenderableWidget(Button.builder(Component.literal("§cX"), b -> {
                lib.remove(n); status = "§e'" + n + "' silindi"; rebuild();
            }).bounds(440, y, 20, 20).build());

            y += 22;
        }
    }

    /** Step a character through the available skin PNGs (plus "default"). */
    private void cycleSkin(Character c) {
        List<String> skins = CharacterLibrary.INSTANCE.availableSkins();
        if (skins.isEmpty()) { status = "§cskins klasörüne PNG koy"; return; }
        int idx = skins.indexOf(c.skin);
        idx++;
        c.skin = idx >= skins.size() ? "" : skins.get(idx);
        CharacterLibrary.INSTANCE.put(c);
    }

    // --- Takes -----------------------------------------------------------

    private void initTakes() {
        TakeLibrary lib = TakeLibrary.INSTANCE;
        int top = 56;

        addRenderableWidget(Button.builder(Component.literal("Sahneyi oynat"), b -> {
            int n = ReplayDirector.INSTANCE.playScene(true);
            status = n > 0 ? "§a" + n + " oyuncu sahnede" : "§cçekim yok";
        }).bounds(10, top, 110, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Baştan"), b -> {
            ReplayDirector.INSTANCE.restart(); status = "§asahne baştan";
        }).bounds(124, top, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Klonları temizle"), b -> {
            ReplayDirector.INSTANCE.clearAll(); status = "§etemizlendi";
        }).bounds(198, top, 120, 20).build());

        List<String> names = lib.names();
        int y = top + 28;
        for (int i = scroll; i < names.size() && y < height - 60; i++) {
            String n = names.get(i);
            MocapRecording r = lib.get(n);
            String cast = r.character == null || r.character.isEmpty() ? "—" : r.character;

            addRenderableWidget(Button.builder(Component.literal(
                    "§f" + n + " §7(" + r.length() + " kare, " + cast + ")"), b -> {
                ReplayDirector.INSTANCE.play(r, true);
                status = "§a'" + n + "' oynuyor";
            }).bounds(10, y, 250, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Karakter»"), b -> {
                cycleCast(r);
                status = "§b'" + n + "' → " + (r.character.isEmpty() ? "varsayılan" : r.character);
                lib.save(r);
                rebuild();
            }).bounds(264, y, 76, 20).build());

            // Mark the moment this actor dies. Press it while the scene is
            // playing (the editor doesn't pause) to catch the exact beat.
            addRenderableWidget(Button.builder(Component.literal(
                    r.deathTick >= 0 ? "§c☠ " + r.deathTick : "§7ölüm"), b -> {
                if (r.deathTick >= 0) {
                    r.deathTick = -1;
                    status = "§7'" + n + "' ölümü kaldırıldı";
                } else {
                    int t = ReplayDirector.INSTANCE.currentTickOf(r);
                    r.deathTick = t >= 0 ? t : r.length() - 1;
                    status = "§c'" + n + "' " + r.deathTick + ". tikte ölüyor";
                }
                lib.save(r);
                rebuild();
            }).bounds(344, y, 56, 20).build());

            addRenderableWidget(Button.builder(Component.literal("§cX"), b -> {
                lib.remove(n); status = "§e'" + n + "' silindi"; rebuild();
            }).bounds(404, y, 20, 20).build());

            y += 22;
        }
    }

    /** Recast an already-recorded take onto another character. */
    private void cycleCast(MocapRecording r) {
        List<String> chars = CharacterLibrary.INSTANCE.names();
        if (chars.isEmpty()) { status = "§cönce karakter oluştur"; return; }
        int idx = chars.indexOf(r.character);
        idx++;
        r.character = idx >= chars.size() ? "" : chars.get(idx);
    }

    // --- Camera ----------------------------------------------------------

    private void initCamera() {
        CameraDirector cam = CameraDirector.INSTANCE;
        int top = 56;

        addRenderableWidget(Button.builder(Component.literal("Nokta ekle (buradan)"), b -> {
            cam.addKeyframeHere();
            status = "§bnokta " + cam.keyCount() + " eklendi";
            rebuild();
        }).bounds(10, top, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Son noktayı sil"), b -> {
            status = cam.removeLast() ? "§esilindi" : "§cnokta yok";
            rebuild();
        }).bounds(164, top, 110, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Yolu temizle"), b -> {
            cam.clearPath(); status = "§etemizlendi"; rebuild();
        }).bounds(278, top, 90, 20).build());

        int y = top + 26;
        addRenderableWidget(Button.builder(Component.literal("Sinematiği oynat"), b -> {
            if (cam.keyCount() < 2) { status = "§cen az 2 nokta gerek"; return; }
            if (ReplayDirector.INSTANCE.activeCount() == 0) ReplayDirector.INSTANCE.playScene(true);
            else ReplayDirector.INSTANCE.restart();
            cam.play();
            onClose();
        }).bounds(10, y, 150, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Hız: " + cam.segmentTicks + "t"), b -> {
            cam.segmentTicks = switch (cam.segmentTicks) {
                case 20 -> 40; case 40 -> 60; case 60 -> 100; case 100 -> 20; default -> 40;
            };
            rebuild();
        }).bounds(164, y, 110, 20).build());
    }

    // --- Render ----------------------------------------------------------

    private void initRender() {
        VideoRecorder vr = VideoRecorder.INSTANCE;
        int top = 56;

        addRenderableWidget(Button.builder(Component.literal(
                vr.isActive() ? "§cKaydı durdur" : "§aVideo kaydını başlat"), b -> {
            if (vr.isActive()) {
                double fps = vr.stop();
                status = "§a" + vr.frameCount() + " kare @ "
                        + String.format(Locale.ROOT, "%.1f", fps) + " fps — kodlanıyor";
            } else {
                status = vr.start() ? "§akayıt başladı (GUI gizlendi)" : "§cbaşlatılamadı";
            }
            rebuild();
        }).bounds(10, top, 170, 20).build());

        addRenderableWidget(Button.builder(Component.literal(
                "FPS: " + (vr.forcedFps <= 0 ? "otomatik" : (int) vr.forcedFps)), b -> {
            vr.forcedFps = switch ((int) vr.forcedFps) {
                case 0 -> 30; case 30 -> 60; case 60 -> 24; default -> 0;
            };
            rebuild();
        }).bounds(184, top, 110, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Çıktı klasörü"), b -> {
            net.minecraft.Util.getPlatform().openFile(VideoRecorder.renderRoot().toFile());
        }).bounds(298, top, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Tam sinematik: sahne + kamera + kayıt"), b -> {
            if (CameraDirector.INSTANCE.keyCount() < 2) { status = "§cen az 2 kamera noktası"; return; }
            ReplayDirector.INSTANCE.playScene(true);
            VideoRecorder.INSTANCE.start();
            CameraDirector.INSTANCE.play();
            onClose();
        }).bounds(10, top + 26, 290, 20).build());
    }

    // --- rendering -------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawString(font, "§lEFMocap §7— Epic Fight sinematik stüdyosu", 10, 10, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partial);

        int y = height - 46;
        String hint = switch (tab) {
            case CHARACTERS -> "§7Skin PNG'lerini config/efmocap/skins içine koy, sonra Skin» ile seç. "
                    + "§a●§7 = kayıt bu karakterle yapılır.";
            case TAKES -> "§7K ile kaydet. §c'ölüm'§7 sahne oynarken tam o anda basılırsa aktör orada ölür — ceset silinmez.";
            case CAMERA -> "§7İstediğin yere uç, bak, 'Nokta ekle'. En az 2 nokta koy — kamera aralarında yumuşak süzülür.";
            case RENDER -> "§7ffmpeg kuruluysa MP4 çıkar; değilse PNG kareler klasörde kalır.";
        };
        g.drawString(font, hint, 10, y, 0xA0A0A0);
        if (!status.isEmpty()) g.drawString(font, status, 10, y + 12, 0xFFFFFF);

        String state = "§7çekim: " + TakeLibrary.INSTANCE.count()
                + "  karakter: " + CharacterLibrary.INSTANCE.count()
                + "  kamera: " + CameraDirector.INSTANCE.keyCount()
                + (MocapRecorder.INSTANCE.isRecording() ? "  §c● KAYITTA" : "")
                + (VideoRecorder.INSTANCE.isActive() ? "  §c● VİDEO" : "");
        g.drawString(font, state, 10, height - 20, 0xFFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta < 0) scroll++;
        else if (scroll > 0) scroll--;
        rebuild();
        return true;
    }
}
