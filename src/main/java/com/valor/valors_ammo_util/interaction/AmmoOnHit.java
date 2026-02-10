package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.component.AmmoInfoComponent;
import org.jspecify.annotations.NonNull;

public class AmmoOnHit extends SimpleInstantInteraction {
    public static final BuilderCodec<AmmoOnHit> CODEC;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = interactionContext.getEntity();

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer != null) {
            AmmoInfoComponent ammoInfoComponent = commandBuffer.getComponent(ref, ValorAmmoUtil.getAmmoInfoComponentType());
            if (ammoInfoComponent != null && ammoInfoComponent.getOnHitId() != null) {
                String onHitId = ammoInfoComponent.getOnHitId();
                RootInteraction onHit = RootInteraction.getAssetMap().getAsset(onHitId);
                if (onHit == null) {
                    ValorAmmoUtil.LOGGER.atWarning().log("RootInteraction " + onHitId + " is undefined");
                }
                else {
                    interactionContext.execute(onHit);
                }
            }
        }
    }

    static {
        CODEC = BuilderCodec.builder(AmmoOnHit.class, AmmoOnHit::new, SimpleInstantInteraction.CODEC)
                .documentation("Applies Valor Ammo on hit effects and then Despawns the projectile")
                .build();
    }
}
