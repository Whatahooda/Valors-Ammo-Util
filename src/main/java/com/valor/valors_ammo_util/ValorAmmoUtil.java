package com.valor.valors_ammo_util;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.valor.valors_ammo_util.component.AmmoInfoComponent;
import com.valor.valors_ammo_util.interaction.*;

public class ValorAmmoUtil extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ComponentType<EntityStore, AmmoInfoComponent> ammoInfoComponent;

    public ValorAmmoUtil(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCodecRegistry(Interaction.CODEC).register("AmmoModifyInventory", AmmoModifyInventoryInteraction.class, AmmoModifyInventoryInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AmmoProjectile", AmmoProjectileInteraction.class, AmmoProjectileInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AmmoOnHit", AmmoOnHit.class, AmmoOnHit.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AmmoOnMiss", AmmoOnMiss.class, AmmoOnMiss.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AmmoRemove", AmmoRemove.class, AmmoRemove.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("AmmoInfo", AmmoInfo.class, AmmoInfo.CODEC);

        ammoInfoComponent = this.getEntityStoreRegistry().registerComponent(AmmoInfoComponent.class, AmmoInfoComponent::new);

        ValorMetaKeys.registerKey();
    }

    public static ComponentType<EntityStore, AmmoInfoComponent> getAmmoInfoComponentType() {
        return ammoInfoComponent;
    }
}
