package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class AmmoInfo extends SimpleInteraction {
    public static final BuilderCodec<AmmoInfo> CODEC;
    protected String modelAssetId;
    protected String interactionOnHitId;
    protected String interactionOnMissId;

    public String getModelAssetId() {
        return modelAssetId;
    }

    public String getInteractionOnHitId() {
        return interactionOnHitId;
    }

    public String getInteractionOnMissId() {
        return interactionOnMissId;
    }

    static {
        CODEC = BuilderCodec.builder(AmmoInfo.class, AmmoInfo::new, SimpleInteraction.CODEC)
                .documentation("Holds ammo information for the created projectile")
                .append(new KeyedCodec<>("ModelAssetId", Codec.STRING), (interaction, m) -> interaction.modelAssetId = m, (interaction) -> interaction.modelAssetId)
                .documentation("Model Id to use for the created projectile")
                .add()
                .append(new KeyedCodec<>("AmmoOnHitId", Codec.STRING), (interaction, r) -> interaction.interactionOnHitId = r, (interaction) -> interaction.interactionOnHitId)
                .documentation("Interaction Id to trigger on Ammo Projectile Hit such as Damage")
                .add()
                .append(new KeyedCodec<>("AmmoOnMissId", Codec.STRING), (interaction, r) -> interaction.interactionOnMissId = r, (interaction) -> interaction.interactionOnMissId)
                .documentation("Interaction Id to trigger on Ammo Projectile Miss")
                .add()
                .build();
    }
}
