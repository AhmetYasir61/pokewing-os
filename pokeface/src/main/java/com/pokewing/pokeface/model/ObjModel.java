package com.pokewing.pokeface.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pokewing.pokeface.PokeFace;

import net.minecraft.client.renderer.texture.OverlayTexture;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal Wavefront OBJ reader, enough for the shapes people actually export
 * from Blockbench and Blender: positions, texture coordinates, normals and
 * faces. Faces with more than three vertices are fanned into triangles, so quads
 * from Blockbench and n-gons from Blender both load.
 *
 * <p>Materials are ignored on purpose — the texture is the PNG that sits next to
 * the OBJ, which is how both exporters lay a model out anyway, and it keeps a
 * dropped-in model working without hand-editing an MTL path.
 */
public final class ObjModel {

    /** Interleaved triangle vertices, ready to push into a buffer. */
    private final float[] positions;
    private final float[] uvs;
    private final float[] normals;
    private final int vertexCount;

    private ObjModel(float[] positions, float[] uvs, float[] normals, int vertexCount) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.vertexCount = vertexCount;
    }

    public int vertexCount() {
        return this.vertexCount;
    }

    public static ObjModel load(Path file) {
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] parts = line.split("\\s+");
                switch (parts[0]) {
                    case "v" -> v.add(new float[]{parseFloat(parts, 1), parseFloat(parts, 2), parseFloat(parts, 3)});
                    case "vt" -> vt.add(new float[]{parseFloat(parts, 1), parseFloat(parts, 2)});
                    case "vn" -> vn.add(new float[]{parseFloat(parts, 1), parseFloat(parts, 2), parseFloat(parts, 3)});
                    case "f" -> {
                        int corners = parts.length - 1;
                        // Fan triangulation: valid for the convex faces exporters emit.
                        for (int i = 2; i < corners; i++) {
                            triangles.add(parseCorner(parts[1]));
                            triangles.add(parseCorner(parts[i]));
                            triangles.add(parseCorner(parts[i + 1]));
                        }
                    }
                    default -> {
                        // Materials, groups and smoothing are not needed here.
                    }
                }
            }
        } catch (Exception e) {
            PokeFace.LOGGER.warn("PokeFace: could not read model {}: {}", file.getFileName(), e.toString());
            return null;
        }

        int count = triangles.size();
        if (count == 0) {
            PokeFace.LOGGER.warn("PokeFace: model {} has no faces.", file.getFileName());
            return null;
        }
        float[] positions = new float[count * 3];
        float[] uvs = new float[count * 2];
        float[] normals = new float[count * 3];

        for (int i = 0; i < count; i++) {
            int[] corner = triangles.get(i);
            float[] pos = pick(v, corner[0], new float[]{0.0F, 0.0F, 0.0F});
            float[] uv = pick(vt, corner[1], new float[]{0.0F, 0.0F});
            float[] normal = pick(vn, corner[2], new float[]{0.0F, 1.0F, 0.0F});
            positions[i * 3] = pos[0];
            positions[i * 3 + 1] = pos[1];
            positions[i * 3 + 2] = pos[2];
            uvs[i * 2] = uv[0];
            // OBJ texture space runs bottom-up, Minecraft's runs top-down.
            uvs[i * 2 + 1] = 1.0F - uv[1];
            normals[i * 3] = normal[0];
            normals[i * 3 + 1] = normal[1];
            normals[i * 3 + 2] = normal[2];
        }
        return new ObjModel(positions, uvs, normals, count);
    }

    private static float parseFloat(String[] parts, int index) {
        return index < parts.length ? Float.parseFloat(parts[index]) : 0.0F;
    }

    /** Parses {@code v}, {@code v/vt} or {@code v/vt/vn}, 1-based with negatives. */
    private static int[] parseCorner(String token) {
        String[] bits = token.split("/", -1);
        int[] out = new int[]{0, 0, 0};
        for (int i = 0; i < 3 && i < bits.length; i++) {
            out[i] = bits[i].isEmpty() ? 0 : Integer.parseInt(bits[i]);
        }
        return out;
    }

    private static float[] pick(List<float[]> source, int index, float[] fallback) {
        if (index == 0 || source.isEmpty()) {
            return fallback;
        }
        int resolved = index > 0 ? index - 1 : source.size() + index;
        return resolved >= 0 && resolved < source.size() ? source.get(resolved) : fallback;
    }

    /**
     * Emits the model into {@code buffer}. The triangles are emitted as quads with
     * the last vertex doubled, because Minecraft's entity vertex formats are quad
     * based — the degenerate corner costs nothing and avoids needing a custom
     * render type just to draw triangles.
     */
    public void render(PoseStack poseStack, VertexConsumer buffer, int light, int argb) {
        PoseStack.Pose pose = poseStack.last();
        float a = (argb >>> 24 & 0xFF) / 255.0F;
        float r = (argb >>> 16 & 0xFF) / 255.0F;
        float g = (argb >>> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        for (int i = 0; i < this.vertexCount; i += 3) {
            emit(buffer, pose, i, r, g, b, a, light);
            emit(buffer, pose, i + 1, r, g, b, a, light);
            emit(buffer, pose, i + 2, r, g, b, a, light);
            emit(buffer, pose, i + 2, r, g, b, a, light);
        }
    }

    private void emit(VertexConsumer buffer, PoseStack.Pose pose, int index,
                      float r, float g, float b, float a, int light) {
        buffer.vertex(pose.pose(), this.positions[index * 3], this.positions[index * 3 + 1],
                        this.positions[index * 3 + 2])
                .color(r, g, b, a)
                .uv(this.uvs[index * 2], this.uvs[index * 2 + 1])
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), this.normals[index * 3], this.normals[index * 3 + 1],
                        this.normals[index * 3 + 2])
                .endVertex();
    }
}
