package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.component.AmmoInfoComponent;
import org.jspecify.annotations.NonNull;

public class AmmoOnMiss extends SimpleInstantInteraction {
    public static final BuilderCodec<AmmoOnMiss> CODEC;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = interactionContext.getEntity();

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer != null) {
            AmmoInfoComponent ammoInfoComponent = commandBuffer.getComponent(ref, ValorAmmoUtil.getAmmoInfoComponentType());
            if (ammoInfoComponent != null && ammoInfoComponent.getOnMissId() != null) {
                String onMissId = ammoInfoComponent.getOnMissId();
                RootInteraction onMiss = RootInteraction.getAssetMap().getAsset(onMissId);
                if (onMiss == null) {
                    ValorAmmoUtil.LOGGER.atWarning().log("RootInteraction " + onMissId + " is undefined");
                }
                else {
                    this.next = onMissId;
                    interactionContext.getState().state = InteractionState.Finished;
                    interactionContext.execute(onMiss);
                }
            }
        }
    }

    static {
        CODEC = BuilderCodec.builder(AmmoOnMiss.class, AmmoOnMiss::new, SimpleInstantInteraction.CODEC)
                .documentation("Applies Valor Ammo on miss effects")
                .build();
    }
}
