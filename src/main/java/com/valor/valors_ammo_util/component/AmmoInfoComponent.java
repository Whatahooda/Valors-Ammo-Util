package com.valor.valors_ammo_util.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class AmmoInfoComponent implements Component<EntityStore> {
    @Nullable
    private String itemId;
    @Nullable
    private String modelId;
    @Nullable
    private String onHitId;
    @Nullable
    private String onMissId;

    public AmmoInfoComponent() {

    }

    public AmmoInfoComponent(@Nullable String itemId, @Nullable String modelId, @Nullable String onHitId, @Nullable String onMissId) {
        this.itemId = itemId;
        this.modelId = modelId;
        this.onHitId = onHitId;
        this.onMissId = onMissId;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new AmmoInfoComponent(this.itemId, this.modelId, this.onHitId, this.onMissId);
    }

    public @Nullable String getItemId() {
        return itemId;
    }

    public @Nullable String getModelId() {
        return modelId;
    }

    public @Nullable String getOnHitId() {
        return onHitId;
    }

    public @Nullable String getOnMissId() {
        return onMissId;
    }
}
