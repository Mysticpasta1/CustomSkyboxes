package vice.customskyboxes.skyboxes;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import vice.customskyboxes.SkyboxManager;
import vice.customskyboxes.mixin.skybox.WorldRendererAccess;
import vice.customskyboxes.util.Utils;
import vice.customskyboxes.util.object.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * All classes that implement {@link AbstractSkybox} should
 * have a default constructor as it is required when checking
 * the type of the skybox.
 */
public abstract class AbstractSkybox {
    /**
     * The current alpha for the skybox. Expects all skyboxes extending this to accommodate this.
     * This variable is responsible for fading in/out skyboxes.
     */
    public transient float alpha;

    // ! These are the options variables.  Do not mess with these.
    protected Fade fade = Fade.ZERO;
    protected float maxAlpha = 1f;
    protected float transitionSpeed = 1;
    protected boolean changeFog = false;
    protected RGBA fogColors = RGBA.ZERO;
    protected boolean renderSunSkyColorTint = true;
    protected boolean shouldRotate = false;
    protected List<String> weather = new ArrayList<>();
    protected List<ResourceLocation> biomes = new ArrayList<>();
    protected Decorations decorations = Decorations.DEFAULT;
    /**
     * Stores identifiers of <b>worlds</b>, not dimension types.
     */
    protected List<ResourceLocation> worlds = new ArrayList<>();
    protected List<HeightEntry> heightRanges = Lists.newArrayList();

