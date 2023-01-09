package vice.customskyboxes.mixin.skybox;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface FpsAccess {

    @Accessor
    int getFps();
}
