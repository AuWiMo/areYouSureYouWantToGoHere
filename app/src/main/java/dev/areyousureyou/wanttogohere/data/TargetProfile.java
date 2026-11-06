package com.auwimo.areyousure.data;

import java.util.UUID;

public final class TargetProfile {
    public enum SourceType {
        WEBSITE,
        APP
    }

    public final String id;
    public final String name;
    public final SourceType sourceType;
    public final String sourceValue;
    public final String wardId;

    public TargetProfile(
            String id,
            String name,
            SourceType sourceType,
            String sourceValue,
            String wardId) {
        this.id = id;
        this.name = name;
        this.sourceType = sourceType;
        this.sourceValue = sourceValue;
        this.wardId = wardId;
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
