package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticData;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticDataProvider;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.valor.valors_ammo_util.ValorAmmoPayload;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.ValorMetaKeys;
import com.valor.valors_ammo_util.component.AmmoInfoComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class ValorAmmoProjectile extends SimpleInstantInteraction implements BallisticDataProvider {
    public static final BuilderCodec<ValorAmmoProjectile> CODEC;
    private String config;

    @Nullable
    public ProjectileConfig getConfig() {
        return ProjectileConfig.getAssetMap().getAsset(this.config);
    }

    @Nullable
    public BallisticData getBallisticData() {
        return this.getConfig();
    }

    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client;
    }

    public boolean needsRemoteSync() {
        return true;
    }

    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        ProjectileConfig config = this.getConfig();
        if (config != null) {
            // All copied from ProjectileInteraction.class
            InteractionSyncData clientState = context.getClientState();
            Ref<EntityStore> ref = context.getEntity();
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

            assert commandBuffer != null;

            boolean hasClientState = clientState != null && clientState.attackerPos != null && clientState.attackerRot != null;
            Vector3d position;
            Vector3d direction;
            UUID generatedUUID;
            if (hasClientState) {
                position = PositionUtil.toVector3d(clientState.attackerPos);
                Vector3f lookVec = PositionUtil.toRotation(clientState.attackerRot);
                direction = new Vector3d(lookVec.getYaw(), lookVec.getPitch());
                generatedUUID = clientState.generatedUUID;
            } else {
                Transform lookVec = TargetUtil.getLook(ref, commandBuffer);
                position = lookVec.getPosition();
                direction = lookVec.getDirection();
                generatedUUID = null;
            }

            Ref<EntityStore> projectile = ProjectileModule.get().spawnProjectile(generatedUUID, ref, commandBuffer, config, position, direction);

            // VAU Logic starts here
            ValorAmmoPayload ammoPayload = context.getMetaStore().getMetaObject(ValorMetaKeys.getMetaKey());
            if (ammoPayload == null) return;

            // Attach ammo info component
            AmmoInfoComponent ammoInfoComponent = new AmmoInfoComponent(ammoPayload.getAmmoItemId(), ammoPayload.getOnHitId(), ammoPayload.getOnMissId());
            context.getCommandBuffer().addComponent(projectile, ValorAmmoUtil.getAmmoInfoComponentType(), ammoInfoComponent);

            // Change projectile ModelAsset based on ammo used
            if (!ammoPayload.getUseModel()) return;

            String modelAssetId = ammoPayload.getModelAssetId();
            if (modelAssetId == null) {
                ValorAmmoUtil.LOGGER.atWarning().log("ModelId not provided for Ammo Item");
                return;
            }

            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
            if (modelAsset == null) {
                ValorAmmoUtil.LOGGER.atWarning().log("ModelAsset " + modelAssetId + " is undefined");
                return;
            }

            Model newModel = Model.createScaledModel(modelAsset, 1);
            context.getCommandBuffer().replaceComponent(projectile, ModelComponent.getComponentType(), new ModelComponent(newModel));

        }
    }

    protected void simulateFirstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        assert commandBuffer != null;

        Ref<EntityStore> ref = context.getEntity();
        Transform lookVec = TargetUtil.getLook(ref, commandBuffer);
        InteractionSyncData state = context.getState();
        state.attackerPos = PositionUtil.toPositionPacket(lookVec.getPosition());
        Vector3f rotation = lookVec.getRotation();
        state.attackerRot = new Direction(rotation.getYaw(), rotation.getPitch(), rotation.getRoll());
    }

    @Nonnull
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.ProjectileInteraction();
    }

    protected void configurePacket(Interaction packet) {
        super.configurePacket(packet);
        com.hypixel.hytale.protocol.ProjectileInteraction p = (com.hypixel.hytale.protocol.ProjectileInteraction)packet;
        ProjectileConfig config = this.getConfig();
        if (config == null) {
            String var10002 = this.getId();
            throw new IllegalStateException("ProjectileInteraction '" + var10002 + "' has no valid ProjectileConfig: " + this.config);
        } else {
            p.configId = this.config;
        }
    }

    static {
        CODEC = ((BuilderCodec.builder(ValorAmmoProjectile.class, ValorAmmoProjectile::new, SimpleInstantInteraction.CODEC)
                .documentation("Fires a projectile modified by ammo used previously in the Interaction chain."))
                .appendInherited(new KeyedCodec<>("Config", Codec.STRING), (o, i) -> o.config = i, (o) -> o.config, (o, p) -> o.config = p.config)
                .addValidator(ProjectileConfig.VALIDATOR_CACHE.getValidator().late())
                .documentation("The ID of the projectile config asset to use for the projectile.")
                .add())
                .build();
    }
}
