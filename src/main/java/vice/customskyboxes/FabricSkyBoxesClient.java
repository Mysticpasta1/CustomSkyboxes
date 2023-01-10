package vice.customskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vice.customskyboxes.resource.SkyboxResourceListener;
import vice.customskyboxes.skyboxes.SkyboxType;

@Mod("customskyboxes")
public class FabricSkyBoxesClient {

    public static final String MODID = "customskyboxes";
    public static final Logger LOGGER = LogManager.getLogger();

    public FabricSkyBoxesClient() {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        SkyboxType.init(bus);
    }

    public static Logger getLogger() {
        return LOGGER;
    }
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