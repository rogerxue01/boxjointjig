package com.rogerxue.machine.boxjoint.boxjoint;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public class BluetoothFragment extends Fragment {
    String TAG = "BluetoothFragment";

    BluetoothSerialUtil.BtSerialListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bluetooth_fragment, container, false);

        final Button connectBtn = root.findViewById(R.id.connect);
        final Button indicator = root.findViewById(R.id.indicator);
        final TextView label = root.findViewById(R.id.label);
        final EditText deviceName = root.findViewById(R.id.entry);

        mListener = new BluetoothSerialUtil.BtSerialListener() {
            @Override
            public void onConnect(boolean connected) {
                indicator.setBackgroundColor(
                        connected ? Color.GREEN : Color.RED);
            }

            @Override
            public void onDataRead(String data) {
                label.setText("Data received: " + data);
            }
        };

        BluetoothSerialUtil.getInstance().addListener(mListener);

        connectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BluetoothSerialUtil.getInstance().connect(deviceName.getText().toString());
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BluetoothSerialUtil.getInstance().removeListener(mListener);

    }
}