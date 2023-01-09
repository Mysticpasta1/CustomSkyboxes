package vice.customskyboxes.resource;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import vice.customskyboxes.FabricSkyBoxesClient;
import vice.customskyboxes.SkyboxManager;
import vice.customskyboxes.skyboxes.AbstractSkybox;
import vice.customskyboxes.skyboxes.SkyboxType;
import vice.customskyboxes.util.JsonObjectWrapper;
import vice.customskyboxes.util.object.internal.Metadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SkyboxResourceListener implements PreparableReloadListener
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();
    private static final JsonObjectWrapper objectWrapper = new JsonObjectWrapper();

    private static SkyboxType<? extends AbstractSkybox> getSkyboxes() {
        SkyboxType<? extends AbstractSkybox> skyType = null;
        for (RegistryObject<SkyboxType<? extends AbstractSkybox>> skyboxType : SkyboxType.Builder.REGISTER.getEntries()) {
            skyType = skyboxType.get();
        }
        return skyType;
    }

    private static AbstractSkybox parseSkyboxJson(ResourceLocation id) {
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

        SkyboxType<? extends AbstractSkybox> type = getSkyboxes();

        Preconditions.checkNotNull(type, "Unknown skybox type: " + metadata.getType().getPath().replace('_', '-'));
        if (metadata.getSchemaVersion() == 1)
        {
            Preconditions.checkArgument(type.isLegacySupported(), "Unsupported schema version '1' for skybox type " + type.getName());
            FabricSkyBoxesClient.getLogger().debug("Using legacy deserializer for skybox " + id.toString());
            skybox = type.instantiate();
            //noinspection ConstantConditions
            type.getDeserializer().getDeserializer().accept(objectWrapper, skybox);
        }
        else
        {
            FabricSkyBoxesClient.LOGGER.info("getSchemaVersion for " + id);

            skybox = type.getCodec(metadata.getSchemaVersion())
                    .decode(JsonOps.INSTANCE, objectWrapper.getFocusedObject())
                    .getOrThrow(false, System.err::println).getFirst();
        }

        FabricSkyBoxesClient.LOGGER.info("returning skybox for " + id);
        return skybox;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier pPreparationBarrier, @NotNull ResourceManager manager, ProfilerFiller pPreparationsProfiler, ProfilerFiller pReloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return new CompletableFuture<>().thenRun(() -> {
            SkyboxManager skyboxManager = SkyboxManager.getInstance();

            // clear registered skyboxes on reload
            skyboxManager.clearSkyboxes();

            // load new skyboxes
            Collection<ResourceLocation> resources = manager.listResources("sky", (string) -> string.getNamespace().endsWith(".json")).keySet();

            for (ResourceLocation id : resources) {

                Resource resource;
                try {
                    resource = manager.getResource(id).get();
                    try {
                        JsonObject json = GSON.fromJson(new InputStreamReader(resource.open()), JsonObject.class);
                        objectWrapper.setFocusedObject(json);
                        AbstractSkybox skybox = SkyboxResourceListener.parseSkyboxJson(id);
                        if (skybox != null) {
                            skyboxManager.addSkybox(skybox);
                        }

                    } catch (IOException e) {
                        FabricSkyBoxesClient.getLogger().error("Error reading skybox " + id.toString());
                        e.printStackTrace();
                    }
                } catch (JsonSyntaxException | JsonIOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}