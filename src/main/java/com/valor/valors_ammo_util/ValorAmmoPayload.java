package com.valor.valors_ammo_util;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.valor.valors_ammo_util.interaction.AmmoInfo;
import org.jspecify.annotations.Nullable;

public class ValorAmmoPayload {
    @Nullable
    private final String ammoInfoId;
    @Nullable
    private final String ammoItemId;
    @Nullable
    private final String modelAssetId;
    @Nullable
    private final String onHit;
    @Nullable
    private final String onMiss;


    private final boolean useModel;

    public ValorAmmoPayload(@Nullable String ammoInfoId, @Nullable String ammoItemId, @Nullable String modelAssetId, @Nullable String onHit, @Nullable String onMiss, boolean useModel) {
        this.ammoInfoId = ammoInfoId;
        this.ammoItemId = ammoItemId;
        this.modelAssetId = modelAssetId;
        this.onHit = onHit;
        this.onMiss = onMiss;
        this.useModel = useModel;
    }

    public @Nullable String getAmmoInfoId() {
        return ammoInfoId;
    }

    public @Nullable String getAmmoItemId() {
        return ammoItemId;
    }

    public @Nullable String getModelAssetId() {
        return modelAssetId;
    }

    public @Nullable String getOnHitId() {
        return onHit;
    }

    public @Nullable String getOnMissId() {
        return onMiss;
    }

    public boolean getUseModel() {
        return useModel;
    }

    public static ValorAmmoPayload generateAmmoPayload(Item item, boolean useItemModel, String itemAmmoInfoVar) {
        AmmoInfo ammoInfo;
        String modelAsset = null;
        String onHit = null;
        String onMiss = null;

        RootInteraction rootInteractionInfo = RootInteraction.getAssetMap().getAsset(item.getInteractionVars().get(itemAmmoInfoVar));
        if (rootInteractionInfo != null) {
            String[] interactionIds = rootInteractionInfo.getInteractionIds();
            if (interactionIds.length == 0) ValorAmmoUtil.LOGGER.atWarning().log("No Interactions in Root Interaction of Interaction Var " + itemAmmoInfoVar);
            ammoInfo = (AmmoInfo) Interaction.getAssetMap().getAsset(rootInteractionInfo.getInteractionIds()[0]);

            if (ammoInfo != null) {
                if (ammoInfo.getModelAssetId() != null) modelAsset = ammoInfo.getModelAssetId();
                if (ammoInfo.getInteractionOnHitId() != null) onHit = ammoInfo.getInteractionOnHitId();
                if (ammoInfo.getInteractionOnMissId() != null) onMiss = ammoInfo.getInteractionOnMissId();
            }
        }

        return new ValorAmmoPayload(item.getInteractionVars().get(itemAmmoInfoVar), item.getId(), modelAsset, onHit, onMiss, useItemModel);
    }
}
