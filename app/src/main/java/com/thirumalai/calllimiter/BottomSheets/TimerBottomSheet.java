package com.thirumalai.calllimiter.BottomSheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textview.MaterialTextView;
import com.thirumalai.calllimiter.R;

public class TimerBottomSheet extends BottomSheetDialogFragment {
    private NumberPicker hourPicker, minutePicker, secondPicker;
    private Button btnDeleteTimer, btnOk, btnCancel;
    private MaterialTextView displayPhoneNumberAndName;
    private String phoneNumber = "", name = "";
    private OnTimeSelectedListener timeSelectedListener;

    // Interface to send data back to MainActivity
    public interface OnTimeSelectedListener {
        void onTimeSelected(int hours, int minutes, int seconds);
        void onTimerReset();
    }

    public TimerBottomSheet() {
        // Required empty public constructor
    }

    public TimerBottomSheet(OnTimeSelectedListener listener) {
        this.timeSelectedListener = listener;
    }
    public TimerBottomSheet(OnTimeSelectedListener listener, String phoneNumber, String name) {
        this.timeSelectedListener = listener;
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer_bottom_sheet, container, false);

        hourPicker = view.findViewById(R.id.hourPicker);
        minutePicker = view.findViewById(R.id.minutePicker);
        secondPicker = view.findViewById(R.id.secondPicker);
        btnDeleteTimer = view.findViewById(R.id.btnDeleteTimer);
        btnOk = view.findViewById(R.id.btnOk);
        btnCancel = view.findViewById(R.id.btnCancel);
        displayPhoneNumberAndName = view.findViewById(R.id.phone_number_name);

        if(!this.phoneNumber.isEmpty()){
            if(!this.name.isEmpty()){
                displayPhoneNumberAndName.setText(this.phoneNumber + "  " + this.name);
            } else {
                displayPhoneNumberAndName.setText(this.phoneNumber);
            }
        } else {
            displayPhoneNumberAndName.setVisibility(View.GONE);
        }

        // Configure Hour Picker (0 to 24)
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(24);

        // Configure Minute Picker (0 to 59)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);

        // Configure Second Picker (0 to 59)
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);

        // Delete Timer (Reset Pickers)
        btnDeleteTimer.setOnClickListener(v -> {
            hourPicker.setValue(0);
            minutePicker.setValue(0);
            secondPicker.setValue(0);
            if (timeSelectedListener != null) {
                timeSelectedListener.onTimerReset();
            }
        });

        // OK Button (Return Selected Values)
        btnOk.setOnClickListener(v -> {
            int selectedHours = hourPicker.getValue();
            int selectedMinutes = minutePicker.getValue();
            int selectedSeconds = secondPicker.getValue();
            if(selectedHours == 0 && selectedMinutes == 0 && selectedSeconds == 0){
                Toast.makeText(getActivity(), "Please choose a proper limit", Toast.LENGTH_SHORT).show();
            } else{
                if (timeSelectedListener != null) {
                    timeSelectedListener.onTimeSelected(selectedHours, selectedMinutes, selectedSeconds);
                }
                dismiss();
            }
        });

        // Cancel Button (Close Bottom Sheet)
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}
