package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.ChangeStatBehaviour;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ValueType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.ModifyInventoryInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.valor.valors_ammo_util.AmmoToStore;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import it.unimi.dsi.fastutil.ints.AbstractInt2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class AmmoModifyInventoryInteraction extends ModifyInventoryInteraction {
    public static final BuilderCodec<AmmoModifyInventoryInteraction> CODEC;

    private String[] tagsToFind;
    private String[] itemsToFind;
    private Integer amountToRemove = 1;
    private boolean noAmmoMixing;
    private boolean autoSetAmmoStat;
    private boolean useItemModel = true;
    private String itemAmmoInfoVar = null;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        PlayerRef playerRef = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), PlayerRef.getComponentType());
        Player player = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), Player.getComponentType());
        if (playerRef == null || player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            ValorAmmoUtil.LOGGER.atInfo().log("No player found");
            return;
        }

        ItemStack heldItem = interactionContext.getHeldItem();
        assert heldItem != null;

        EntityStatMap statMap = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), EntityStatMap.getComponentType());
        assert statMap != null;
        EntityStatValue ammoStat = statMap.get(DefaultEntityStatTypes.getAmmo());
        float alreadyLoaded = 0;
        double remainingAmmoCost = amountToRemove;
        double ammoUsed = 0;

        if (ammoStat != null && ammoStat.getMax() > 0) {
            alreadyLoaded = ammoStat.get();
            remainingAmmoCost = Math.min(remainingAmmoCost, ammoStat.getMax() - alreadyLoaded);
        }

        // Check for ammo already loaded in the item
        AmmoToStore ammoToStore = new AmmoToStore();
        if (alreadyLoaded != 0) {
            String[] existingIds = heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_ID);
            int[] existingQuantities = heldItem.getFromMetadataOrNull(AmmoToStore.KEYED_CODEC_QUANTITY);

            if (existingIds != null && existingQuantities != null) {
                ammoToStore = new AmmoToStore(existingIds, existingQuantities);
            }
        }

        if (alreadyLoaded > 0 && alreadyLoaded >= ammoStat.getMax()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Search for our item to use as ammunition
        ArrayList<Short> ammoFound = new ArrayList<>();
        ItemContainer playerInventoryAll = player.getInventory().getCombinedEverything();

        searchInventoryForAmmo(playerInventoryAll, ammoFound, tagsToFind, itemsToFind, noAmmoMixing);
        if (ammoFound.isEmpty()) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Use up ammo
        for (short i : ammoFound) {
            ItemStack itemStack = playerInventoryAll.getItemStack(i);
            assert itemStack != null;
            if (itemStack.getMaxDurability() > 0) {
                if (itemStack.getDurability() < remainingAmmoCost) {
                    ammoToStore.addItem(itemStack.getItemId(), (int) itemStack.getDurability());

                    remainingAmmoCost -= itemStack.getDurability();
                    ammoUsed += itemStack.getDurability();
                    playerInventoryAll.replaceItemStackInSlot(i, itemStack, itemStack.withDurability(0));
                }
                else {
                    ammoToStore.addItem(itemStack.getItemId(), (int) remainingAmmoCost);

                    playerInventoryAll.replaceItemStackInSlot(i, itemStack, itemStack.withDurability(itemStack.getDurability() - remainingAmmoCost));
                    ammoUsed += remainingAmmoCost;
                    break;
                }
            }
            else {
                if (itemStack.getQuantity() < remainingAmmoCost) {
                    ammoToStore.addItem(itemStack.getItemId(), itemStack.getQuantity());

                    remainingAmmoCost -= itemStack.getQuantity();
                    ammoUsed += itemStack.getDurability();
                    playerInventoryAll.removeItemStackFromSlot(i);
                }
                else {
                    ammoToStore.addItem(itemStack.getItemId(), (int) remainingAmmoCost);

                    playerInventoryAll.removeItemStackFromSlot(i, (int) remainingAmmoCost);
                    ammoUsed += remainingAmmoCost;
                    break;
                }
            }
        }

        if (autoSetAmmoStat) {
            statMap.setStatValue(DefaultEntityStatTypes.getAmmo(), (float) ammoUsed + alreadyLoaded);
        }

        ItemStack stackWithAmmoData = ammoToStore.addMetadataToStack(heldItem);

        interactionContext.setHeldItem(
                stackWithAmmoData
        );
        interactionContext.getHeldItemContainer().replaceItemStackInSlot(
                interactionContext.getHeldItemSlot(), heldItem, stackWithAmmoData
        );
    }

    private void searchInventoryForAmmo(ItemContainer inventoryContainer,
                                        ArrayList<Short> ammoFound,
                                        String[] tags,
                                        String[] items,
                                        boolean noMixing
    ) {
        if ((tags == null || tags.length == 0) && (items == null || items.length == 0)) {
            return;
        }

        String firstAmmo = null;

        for (short i = 0; i < inventoryContainer.getCapacity(); i++) {
            ItemStack itemStack = inventoryContainer.getItemStack(i);
            if (itemStack == null ||
                    itemStack.isEmpty() ||
                    itemStack.isBroken() ||
                    itemStack.getItem().getData() == null
            ) continue;
            Item item = itemStack.getItem();

            // If we aren't mixing, only look for same items as the first we saw
            if (noMixing && item.getId().equals(firstAmmo)) {
                ammoFound.add(i);
            }
            else {
                // If the item is ammo AND we don't care about durability, add it
                // If the item is ammo, we DO care, then make sure it has durability to be used, then add it
                if (isItemAmmo(item, tags, items)) {
                    ammoFound.add(i);

                    // Keep track of the first ammo we are reloading with
                    // if we don't want to mix together different items
                    if (noMixing) {
                        firstAmmo = item.getId();
                    }
                }
            }
        }
    }

    private boolean isItemAmmo(@NonNull Item item, String[] tags, String[] items) {
        if (itemHasTag(item, tags) != null) return true;
        return itemIsSpecified(item, items);
    }

    @Nullable
    private String itemHasTag(@NonNull Item item, String[] tags) {
        if (tags != null && tags.length > 0) {
            Map<String, String[]> rawTags = item.getData().getRawTags();
            for (String tag : tags) {
                if (rawTags.containsKey(tag)) {
                    return tag;
                }
            }
        }
        return null;
    }

    private boolean itemIsSpecified(@NonNull Item item, String[] items) {
        return items != null && items.length > 0 && Arrays.asList(items).contains(item.getId());
    }

    static {
        CODEC = BuilderCodec.builder(AmmoModifyInventoryInteraction.class, AmmoModifyInventoryInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Ammo Modify Inventory will check for ammunition of your specified tag and remove it from the inventory")
                .appendInherited(
                        new KeyedCodec<>("TagsToFind", BuilderCodec.STRING_ARRAY),
                        (interaction, s) -> interaction.tagsToFind = s,
                        (interaction) -> interaction.tagsToFind,
                        (interaction, parent) -> interaction.tagsToFind = parent.tagsToFind
                )
                .documentation("Items with these Ammo Tags will be treated as ammo")
                .add()
                .appendInherited(
                        new KeyedCodec<>("ItemsToFind", BuilderCodec.STRING_ARRAY),
                        (interaction, s) -> interaction.itemsToFind = s,
                        (interaction) -> interaction.itemsToFind,
                        (interaction, parent) -> interaction.itemsToFind = parent.itemsToFind
                )
                .documentation("A list of specific items to use as ammo")
                .add()
                .appendInherited(
                        new KeyedCodec<>("AmountToRemove", BuilderCodec.INTEGER),
                        (interaction, i) -> interaction.amountToRemove = i,
                        (interaction) -> interaction.amountToRemove,
                        (interaction, parent) -> interaction.amountToRemove = parent.amountToRemove
                )
                .add()
                .appendInherited(
                        new KeyedCodec<>("NoAmmoMixing", BuilderCodec.BOOLEAN),
                        (interaction, b) -> interaction.noAmmoMixing = b,
                        (interaction) -> interaction.noAmmoMixing,
                        (interaction, parent) -> interaction.noAmmoMixing = parent.noAmmoMixing
                )
                .documentation("Set to true if you don't want items with different ids to be used together when removing more than 1 item")
                .add()
                .appendInherited(
                        new KeyedCodec<>("ManuallyTrackAmmoStat", BuilderCodec.BOOLEAN),
                        (interaction, b) -> interaction.autoSetAmmoStat = !b,
                        (interaction) -> !interaction.autoSetAmmoStat,
                        (interaction, parent) -> interaction.autoSetAmmoStat = parent.autoSetAmmoStat
                )
                .documentation("Set to true if you want to handle the Ammo Stat yourself, instead of it being automatically set. Typically done when immediately firing projectiles.")
                .add()
                .appendInherited(
                        new KeyedCodec<>("AmmoInfoVar", BuilderCodec.STRING),
                        (interaction, s) -> interaction.itemAmmoInfoVar = s,
                        (interaction) -> interaction.itemAmmoInfoVar,
                        (interaction, parent) -> interaction.itemAmmoInfoVar = parent.itemAmmoInfoVar
                )
                .documentation("The Interaction Var where your AmmoInfo lives on your ammo item. Can be null")
                .add()
                .appendInherited(
                        new KeyedCodec<>("UseItemModel", BuilderCodec.BOOLEAN),
                        (interaction, b) -> interaction.useItemModel = b,
                        (interaction) -> interaction.useItemModel,
                        (interaction, parent) -> interaction.useItemModel = parent.useItemModel
                )
                .documentation("Set to False if you don't want the AmmoInfo to override the projectile model")
                .add()
                .build();
    }
}
