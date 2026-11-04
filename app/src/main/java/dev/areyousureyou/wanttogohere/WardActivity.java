package dev.areyousureyou.wanttogohere;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import dev.areyousureyou.wanttogohere.data.WardProfile;
import dev.areyousureyou.wanttogohere.enforcement.PassphraseMatcher;
import dev.areyousureyou.wanttogohere.enforcement.WardAccessibilityService;

import java.util.Locale;

/** The pause shown when an observed target has an active ward. */
public final class WardActivity extends Activity {
    public static final String EXTRA_TARGET_ID = "target_id";
    public static final String EXTRA_TARGET_NAME = "target_name";
    public static final String EXTRA_WARD_NAME = "ward_name";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_WAIT_SECONDS = "wait_seconds";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_SHOW_PASSPHRASE = "show_passphrase";
    public static final String EXTRA_CASE_SENSITIVE = "case_sensitive";

    private CountDownTimer timer;
    private TextView status;
    private EditText answer;
    private Button waitContinue;
    private String passphrase;
    private boolean caseSensitive;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(10, 11, 9));
        window.setNavigationBarColor(Color.rgb(10, 11, 9));
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String wardName = getIntent().getStringExtra(EXTRA_WARD_NAME);
        String targetName = getIntent().getStringExtra(EXTRA_TARGET_NAME);
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        passphrase = getIntent().getStringExtra(EXTRA_PASSPHRASE);
        caseSensitive = getIntent().getBooleanExtra(EXTRA_CASE_SENSITIVE, false);
        boolean showPassphrase = getIntent().getBooleanExtra(EXTRA_SHOW_PASSPHRASE, false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(28), dp(28), dp(28), dp(28));
        layout.setBackgroundColor(Color.rgb(10, 11, 9));
        TextView heading = label(wardName == null ? "Take a moment" : wardName, 27, Color.WHITE);
        layout.addView(heading);
        TextView detail = label("Before opening " + (targetName == null ? "this target" : targetName), 17,
                Color.LTGRAY);
        detail.setPadding(0, dp(12), 0, dp(22));
        layout.addView(detail);
        status = label("", 20, Color.rgb(179, 190, 126));
        layout.addView(status);

        if (WardProfile.Mode.WAIT.name().equals(mode)) {
            Button close = button("CLOSE");
            close.setOnClickListener(view -> cancelTarget());
            layout.addView(close, buttonParams());
            waitContinue = button("CONTINUE");
            waitContinue.setVisibility(View.GONE);
            waitContinue.setOnClickListener(view -> finish());
            layout.addView(waitContinue, buttonParams());
            startWait(layout, getIntent().getIntExtra(EXTRA_WAIT_SECONDS, 1));
        } else {
            if (showPassphrase && passphrase != null) {
                TextView prompt = label("Passphrase: " + passphrase, 16, Color.LTGRAY);
                prompt.setPadding(0, dp(16), 0, dp(8));
                layout.addView(prompt);
            }
            answer = new EditText(this);
            answer.setSingleLine(true);
            answer.setHint("Enter passphrase");
            answer.setTextColor(Color.WHITE);
            answer.setHintTextColor(Color.GRAY);
            answer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(answer, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
            Button submit = button("CONTINUE");
            submit.setOnClickListener(view -> checkAnswer());
            layout.addView(submit, buttonParams());
            Button close = button("CLOSE");
            close.setOnClickListener(view -> cancelTarget());
            layout.addView(close, buttonParams());
        }
        setContentView(layout);
    }

    private void startWait(LinearLayout layout, int seconds) {
        long duration = Math.max(1, seconds) * 1000L;
        timer = new CountDownTimer(duration, 250) {
            @Override public void onTick(long remaining) {
                status.setText(String.format(Locale.getDefault(), "Wait %d seconds",
                        (remaining + 999) / 1000));
            }
            @Override public void onFinish() {
                status.setText("You can continue now.");
                waitContinue.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void checkAnswer() {
        if (passphrase == null || answer == null) return;
        String entered = answer.getText().toString();
        boolean matches = PassphraseMatcher.matches(passphrase, entered, caseSensitive);
        if (matches) finish();
        else {
            answer.setError("That passphrase does not match");
            Toast.makeText(this, "Try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelTarget() {
        finish();
        WardAccessibilityService.returnToPreviousPage();
    }

    @Override public void onBackPressed() {
        cancelTarget();
    }

    private TextView label(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(12);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
