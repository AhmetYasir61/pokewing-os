package com.pokewing.efmocap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws an attachment {@code .obj} with the transform already on the stack.
 *
 * <p>Shared by two callers: our own world pass, which pins the model to an Epic
 * Fight bone, and the BBS form type, which lets the same models be picked from
 * inside BBS's character editor.</p>
 */
public final class ObjRender {
    private ObjRender() {}

    /** Look up a model by file name; null when it isn't there or is empty. */
    public static ObjModel model(String name) {
        if (name == null || name.isEmpty()) return null;
        ObjModel m = CharacterLibrary.INSTANCE.model(name);
        return m == null || m.isEmpty() ? null : m;
    }

    /** Resolve a texture file name, falling back to the skeleton sheet. */
    public static ResourceLocation texture(String name) {
        if (name != null && !name.isEmpty()) {
            ResourceLocation rl = CharacterLibrary.INSTANCE.texture(name);
            if (rl != null) return rl;
        }
        return CloneEntity.SKELETON;
    }

    /** Emit the mesh into a buffer the caller owns. */
    public static void emit(ObjModel model, PoseStack stack, VertexConsumer vc, int light) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        for (ObjModel.Tri t : model.tris) {
            for (int i = 0; i < 3; i++) {
                vc.vertex(pose, t.x[i], t.y[i], t.z[i])
                        .color(255, 255, 255, 255)
                        .uv(t.u[i], t.v[i])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light)
                        .normal(normal, t.nx[i], t.ny[i], t.nz[i])
                        .endVertex();
            }
        }
    }

    /**
     * Draw a named model with its own buffer, for callers (like BBS) that don't
     * hand us one. Returns false when the model isn't loadable.
     */
    public static boolean draw(String modelName, String textureName, PoseStack stack,
                               float scale, int light) {
        ObjModel m = model(modelName);
        if (m == null) return false;
        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        stack.pushPose();
        stack.scale(scale, scale, scale);
        emit(m, stack, buffers.getBuffer(
                RenderType.entityCutoutNoCull(texture(textureName))), light);
        stack.popPose();
        buffers.endBatch();
        return true;
    }
}
