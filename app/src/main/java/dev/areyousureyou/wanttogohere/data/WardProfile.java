package com.auwimo.areyousure.data;

import java.util.UUID;

public final class WardProfile {
    public enum Mode {
        WAIT,
        PASSPHRASE
    }

    public final String id;
    public final String name;
    public final Mode mode;
    public final int waitSeconds;
    public final String passphrase;
    public final boolean showPassphrase;
    public final boolean caseSensitive;

    public WardProfile(
            String id,
            String name,
            Mode mode,
            int waitSeconds,
            String passphrase,
            boolean showPassphrase,
            boolean caseSensitive) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.waitSeconds = waitSeconds;
        this.passphrase = passphrase;
        this.showPassphrase = showPassphrase;
        this.caseSensitive = caseSensitive;
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
