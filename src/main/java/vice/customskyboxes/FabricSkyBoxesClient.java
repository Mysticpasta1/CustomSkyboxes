package vice.customskyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import vice.customskyboxes.resource.SkyboxResourceListener;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.SkyboxType;

@Mod("customskyboxes")
public class FabricSkyBoxesClient {

    public static final String MODID = "customskyboxes";
    public static final Logger LOGGER = LogManager.getLogger();

    public FabricSkyBoxesClient() {
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        SkyboxType.init(bus);

        for (RegistryObject<SkyboxType<? extends AbstractSkybox>> skyboxTypeRegistryObject : SkyboxType.REGISTER.getEntries()) {
            SkyboxType.SkyboxTypes.put(new ResourceLocation(FabricSkyBoxesClient.MODID, skyboxTypeRegistryObject.getId().getPath()), skyboxTypeRegistryObject); //adds all 5 SkyboxTypes
        }

        System.out.println(SkyboxType.SkyboxTypes.values()); //null, no SkyboxTypes found

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