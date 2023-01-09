package vice.customskyboxes.skyboxes.textured;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import vice.customskyboxes.mixin.skybox.FpsAccess;
import vice.customskyboxes.mixin.skybox.WorldRendererAccess;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.SkyboxType;
import vice.customskyboxes.util.object.*;
import java.util.List;

public class AnimatedSquareTexturedSkybox extends SquareTexturedSkybox {
    public static final Codec<AnimatedSquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultProperties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getDefaultProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.NO_CONDITIONS).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
            Textures.CODEC.listOf().fieldOf("animatedTexture").forGetter(AnimatedSquareTexturedSkybox::getAnimationTextures)
    ).apply(instance, AnimatedSquareTexturedSkybox::new));
    private final List<Textures> animationTextures;
    private final long frameTimeMillis;
    private int count = 0;
    private long lastTime = 0L;

    @Override
    public SkyboxType<? extends AbstractSkybox> getType() {
        return SkyboxType.ANIMATED_SQUARE_TEXTURED_SKYBOX.get();
    }

    public AnimatedSquareTexturedSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, Blend blend, List<Textures> animationTextures) {
        super(properties, conditions, decorations, blend, null);
        this.animationTextures = animationTextures;
        int fps = ((FpsAccess) Minecraft.getInstance()).getFps();
        if (fps > 0 && fps <= 360) {
            this.frameTimeMillis = (long) (1000F / fps);
        } else {
            this.frameTimeMillis = 16L;
        }
    }

    @Override
    public void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta) {
        if (this.lastTime == 0L) this.lastTime = System.currentTimeMillis();
        this.textures = this.getAnimationTextures().get(this.count);

        super.renderSkybox(worldRendererAccess, matrices, tickDelta);

        if (System.currentTimeMillis() >= (this.lastTime + this.frameTimeMillis)) {
            if (this.count < this.getAnimationTextures().size()) {
                if (this.count + 1 == this.getAnimationTextures().size()) {
                    this.count = 0;
                } else {
                    this.count++;
                }
            }
            this.lastTime = System.currentTimeMillis();
        }
    }

    public List<Textures> getAnimationTextures() {
        return this.animationTextures;
    }
}
