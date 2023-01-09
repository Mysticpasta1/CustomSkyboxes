package vice.customskyboxes.resource;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraftforge.registries.RegistryObject;
import vice.customskyboxes.FabricSkyBoxesClient;
import vice.customskyboxes.SkyboxManager;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.MonoColorSkybox;
import vice.customskyboxes.skyboxes.SkyboxType;
import vice.customskyboxes.util.JsonObjectWrapper;
import vice.customskyboxes.util.object.internal.Metadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

public class SkyboxResourceListener implements ResourceManagerReloadListener
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();
    private static final JsonObjectWrapper objectWrapper = new JsonObjectWrapper();

    private static AbstractSkybox parseSkyboxJson(Resource id) {
        AbstractSkybox skybox;
        Metadata metadata;
        FabricSkyBoxesClient.LOGGER.info("parseSkyboxJson for " + id);

        try {
            metadata = Metadata.CODEC.decode(JsonOps.INSTANCE, objectWrapper.getFocusedObject()).getOrThrow(false, System.err::println).getFirst();
        } catch (RuntimeException e) {
            FabricSkyBoxesClient.getLogger().warn("Skipping invalid skybox " + id.toString(), e);
            FabricSkyBoxesClient.getLogger().warn(objectWrapper.toString());
            return null;
        }

        FabricSkyBoxesClient.LOGGER.info("decoded metadata for " + id);

        System.out.println(SkyboxType.SkyboxTypes.get(new ResourceLocation(FabricSkyBoxesClient.MODID, "single_sprite_square_texture")));

        SkyboxType<? extends AbstractSkybox> type = SkyboxType.SkyboxTypes.get(metadata.getType()).get();

        Preconditions.checkNotNull(type, "Unknown skybox type: " + metadata.getType().getPath().replace('_', '-'));
        if (metadata.getSchemaVersion() == 1) {
            Preconditions.checkArgument(type.isLegacySupported(), "Unsupported schema version '1' for skybox type " + type.getName());
            FabricSkyBoxesClient.getLogger().debug("Using legacy deserializer for skybox " + id.toString());
            skybox = type.instantiate();
            //noinspection ConstantConditions
            type.getDeserializer().getDeserializer().accept(objectWrapper, skybox);
        } else {
            FabricSkyBoxesClient.LOGGER.info("getSchemaVersion for " + id);

            skybox = type.getCodec(metadata.getSchemaVersion())
                    .decode(JsonOps.INSTANCE, objectWrapper.getFocusedObject())
                    .getOrThrow(false, System.err::println).getFirst();
        }

        FabricSkyBoxesClient.LOGGER.info("returning skybox for " + id);
        return skybox;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();

        // clear registered skyboxes on reload
        skyboxManager.clearSkyboxes();

        // load new skyboxes
        Collection<Resource> resources = manager.listResources("sky", (string) -> !string.getNamespace().endsWith("json")).values();

        for (Resource id : resources) {

            Resource resource;

            try {
                resource = id;
                try {
                    JsonObject json = GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class);
                    objectWrapper.setFocusedObject(json);
                    AbstractSkybox skybox = SkyboxResourceListener.parseSkyboxJson(id);
                    if (skybox != null) {
                        skyboxManager.addSkybox(skybox);
                    }
                } catch (IOException e) {
                    FabricSkyBoxesClient.getLogger().error("Error reading skybox " + id);
                    e.printStackTrace();
                }
            } catch (JsonSyntaxException | JsonIOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}