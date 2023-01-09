package vice.customskyboxes.mixin.skybox;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccess {

    @Accessor
    VertexBuffer getSkyBuffer();

    @Accessor
    VertexBuffer getStarBuffer();

    @Accessor
    VertexBuffer getDarkBuffer();

    @Deprecated
    @Accessor("SUN_LOCATION")
    static ResourceLocation getSun() {
        throw new AssertionError();
    }

    @Deprecated
    @Accessor("MOON_LOCATION")
    static ResourceLocation getMoonPhases(){
        throw new AssertionError();
    }
}
