package dev.pandemonium.mixin;

import dev.pandemonium.Condition;
import dev.pandemonium.client.ClientState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Poses de enfermedad en el modelo del jugador, con rebote de muelle ("bouncy"), visibles para todos. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow public ModelPart head;
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftLeg;

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void pandemonium$pose(HumanoidRenderState state, CallbackInfo ci) {
        ClientState.Blend b = ClientState.blendAt(state.x, state.y, state.z);
        if (b == null) return;
        float age = state.ageInTicks;

        float hunch = b.w(Condition.POSE_HUNCH);
        if (hunch != 0f) {
            float breathe = (float) Math.sin(age * 0.15f) * 0.03f;
            body.xRot += (0.28f + breathe) * hunch;
            head.xRot += (0.22f - breathe) * hunch;
            rightArm.xRot += 0.2f * hunch;
            leftArm.xRot += 0.2f * hunch;
        }
        float arms = b.w(Condition.POSE_ARMS);
        if (arms != 0f) {
            float sway = (float) Math.sin(age * 0.2f) * 0.1f;
            rightArm.xRot = lerp(rightArm.xRot, -1.45f + sway, arms);
            leftArm.xRot = lerp(leftArm.xRot, -1.45f - sway, arms);
            rightArm.yRot = lerp(rightArm.yRot, -0.10f, arms);
            leftArm.yRot = lerp(leftArm.yRot, 0.10f, arms);
            head.xRot += (float) Math.sin(age * 0.2f) * 0.05f * arms;
        }
        float feel = b.w(Condition.POSE_FEEL);
        if (feel != 0f) {
            rightArm.xRot = lerp(rightArm.xRot, -0.9f + (float) Math.sin(age * 0.15f) * 0.3f, feel);
            leftArm.xRot = lerp(leftArm.xRot, -0.9f + (float) Math.sin(age * 0.15f + Math.PI) * 0.3f, feel);
            head.yRot += (float) Math.sin(age * 0.1f) * 0.3f * feel;
        }
        float limp = b.w(Condition.POSE_LIMP);
        if (limp != 0f) {
            body.zRot += 0.1f * limp;
            head.zRot -= 0.05f * limp;
            rightLeg.xRot = lerp(rightLeg.xRot, rightLeg.xRot * 0.3f, limp);
        }
        float cough = b.w(Condition.POSE_COUGH);
        if (cough != 0f) {
            head.xRot += (0.5f + (float) Math.sin(age * 2.2f) * 0.1f) * cough;
            body.xRot += 0.15f * cough;
            rightArm.xRot = lerp(rightArm.xRot, -2.0f, cough);
            rightArm.yRot = lerp(rightArm.yRot, -0.5f, cough);
        }
        float vomit = b.w(Condition.POSE_VOMIT);
        if (vomit != 0f) {
            head.xRot += 0.8f * vomit;
            body.xRot += 0.35f * vomit;
        }
        float scratch = b.w(Condition.POSE_SCRATCH);
        if (scratch != 0f) {
            leftArm.xRot = lerp(leftArm.xRot, -1.5f + (float) Math.sin(age * 1.6f) * 0.5f, scratch);
            leftArm.yRot = lerp(leftArm.yRot, 0.6f, scratch);
            head.zRot += 0.1f * scratch;
        }
        float cover = b.w(Condition.POSE_COVER);
        if (cover != 0f) {
            float tremble = (float) Math.sin(age * 2.5f) * 0.04f;
            rightArm.xRot = lerp(rightArm.xRot, -2.4f + tremble, cover);
            leftArm.xRot = lerp(leftArm.xRot, -2.4f - tremble, cover);
            rightArm.yRot = lerp(rightArm.yRot, -0.5f, cover);
            leftArm.yRot = lerp(leftArm.yRot, 0.5f, cover);
            head.xRot += 0.3f * cover;
            body.zRot += tremble * cover;
        }
        float slump = b.w(Condition.POSE_SLUMP);
        if (slump != 0f) {
            head.xRot += 0.9f * slump;
            body.xRot += 0.5f * slump;
            body.zRot += (float) Math.sin(age * 0.05f) * 0.06f * slump;
            rightArm.xRot += 0.4f * slump;
            leftArm.xRot += 0.4f * slump;
        }
        float twitch = b.w(Condition.POSE_TWITCH);
        if (twitch != 0f) {
            head.yRot += (float) (Math.sin(age * 5.3f) * 0.25 + Math.sin(age * 11.1f) * 0.12) * twitch;
            rightArm.zRot += (float) Math.sin(age * 7.7f) * 0.2f * twitch;
            leftArm.zRot -= (float) Math.sin(age * 6.1f) * 0.2f * twitch;
        }
        float shiver = b.w(Condition.POSE_SHIVER);
        if (shiver != 0f) {
            float j = (float) Math.sin(age * 3.1f) * 0.035f * shiver;
            body.zRot += j;
            head.zRot -= j * 1.5f;
            rightArm.zRot += j * 2f;
            leftArm.zRot -= j * 2f;
        }
        float lean = 0.26f * (b.w(Condition.POSE_LEAN_R) - b.w(Condition.POSE_LEAN_L));
        if (lean != 0f) {
            body.zRot += lean;
            head.zRot += lean * 0.5f;
            rightArm.zRot += lean;
            leftArm.zRot += lean;
        }
    }
}
