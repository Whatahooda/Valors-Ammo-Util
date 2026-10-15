package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.HashUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.ModelOverride;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionEntry;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.component.ImpactModifiers;
import com.hypixel.hytale.server.core.modules.projectile.component.ProjectileLaunchContext;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticData;
import com.hypixel.hytale.server.core.modules.projectile.config.BallisticDataProvider;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.event.ProjectileLaunchEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.valor.valors_ammo_util.AmmoToStore;
import com.valor.valors_ammo_util.ValorAmmoPayload;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.component.AmmoInfoComponent;
import com.valor.valors_ammo_util.component.LoadedAmmoComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public class AmmoProjectileInteraction extends SimpleInstantInteraction implements BallisticDataProvider {
    public static final BuilderCodec<AmmoProjectileInteraction> CODEC;
    private String config;
    protected boolean ignorePitch;
    protected boolean ignoreYaw;
    protected boolean ignoreRoll;
    protected float lifetime;

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
            Ref<EntityStore> owningEntity = context.getOwningEntity();
            Ref<EntityStore> creatorRef = owningEntity != null && owningEntity.isValid() ? owningEntity : context.getEntity();
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

            assert commandBuffer != null;

            InteractionSyncData clientState = context.getClientState();
            UUID generatedUUID = null;
            if (clientState != null && clientState.generatedUUID != null) {
                generatedUUID = clientState.generatedUUID;
            }

            Transform spawnTransform = this.getProjectileSpawnSource(clientState, context);
            Rotation3f rotation = spawnTransform.getRotation();
            if (this.ignorePitch) {
                rotation.setPitch(0.0F);
            }

            if (this.ignoreYaw) {
                rotation.setYaw(0.0F);
            }

            if (this.ignoreRoll) {
                rotation.setRoll(0.0F);
            }

            InteractionChain chain = context.getChain();
            InteractionEntry entry = context.getEntry();
            Long scaleSeed = chain != null && entry != null ? HashUtil.hash((long)chain.getChainId(), (long)entry.getIndex()) : null;
            Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(generatedUUID, scaleSeed, creatorRef, commandBuffer, config, spawnTransform.getPosition(), spawnTransform.getDirection(), (long)(this.lifetime * 1000.0F));


            ImpactModifiers impactModifiers = ImpactModifiers.collect(context, creatorRef, commandBuffer);
            ItemStack heldItem = context.getHeldItem();
            ProjectileLaunchContext launchContext = new ProjectileLaunchContext(type, heldItem != null && heldItem.getItem().getWeapon() != null);
            commandBuffer.run((store) -> {
                if (projectileRef.isValid()) {
                    if (impactModifiers != null) {
                        store.putComponent(projectileRef, ImpactModifiers.getComponentType(), impactModifiers);
                    }

                    store.putComponent(projectileRef, ProjectileLaunchContext.getComponentType(), launchContext);
                }
            });
            Ref<EntityStore> launcherRef = context.getEntity();
            if (launcherRef.isValid()) {
                commandBuffer.invoke(launcherRef, new ProjectileLaunchEvent(projectileRef, type, launchContext.isLaunchedByWeapon()));
            }

            // VAU Logic starts here
            // Get ammo info from the held item metadata
            if (heldItem == null) {
                ValorAmmoUtil.LOGGER.atWarning().log("heldItem is null");
                return;
            }

            LoadedAmmoComponent loadedAmmoComponent = commandBuffer.getComponent(creatorRef, ValorAmmoUtil.getLoadedAmmoComponentType());
            if (loadedAmmoComponent == null) {
                return;
            }

            String[] itemIdsRaw = loadedAmmoComponent.getItemIds();
            int[] itemQuantitiesRaw = loadedAmmoComponent.getItemQuantities();

            // If there's no ammo info, stop here
            AmmoToStore ammoToUse = new AmmoToStore(itemIdsRaw, itemQuantitiesRaw);
            if (ammoToUse.size() < 1) {
                return;
            }

            Item ammoItem = Item.getAssetMap().getAsset(ammoToUse.getItemId(0));
            assert ammoItem != null;

            ValorAmmoPayload ammoPayload = ValorAmmoPayload.generateAmmoPayload(ammoItem, true, AmmoInfo.AMMO_INFO_VAR_ID);

            // Now remove 1 from the used item quantity and apply the change to the held item
            ammoToUse.useItem();

            ArrayList<String> nextIds = new ArrayList<>();
            ArrayList<Integer> nextQty = new ArrayList<>();
            for (int idx = 0; idx < ammoToUse.size(); idx++) {
                nextIds.add(ammoToUse.getItemId(idx));
                nextQty.add(ammoToUse.getItemQuantity(idx));
            }
            LoadedAmmoComponent updatedLoadedAmmo = new LoadedAmmoComponent(
                    AmmoToStore.listToArray(nextIds),
                    nextQty.stream().mapToInt(Integer::intValue).toArray()
            );
            commandBuffer.replaceComponent(creatorRef, ValorAmmoUtil.getLoadedAmmoComponentType(), updatedLoadedAmmo);

//            EntityStatMap statMap = context.getEntity().getStore().getComponent(context.getEntity(), EntityStatMap.getComponentType());
//            if (statMap != null) {
//            statMap.setStatValue(DefaultEntityStatTypes.getAmmo(), AmmoToStore.getStoredAmmoCount(
//                updatedLoadedAmmo.getItemIds(),
//                updatedLoadedAmmo.getItemQuantities()
//            ));
//            }

            // Attach ammo info component
            AmmoInfoComponent ammoInfoComponent = new AmmoInfoComponent(ammoPayload.getAmmoItemId(), ammoPayload.getModelAssetId(), ammoPayload.getOnHitId(), ammoPayload.getOnMissId());
            commandBuffer.addComponent(projectileRef, ValorAmmoUtil.getAmmoInfoComponentType(), ammoInfoComponent);

            // Change projectile ModelAsset based on ammo used
            if (!ammoPayload.getUseModel()) {
                return;
            }

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
            commandBuffer.replaceComponent(
                    projectileRef,
                    ModelComponent.getComponentType(),
                    new ModelComponent(newModel)
            );

            commandBuffer.replaceComponent(
                    projectileRef,
                    PersistentModel.getComponentType(),
                    new PersistentModel(newModel.toReference())
            );
            if (newModel.getBoundingBox() != null) commandBuffer.replaceComponent(
                    projectileRef,
                    BoundingBox.getComponentType(),
                    new BoundingBox(newModel.getBoundingBox())
            );
        }
    }

    protected void simulateFirstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

        assert commandBuffer != null;

        Ref<EntityStore> ref = context.getEntity();
        Transform lookVec = TargetUtil.getLook(ref, commandBuffer);
        InteractionSyncData state = context.getState();
        state.attackerPos = PositionUtil.toPositionPacket(lookVec.getPosition());
        Rotation3f rotation = lookVec.getRotation();
        state.attackerRot = new Direction(rotation.yaw(), rotation.pitch(), rotation.roll());
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
            p.ignorePitch = this.ignorePitch;
            p.ignoreYaw = this.ignoreYaw;
            p.ignoreRoll = this.ignoreRoll;
        }
    }

    @Nonnull
    private Transform getProjectileSpawnSource(@Nullable InteractionSyncData clientData, @Nonnull InteractionContext context) {
        Transform serverTransform = TargetUtil.getLook(context.getEntity(), context.getCommandBuffer());
        if (clientData != null && clientData.attackerPos != null && clientData.attackerRot != null) {
            double distSq = serverTransform.getPosition().distanceSquared(clientData.attackerPos.x, clientData.attackerPos.y, clientData.attackerPos.z);
            Velocity velocityComponent = (Velocity)context.getCommandBuffer().getComponent(context.getEntity(), Velocity.getComponentType());
            double clientVelocityLengthSq = velocityComponent != null ? velocityComponent.getClientVelocity().lengthSquared() : (double)0.0F;
            double desyncAllowanceSq = Math.max(clientVelocityLengthSq, (double)0.5F);
            if (distSq > desyncAllowanceSq * (double)1.25F) {
                ValorAmmoUtil.LOGGER.at(Level.WARNING).log("%s was too far from requested projectile spawn position (%f > %f)", EntityUtils.getEntityName(context.getEntity(), context.getCommandBuffer()), distSq, desyncAllowanceSq);
                return serverTransform;
            } else {
                return new Transform(clientData.attackerPos.x, clientData.attackerPos.y, clientData.attackerPos.z, clientData.attackerRot.pitch, clientData.attackerRot.yaw, clientData.attackerRot.roll);
            }
        } else {
            return serverTransform;
        }
    }

    static {
        CODEC = BuilderCodec.builder(AmmoProjectileInteraction.class, AmmoProjectileInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Fires a projectile.")
                .appendInherited(
                        new KeyedCodec<>("Config", Codec.STRING),
                        (o, i) -> o.config = i,
                        (o) -> o.config, (o, p) -> o.config = p.config
                )
                .addValidator(ProjectileConfig.VALIDATOR_CACHE.getValidator().late())
                .documentation("The ID of the projectile config asset to use for the projectile.")
                .add()
                .appendInherited(
                        new KeyedCodec<>("IgnorePitch", Codec.BOOLEAN),
                        (o, i) -> o.ignorePitch = i,
                        (o) -> o.ignorePitch,
                        (o, p) -> o.ignorePitch = p.ignorePitch
                )
                .documentation("If true, the shooter's pitch is set to 0 before the launch direction is computed, so the projectile's pitch is fixed by the projectile config's SpawnRotationOffset.")
                .add()
                .appendInherited(
                        new KeyedCodec<>("IgnoreYaw", Codec.BOOLEAN),
                        (o, i) -> o.ignoreYaw = i,
                        (o) -> o.ignoreYaw, (o, p) -> o.ignoreYaw = p.ignoreYaw
                )
                .documentation("If true, the shooter's yaw is set to 0 before the launch direction is computed, so the projectile's yaw is fixed by the projectile config's SpawnRotationOffset.")
                .add()
                .appendInherited(
                        new KeyedCodec<>("IgnoreRoll", Codec.BOOLEAN),
                        (o, i) -> o.ignoreRoll = i,
                        (o) -> o.ignoreRoll, (o, p) -> o.ignoreRoll = p.ignoreRoll
                )
                .documentation("If true, the shooter's roll is set to 0 before the launch direction is computed, so the projectile's roll is fixed by the projectile config's SpawnRotationOffset.")
                .add()
                .appendInherited(
                        new KeyedCodec<>("Lifetime", Codec.FLOAT),
                        (o, i) -> o.lifetime = i,
                        (o) -> o.lifetime,
                        (o, p) -> o.lifetime = p.lifetime
                )
                .addValidator(Validators.greaterThanOrEqual(0.0F))
                .documentation("The maximum lifetime of the projectile in seconds, after which it will despawn.")
                .add()
                .build();
    }
}
