package com.rogerxue.machine.boxjoint.boxjoint;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

public class MainActivity extends Activity {
    private static final String TAG = "boxjoint-main";

    private BluetoothFragment mBluetoothFragment;
    private BoxJointCalculationFragment mBoxjointFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_boxjoint:
                    switchToCalculator();
                    return true;
                case R.id.navigation_bluetooth:
                    switchToBluetooth();
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothFragment = new BluetoothFragment();
        mBoxjointFragment = new BoxJointCalculationFragment();

        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.content) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }
            switchToCalculator();
        }

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void switchToBluetooth() {
        getFragmentManager().beginTransaction()
                .replace(R.id.content, mBluetoothFragment).commit();
    }

    private void switchToCalculator() {
        getFragmentManager().beginTransaction()
                .replace(R.id.content, mBoxjointFragment).commit();
    }
}
