package com.modernlife.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public record DocumentData(UUID ownerUuid, String ownerName, long issueDay, @Nullable LicenseType licenseType) {
    public static final String TAG_OWNER_UUID = "OwnerUuid";
    public static final String TAG_OWNER_NAME = "OwnerName";
    public static final String TAG_ISSUE_DAY = "IssueDay";
    public static final String TAG_LICENSE_TYPE = "LicenseType";

    public static Optional<DocumentData> read(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_OWNER_UUID)) {
            return Optional.empty();
        }
        final LicenseType licenseType = tag.contains(TAG_LICENSE_TYPE)
                ? LicenseType.fromId(tag.getString(TAG_LICENSE_TYPE))
                : null;
        return Optional.of(new DocumentData(
                tag.getUUID(TAG_OWNER_UUID),
                tag.getString(TAG_OWNER_NAME),
                tag.getLong(TAG_ISSUE_DAY),
                licenseType
        ));
    }

    public static void write(final ItemStack stack, final DocumentData data) {
        final CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_OWNER_UUID, data.ownerUuid());
        tag.putString(TAG_OWNER_NAME, data.ownerName());
        tag.putLong(TAG_ISSUE_DAY, data.issueDay());
        if (data.licenseType() != null) {
            tag.putString(TAG_LICENSE_TYPE, data.licenseType().getId());
        }
    }

    public boolean belongsTo(final UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }
}
