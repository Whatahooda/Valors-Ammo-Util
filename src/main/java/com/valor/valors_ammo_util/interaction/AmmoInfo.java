package com.valor.valors_ammo_util.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class AmmoInfo extends SimpleInteraction {
    public static final BuilderCodec<AmmoInfo> CODEC;
    public static final String AMMO_INFO_VAR_ID = "Ammo_Info";

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
                .append(new KeyedCodec<>("AmmoOnHitId", RootInteraction.CHILD_ASSET_CODEC), (interaction, m) -> interaction.interactionOnHitId = m, interaction -> interaction.interactionOnHitId)
                .documentation("Interaction Id to trigger on Ammo Projectile Hit such as Damage")
                .add()
                .append(new KeyedCodec<>("AmmoOnMissId", RootInteraction.CHILD_ASSET_CODEC), (interaction, m) -> interaction.interactionOnMissId = m, interaction -> interaction.interactionOnMissId)
                .documentation("Interaction Id to trigger on Ammo Projectile Miss")
                .add()
                .build();
    }
}
