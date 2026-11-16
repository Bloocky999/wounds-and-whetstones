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

/**
 * Poses de enfermedad en el modelo del jugador, con rebote de muelle, visibles para todos.
 * El torso se dobla girando alrededor de la CADERA: la cabeza y los brazos se recolocan para
 * seguir al torso y las piernas se quedan donde estan.
 */
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
        // solo jugadores (no zombis ni esqueletos que esten cerca de uno)
        String n = state.getClass().getSimpleName();
        if (!(n.contains("Player") || n.contains("Avatar"))) return;

        // posiciones laterales/frontales de cabeza y torso: siempre limpias (por si el modelo no las reinicia)
        body.x = 0f;
        body.z = 0f;
        head.x = 0f;
        head.z = 0f;

        ClientState.Blend b = ClientState.blendAt(state.x, state.y, state.z);
        if (b == null) return;
        float age = state.ageInTicks;

        // inclinacion total del torso (bx = adelante, bz = de lado) que se aplica al final
        float bx = 0f;
        float bz = 0f;

        float hunch = b.w(Condition.POSE_HUNCH);
        if (hunch != 0f) {
            float breathe = (float) Math.sin(age * 0.15f) * 0.03f;
            bx += (0.30f + breathe) * hunch;
            head.xRot += 0.12f * hunch;
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
            bz += 0.10f * limp;
            rightLeg.xRot = lerp(rightLeg.xRot, rightLeg.xRot * 0.3f, limp);
        }
        float cough = b.w(Condition.POSE_COUGH);
        if (cough != 0f) {
            bx += 0.15f * cough;
            head.xRot += (0.35f + (float) Math.sin(age * 2.2f) * 0.1f) * cough;
            rightArm.xRot = lerp(rightArm.xRot, -2.0f, cough);
            rightArm.yRot = lerp(rightArm.yRot, -0.5f, cough);
        }
        float vomit = b.w(Condition.POSE_VOMIT);
        if (vomit != 0f) {
            bx += 0.40f * vomit;
            head.xRot += 0.5f * vomit;
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
            bx += 0.20f * cover;
            head.xRot += 0.3f * cover;
            bz += tremble * cover;
        }
        float slump = b.w(Condition.POSE_SLUMP);
        if (slump != 0f) {
            bx += 0.60f * slump;
            head.xRot += 0.6f * slump;
            bz += (float) Math.sin(age * 0.05f) * 0.06f * slump;
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
            bz += j;
            head.zRot -= j * 1.5f;
            rightArm.zRot += j * 2f;
            leftArm.zRot -= j * 2f;
        }
        float lame = b.w(Condition.POSE_LAMEARM);
        if (lame != 0f) {
            leftArm.xRot = lerp(leftArm.xRot, 0.08f, lame);
            leftArm.yRot = lerp(leftArm.yRot, 0f, lame);
            leftArm.zRot = lerp(leftArm.zRot, -0.12f, lame);
            bz += 0.04f * lame;
        }
        // artrosis: torso torcido 15 grados (0,2618 rad) hacia un lado
        bz += 0.2618f * (b.w(Condition.POSE_LEAN_R) - b.w(Condition.POSE_LEAN_L));

        applyBend(bx, bz);
    }

    /**
     * Dobla el torso alrededor de la cadera (pivote a 12 unidades bajo el cuello) y recoloca
     * cabeza y brazos para que sigan al torso. Las piernas no se mueven.
     */
    private void applyBend(float bx, float bz) {
        if (bx == 0f && bz == 0f) return;
        float cx = (float) Math.cos(bx);
        float sx = (float) Math.sin(bx);
        float cz = (float) Math.cos(bz);
        float sz = (float) Math.sin(bz);

        // cuello (12 sobre la cadera)
        float neckX = 12f * sz;
        float neckY = 12f * (1f - cx) + 12f * (1f - cz);
        float neckZ = -12f * sx;
        body.x = neckX;
        body.y += neckY;
        body.z = neckZ;
        body.xRot += bx;
        body.zRot += bz;
        head.x = neckX;
        head.y += neckY;
        head.z = neckZ;
        head.xRot += bx;
        head.zRot += bz;

        // hombros (10 sobre la cadera, a 5 de cada lado)
        float armY = 10f * (1f - cx) + 10f * (1f - cz);
        float armZ = -10f * sx;
        rightArm.x += 5f * (1f - cz) + 10f * sz;
        rightArm.y += armY - 5f * sz;
        rightArm.z += armZ;
        leftArm.x += -5f * (1f - cz) + 10f * sz;
        leftArm.y += armY + 5f * sz;
        leftArm.z += armZ;
        // los brazos cuelgan casi rectos: solo siguen un poco la inclinacion
        rightArm.xRot += bx * 0.4f;
        leftArm.xRot += bx * 0.4f;
        rightArm.zRot += bz * 0.5f;
        leftArm.zRot += bz * 0.5f;
    }
}
