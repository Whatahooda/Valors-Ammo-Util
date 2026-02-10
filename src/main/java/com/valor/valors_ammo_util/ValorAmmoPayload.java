package com.valor.valors_ammo_util;

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
}
