package com.thirumalai.calllimiter.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.thirumalai.calllimiter.BottomSheets.TimerBottomSheet;
import com.thirumalai.calllimiter.Data.PreferenceHelper;
import com.thirumalai.calllimiter.R;
import com.thirumalai.calllimiter.Utils.SystemBarHelper;
import com.thirumalai.calllimiter.Utils.ThemeUtils;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class Settings extends AppCompatActivity {
    private LinearLayout layoutTheme, githubIssues, permissions, about, timeLimitForAllNumbers, warningReminderTimeLayout;
    private TextView selectedThemeText, bufferValueText, timeLimit, warningReminderTimeText;
    private ImageView backBtn;
    private SeekBar bufferBar;
    private MaterialSwitch switchBtn, callStartBufferSwitchBtn, limitResetForEachCallSwitchBtn, warningReminderSwitchBtn;
    private boolean isChecked = false, isCallStartBufferEnabled = true, islimitRestForEachCallEnabled = false, isWarningReminderEnabled = true;
    private final int[] BUFFER_VALUES = {10, 20, 30, 60, 120, 180, 240, 300};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View rootView = findViewById(android.R.id.content);
        SystemBarHelper.setupStatusBarAppearance(getWindow(), getResources(), rootView);

        PreferenceHelper.init(this);

        selectedThemeText = findViewById(R.id.selected_theme_text);
        backBtn = findViewById(R.id.back_btn);
        layoutTheme = findViewById(R.id.theme);
        githubIssues = findViewById(R.id.github_issues);
        permissions = findViewById(R.id.permissions);
        about = findViewById(R.id.about_settings);
        bufferBar = findViewById(R.id.buffer_time_seek_bar);
        bufferValueText = findViewById(R.id.buffer_time_value);
        switchBtn = findViewById(R.id.limit_all_numbers_switch);
        timeLimitForAllNumbers = findViewById(R.id.time_limit_all_numbers);
        timeLimit = findViewById(R.id.time_limit_all_numbers_text);
        callStartBufferSwitchBtn = findViewById(R.id.call_start_buffer_time);
        limitResetForEachCallSwitchBtn = findViewById(R.id.limit_reset_each_call);
        warningReminderSwitchBtn = findViewById(R.id.warning_reminder_switch);
        warningReminderTimeLayout = findViewById(R.id.warning_reminder_time_layout);
        warningReminderTimeText = findViewById(R.id.warning_reminder_time_text);

        isChecked = PreferenceHelper.getLimitForAllNumbersEnabled();
        isCallStartBufferEnabled = PreferenceHelper.getCallStartBufferValue();
        islimitRestForEachCallEnabled = PreferenceHelper.getLimitForEachCallValue();
        isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled();
        int timeLimit1 = PreferenceHelper.getTimeLimitForAllNumbers();

        int hours = timeLimit1 / 3600;
        int minutes = (timeLimit1 % 3600) / 60;
        int seconds = timeLimit1 % 60;
        String hoursStr = (hours < 10) ? "0" + hours : Integer.toString(hours);
        String minutesStr = (minutes < 10) ? "0" + minutes : Integer.toString(minutes);
        String secondsStr = (seconds < 10) ? "0" + seconds : Integer.toString(seconds);
        timeLimit.setText(hoursStr + ":" + minutesStr + ":" + secondsStr);

        callStartBufferSwitchBtn.setChecked(isCallStartBufferEnabled);

        switchBtn.setChecked(isChecked);
        if(isChecked){
            timeLimitForAllNumbers.setVisibility(View.VISIBLE);
        } else {
            timeLimitForAllNumbers.setVisibility(View.GONE);
        }

        int bufferTime = PreferenceHelper.getBufferTime();
        bufferBar.setMax(BUFFER_VALUES.length - 1);
        int index = 0;
        for(int i = 0; i < BUFFER_VALUES.length; i++){
            if(BUFFER_VALUES[i] == bufferTime){
                index = i;
            }
        }

        String currentTheme = PreferenceHelper.getTheme();
        if ("OLED".equals(currentTheme)) {
            selectedThemeText.setText(getString(R.string.dark_oled));
        } else {
            selectedThemeText.setText(currentTheme);
        }
        bufferBar.setProgress(index);
        bufferValueText.setText(formatBufferTime(bufferTime));

        limitResetForEachCallSwitchBtn.setChecked(islimitRestForEachCallEnabled);

        layoutTheme.setOnClickListener(v -> showThemeBottomSheet());

        backBtn.setOnClickListener(view -> finish());

        bufferBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int selectedValue = BUFFER_VALUES[progress];
                bufferValueText.setText(formatBufferTime(selectedValue));
                PreferenceHelper.saveBufferTime(selectedValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        about.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.this, About.class);
            startActivity(intent);
        });

        permissions.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.this, Permissions.class);
            startActivity(intent);
        });

        callStartBufferSwitchBtn.setOnCheckedChangeListener((compoundButton, b) -> PreferenceHelper.updateCallStartBufferValue(b));

        githubIssues.setOnClickListener(view -> {
            String url = "https://github.com/Thiru-Malai/CallLimiter/issues";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        switchBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            PreferenceHelper.updateLimitForAllNumbersEnabled(b);
            isChecked = b;
            if(b){
                timeLimitForAllNumbers.setVisibility(View.VISIBLE);
            } else {
                timeLimitForAllNumbers.setVisibility(View.GONE);
            }
        });

        timeLimit.setOnClickListener(view -> {
            TimerBottomSheet bottomSheet = new TimerBottomSheet(new TimerBottomSheet.OnTimeSelectedListener() {
                @Override
                public void onTimeSelected(int hours1, int minutes1, int seconds1) {
                    String hStr = (hours1 < 10) ? "0" + hours1 : Integer.toString(hours1);
                    String mStr = (minutes1 < 10) ? "0" + minutes1 : Integer.toString(minutes1);
                    String sStr = (seconds1 < 10) ? "0" + seconds1 : Integer.toString(seconds1);
                    timeLimit.setText(hStr + ":" + mStr + ":" + sStr);

                    int timeLimitInSeconds = (hours1 * 3600) + (minutes1 * 60) + seconds1;
                    PreferenceHelper.updateTimeLimitForAllNumbers(timeLimitInSeconds);
                }

                @Override
                public void onTimerReset() { }
            });
            bottomSheet.show(getSupportFragmentManager(), "TimerBottomSheet");
        });

        limitResetForEachCallSwitchBtn.setOnCheckedChangeListener((compoundButton, b) -> {
            PreferenceHelper.updateLimitForEachCallValue(b);
            if(b){
                Map<String, ?> all = PreferenceHelper.getAllContact();
                for (String phoneNumber : all.keySet()) {
                    try{
                        JSONObject jsonObject = new JSONObject((String) Objects.requireNonNull(all.get(phoneNumber)));
                        int limit = jsonObject.getInt("limit");
                        jsonObject.put("remaining_time", limit);
                        PreferenceHelper.saveContact(phoneNumber, jsonObject.toString());
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

        warningReminderSwitchBtn.setChecked(isWarningReminderEnabled);
        warningReminderTimeLayout.setVisibility(isWarningReminderEnabled ? View.VISIBLE : View.GONE);
        warningReminderTimeText.setText(formatThresholds(PreferenceHelper.getWarningReminderThresholds()));

        warningReminderSwitchBtn.setOnCheckedChangeListener((compoundButton, enabled) -> {
            PreferenceHelper.updateWarningReminderEnabled(enabled);
            warningReminderTimeLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        });

        warningReminderTimeLayout.setOnClickListener(v -> showWarningThresholdsDialog());
    }

    private void showWarningThresholdsDialog() {
        String[] labels = {"5 s", "10 s", "15 s", "20 s", "30 s", "45 s", "60 s", "120 s"};
        int[] values = {5, 10, 15, 20, 30, 45, 60, 120};
        boolean[] checkedItems = new boolean[values.length];
        Set<Integer> currentThresholds = PreferenceHelper.getWarningReminderThresholds();

        for (int i = 0; i < values.length; i++) {
            checkedItems[i] = currentThresholds.contains(values[i]);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warning_reminder_thresholds)
                .setMultiChoiceItems(labels, checkedItems, (dialog, which, isChecked1) -> checkedItems[which] = isChecked1)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Set<Integer> newThresholds = new TreeSet<>(Collections.reverseOrder());
                    for (int i = 0; i < values.length; i++) {
                        if (checkedItems[i]) {
                            newThresholds.add(values[i]);
                        }
                    }
                    if (newThresholds.isEmpty()) {
                        newThresholds.add(15);
                    }
                    PreferenceHelper.updateWarningReminderThresholds(newThresholds);
                    warningReminderTimeText.setText(formatThresholds(newThresholds));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String formatThresholds(Set<Integer> thresholds) {
        StringBuilder sb = new StringBuilder();
        for (Integer t : thresholds) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(formatBufferTime(t));
        }
        return sb.length() > 0 ? sb.toString() : "15 s";
    }

    private void showThemeBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.theme_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        String selectedTheme = PreferenceHelper.getTheme();

        LinearLayout optionSystem = sheetView.findViewById(R.id.option_system);
        LinearLayout optionLight = sheetView.findViewById(R.id.option_light);
        LinearLayout optionDark = sheetView.findViewById(R.id.option_dark);
        LinearLayout optionOled = sheetView.findViewById(R.id.option_oled);

        RadioButton systemRadioBtn = sheetView.findViewById(R.id.system_radio_btn);
        RadioButton lightRadioBtn = sheetView.findViewById(R.id.light_radio_btn);
        RadioButton darkRadioBtn = sheetView.findViewById(R.id.dark_radio_btn);
        RadioButton oledRadioBtn = sheetView.findViewById(R.id.oled_radio_btn);

        switch (selectedTheme){
            case "Light":
                lightRadioBtn.setChecked(true);
                break;
            case "Dark":
                darkRadioBtn.setChecked(true);
                break;
            case "OLED":
                if (oledRadioBtn != null) oledRadioBtn.setChecked(true);
                break;
            default:
                systemRadioBtn.setChecked(true);
                break;
        }

        optionSystem.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("System");
            selectedThemeText.setText("System");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        optionLight.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("Light");
            selectedThemeText.setText("Light");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        optionDark.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("Dark");
            selectedThemeText.setText("Dark");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        if (optionOled != null) {
            optionOled.setOnClickListener(v -> {
                PreferenceHelper.saveTheme("OLED");
                selectedThemeText.setText(getString(R.string.dark_oled));
                ThemeUtils.applyTheme(Settings.this);
                recreate();
                bottomSheetDialog.dismiss();
            });
        }

        systemRadioBtn.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("System");
            selectedThemeText.setText("System");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        lightRadioBtn.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("Light");
            selectedThemeText.setText("Light");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        darkRadioBtn.setOnClickListener(v -> {
            PreferenceHelper.saveTheme("Dark");
            selectedThemeText.setText("Dark");
            ThemeUtils.applyTheme(Settings.this);
            recreate();
            bottomSheetDialog.dismiss();
        });

        if (oledRadioBtn != null) {
            oledRadioBtn.setOnClickListener(v -> {
                PreferenceHelper.saveTheme("OLED");
                selectedThemeText.setText(getString(R.string.dark_oled));
                ThemeUtils.applyTheme(Settings.this);
                recreate();
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private String formatBufferTime(int seconds) {
        if (seconds < 60) {
            return seconds + " s";
        } else {
            int minutes = seconds / 60;
            return minutes + " min";
        }
    }
}