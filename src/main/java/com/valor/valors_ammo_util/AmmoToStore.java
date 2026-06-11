package com.valor.valors_ammo_util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

public class AmmoToStore {
    public final static KeyedCodec<String[]> KEYED_CODEC_ID = new KeyedCodec<>("ItemIds", Codec.STRING_ARRAY);
    public final static KeyedCodec<int[]> KEYED_CODEC_QUANTITY = new KeyedCodec<>("ItemQuantity", Codec.INT_ARRAY);

    private final ArrayList<String> itemIds;
    private final ArrayList<Integer> itemQuantity;

    public AmmoToStore() {
        this.itemIds = new ArrayList<>();
        this.itemQuantity = new ArrayList<>();
    }

    public AmmoToStore(String[] existingIds, int[] existingQuantities) {
        if (existingIds == null || existingQuantities == null || existingIds.length != existingQuantities.length) {
            this.itemIds = new ArrayList<>();
            this.itemQuantity = new ArrayList<>();
            return;
        }
        this.itemIds = new ArrayList<>();
        this.itemIds.addAll(Arrays.asList(existingIds));
        this.itemQuantity = new ArrayList<>();
        for (int i : existingQuantities) {
            this.itemQuantity.add(i);
        }
    }

    public int size() {
        return this.itemIds.size();
    }

    public String getItemId(int index) {
        return this.itemIds.get(index);
    }

    public Integer getItemQuantity(int index) {
        return this.itemQuantity.get(index);
    }

    public void addItem(String itemId, Integer quantity) {
        if (this.itemIds.isEmpty() || !this.itemIds.getLast().equals(itemId)) {
            this.itemIds.add(itemId);
            this.itemQuantity.add(quantity);
            return;
        }

        // Contribute to most recent item count to condense information IF we are adding the same item id
        this.itemQuantity.set(this.itemQuantity.size() - 1, this.itemQuantity.getLast() + quantity);
    }

    public void useItem() {
        if (this.getItemQuantity(0) - 1 == 0) {
            this.itemIds.removeFirst();
            this.itemQuantity.removeFirst();
        }
        else {
            this.itemQuantity.set(0, this.itemQuantity.getFirst() - 1);
        }
    }

    public static ItemStack addMetadataToStack(ItemStack itemStack, ArrayList<String> itemIds, ArrayList<Integer> itemQuantity) {
        String[] itemIdsRaw = listToArray(itemIds);
        ItemStack withIds = itemStack.withMetadata(KEYED_CODEC_ID, itemIdsRaw);

        int[] itemQuantityRaw = itemQuantity.stream().mapToInt(i -> i).toArray();
        ItemStack withAll = withIds.withMetadata(KEYED_CODEC_QUANTITY, itemQuantityRaw);

        return withAll;
    }

    public static ItemStack removeMetadataFromStack(ItemStack itemStack) {
        ItemStack withoutIds = itemStack.withMetadata(KEYED_CODEC_ID, null);
        return withoutIds.withMetadata(KEYED_CODEC_QUANTITY, null);
    }

    public static int getStoredAmmoCount(String[] existingIds, int[] existingQuantities) {
        if (existingIds == null || existingQuantities == null || existingIds.length != existingQuantities.length) {
            return 0;
        }

        int total = 0;
        for (int quantity : existingQuantities) {
            if (quantity > 0) {
                total += quantity;
            }
        }
        return total;
    }

    public static String metadataDebugString(String[] existingIds, int[] existingQuantities) {
        if (existingIds == null || existingQuantities == null) {
            return "ids=null, qty=null";
        }
        if (existingIds.length != existingQuantities.length) {
            return "length-mismatch ids=" + existingIds.length + ", qty=" + existingQuantities.length;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < existingIds.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(existingIds[i]).append(":").append(existingQuantities[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    public ItemStack addMetadataToStack(ItemStack itemStack) {
        return addMetadataToStack(itemStack, this.itemIds, this.itemQuantity);
    }

    @Nonnull
    public static String[] listToArray(@Nonnull ArrayList<String> stringArrayList) {
        int size = stringArrayList.size();
        String[] retVal = new String[size];
        for (int i = 0; i < size; i++) {
            retVal[i] = stringArrayList.get(i);
        }
        return retVal;
    }
}
