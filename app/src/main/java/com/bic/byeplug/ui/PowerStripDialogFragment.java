package com.bic.byeplug.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bic.byeplug.R;

public class PowerStripDialogFragment extends DialogFragment {

    private static final String ARG_DEVICE_ID = "deviceId";

    public static PowerStripDialogFragment newInstance(String deviceId) {
        PowerStripDialogFragment f = new PowerStripDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_DEVICE_ID, deviceId);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_powerstrip, null);

        // TODO: v.findViewById(...)로 outlet 버튼들 연결해서 제어 붙이면 됨

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
    }
}
