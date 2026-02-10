package com.valor.valors_ammo_util;

import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

public class ValorMetaKeys {
    private static MetaKey<ValorAmmoPayload> AMMO_METAKEY;

    private ValorMetaKeys() {}

    public static void registerKey() {
        AMMO_METAKEY = Interaction.META_REGISTRY.registerMetaObject();
    }

    public static MetaKey<ValorAmmoPayload> getMetaKey() {
        return AMMO_METAKEY;
    }
}
