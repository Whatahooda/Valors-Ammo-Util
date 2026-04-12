package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.valor.valors_ammo_util.AmmoToStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

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
        assert interactionContext.getHeldItemContainer() != null;

        // Get metadata from held item
        String[] existingIds = heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_ID);
        int[] existingQuantities = heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_QUANTITY);

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

        for (int i = 0; i < existingIds.length; i++) {
            String itemId = existingIds[i];
            Item storedAmmoItem = Item.getAssetMap().getAsset(itemId);
            // Make sure our stored item exists, and we DO NOT return durability ammo
            if (storedAmmoItem == null || storedAmmoItem.getMaxDurability() > 0) continue;

            ItemStack returnedItemStack = new ItemStack(itemId, existingQuantities[i]);
            inventory.addItemStack(returnedItemStack);
        }

        // Remove metadata from the held item
        ItemStack heldItemWithoutAmmo = AmmoToStore.addMetadataToStack(heldItem, new ArrayList<>(0), new ArrayList<>(0));

        interactionContext.setHeldItem(heldItemWithoutAmmo);
        interactionContext.getHeldItemContainer().replaceItemStackInSlot(
                interactionContext.getHeldItemSlot(), heldItem, heldItemWithoutAmmo
        );
    }

    static {
        CODEC = BuilderCodec.builder(AmmoUnloadInteraction.class, AmmoUnloadInteraction::new, AmmoUnloadInteraction.CODEC)
                .documentation("Ammo Unload will return all ammo currently stored in the loadable item")
                .build();
    }
}
