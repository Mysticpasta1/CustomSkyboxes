package vice.customskyboxes.mixin.skybox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import vice.customskyboxes.SkyboxManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class SkyboxRenderMixin {
    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void renderCustomSkyboxes(PoseStack matrices, Matrix4f pProjectionMatrix, float tickDelta, Camera pCamera, boolean p_202428_, Runnable pSkyFogSetup, CallbackInfo ci) {

        float total = SkyboxManager.getInstance().getTotalAlpha();
        SkyboxManager.getInstance().renderSkyboxes((WorldRendererAccess) this, matrices, tickDelta);
        if (total > SkyboxManager.MINIMUM_ALPHA) {
            ci.cancel();
        }
    }
}
