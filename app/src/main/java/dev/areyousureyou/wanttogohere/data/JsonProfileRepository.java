package com.auwimo.areyousure.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class JsonProfileRepository implements ProfileRepository {
    private static final String FILE_NAME = "profiles.json";

    private final File file;
    private List<WardProfile> wards;
    private List<TargetProfile> targets;

    public JsonProfileRepository(Context context) throws IOException {
        file = new File(context.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            readFromDisk();
        } else {
            wards = new ArrayList<>();
            targets = new ArrayList<>();
        }
    }

    @Override
    public synchronized List<WardProfile> getWards() {
        return new ArrayList<>(wards);
    }

    @Override
    public synchronized List<TargetProfile> getTargets() {
        return new ArrayList<>(targets);
    }

    @Override
    public synchronized void saveWard(WardProfile ward) throws IOException {
        List<WardProfile> updated = new ArrayList<>(wards);
        int existingIndex = indexOfWard(updated, ward.id);
        if (existingIndex >= 0) {
            updated.set(existingIndex, ward);
        } else {
            updated.add(ward);
        }
        writeToDisk(updated, targets);
        wards = updated;
    }

    @Override
    public synchronized boolean deleteWard(String wardId) throws IOException {
        for (TargetProfile target : targets) {
            if (target.wardId.equals(wardId)) {
                return false;
            }
        }

        List<WardProfile> updated = new ArrayList<>(wards);
        int index = indexOfWard(updated, wardId);
        if (index < 0) {
            return true;
        }

        updated.remove(index);
        writeToDisk(updated, targets);
        wards = updated;
        return true;
    }

    @Override
    public synchronized void saveTarget(TargetProfile target) throws IOException {
        if (indexOfWard(wards, target.wardId) < 0) {
            throw new IOException("The selected ward no longer exists.");
        }

        List<TargetProfile> updated = new ArrayList<>(targets);
        int existingIndex = indexOfTarget(updated, target.id);
        if (existingIndex >= 0) {
            updated.set(existingIndex, target);
        } else {
            updated.add(target);
        }
        writeToDisk(wards, updated);
        targets = updated;
    }

    @Override
    public synchronized void deleteTarget(String targetId) throws IOException {
        List<TargetProfile> updated = new ArrayList<>(targets);
        int index = indexOfTarget(updated, targetId);
        if (index < 0) {
            return;
        }

        updated.remove(index);
        writeToDisk(wards, updated);
        targets = updated;
    }

    private void readFromDisk() throws IOException {
        StringBuilder contents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contents.append(line);
            }
        }

        try {
            JSONObject root = new JSONObject(contents.toString());
            List<WardProfile> loadedWards = readWards(root.getJSONArray("wards"));
            List<TargetProfile> loadedTargets = readTargets(root.getJSONArray("targets"));
            for (TargetProfile target : loadedTargets) {
                if (indexOfWard(loadedWards, target.wardId) < 0) {
                    throw new IOException("A target refers to a missing ward.");
                }
            }
            wards = loadedWards;
            targets = loadedTargets;
        } catch (JSONException | IllegalArgumentException exception) {
            throw new IOException("Saved profiles could not be read.", exception);
        }
    }

    private void writeToDisk(List<WardProfile> wardsToSave, List<TargetProfile> targetsToSave)
            throws IOException {
        JSONObject root = new JSONObject();
        try {
            root.put("version", 1);
            root.put("wards", writeWards(wardsToSave));
            root.put("targets", writeTargets(targetsToSave));
        } catch (JSONException exception) {
            throw new IOException("Profiles could not be prepared for saving.", exception);
        }

        File temporaryFile = new File(file.getParentFile(), FILE_NAME + ".tmp");
        byte[] bytes;
        try {
            bytes = root.toString(2).getBytes(StandardCharsets.UTF_8);
        } catch (JSONException exception) {
            throw new IOException("Profiles could not be prepared for saving.", exception);
        }

        try (FileOutputStream output = new FileOutputStream(temporaryFile)) {
            output.write(bytes);
            output.getFD().sync();
        }

        File backupFile = new File(file.getParentFile(), FILE_NAME + ".bak");
        if (backupFile.exists() && !backupFile.delete()) {
            throw new IOException("Could not prepare the saved profiles file.");
        }

        boolean movedOldFile = false;
        if (file.exists()) {
            movedOldFile = file.renameTo(backupFile);
            if (!movedOldFile) {
                throw new IOException("Could not replace the saved profiles file.");
            }
        }

        if (!temporaryFile.renameTo(file)) {
            if (movedOldFile) {
                backupFile.renameTo(file);
            }
            throw new IOException("Could not save profiles to app storage.");
        }

        if (movedOldFile) {
            backupFile.delete();
        }
    }

    private JSONArray writeWards(List<WardProfile> values) throws JSONException {
        JSONArray array = new JSONArray();
        for (WardProfile ward : values) {
            JSONObject item = new JSONObject();
            item.put("id", ward.id);
            item.put("name", ward.name);
            item.put("mode", ward.mode.name());
            item.put("waitSeconds", ward.waitSeconds);
            item.put("passphrase", ward.passphrase);
            item.put("showPassphrase", ward.showPassphrase);
            item.put("caseSensitive", ward.caseSensitive);
            array.put(item);
        }
        return array;
    }

    private JSONArray writeTargets(List<TargetProfile> values) throws JSONException {
        JSONArray array = new JSONArray();
        for (TargetProfile target : values) {
            JSONObject item = new JSONObject();
            item.put("id", target.id);
            item.put("name", target.name);
            item.put("sourceType", target.sourceType.name());
            item.put("sourceValue", target.sourceValue);
            item.put("wardId", target.wardId);
            array.put(item);
        }
        return array;
    }

    private List<WardProfile> readWards(JSONArray array) throws JSONException {
        List<WardProfile> result = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            result.add(new WardProfile(
                    item.getString("id"),
                    item.getString("name"),
                    WardProfile.Mode.valueOf(item.getString("mode")),
                    item.getInt("waitSeconds"),
                    item.getString("passphrase"),
                    item.getBoolean("showPassphrase"),
                    item.getBoolean("caseSensitive")));
        }
        return result;
    }

    private List<TargetProfile> readTargets(JSONArray array) throws JSONException {
        List<TargetProfile> result = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            result.add(new TargetProfile(
                    item.getString("id"),
                    item.getString("name"),
                    TargetProfile.SourceType.valueOf(item.getString("sourceType")),
                    item.getString("sourceValue"),
                    item.getString("wardId")));
        }
        return result;
    }

    private int indexOfWard(List<WardProfile> values, String id) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id.equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private int indexOfTarget(List<TargetProfile> values, String id) {
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id.equals(id)) {
                return index;
            }
        }
        return -1;
    }
}
