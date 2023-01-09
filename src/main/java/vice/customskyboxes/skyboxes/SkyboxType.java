package vice.customskyboxes.skyboxes;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import vice.customskyboxes.FabricSkyBoxesClient;
import vice.customskyboxes.skyboxes.textured.AnimatedSquareTexturedSkybox;
import vice.customskyboxes.skyboxes.textured.SingleSpriteAnimatedSquareTexturedSkybox;
import vice.customskyboxes.skyboxes.textured.SingleSpriteSquareTexturedSkybox;
import vice.customskyboxes.skyboxes.textured.SquareTexturedSkybox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkyboxType<T extends AbstractSkybox> {
    private final BiMap<Integer, Codec<T>> codecBiMap;
    private final boolean legacySupported;
    private final String name;
    @Nullable
    private final Supplier<T> factory;
    @Nullable
    private final LegacyDeserializer<T> deserializer;

    public SkyboxType(BiMap<Integer, Codec<T>> codecBiMap, boolean legacySupported, String name, @Nullable Supplier<T> factory, @Nullable LegacyDeserializer<T> deserializer) {
        this.codecBiMap = codecBiMap;
        this.legacySupported = legacySupported;
        this.name = name;
        this.factory = factory;
        this.deserializer = deserializer;
    }

    public String getName() {
        return this.name;
    }

    public boolean isLegacySupported() {
        return this.legacySupported;
    }

    @NotNull
    public T instantiate() {
        return Objects.requireNonNull(Objects.requireNonNull(this.factory, "Can't instantiate from a null factory").get());
    }

    @Nullable
    public LegacyDeserializer<T> getDeserializer() {
        return this.deserializer;
    }

    public ResourceLocation createId(String namespace) {
        return this.createIdFactory().apply(namespace);
    }

    public Function<String, ResourceLocation> createIdFactory() {
        return (ns) -> new ResourceLocation(ns, this.getName().replace('-', '_'));
    }

    public Codec<T> getCodec(int schemaVersion) {
        return Objects.requireNonNull(this.codecBiMap.get(schemaVersion), String.format("Unsupported schema version '%d' for skybox type %s", schemaVersion, this.name));
    }

    public static DeferredRegister<SkyboxType<? extends AbstractSkybox>> REGISTER = DeferredRegister.create(new ResourceLocation(FabricSkyBoxesClient.MODID, "skybox_type"), FabricSkyBoxesClient.MODID);

    public static Map<ResourceLocation, RegistryObject<SkyboxType<? extends AbstractSkybox>>> SkyboxTypes = new HashMap<>();

    public static void init(IEventBus bus) {
        REGISTER.makeRegistry(() ->
                new RegistryBuilder<SkyboxType<? extends AbstractSkybox>>().setMaxID(Integer.MAX_VALUE - 1).onAdd((owner, stage, id, key, obj, old) ->
                        FabricSkyBoxesClient.LOGGER.info("SkyboxType Added: " + key.location() + " ")
                ).setDefaultKey(new ResourceLocation(FabricSkyBoxesClient.MODID, "null"))
        );
        REGISTER.register(bus);
    }

    public static RegistryObject<SkyboxType<? extends AbstractSkybox>> MONO_COLOR_SKYBOX =
            REGISTER.register("monocolor", () ->
            Builder.create(MonoColorSkybox.class, "monocolor")
                    .legacySupported()
                    .deserializer(LegacyDeserializer.MONO_COLOR_SKYBOX_DESERIALIZER)
                    .factory(MonoColorSkybox::new)
                    .add(2, MonoColorSkybox.CODEC)
                    .build()
    );

    public static RegistryObject<SkyboxType<? extends AbstractSkybox>> SQUARE_TEXTURED_SKYBOX =
            REGISTER.register("square-textured", () ->
            Builder.create(SquareTexturedSkybox.class, "square-textured")
                    .deserializer(LegacyDeserializer.SQUARE_TEXTURED_SKYBOX_DESERIALIZER)
                    .legacySupported()
                    .factory(SquareTexturedSkybox::new)
                    .add(2, SquareTexturedSkybox.CODEC)
                    .build()
    );

    public static RegistryObject<SkyboxType<? extends AbstractSkybox>> SINGLE_SPRITE_SQUARE_TEXTURED_SKYBOX =
            REGISTER.register("single-sprite-square-textured", () ->
            Builder.create(SingleSpriteSquareTexturedSkybox.class, "single-sprite-square-textured")
                    .add(2, SingleSpriteSquareTexturedSkybox.CODEC)
                    .build()
    );

    public static RegistryObject<SkyboxType<? extends AbstractSkybox>> ANIMATED_SQUARE_TEXTURED_SKYBOX =
            REGISTER.register("animated-square-textured", () ->
            Builder.create(AnimatedSquareTexturedSkybox.class, "animated-square-textured")
                    .add(2, AnimatedSquareTexturedSkybox.CODEC)
                    .build()
    );

    public static RegistryObject<SkyboxType<? extends AbstractSkybox>> SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED_SKYBOX =
            REGISTER.register("single-sprite-animated-square-textured", () ->
            Builder.create(SingleSpriteAnimatedSquareTexturedSkybox.class, "single-sprite-animated-square-textured")
                    .add(2, SingleSpriteAnimatedSquareTexturedSkybox.CODEC)
                    .build()
    );

    public static Codec<ResourceLocation> SKYBOX_ID_CODEC = Codec.STRING.xmap((s) -> {
        if (!s.contains(":")) {
            return new ResourceLocation(FabricSkyBoxesClient.MODID, s.replace('-', '_'));
        }
        return new ResourceLocation(s.replace('-', '_'));
    }, (id) -> {
        if (id.getNamespace().equals(FabricSkyBoxesClient.MODID)) {
            return id.getPath().replace('_', '-');
        }
        return id.toString().replace('_', '-');
    });

    public static class Builder<T extends AbstractSkybox> {
        private String name;
        private final ImmutableBiMap.Builder<Integer, Codec<T>> builder = ImmutableBiMap.builder();
        private boolean legacySupported = false;
        private Supplier<T> factory;
        private LegacyDeserializer<T> deserializer;

        private Builder() {
        }

        public static <S extends AbstractSkybox> Builder<S> create(@SuppressWarnings("unused") Class<S> clazz, String name) {
            Builder<S> builder = new Builder<>();
            builder.name = name;
            return builder;
        }

        public static <S extends AbstractSkybox> Builder<S> create(String name) {
            Builder<S> builder = new Builder<>();
            builder.name = name;
            return builder;
        }

        protected Builder<T> legacySupported() {
            this.legacySupported = true;
            return this;
        }

        protected Builder<T> factory(Supplier<T> factory) {
            this.factory = factory;
            return this;
        }

        protected Builder<T> deserializer(LegacyDeserializer<T> deserializer) {
            this.deserializer = deserializer;
            return this;
        }

        public Builder<T> add(int schemaVersion, Codec<T> codec) {
            Preconditions.checkArgument(schemaVersion >= 2, "schema version was lesser than 2");
            Preconditions.checkNotNull(codec, "codec was null");
            this.builder.put(schemaVersion, codec);
            return this;
        }

        public SkyboxType<T> build() {
            if (this.legacySupported) {
                Preconditions.checkNotNull(this.factory, "factory was null");
                Preconditions.checkNotNull(this.deserializer, "deserializer was null");
            }
            return new SkyboxType<>(this.builder.build(), this.legacySupported, this.name, this.factory, this.deserializer);
        }
    }
}
