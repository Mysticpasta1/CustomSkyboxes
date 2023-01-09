package vice.customskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import vice.customskyboxes.resource.SkyboxResourceListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("customskyboxes")
public class FabricSkyBoxesClient {

    public static final String MODID = "customskyboxes";
    public static final Logger LOGGER = LogManager.getLogger();

    public FabricSkyBoxesClient() {
        MinecraftForge.EVENT_BUS.register(this);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> DistExecution::clientStart);
    }



    public static Logger getLogger() { return LOGGER; }
}

class DistExecution
{
    public static void clientStart()
    {
        SkyboxResourceListener reloadListener = new SkyboxResourceListener();

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager reloadableResourceManager) {
            reloadableResourceManager.registerReloadListener(reloadListener);
        }
    }
}