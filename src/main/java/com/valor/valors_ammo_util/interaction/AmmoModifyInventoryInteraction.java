package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.ModifyInventoryInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.valor.valors_ammo_util.ValorAmmoPayload;
import com.valor.valors_ammo_util.ValorAmmoUtil;
import com.valor.valors_ammo_util.ValorMetaKeys;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class AmmoModifyInventoryInteraction extends ModifyInventoryInteraction {
    public static final BuilderCodec<AmmoModifyInventoryInteraction> CODEC;

    private String[] tagsToFind;
    private Integer amountToRemove = 1;
    private boolean useItemModel = true;
    private String itemAmmoInfoVar = null;

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {
        PlayerRef playerRef = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), PlayerRef.getComponentType());
        Player player = interactionContext.getEntity().getStore().getComponent(interactionContext.getEntity(), Player.getComponentType());
        if (playerRef == null || player == null) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Search for our item to use as ammunition
        ItemStack itemStack;
        Item item;
        ItemContainer playerInventoryAll = player.getInventory().getCombinedEverything();

        short stackIndexToModify = searchInventoryForTags(playerInventoryAll, tagsToFind);
        if (stackIndexToModify == -1) {
            interactionContext.getState().state = InteractionState.Failed;
            return;
        }

        // Use up ammo
        // TODO Needs to account for using more than one item
        itemStack = playerInventoryAll.getItemStack(stackIndexToModify);
        assert itemStack != null;
        item = itemStack.getItem();
        playerInventoryAll.removeItemStackFromSlot(stackIndexToModify, amountToRemove);

        // Save the ammo information to alter the projectile on creation
        ValorAmmoPayload ammoPayload = generateAmmoPayload(item, this.useItemModel);
        interactionContext.getMetaStore().putMetaObject(ValorMetaKeys.getMetaKey(), ammoPayload);
    }

    private short searchInventoryForTags(ItemContainer inventoryContainer, String[] tags) {
        if (tags == null || tags.length == 0) return -1;
        for (short i = 0; i < inventoryContainer.getCapacity(); i++) {
            ItemStack itemStack = inventoryContainer.getItemStack(i);
            if (itemStack == null || itemStack.isEmpty()) continue;

            Item item = itemStack.getItem();
            if (item.getData() == null) continue;

            Map<String, String[]> rawTags = item.getData().getRawTags();

            for (String tag : tags) {
                if (rawTags.containsKey(tag)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private ValorAmmoPayload generateAmmoPayload(Item item, boolean useItemModel) {
        AmmoInfo ammoInfo;
        String modelAsset = null;
        String onHit = null;
        String onMiss = null;

        RootInteraction rootInteractionInfo = RootInteraction.getAssetMap().getAsset(item.getInteractionVars().get(this.itemAmmoInfoVar));
        if (rootInteractionInfo != null) {
            String[] interactionIds = rootInteractionInfo.getInteractionIds();
            if (interactionIds.length == 0) ValorAmmoUtil.LOGGER.atWarning().log("No Interactions in Root Interaction of Interaction Var " + this.itemAmmoInfoVar);
            ammoInfo = (AmmoInfo) Interaction.getAssetMap().getAsset(rootInteractionInfo.getInteractionIds()[0]);

            if (ammoInfo != null) {
                if (ammoInfo.modelAssetId != null) modelAsset = ammoInfo.getModelAssetId();
                if (ammoInfo.interactionOnHitId != null) onHit = ammoInfo.getInteractionOnHitId();
                if (ammoInfo.interactionOnMissId != null) onMiss = ammoInfo.getInteractionOnMissId();
            }
        }

        return new ValorAmmoPayload(item.getInteractionVars().get(this.itemAmmoInfoVar), item.getId(), modelAsset, onHit, onMiss, useItemModel);
    }


    static {
        CODEC = BuilderCodec.builder(AmmoModifyInventoryInteraction.class, AmmoModifyInventoryInteraction::new, SimpleInstantInteraction.CODEC)
                .documentation("Valor's Use Ammo will check for ammunition of your specified tag and remove it from the inventory")
                .append(new KeyedCodec<>("TagsToFind", BuilderCodec.STRING_ARRAY), (interaction, s) -> interaction.tagsToFind = s, (interaction) -> interaction.tagsToFind)
                .documentation("Items with these Ammo Tags will be treated as ammo")
                .add()
                .append(new KeyedCodec<>("AmountToRemove", BuilderCodec.INTEGER), (interaction, i) -> interaction.amountToRemove = i, (interaction) -> interaction.amountToRemove)
                .add()
                .append(new KeyedCodec<>("AmmoInfoVar", BuilderCodec.STRING), (interaction, b) -> interaction.itemAmmoInfoVar = b, (interaction) -> interaction.itemAmmoInfoVar)
                .documentation("The Interaction Var where your AmmoInfo lives on your ammo item. Can be null")
                .add()
                .append(new KeyedCodec<>("UseItemModel", BuilderCodec.BOOLEAN), (interaction, b) -> interaction.useItemModel = b, (interaction) -> interaction.useItemModel)
                .documentation("Set to False if you don't want the AmmoInfo to override the projectile model")
                .add()
                .build();
    }
}
