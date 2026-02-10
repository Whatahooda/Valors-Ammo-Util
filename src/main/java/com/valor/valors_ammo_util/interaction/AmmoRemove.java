package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import org.jspecify.annotations.NonNull;

public class AmmoRemove extends SimpleInstantInteraction {
    public static BuilderCodec<AmmoRemove> CODEC;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        // This interaction makes sure to remove the Ref that owns the chain
        // Instead of somehow removing the Player Ref(like RemoveEntity did) which would cause desyncs

        if (interactionContext.getCommandBuffer() == null) return;

        Ref<EntityStore> ref = interactionContext.getEntity();
        if (interactionContext.getCommandBuffer().getArchetype(ref).contains(Player.getComponentType())) {
            ValorAmmoUtil.LOGGER.atWarning().log("Calling AmmoRemove on Player Reference, canceling");
            return;
        }

        World world = interactionContext.getCommandBuffer().getExternalData().getWorld();
        world.execute(() -> {
            if (ref.isValid()) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
        });
    }

    static {
        CODEC = BuilderCodec.builder(AmmoRemove.class, AmmoRemove::new, SimpleInstantInteraction.CODEC)
                .documentation("Always removes the owner of the interaction chain, this resolves a player desync with custom projectiles")
                .build();
    }
}
