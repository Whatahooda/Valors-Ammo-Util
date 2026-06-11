package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.valor.valors_ammo_util.AmmoToStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.component.LoadedAmmoComponent;
import org.jspecify.annotations.NonNull;

public class AmmoUnloadInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<AmmoUnloadInteraction> CODEC;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        ValorAmmoUtil.LOGGER.atInfo().log("Starting Ammo Unload");

        PlayerRef playerRef = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), PlayerRef.getComponentType());
        Player player = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), Player.getComponentType());
        if (playerRef == null || player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            ValorAmmoUtil.LOGGER.atInfo().log("No player found");
            return;
        }

        ItemStack heldItem = interactionContext.getHeldItem();
        assert heldItem != null;

        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }
        Ref<EntityStore> playerEntity = interactionContext.getEntity();

        LoadedAmmoComponent loadedAmmoComponent = commandBuffer.getComponent(playerEntity, ValorAmmoUtil.getLoadedAmmoComponentType());
        if (loadedAmmoComponent == null) {
            interactionContext.getState().state = InteractionState.Failed;
            ValorAmmoUtil.LOGGER.atInfo().log("Metadata error, no ammo found or invalid metadata");
            return;
        }

        ValorAmmoUtil.LOGGER.atInfo().log(
            "[VAU][DrawCancel] player=%s bowId=%s bowHash=%d bowIdentity=%d metadataBefore=%s fullMetaBefore=%s",
            playerRef.getUuid(),
            heldItem.getItemId(),
            heldItem.hashCode(),
            System.identityHashCode(heldItem),
            AmmoToStore.metadataDebugString(
                heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_ID),
                heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_QUANTITY)
            ),
            String.valueOf(heldItem.getMetadata())
        );

        // Get metadata from held item
        String[] existingIds = loadedAmmoComponent.getItemIds();
        int[] existingQuantities = loadedAmmoComponent.getItemQuantities();

        if (
                existingIds == null || existingIds.length == 0 ||
                existingQuantities == null || existingQuantities.length ==0 ||
                existingIds.length != existingQuantities.length
        ) {
            interactionContext.getState().state = InteractionState.Failed;
            ValorAmmoUtil.LOGGER.atInfo().log("Metadata error, no ammo found or invalid metadata");
            return;
        }
        ValorAmmoUtil.LOGGER.atInfo().log("Found ammo to return");

        // Return ammo in the data to the player inventory
        CombinedItemContainer inventory = InventoryComponent.getCombined(
                interactionContext.getEntity().getStore(),
                interactionContext.getEntity(),
                InventoryComponent.getComponentTypeById(InventoryComponent.HOTBAR_SECTION_ID),
                InventoryComponent.getComponentTypeById(InventoryComponent.STORAGE_SECTION_ID),
                InventoryComponent.getComponentTypeById(InventoryComponent.BACKPACK_SECTION_ID)
        );

        int returnedCount = 0;
        for (int i = 0; i < existingIds.length; i++) {
            String itemId = existingIds[i];
            Item storedAmmoItem = Item.getAssetMap().getAsset(itemId);
            // Make sure our stored item exists, and we DO NOT return durability ammo
            if (storedAmmoItem == null || storedAmmoItem.getMaxDurability() > 0) continue;

            int countBefore = getItemCountInInventory(inventory, itemId);

            ItemStack returnedItemStack = new ItemStack(itemId, existingQuantities[i]);
            inventory.addItemStack(returnedItemStack);
            returnedCount += existingQuantities[i];

            int countAfter = getItemCountInInventory(inventory, itemId);
            ValorAmmoUtil.LOGGER.atInfo().log(
                    "[VAU][ArrowReturnedItem] player=%s arrowId=%s requestedReturn=%d inventoryBefore=%d inventoryAfter=%d",
                    playerRef.getUuid(),
                    itemId,
                    existingQuantities[i],
                    countBefore,
                    countAfter
            );
        }

        EntityStatMap statMap = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), EntityStatMap.getComponentType());
        if (statMap != null) {
            statMap.setStatValue(DefaultEntityStatTypes.getAmmo(), 0);
        }

        commandBuffer.replaceComponent(playerEntity, ValorAmmoUtil.getLoadedAmmoComponentType(), new LoadedAmmoComponent());

        ValorAmmoUtil.LOGGER.atInfo().log(
            "[VAU][ArrowReturned] player=%s returnedCount=%d replaceSucceeded=%s metadataAfter=%s fullMetaAfter=%s",
                playerRef.getUuid(),
                returnedCount,
            true,
                AmmoToStore.metadataDebugString(
                new String[0],
                new int[0]
            ),
            String.valueOf(heldItem.getMetadata())
        );
    }

    private int getItemCountInInventory(CombinedItemContainer inventory, String itemId) {
        int total = 0;
        for (short slot = 0; slot < inventory.getCapacity(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (stack == null || stack.isEmpty() || !itemId.equals(stack.getItemId())) {
                continue;
            }
            total += stack.getQuantity();
        }
        return total;
    }

    static {
        CODEC = BuilderCodec.builder(AmmoUnloadInteraction.class, AmmoUnloadInteraction::new, AmmoUnloadInteraction.CODEC)
                .documentation("Ammo Unload will return all ammo currently stored in the loadable item")
                .build();
    }
}
