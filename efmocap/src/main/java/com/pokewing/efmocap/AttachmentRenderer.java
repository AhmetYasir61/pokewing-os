package com.pokewing.efmocap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws each character's attached models on their clone.
 *
 * <p>Epic Fight replaces player rendering with its own renderer, so rather than
 * hooking into it we draw in world space after entities: ask Epic Fight for the
 * bone's transform in the current pose, place it under the actor's world
 * transform, and emit the OBJ's triangles there. Epic Fight's armature space is
 * Z-up (its rigs come from Blender), which is why the pose matrix is rotated
 * into Minecraft's Y-up before use.</p>
 */
@Mod.EventBusSubscriber(modid = EFMocap.MOD_ID, value = Dist.CLIENT)
public final class AttachmentRenderer {
    private AttachmentRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!EFMocap.isEpicFightLoaded()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack stack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();

        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof CloneEntity clone)) continue;
            Character c = clone.character;
            if (c == null || c.attachments.isEmpty()) continue;

            Object patch = EpicFightBridge.getPatch(clone);
            if (patch == null) continue;

            double ex = Mth.lerp(partial, clone.xOld, clone.getX());
            double ey = Mth.lerp(partial, clone.yOld, clone.getY());
            double ez = Mth.lerp(partial, clone.zOld, clone.getZ());
            float bodyYaw = Mth.rotLerp(partial, clone.yBodyRotO, clone.yBodyRot);

            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(ex, ey, ez));

            for (Attachment a : c.attachments) {
                boolean useForm = a.bbsForm != null && !a.bbsForm.isEmpty();
                ObjModel model = null;
                if (!useForm) {
                    if (a.model == null || a.model.isEmpty()) continue;
                    model = CharacterLibrary.INSTANCE.model(a.model);
                    if (model == null || model.isEmpty()) continue;
                }

                float[] bone = EpicFightBridge.boneMatrix(patch, a.bone, partial);
                if (bone == null) continue;

                stack.pushPose();
                stack.translate(ex - cam.x, ey - cam.y, ez - cam.z);
                // Into the actor's model space.
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - bodyYaw));

                // Epic Fight armature space is Z-up; bring it into Y-up.
                stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                stack.mulPoseMatrix(new Matrix4f(
                        bone[0], bone[4], bone[8], bone[12],
                        bone[1], bone[5], bone[9], bone[13],
                        bone[2], bone[6], bone[10], bone[14],
                        bone[3], bone[7], bone[11], bone[15]));
                stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));

                // Author-controlled placement, in pixels.
                stack.translate(a.offsetX / 16.0, a.offsetY / 16.0, a.offsetZ / 16.0);
                if (a.rotY != 0) stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(a.rotY));
                if (a.rotX != 0) stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(a.rotX));
                if (a.rotZ != 0) stack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(a.rotZ));
                float s = a.scale * (float) Settings.attachScale;
                stack.scale(s, s, s);

                if (useForm) {
                    // BBS draws its own form; it manages its buffers itself.
                    com.pokewing.efmocap.bbs.BBSForms.render(
                            com.pokewing.efmocap.bbs.BBSForms.load(a.bbsForm),
                            clone, stack, light, OverlayTexture.NO_OVERLAY, partial);
                } else {
                    VertexConsumer vc = buffers.getBuffer(
                            RenderType.entityCutoutNoCull(textureFor(c, a)));
                    emit(model, stack, vc, light);
                }
                stack.popPose();
            }
        }
        buffers.endBatch();
    }

    private static ResourceLocation textureFor(Character c, Attachment a) {
        if (a.texture != null && !a.texture.isEmpty()) {
            ResourceLocation rl = CharacterLibrary.INSTANCE.texture(a.texture);
            if (rl != null) return rl;
        }
        if (!c.skin.isEmpty()) {
            ResourceLocation rl = CharacterLibrary.INSTANCE.texture(c.skin);
            if (rl != null) return rl;
        }
        return CloneEntity.SKELETON;
    }

    private static void emit(ObjModel model, PoseStack stack, VertexConsumer vc, int light) {
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
}
