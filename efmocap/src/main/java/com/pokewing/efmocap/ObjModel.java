package com.pokewing.efmocap;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A minimal Wavefront OBJ reader — enough for the cosmetic parts people model
 * in Blockbench or Blender and drop into the attachments folder. Triangles and
 * quads are supported (quads are split), along with texture coordinates and
 * normals; materials are ignored, since the part is drawn with its own PNG.
 */
public final class ObjModel {
    /** One triangle, ready to hand to the renderer. */
    public static final class Tri {
        public final float[] x = new float[3], y = new float[3], z = new float[3];
        public final float[] u = new float[3], v = new float[3];
        public final float[] nx = new float[3], ny = new float[3], nz = new float[3];
    }

    public final List<Tri> tris = new ArrayList<>();
    public final String name;

    private ObjModel(String name) { this.name = name; }

    public boolean isEmpty() { return tris.isEmpty(); }
    public int triangleCount() { return tris.size(); }

    public static ObjModel load(Path file) {
        ObjModel m = new ObjModel(file.getFileName().toString());
        List<float[]> verts = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> norms = new ArrayList<>();

        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] p = line.split("\\s+");
                switch (p[0]) {
                    case "v" -> verts.add(new float[] {f(p, 1), f(p, 2), f(p, 3)});
                    case "vt" -> uvs.add(new float[] {f(p, 1), f(p, 2)});
                    case "vn" -> norms.add(new float[] {f(p, 1), f(p, 2), f(p, 3)});
                    case "f" -> face(m, p, verts, uvs, norms);
                    default -> { /* mtllib, usemtl, o, g, s — not needed */ }
                }
            }
        } catch (Exception e) {
            EFMocap.LOG.warn("[efmocap] could not read obj {}", file, e);
            return m;
        }
        EFMocap.LOG.info("[efmocap] loaded obj {} ({} tris)", m.name, m.tris.size());
        return m;
    }

    /** Fan-triangulate a face; OBJ indices are 1-based and may be negative. */
    private static void face(ObjModel m, String[] p, List<float[]> verts,
                             List<float[]> uvs, List<float[]> norms) {
        int n = p.length - 1;
        if (n < 3) return;
        for (int i = 1; i + 1 < n; i++) {
            Tri t = new Tri();
            if (!corner(t, 0, p[1], verts, uvs, norms)) continue;
            if (!corner(t, 1, p[i + 1], verts, uvs, norms)) continue;
            if (!corner(t, 2, p[i + 2], verts, uvs, norms)) continue;
            m.tris.add(t);
        }
    }

    private static boolean corner(Tri t, int slot, String ref, List<float[]> verts,
                                  List<float[]> uvs, List<float[]> norms) {
        String[] parts = ref.split("/");
        int vi = idx(parts.length > 0 ? parts[0] : "", verts.size());
        if (vi < 0 || vi >= verts.size()) return false;
        float[] v = verts.get(vi);
        t.x[slot] = v[0]; t.y[slot] = v[1]; t.z[slot] = v[2];

        if (parts.length > 1 && !parts[1].isEmpty()) {
            int ti = idx(parts[1], uvs.size());
            if (ti >= 0 && ti < uvs.size()) {
                float[] uv = uvs.get(ti);
                t.u[slot] = uv[0];
                // OBJ counts V from the bottom; Minecraft textures from the top.
                t.v[slot] = 1f - uv[1];
            }
        }
        if (parts.length > 2 && !parts[2].isEmpty()) {
            int ni = idx(parts[2], norms.size());
            if (ni >= 0 && ni < norms.size()) {
                float[] nn = norms.get(ni);
                t.nx[slot] = nn[0]; t.ny[slot] = nn[1]; t.nz[slot] = nn[2];
            } else {
                t.ny[slot] = 1f;
            }
        } else {
            t.ny[slot] = 1f;
        }
        return true;
    }

    private static int idx(String s, int count) {
        try {
            int i = Integer.parseInt(s.trim());
            return i < 0 ? count + i : i - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static float f(String[] p, int i) {
        try {
            return i < p.length ? Float.parseFloat(p[i].replace(",", ".")) : 0f;
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    public static boolean isObj(Path p) {
        return p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj");
    }
}