    /**
     * The main render method for a skybox.
     * Override this if you are creating a skybox from this one.
     *
     * @param worldRendererAccess Access to the worldRenderer as skyboxes often require it.
     * @param matrices            The current PoseStack.
     * @param tickDelta           The current tick delta.
     */
    public abstract void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta);

    protected AbstractSkybox() {
    }

    protected AbstractSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations) {
        this.fade = properties.getFade();
        this.maxAlpha = properties.getMaxAlpha();
        this.transitionSpeed = properties.getTransitionSpeed();
        this.changeFog = properties.isChangeFog();
        this.fogColors = properties.getFogColors();
        this.renderSunSkyColorTint = properties.isRenderSunSkyTint();
        this.shouldRotate = properties.isShouldRotate();
        this.weather = conditions.getWeathers().stream().map(Weather::toString).distinct().collect(Collectors.toList());
        this.biomes = conditions.getBiomes();
        this.worlds = conditions.getWorlds();
        this.heightRanges = conditions.getHeights();
        this.decorations = decorations;
    }

    /**
     * Calculates the alpha value for the current time and conditions and returns it.
     *
     * @return The new alpha value.
     */
    public final float getAlpha() {
        if (!fade.isAlwaysOn()) {
            int currentTime = (int) Objects.requireNonNull(Minecraft.getInstance().level).getDayTime() % 24000; // modulo so that it's bound to 24000
            int durationin = Utils.getTicksBetween(this.fade.getStartFadeIn(), this.fade.getEndFadeIn());
            int durationout = Utils.getTicksBetween(this.fade.getStartFadeOut(), this.fade.getEndFadeOut());

            int startFadeIn = this.fade.getStartFadeIn() % 24000;
            int endFadeIn = this.fade.getEndFadeIn() % 24000;

            if (endFadeIn < startFadeIn) {
                endFadeIn += 24000;
            }

            int startFadeOut = this.fade.getStartFadeOut() % 24000;
            int endFadeOut = this.fade.getEndFadeOut() % 24000;

            if (startFadeOut < endFadeIn) {
                startFadeOut += 24000;
            }

            if (endFadeOut < startFadeOut) {
                endFadeOut += 24000;
            }

            int tempInTime = currentTime;

            if (tempInTime < startFadeIn) {
                tempInTime += 24000;
            }

            int tempFullTime = currentTime;

            if (tempFullTime < endFadeIn) {
                tempFullTime += 24000;
            }

            int tempOutTime = currentTime;

            if (tempOutTime < startFadeOut) {
                tempOutTime += 24000;
            }

            float maxPossibleAlpha;

            if (startFadeIn < tempInTime && endFadeIn >= tempInTime) {
                maxPossibleAlpha = 1f - (((float) (endFadeIn - tempInTime)) / durationin); // fading in

            } else if (endFadeIn < tempFullTime && startFadeOut >= tempFullTime) {
                maxPossibleAlpha = 1f; // fully faded in

            } else if (startFadeOut < tempOutTime && endFadeOut >= tempOutTime) {
                maxPossibleAlpha = (float) (endFadeOut - tempOutTime) / durationout; // fading out

            } else {
                maxPossibleAlpha = 0f; // default not showing
            }

            maxPossibleAlpha *= maxAlpha;
            if (checkBiomes() && checkHeights() && checkWeather() && checkEffect()) { // check if environment is invalid
                if (alpha >= maxPossibleAlpha) {
                    alpha = maxPossibleAlpha;
                } else {
                    alpha += transitionSpeed;
                    if (alpha > maxPossibleAlpha) alpha = maxPossibleAlpha;
                }
            } else {
                if (alpha > 0f) {
                    alpha -= transitionSpeed;
                    if (alpha < 0f) alpha = 0f;
                } else {
                    alpha = 0f;
                }
            }
        } else {
            alpha = 1f;
        }

        if (alpha > SkyboxManager.MINIMUM_ALPHA) {
            if (changeFog) {
                SkyboxManager.shouldChangeFog = true;
                SkyboxManager.fogRed = this.fogColors.getRed();
                SkyboxManager.fogBlue = this.fogColors.getBlue();
                SkyboxManager.fogGreen = this.fogColors.getGreen();
            }
            if (!renderSunSkyColorTint) {
                SkyboxManager.renderSunriseAndSet = false;
            }
        }

        // sanity checks
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;

        return alpha;
    }

    /**
     * @return Whether the current biomes and dimensions are valid for this skybox.
     */
    protected boolean checkBiomes() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);
        if (worlds.isEmpty()|| worlds.contains(client.level.dimension().location())) {
            if (biomes.isEmpty()) return true;
            assert client.player != null;
            return biomes.contains(client.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(client.level.getBiome(client.player.blockPosition()).value()));
        }
        return false;
    }
    
    /*
		Check if player has an effect that should prevent skybox from showing
     */
    protected boolean checkEffect() {
    	Player player = Minecraft.getInstance().player;
    	Collection<MobEffectInstance> activeEffects = player.getActiveEffects();
    	if (!activeEffects.isEmpty()) {
    		for (MobEffectInstance statusEffectInstance : Ordering.natural().reverse().sortedCopy(activeEffects)) {
                MobEffect statusEffect = statusEffectInstance.getEffect();
    			if (statusEffect.equals(MobEffects.BLINDNESS)) {
    				return false;
    			}
    		}
    	}
    	return true;
    }

    /**
     * @return Whether the current heights are valid for this skybox.
     */
    protected boolean checkHeights() {
        double playerHeight = Objects.requireNonNull(Minecraft.getInstance().player).getY();
        boolean inRange = false;
        for (HeightEntry heightRange : this.heightRanges) {
            inRange = heightRange.getMin() < playerHeight && heightRange.getMax() > playerHeight;
            if (inRange) break;
        }
        return this.heightRanges.isEmpty() || inRange;
    }

    /**
     * @return Whether the current weather is valid for this skybox.
     */
    protected boolean checkWeather() {
        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        Player player = Objects.requireNonNull(Minecraft.getInstance().player);
        Biome.Precipitation precipitation = world.getBiome(player.blockPosition()).value().getPrecipitation();
        if (weather.size() > 0) {
            if (weather.contains("thunder") && world.isThundering()) {
                return true;
            } else if (weather.contains("snow") && world.isRaining() && precipitation == Biome.Precipitation.SNOW) {
                return true;
            } else if (weather.contains("rain") && world.isRaining() && !world.isThundering()) {
                return true;
            } else return weather.contains("clear");
        } else {
            return true;
        }
    }

    public abstract SkyboxType<? extends AbstractSkybox> getType();

    public void renderDecorations(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, BufferBuilder bufferBuilder, float alpha) {
        if (!SkyboxManager.getInstance().hasRenderedDecorations())
        {
            Vector3f rotationStatic = decorations.getRotation().getStatic();
            Vector3f rotationAxis = decorations.getRotation().getAxis();

            RenderSystem.enableTexture();
            matrices.pushPose();
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
            ClientLevel world = Minecraft.getInstance().level;
            assert world != null;
            RenderSystem.enableTexture();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationAxis.x()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationAxis.y()));
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationAxis.z()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
            matrices.mulPose(Vector3f.XP.rotationDegrees(world.getTimeOfDay(tickDelta) * 360.0F * decorations.getRotation().getRotationSpeed()));
            matrices.mulPose(Vector3f.ZN.rotationDegrees(rotationAxis.z()));
            matrices.mulPose(Vector3f.YN.rotationDegrees(rotationAxis.y()));
            matrices.mulPose(Vector3f.XN.rotationDegrees(rotationAxis.x()));
            float r = 1.0F - world.getRainLevel(tickDelta);
            // sun
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            Matrix4f matrix4f2 = matrices.last().pose();
            float s = 30.0F;
            if (decorations.isSunEnabled()) {
                Minecraft.getInstance().getTextureManager().bindForSetup(this.decorations.getSunTexture());
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix4f2, -s, 100.0F, -s).uv(0.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, s, 100.0F, -s).uv(1.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, s, 100.0F, s).uv(1.0F, 1.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, -s, 100.0F, s).uv(0.0F, 1.0F).endVertex();
                BufferUploader.draw(bufferBuilder.end());
            }
            // moon
            s = 20.0F;
            if (decorations.isMoonEnabled()) {
                Minecraft.getInstance().getTextureManager().bindForSetup(this.decorations.getMoonTexture());
                int t = world.getMoonPhase();
                int u = t % 4;
                int v = t / 4 % 2;
                float w = (float) (u) / 4.0F;
                float o = (float) (v) / 2.0F;
                float p = (float) (u + 1) / 4.0F;
                float q = (float) (v + 1) / 2.0F;
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix4f2, -s, -100.0F, s).uv(p, q).endVertex();
                bufferBuilder.vertex(matrix4f2, s, -100.0F, s).uv(w, q).endVertex();
                bufferBuilder.vertex(matrix4f2, s, -100.0F, -s).uv(w, o).endVertex();
                bufferBuilder.vertex(matrix4f2, -s, -100.0F, -s).uv(p, o).endVertex();
                BufferUploader.draw(bufferBuilder.end());
            }
            // stars
            if (decorations.isStarsEnabled()) {
                RenderSystem.disableTexture();
                float aa = world.getStarBrightness(tickDelta) * r;
                if (aa > 0.0F) {
                    RenderSystem.setShaderColor(aa, aa, aa, aa);
                    worldRendererAccess.getStarBuffer().bind();
                    worldRendererAccess.getStarBuffer().draw();
                    VertexBuffer.unbind();
                }
            }
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            matrices.popPose();
        }
    }

    public Fade getFade() {
        return this.fade;
    }

    public float getMaxAlpha() {
        return this.maxAlpha;
    }

    public float getTransitionSpeed() {
        return this.transitionSpeed;
    }

    public boolean isChangeFog() {
        return this.changeFog;
    }

    public RGBA getFogColors() {
        return this.fogColors;
    }

    public boolean isRenderSunSkyColorTint() {
        return this.renderSunSkyColorTint;
    }

    public boolean isShouldRotate() {
        return this.shouldRotate;
    }

    public Decorations getDecorations() {
        return this.decorations;
    }

    public List<String> getWeather() {
        return this.weather;
    }

    public List<ResourceLocation> getBiomes() {
        return this.biomes;
    }

    public List<ResourceLocation> getWorlds() {
        return this.worlds;
    }

    public List<HeightEntry> getHeightRanges() {
        return this.heightRanges;
    }

    public DefaultProperties getDefaultProperties() {
        return DefaultProperties.ofSkybox(this);
    }

    public Conditions getConditions() {
        return Conditions.ofSkybox(this);
    }
}
