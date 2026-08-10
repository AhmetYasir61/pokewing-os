package com.pokewing.efmocap;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Character creator: build the cast — skin, body model, what's left when they
 * decay, and cosmetic parts pinned to Epic Fight bones (tail, ears, horns),
 * each with its own placement controls.
 */
public class CharacterScreen extends StudioScreen {
    /** Epic Fight biped joints an attachment can hang from. */
    private static final String[] BONES = {
            "Head", "Chest", "Torso", "Root",
            "Shoulder_R", "Arm_R", "Hand_R",
            "Shoulder_L", "Arm_L", "Hand_L",
            "Thigh_R", "Leg_R", "Thigh_L", "Leg_L"
    };

    private static String sel = "";
    private static int selAttach = -1;

    private EditBox nameBox;

    public CharacterScreen() { super("Karakter oluşturucu"); }

    @Override protected String listTitle() { return "KARAKTERLER"; }
    @Override protected int pageIndex() { return 1; }

    @Override
    protected void buildContent() {
        CharacterLibrary lib = CharacterLibrary.INSTANCE;
        List<String> names = lib.names();
        if ((sel.isEmpty() || lib.get(sel) == null) && !names.isEmpty()) sel = names.get(0);

        // --- list
        int y = contentY + 20;
        for (String n : names) {
            if (y > tlY - 46) break;
            final String name = n;
            boolean cast = n.equals(lib.active);
            row(listX + 4, y, listW - 8, (cast ? "● " : "   ") + n,
                    cast ? "Kayıt bu karakterle yapılıyor" : null,
                    sel.equals(n), () -> { sel = name; selAttach = -1; rebuild(); });
            y += 20;
        }

        nameBox = new EditBox(font, listX + 4, tlY - 26, listW - 76, 18,
                Component.literal("isim"));
        nameBox.setHint(Component.literal("yeni karakter"));
        addRenderableWidget(nameBox);
        btn(listX + listW - 68, tlY - 26, 64, "+ Ekle", null, () -> {
            String n = nameBox.getValue().trim();
            if (n.isEmpty()) { status = "§cisim gir"; return; }
            lib.create(n); sel = n; selAttach = -1;
            status = "§a'" + n + "' oluşturuldu";
            rebuild();
        });

        Character c = lib.get(sel);
        if (c == null) return;

        // --- identity
        int px = propX + 8, py = contentY + 22;
        boolean isCast = sel.equals(lib.active);
        toggle(px, py, 190, isCast ? "● Kayıt bu karakterle" : "Rolü buna ver", isCast, () -> {
            lib.active = sel; lib.save();
            status = "§a'" + sel + "' seçildi — oyunda K ile bununla kaydet";
            rebuild();
        });
        danger(propX + propW - 96, py, 88, "Sil", () -> {
            lib.remove(sel); sel = ""; selAttach = -1; rebuild();
        });

        py += 26;
        section(px, py, "GÖRÜNÜM");
        py += 14;
        btn(px, py, 150, "Skin: " + (c.skin.isEmpty() ? "varsayılan" : c.skin),
                "config/efmocap/skins içindeki PNG'ler arasında geçer", () -> {
            c.skin = cycleSkin(c.skin);
            lib.put(c); rebuild();
        });
        toggle(px + 156, py, 80, c.slim ? "ince kol" : "kalın kol", c.slim, () -> {
            c.slim = !c.slim; lib.put(c); rebuild();
        });
        btn(px + 242, py, 100, "Skin klasörü", null,
                () -> net.minecraft.Util.getPlatform().openFile(
                        CharacterLibrary.skinsDir().toFile()));

        py += 22;
        btn(px, py, 190, "Çürüyünce: " + (c.decaySkin.isEmpty() ? "iskelet (varsayılan)" : c.decaySkin),
                "Ceset çürüdükten sonraki doku", () -> {
            c.decaySkin = cycleSkin(c.decaySkin);
            lib.put(c); rebuild();
        });

        // --- attachments
        py += 28;
        section(px, py, "EKLER  §8(.obj modelleri)");
        py += 14;
        btn(px, py, 110, "+ Ek ekle", "Boş bir ek oluşturur, sonra modelini seç", () -> {
            List<String> models = lib.availableModels();
            c.attachments.add(new Attachment(models.isEmpty() ? "" : models.get(0), "Head"));
            selAttach = c.attachments.size() - 1; lib.put(c); rebuild();
        });
        btn(px + 116, py, 120, "Model klasörü",
                "Blockbench/Blender'dan .obj olarak buraya at", () ->
                net.minecraft.Util.getPlatform().openFile(
                        CharacterLibrary.attachmentsDir().toFile()));
        btn(px + 242, py, 100, "Modelleri yenile",
                "Dosyaları değiştirdiysen yeniden okur", () -> {
            lib.reloadModels();
            com.pokewing.efmocap.bbs.BBSForms.reload();
            int installed = com.pokewing.efmocap.bbs.BBSAssets.install();
            status = "§b" + lib.availableModels().size() + " obj, "
                    + com.pokewing.efmocap.bbs.BBSForms.available().size() + " BBS formu"
                    + (installed > 0 ? ", " + installed + " dosya BBS'e kuruldu" : "");
            rebuild();
        });

        py += 22;
        btn(px, py, 226, "BBS model klasörü",
                "Buraya attıkların BBS'te 'efmocap' kategorisi olarak görünür", () ->
                net.minecraft.Util.getPlatform().openFile(
                        com.pokewing.efmocap.bbs.BBSAssets.sourceDirEnsured().toFile()));

        py += 24;
        for (int i = 0; i < c.attachments.size() && py < tlY - 120; i++) {
            Attachment a = c.attachments.get(i);
            final int idx = i;
            row(px, py, 200, a.describe(), null, selAttach == i,
                    () -> { selAttach = idx; rebuild(); });
            danger(px + 206, py, 22, "×", () -> {
                c.attachments.remove(idx);
                if (selAttach >= c.attachments.size()) selAttach = c.attachments.size() - 1;
                lib.put(c); rebuild();
            });
            py += 20;
        }

        // --- attachment properties
        if (selAttach >= 0 && selAttach < c.attachments.size()) {
            Attachment a = c.attachments.get(selAttach);
            py += 6;
            section(px, py, "SEÇİLİ EK");
            py += 14;

            btn(px, py, 170, "Model: " + (a.model.isEmpty() ? "seç" : a.model),
                    "attachments klasöründeki .obj dosyaları arasında geçer", () -> {
                List<String> models = lib.availableModels();
                if (models.isEmpty()) {
                    status = "§cattachments klasörüne .obj koy";
                    return;
                }
                int i = models.indexOf(a.model) + 1;
                a.model = models.get(i >= models.size() ? 0 : i);
                lib.put(c); rebuild();
            });
            btn(px + 176, py, 140, "Doku: " + (a.texture.isEmpty() ? "karakter skini" : a.texture),
                    "Ek için ayrı PNG kullan", () -> {
                a.texture = cycleSkin(a.texture);
                lib.put(c); rebuild();
            });

            py += 22;
            // A BBS form wins over the obj, so BBS's own editor can build the part.
            btn(px, py, 316, "BBS formu: " + (a.bbsForm.isEmpty() ? "yok (obj kullanılıyor)" : a.bbsForm),
                    "BBS'te tasarlayıp config/efmocap/forms içine attığın formlar", () -> {
                List<String> forms = com.pokewing.efmocap.bbs.BBSForms.available();
                if (forms.isEmpty()) {
                    status = "§cconfig/efmocap/forms içine BBS formu (.json) koy";
                    return;
                }
                int i = forms.indexOf(a.bbsForm) + 1;
                a.bbsForm = i >= forms.size() ? "" : forms.get(i);
                lib.put(c); rebuild();
            });

            py += 22;
            btn(px, py, 130, "Kemik: " + a.bone, "Hangi Epic Fight kemiğine bağlı", () -> {
                int i = indexOf(BONES, a.bone) + 1;
                a.bone = BONES[i >= BONES.length ? 0 : i];
                lib.put(c); rebuild();
            });
            btn(px + 136, py, 110, String.format(Locale.ROOT, "Ölçek: %.2f", a.scale), null, () -> {
                a.scale = a.scale >= 3.0f ? 0.25f : a.scale + 0.25f;
                lib.put(c); rebuild();
            });

            py += 22;
            nudge(px, py, "X", a.offsetX, v -> { a.offsetX = v; lib.put(c); });
            nudge(px + 108, py, "Y", a.offsetY, v -> { a.offsetY = v; lib.put(c); });
            nudge(px + 216, py, "Z", a.offsetZ, v -> { a.offsetZ = v; lib.put(c); });

            py += 22;
            spin(px, py, "dX", a.rotX, v -> { a.rotX = v; lib.put(c); });
            spin(px + 108, py, "dY", a.rotY, v -> { a.rotY = v; lib.put(c); });
            spin(px + 216, py, "dZ", a.rotZ, v -> { a.rotZ = v; lib.put(c); });
        }
    }

