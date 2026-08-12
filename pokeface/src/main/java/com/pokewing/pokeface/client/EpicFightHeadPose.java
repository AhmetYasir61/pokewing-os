package com.pokewing.pokeface.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pokewing.pokeface.PokeFace;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Puts the pose stack on Epic Fight's animated head joint.
 *
 * <p>Epic Fight renders players from its own armature, so the vanilla head
 * {@code ModelPart} says nothing about where the head actually is mid-animation.
 * The transform reproduced here is the one Epic Fight itself uses in
 * {@code PatchedHeadLayer} to hang a helmet on the head:
 *
 * <pre>
 *   poseStack.mulPose(Y, 180deg)                  // EF's mulPoseStack, verbatim
 *   mulStack(poseStack, patch.getModelMatrix(pt)) // body/root transform
 *   if (upsideDown) translate(0, bbHeight + 0.1, 0), mulPose(Z, 180deg)
 *   if (crouching)  translate(0, 0.15, 0)         // PatchedLivingEntityRenderer
 *   mulStack(poseStack, new OpenMatrix4f().scale(-1, -1, 1)
 *                           .mulFront(armature.getPoseMatrices()[headId]))
 * </pre>
 *
 * <p>The 180 degree Y rotation is the part that is easy to miss and the part that
 * detaches the face from the player when it is missing: Epic Fight applies it in
 * {@code PatchedEntityRenderer.mulPoseStack} before anything else. The crouch
 * offset is the other easy miss — {@code PatchedLivingEntityRenderer} adds it
 * after the model matrix, which is why the face slid off the head the moment the
 * player crouched. The joint
 * matrices come from {@code armature.getPoseMatrices()} — the live array Epic
 * Fight hands to its own layers, already posed for this frame — rather than from
 * a separately computed pose, which does not carry the same bind composition.
 *
 * <p>The {@code scale(-1,-1,1)} is what converts Epic Fight's armature space into
 * the vanilla model-part convention, which is exactly the space
 * {@link FaceRenderer#renderInHeadSpace} draws in.
 *
 * <p>Everything is reflective and every failure returns false, in which case the
 * caller skips drawing rather than putting a face at a wrong position.
 */
public final class EpicFightHeadPose {

    private static final String[] HEAD_JOINT_NAMES = {"Head", "head", "Head_Root"};
    /** Torso joint names across Epic Fight's armatures, best match first. */
    private static final String[] BODY_JOINT_NAMES = {"Torso", "Chest", "Spine", "Root", "Rot"};

    private static boolean resolved;
    private static boolean available;

    private static Method getEntityPatch;      // EpicFightCapabilities.getPlayerPatch(Player)
    private static Method getArmature;         // LivingEntityPatch#getArmature()
    private static Method getModelMatrix;      // LivingEntityPatch#getModelMatrix(float)
    private static Method getPoseMatrices;     // Armature#getPoseMatrices()
    private static Method searchJointByName;   // Armature#searchJointByName(String)
    private static Method getJointId;          // Joint#getId()
    private static Method mulStack;            // MathUtils.mulStack(PoseStack, OpenMatrix4f)
    private static Method scale;               // OpenMatrix4f#scale(Vec3f)
    private static Method mulFront;            // OpenMatrix4f#mulFront(OpenMatrix4f)
    private static Class<?> openMatrixClass;
    private static Class<?> vec3fClass;

    private EpicFightHeadPose() {
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> livingPatch = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            Class<?> armatureClass = Class.forName("yesman.epicfight.api.model.Armature");
            Class<?> jointClass = Class.forName("yesman.epicfight.api.animation.Joint");
            Class<?> mathUtils = Class.forName("yesman.epicfight.api.utils.math.MathUtils");
            openMatrixClass = Class.forName("yesman.epicfight.api.utils.math.OpenMatrix4f");
            vec3fClass = Class.forName("yesman.epicfight.api.utils.math.Vec3f");

            getEntityPatch = caps.getMethod("getPlayerPatch", Player.class);
            getArmature = livingPatch.getMethod("getArmature");
            getModelMatrix = livingPatch.getMethod("getModelMatrix", float.class);
            getPoseMatrices = armatureClass.getMethod("getPoseMatrices");
            searchJointByName = armatureClass.getMethod("searchJointByName", String.class);
            getJointId = jointClass.getMethod("getId");
            mulStack = mathUtils.getMethod("mulStack", PoseStack.class, openMatrixClass);
            scale = openMatrixClass.getMethod("scale", vec3fClass);
            mulFront = openMatrixClass.getMethod("mulFront", openMatrixClass);

            available = true;
            PokeFace.LOGGER.info("PokeFace: Epic Fight head bone resolved, face follows EF animations.");
        } catch (Throwable t) {
            available = false;
            PokeFace.LOGGER.warn("PokeFace: could not resolve Epic Fight's head bone ({}). "
                    + "The face is not drawn on Epic Fight's renderer.", t.toString());
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /**
     * Transforms {@code poseStack} from the entity origin onto the animated head.
     *
     * @return true when the stack was transformed; the caller must then pop it.
     */
    public static boolean apply(PoseStack poseStack, Player player, float partialTick) {
        return apply(poseStack, player, partialTick, HEAD_JOINT_NAMES);
    }

    /** Same, for the torso — the anchor body attachments hang from. */
    public static boolean applyBody(PoseStack poseStack, Player player, float partialTick) {
        return apply(poseStack, player, partialTick, BODY_JOINT_NAMES);
    }

    private static boolean apply(PoseStack poseStack, Player player, float partialTick, String[] jointNames) {
        resolve();
        if (!available) {
            return false;
        }
        try {
            Object patch = getEntityPatch.invoke(null, player);
            if (patch == null) {
                return false;
            }
            Object armature = getArmature.invoke(patch);
            if (armature == null) {
                return false;
            }
            Object joint = findJoint(armature, jointNames);
            if (joint == null) {
                return false;
            }
            int jointId = (Integer) getJointId.invoke(joint);
            Object matrices = getPoseMatrices.invoke(armature);
            if (matrices == null || jointId < 0 || jointId >= Array.getLength(matrices)) {
                return false;
            }
            Object jointMatrix = Array.get(matrices, jointId);
            Object modelMatrix = getModelMatrix.invoke(patch, partialTick);

            Object headMatrix = openMatrixClass.getDeclaredConstructor().newInstance();
            Object minusOne = vec3fClass.getConstructor(float.class, float.class, float.class)
                    .newInstance(-1.0F, -1.0F, 1.0F);
            headMatrix = scale.invoke(headMatrix, minusOne);
            headMatrix = mulFront.invoke(headMatrix, jointMatrix);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            if (modelMatrix != null) {
                mulStack.invoke(null, poseStack, modelMatrix);
            }
            if (LivingEntityRenderer.isEntityUpsideDown(player)) {
                poseStack.translate(0.0D, player.getBbHeight() + 0.1D, 0.0D);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
            if (player.isCrouching()) {
                poseStack.translate(0.0D, 0.15D, 0.0D);
            }
            mulStack.invoke(null, poseStack, headMatrix);
            return true;
        } catch (Throwable t) {
            PokeFace.LOGGER.debug("PokeFace: Epic Fight head transform failed: {}", t.toString());
            return false;
        }
    }

    private static Object findJoint(Object armature, String[] jointNames) throws Exception {
        for (String name : jointNames) {
            Object joint = searchJointByName.invoke(armature, name);
            if (joint != null) {
                return joint;
            }
        }
        return null;
    }
}
