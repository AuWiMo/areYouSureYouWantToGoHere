package dev.areyousureyou.wanttogohere;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import dev.areyousureyou.wanttogohere.data.JsonProfileRepository;
import dev.areyousureyou.wanttogohere.data.ProfileRepository;
import dev.areyousureyou.wanttogohere.data.TargetProfile;
import dev.areyousureyou.wanttogohere.data.WardProfile;
import dev.areyousureyou.wanttogohere.ui.theme.AppTheme;
import dev.areyousureyou.wanttogohere.ui.theme.ThemeManager;

import java.io.IOException;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int TAB_WARDS = 0;
    private static final int TAB_TARGETS = 1;

    private AppTheme theme;
    private ProfileRepository profiles;
    private String loadError;
    private int selectedTab = TAB_WARDS;

    private LinearLayout root;
    private FrameLayout content;
    private Button addButton;
    private Button wardsTab;
    private Button targetsTab;
    private EditText appPackageInput;
    private EditText appTargetNameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = ThemeManager.getActiveTheme(this);
        Window window = getWindow();
        window.setStatusBarColor(theme.background);
        window.setNavigationBarColor(theme.background);

        try {
            profiles = new JsonProfileRepository(this);
        } catch (IOException exception) {
            loadError = "Saved profiles could not be opened. Your data has not been replaced.";
        }

        buildLayout();
        render();
    }

    private void buildLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.background);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout pageNavigation = new LinearLayout(this);
        pageNavigation.setOrientation(LinearLayout.HORIZONTAL);
        wardsTab = button("WARDS", false);
        targetsTab = button("TARGETS", false);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        pageNavigation.addView(wardsTab, tabParams);
        LinearLayout.LayoutParams secondTabParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        secondTabParams.leftMargin = dp(8);
        pageNavigation.addView(targetsTab, secondTabParams);
        root.addView(pageNavigation);

        wardsTab.setOnClickListener(view -> {
            selectedTab = TAB_WARDS;
            render();
        });
        targetsTab.setOnClickListener(view -> {
            selectedTab = TAB_TARGETS;
            render();
        });

        content = new FrameLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        contentParams.topMargin = dp(14);
        contentParams.bottomMargin = dp(14);
        root.addView(content, contentParams);

        addButton = button("+  ADD WARD", true);
        addButton.setTextSize(14);
        addButton.setOnClickListener(view -> {
            if (selectedTab == TAB_WARDS) {
                editWard(null);
            } else {
                editTarget(null);
            }
        });
        root.addView(addButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        setContentView(root);
    }

    private void render() {
        boolean showingWards = selectedTab == TAB_WARDS;
        addButton.setText(showingWards ? "+  ADD WARD" : "+  ADD TARGET");
        addButton.setEnabled(profiles != null);
        styleTab(wardsTab, showingWards);
        styleTab(targetsTab, !showingWards);
        content.removeAllViews();

        if (loadError != null) {
            TextView error = text(loadError, 16, theme.primaryText);
            error.setGravity(Gravity.CENTER);
            content.addView(error, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, dp(8));
        scroll.addView(list);
        content.addView(scroll);

        if (showingWards) {
            renderWards(list);
        } else {
            renderTargets(list);
        }
    }

    private void renderWards(LinearLayout list) {
        List<WardProfile> wards = profiles.getWards();
        if (wards.isEmpty()) {
            addEmptyMessage(list, "No wards yet. Add one to decide how a pause should work.");
            return;
        }

        for (WardProfile ward : wards) {
            String description = ward.mode == WardProfile.Mode.WAIT
                    ? "Wait " + ward.waitSeconds + " seconds"
                    : passphraseDescription(ward);
            addProfileCard(list, ward.name, description,
                    () -> editWard(ward),
                    () -> confirmDeleteWard(ward));
        }
    }

    private void renderTargets(LinearLayout list) {
        List<TargetProfile> targets = profiles.getTargets();
        if (targets.isEmpty()) {
            addEmptyMessage(list, "No targets yet. Add a website or app and assign it a ward.");
            return;
        }

        for (TargetProfile target : targets) {
            String kind = target.sourceType == TargetProfile.SourceType.WEBSITE
                    ? "Website" : "App";
            String wardName = findWardName(target.wardId);
            String description = kind + " · " + target.sourceValue + "\nWard · " + wardName;
            addProfileCard(list, target.name, description,
                    () -> editTarget(target),
                    () -> confirmDeleteTarget(target));
        }
    }

    private void addEmptyMessage(LinearLayout list, String message) {
        TextView empty = text(message, 16, theme.secondaryText);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(24), dp(24), dp(24));
        list.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void addProfileCard(
            LinearLayout list,
            String name,
            String description,
            Runnable editAction,
            Runnable deleteAction) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(12));
        card.setBackground(outlinedBackground(theme.surface, theme.edge, theme.panelCornerDp));

        TextView nameView = text(name, 18, theme.primaryText);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameView.setLetterSpacing(theme.profileNameLetterSpacing);
        card.addView(nameView);

        TextView detailView = text(description, 14, theme.secondaryText);
        detailView.setPadding(0, dp(6), 0, dp(8));
        card.addView(detailView);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button edit = button("EDIT", false);
        Button delete = button("DELETE", false);
        edit.setOnClickListener(view -> editAction.run());
        delete.setOnClickListener(view -> deleteAction.run());
        actions.addView(edit, new LinearLayout.LayoutParams(dp(86), dp(38)));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(92), dp(38));
        deleteParams.leftMargin = dp(8);
        actions.addView(delete, deleteParams);
        card.addView(actions);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        list.addView(card, cardParams);
    }

    private void editWard(WardProfile existing) {
        LinearLayout form = formLayout();
        EditText nameInput = addInput(form, "Ward name", InputType.TYPE_CLASS_TEXT,
                existing == null ? "" : existing.name);
        TextView modeLabel = addLabel(form, "Ward mode");

        RadioGroup modeGroup = new RadioGroup(this);
        modeGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton waitOption = radio("Wait for a set time");
        RadioButton passphraseOption = radio("Enter a passphrase");
        modeGroup.addView(waitOption);
        modeGroup.addView(passphraseOption);
        form.addView(modeGroup);

        EditText waitInput = addInput(form, "Seconds", InputType.TYPE_CLASS_NUMBER,
                existing == null ? "60" : String.valueOf(existing.waitSeconds));
        EditText passphraseInput = addInput(form, "Passphrase", InputType.TYPE_CLASS_TEXT,
                existing == null ? "" : existing.passphrase);
        CheckBox showPassphrase = checkBox("Show passphrase on the ward screen");
        CheckBox caseSensitive = checkBox("Passphrase is case-sensitive");
        form.addView(showPassphrase);
        form.addView(caseSensitive);

        boolean startsInWaitMode = existing == null || existing.mode == WardProfile.Mode.WAIT;
        waitOption.setChecked(startsInWaitMode);
        passphraseOption.setChecked(!startsInWaitMode);
        showPassphrase.setChecked(existing != null && existing.showPassphrase);
        caseSensitive.setChecked(existing != null && existing.caseSensitive);

        Runnable updateModeFields = () -> {
            boolean waitMode = waitOption.isChecked();
            modeLabel.setVisibility(View.VISIBLE);
            waitInput.setVisibility(waitMode ? View.VISIBLE : View.GONE);
            passphraseInput.setVisibility(waitMode ? View.GONE : View.VISIBLE);
            showPassphrase.setVisibility(waitMode ? View.GONE : View.VISIBLE);
            caseSensitive.setVisibility(waitMode ? View.GONE : View.VISIBLE);
        };
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> updateModeFields.run());
        updateModeFields.run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New ward" : "Edit ward")
                .setView(wrapForm(form))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = valueOf(nameInput).trim();
                    if (name.isEmpty()) {
                        nameInput.setError("Enter a ward name");
                        return;
                    }

                    WardProfile.Mode mode = waitOption.isChecked()
                            ? WardProfile.Mode.WAIT : WardProfile.Mode.PASSPHRASE;
                    int seconds = 0;
                    String passphrase = valueOf(passphraseInput);
                    if (mode == WardProfile.Mode.WAIT) {
                        try {
                            seconds = Integer.parseInt(valueOf(waitInput).trim());
                        } catch (NumberFormatException exception) {
                            waitInput.setError("Enter a whole number of seconds");
                            return;
                        }
                        if (seconds < 1) {
                            waitInput.setError("Use at least 1 second");
                            return;
                        }
                    } else if (passphrase.isEmpty()) {
                        passphraseInput.setError("Enter a passphrase");
                        return;
                    }

                    WardProfile ward = new WardProfile(
                            existing == null ? WardProfile.newId() : existing.id,
                            name,
                            mode,
                            seconds,
                            passphrase,
                            showPassphrase.isChecked(),
                            caseSensitive.isChecked());
                    try {
                        profiles.saveWard(ward);
                        dialog.dismiss();
                        render();
                    } catch (IOException exception) {
                        showStorageError(exception);
                    }
                }));
        showDialog(dialog);
    }

    private void editTarget(TargetProfile existing) {
        List<WardProfile> wards = profiles.getWards();
        if (wards.isEmpty()) {
            showMessage("Add a ward first", "Every target needs a ward profile.");
            return;
        }

        LinearLayout form = formLayout();
        EditText nameInput = addInput(form, "Target name", InputType.TYPE_CLASS_TEXT,
                existing == null ? "" : existing.name);
        addLabel(form, "Target source");
        RadioGroup sourceGroup = new RadioGroup(this);
        sourceGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton websiteOption = radio("Website");
        RadioButton appOption = radio("App");
        sourceGroup.addView(websiteOption);
        sourceGroup.addView(appOption);
        form.addView(sourceGroup);

        TargetProfile.SourceType startingType = existing == null
                ? TargetProfile.SourceType.WEBSITE : existing.sourceType;
        websiteOption.setChecked(startingType == TargetProfile.SourceType.WEBSITE);
        appOption.setChecked(startingType == TargetProfile.SourceType.APP);
        EditText sourceValueInput = addInput(form,
                startingType == TargetProfile.SourceType.WEBSITE
                        ? "Website or domain" : "App package name",
                InputType.TYPE_CLASS_TEXT,
                existing == null ? "" : existing.sourceValue);
        appPackageInput = sourceValueInput;
        appTargetNameInput = nameInput;
        LinearLayout appPickerRow = new LinearLayout(this);
        appPickerRow.setOrientation(LinearLayout.HORIZONTAL);
        Button browseAppsButton = button("Browse apps", false);
        LinearLayout.LayoutParams browseParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        browseParams.bottomMargin = dp(8);
        appPickerRow.addView(browseAppsButton, browseParams);
        form.addView(appPickerRow);
        appPickerRow.setVisibility(startingType == TargetProfile.SourceType.APP
                ? View.VISIBLE : View.GONE);
        browseAppsButton.setOnClickListener(view -> openAppPicker());
        String[] selectedWardId = {existing == null ? wards.get(0).id : existing.wardId};
        Button wardPicker = button("", false);
        wardPicker.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        updateWardButton(wardPicker, wards, selectedWardId[0]);
        form.addView(wardPicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        wardPicker.setOnClickListener(view -> chooseWard(wards, selectedWardId, wardPicker));

        sourceGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean website = websiteOption.isChecked();
            sourceValueInput.setHint(website ? "Website or domain" : "App package name");
            appPickerRow.setVisibility(website ? View.GONE : View.VISIBLE);
            sourceValueInput.setInputType(website
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                    : InputType.TYPE_CLASS_TEXT);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "New target" : "Edit target")
                .setView(wrapForm(form))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = valueOf(nameInput).trim();
                    String sourceValue = valueOf(sourceValueInput).trim();
                    if (name.isEmpty()) {
                        nameInput.setError("Enter a target name");
                        return;
                    }
                    if (sourceValue.isEmpty()) {
                        sourceValueInput.setError(websiteOption.isChecked()
                                ? "Enter a website or domain" : "Enter an app package name");
                        return;
                    }

                    TargetProfile target = new TargetProfile(
                            existing == null ? TargetProfile.newId() : existing.id,
                            name,
                            websiteOption.isChecked()
                                    ? TargetProfile.SourceType.WEBSITE
                                    : TargetProfile.SourceType.APP,
                            sourceValue,
                            selectedWardId[0]);
                    try {
                        profiles.saveTarget(target);
                        dialog.dismiss();
                        render();
                    } catch (IOException exception) {
                        showStorageError(exception);
                    }
                }));
        showDialog(dialog);
    }

    private void openAppPicker() {
        Intent launchableApps = new Intent(Intent.ACTION_MAIN);
        launchableApps.addCategory(Intent.CATEGORY_LAUNCHER);
        Intent picker = new Intent(Intent.ACTION_PICK_ACTIVITY);
        picker.putExtra(Intent.EXTRA_INTENT, launchableApps);
        try {
            startActivityForResult(picker, 1);
        } catch (ActivityNotFoundException exception) {
            showMessage("App picker unavailable", "This device does not provide the Android app picker.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1 || resultCode != RESULT_OK || data == null
                || data.getComponent() == null || appPackageInput == null) {
            return;
        }

        ComponentName selectedApp = data.getComponent();
        String packageName = selectedApp.getPackageName();
        appPackageInput.setText(packageName);
        if (appTargetNameInput != null && valueOf(appTargetNameInput).trim().isEmpty()) {
            try {
                CharSequence label = getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(packageName, 0));
                appTargetNameInput.setText(label);
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                appTargetNameInput.setText(packageName);
            }
        }
    }

    private void chooseWard(List<WardProfile> wards, String[] selectedId, Button button) {
        String[] names = new String[wards.size()];
        int selectedIndex = 0;
        for (int index = 0; index < wards.size(); index++) {
            names[index] = wards.get(index).name;
            if (wards.get(index).id.equals(selectedId[0])) {
                selectedIndex = index;
            }
        }
        showDialog(new AlertDialog.Builder(this)
                .setTitle("Apply ward")
                .setSingleChoiceItems(names, selectedIndex, (dialog, which) -> {
                    selectedId[0] = wards.get(which).id;
                    updateWardButton(button, wards, selectedId[0]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create());
    }

    private void updateWardButton(Button button, List<WardProfile> wards, String wardId) {
        String wardName = "Choose a ward";
        for (WardProfile ward : wards) {
            if (ward.id.equals(wardId)) {
                wardName = ward.name;
                break;
            }
        }
        button.setText("APPLIED WARD  ·  " + wardName);
    }

    private void confirmDeleteWard(WardProfile ward) {
        showDialog(new AlertDialog.Builder(this)
                .setTitle("Delete ward?")
                .setMessage("\"" + ward.name + "\" will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        if (profiles.deleteWard(ward.id)) {
                            render();
                        } else {
                            showMessage("Ward is in use",
                                    "Remove or edit its targets before deleting this ward.");
                        }
                    } catch (IOException exception) {
                        showStorageError(exception);
                    }
                })
                .create());
    }

    private void confirmDeleteTarget(TargetProfile target) {
        showDialog(new AlertDialog.Builder(this)
                .setTitle("Delete target?")
                .setMessage("\"" + target.name + "\" will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        profiles.deleteTarget(target.id);
                        render();
                    } catch (IOException exception) {
                        showStorageError(exception);
                    }
                })
                .create());
    }

    private String findWardName(String wardId) {
        for (WardProfile ward : profiles.getWards()) {
            if (ward.id.equals(wardId)) {
                return ward.name;
            }
        }
        return "Missing ward";
    }

    private String passphraseDescription(WardProfile ward) {
        String shown = ward.showPassphrase ? "shown" : "hidden";
        String caseRule = ward.caseSensitive ? "case-sensitive" : "not case-sensitive";
        return "Passphrase · " + shown + " · " + caseRule;
    }

    private LinearLayout formLayout() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(8));
        return form;
    }

    private ScrollView wrapForm(LinearLayout form) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(form);
        return scroll;
    }

    private EditText addInput(LinearLayout form, String hint, int inputType, String value) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(theme.primaryText);
        input.setHintTextColor(theme.secondaryText);
        input.setTextSize(16);
        input.setInputType(inputType);
        input.setHint(hint);
        input.setText(value);
        input.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(theme.secondaryText));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(8);
        form.addView(input, params);
        return input;
    }

    private TextView addLabel(LinearLayout form, String value) {
        TextView label = text(value, 14, theme.secondaryText);
        label.setPadding(0, dp(10), 0, dp(4));
        form.addView(label);
        return label;
    }

    private RadioButton radio(String label) {
        RadioButton radio = new RadioButton(this);
        radio.setText(label);
        radio.setTextColor(theme.primaryText);
        radio.setButtonTintList(android.content.res.ColorStateList.valueOf(theme.accent));
        return radio;
    }

    private CheckBox checkBox(String label) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(label);
        checkBox.setTextColor(theme.primaryText);
        checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(theme.accent));
        return checkBox;
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        return textView;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setTextColor(primary ? theme.accentText : theme.primaryText);
        button.setLetterSpacing(theme.buttonLetterSpacing);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setBackground(primary
                ? solidBackground(theme.accent, theme.controlCornerDp)
                : outlinedBackground(theme.surface, theme.edge, theme.controlCornerDp));
        return button;
    }

    private void styleTab(Button tab, boolean selected) {
        tab.setTextColor(selected ? theme.accentText : theme.primaryText);
        tab.setBackground(selected
                ? solidBackground(theme.accent, theme.controlCornerDp)
                : outlinedBackground(theme.background, theme.edge, theme.controlCornerDp));
    }

    private GradientDrawable solidBackground(int color, int cornerRadiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(cornerRadiusDp));
        return drawable;
    }

    private GradientDrawable outlinedBackground(
            int color, int outlineColor, int cornerRadiusDp) {
        GradientDrawable drawable = solidBackground(color, cornerRadiusDp);
        drawable.setStroke(dp(theme.borderWidthDp), outlineColor);
        return drawable;
    }

    private void showStorageError(IOException exception) {
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
    }

    private void showDialog(AlertDialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    outlinedBackground(theme.surface, theme.edge, theme.panelCornerDp));
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setTextColor(theme.accent);
        }
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negative != null) {
            negative.setTextColor(theme.secondaryText);
        }
    }

    private void showMessage(String title, String message) {
        showDialog(new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .create());
    }

    private String valueOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