    /** Small label + −/+ pair for a pixel offset. */
    private void nudge(int x, int y, String label, float value, java.util.function.Consumer<Float> set) {
        zones.add(new Zone(x, y, 46, 18,
                label + " " + String.format(Locale.ROOT, "%.1f", value),
                null, () -> {}, Zone.ROW, false));
        btn(x + 48, y, 22, "−", null, () -> { set.accept(value - 0.5f); rebuild(); });
        btn(x + 72, y, 22, "+", null, () -> { set.accept(value + 0.5f); rebuild(); });
    }

    /** Section captions are plain text, so they're drawn after the widgets. */
    /** Label + −/+ pair for a rotation, in degrees. */
    private void spin(int x, int y, String label, float value, java.util.function.Consumer<Float> set) {
        zones.add(new Zone(x, y, 46, 18,
                label + " " + String.format(Locale.ROOT, "%.0f", value),
                null, () -> {}, Zone.ROW, false));
        btn(x + 48, y, 22, "−", null, () -> { set.accept(value - 15f); rebuild(); });
        btn(x + 72, y, 22, "+", null, () -> { set.accept(value + 15f); rebuild(); });
    }

    private void section(int x, int y, String label) {
        sections.add(new int[] {x, y});
        sectionLabels.add(label);
    }

    private final java.util.List<int[]> sections = new java.util.ArrayList<>();
    private final java.util.List<String> sectionLabels = new java.util.ArrayList<>();

    @Override
    protected void rebuild() {
        sections.clear();
        sectionLabels.clear();
        super.rebuild();
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        for (int i = 0; i < sections.size(); i++) {
            int[] p = sections.get(i);
            g.drawString(font, "§8" + sectionLabels.get(i), p[0], p[1], Theme.TEXT_DIM, false);
        }
    }

    private String cycleSkin(String current) {
        List<String> skins = CharacterLibrary.INSTANCE.availableSkins();
        if (skins.isEmpty()) { status = "§cönce skins klasörüne PNG koy"; return current; }
        int i = skins.indexOf(current) + 1;
        return i >= skins.size() ? "" : skins.get(i);
    }

    private static int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return -1;
    }
}
