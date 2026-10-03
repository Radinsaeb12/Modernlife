package com.modernlife.data;

public enum LicenseType {
    VEHICLE("vehicle"),
    MOUNT("mount");

    private final String id;

    LicenseType(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static LicenseType fromId(final String id) {
        for (final LicenseType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
