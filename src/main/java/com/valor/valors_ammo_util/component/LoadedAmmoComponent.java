package com.valor.valors_ammo_util.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Arrays;

public class LoadedAmmoComponent implements Component<EntityStore> {
    private final ArrayList<String> itemIds;
    private final ArrayList<Integer> itemQuantities;

    public LoadedAmmoComponent() {
        this.itemIds = new ArrayList<>();
        this.itemQuantities = new ArrayList<>();
    }

    public LoadedAmmoComponent(String[] ids, int[] quantities) {
        this();
        if (ids == null || quantities == null || ids.length != quantities.length) {
            return;
        }

        this.itemIds.addAll(Arrays.asList(ids));
        for (int quantity : quantities) {
            this.itemQuantities.add(quantity);
        }
    }

    public String[] getItemIds() {
        return this.itemIds.toArray(new String[0]);
    }

    public int[] getItemQuantities() {
        return this.itemQuantities.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Component<EntityStore> clone() {
        return new LoadedAmmoComponent(this.getItemIds(), this.getItemQuantities());
    }
}