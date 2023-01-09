package vice.customskyboxes.skyboxes.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import vice.customskyboxes.mixin.skybox.WorldRendererAccess;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.RotatableSkybox;
import vice.customskyboxes.util.object.*;

public abstract class TexturedSkybox extends AbstractSkybox implements RotatableSkybox
{
    public Rotation rotation;
    public Blend blend;

    protected TexturedSkybox() {
    }

    protected TexturedSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, Blend blend) {
        super(properties, conditions, decorations);
        this.blend = blend;
        this.rotation = properties.getRotation();
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param worldRendererAccess Access to the worldRenderer as skyboxes often require it.
     * @param matrices            The current MatrixStack.
     * @param tickDelta           The current tick delta.
     */
    @Override
    public final void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        blend.applyBlendFunc();

        Vector3f rotationStatic = this.rotation.getStatic();

        ClientLevel world = Minecraft.getInstance().level;
        assert world != null;
        float timeRotation = !this.shouldRotate ? 0 : ((float) world.getDayTime() / 24000) * 360;

        matrices.pushPose();
        this.applyTimeRotation(matrices, timeRotation);
        matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
        this.renderSkybox(worldRendererAccess, matrices, tickDelta);
        matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
        matrices.popPose();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        this.renderDecorations(worldRendererAccess, matrices, tickDelta, bufferBuilder, this.alpha);

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta);

    private void applyTimeRotation(PoseStack matrices, float timeRotation) {
        // Very ugly, find a better way to do this
        Vector3f timeRotationAxis = this.rotation.getAxis();
        matrices.mulPose(Vector3f.XP.rotationDegrees(timeRotationAxis.x()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Vector3f.ZP.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(timeRotation * rotation.getRotationSpeed()));
        matrices.mulPose(Vector3f.ZN.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Vector3f.YN.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Vector3f.XN.rotationDegrees(timeRotationAxis.x()));
    }

    public Blend getBlend() {
        return this.blend;
    }

    public Rotation getRotation() {
        return this.rotation;
    }
}
