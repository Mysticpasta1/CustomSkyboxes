package vice.customskyboxes.skyboxes.textured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.SkyboxType;
import vice.customskyboxes.util.object.*;

public class SingleSpriteSquareTexturedSkybox extends SquareTexturedSkybox {
	public static Codec<SingleSpriteSquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			DefaultProperties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getDefaultProperties),
			Conditions.CODEC.optionalFieldOf("conditions", Conditions.NO_CONDITIONS).forGetter(AbstractSkybox::getConditions),
			Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
			Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
			Texture.CODEC.fieldOf("texture").forGetter(SingleSpriteSquareTexturedSkybox::getTexture)
	).apply(instance, SingleSpriteSquareTexturedSkybox::new));
	protected Texture texture;

	public SingleSpriteSquareTexturedSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, Blend blend, Texture texture) {
		super(properties, conditions, decorations, blend, Util.make(() -> new Textures(
				texture.withUV(1.0F / 3.0F + 0.005F, 1.0F / 2.0F + 0.005F, 2.0F / 3.0F - 0.005F, 1 - 0.005F),
				texture.withUV(2.0F / 3.0F + 0.005F, 0 + 0.005F, 1 - 0.005F, 1.0F / 2.0F - 0.005F),
				texture.withUV(2.0F / 3.0F + 0.005F, 1.0F / 2.0F + 0.005F, 1 - 0.005F, 1 - 0.005F),
				texture.withUV(0 + 0.005F, 1.0F / 2.0F + 0.005F, 1.0F / 3.0F - 0.005F, 1 - 0.005F),
				texture.withUV(1.0F / 3.0F + 0.005F, 0 + 0.005F, 2.0F / 3.0F - 0.005F, 1.0F / 2.0F - 0.005F),
				texture.withUV(0 + 0.005F, 0 + 0.005F, 1.0F / 3.0F - 0.005F, 1.0F / 2.0F - 0.005F)
		)));
		this.texture = texture;
	}

	@Override
	public SkyboxType<? extends AbstractSkybox> getType() {
		return SkyboxType.SINGLE_SPRITE_SQUARE_TEXTURED_SKYBOX.get();
	}

	public Texture getTexture() {
		return this.texture;
	}
}
