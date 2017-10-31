package com.rogerxue.machine.boxjoint.boxjoint;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoxJointCalculationFragment extends Fragment {
    private static final String TAG = "boxjoint-fragment";

    private static final String KERF_KEY = "kerf";
    private static final String FINGER_PATTERN_KEY = "fingerPattern";
    private static final String TOLERANCE_KEY = "tolerance";
    private static final String STOCK_WIDTH_KEY = "stockWidth";
    private static final String SMOOTHNESS_KEY = "smoothness";

    private List<Integer> gapFirstMoves;
    private List<Integer> fingerFirstMoves;
    private TextView mFingerPattern;
    private TextView mGapFirstOutput;
    private TextView mFingerFirstOutput;
    private TextView mKerf;
    private TextView mTolerance;
    private TextView mStockWidth;
    private TextView mSmoothness;
    private TextView mMessage;

    // random generator
    private TextView mMinWidth;
    private TextView mMaxWidth;
    private TextView mNumOfPairs;
    private Button mRandomBtn;

    private BluetoothSerialUtil mBluetoothSerialUtil;

    private final MoveCalculator mMoveCalculator =
            new MoveCalculator();

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KERF_KEY, mKerf.getText());
        outState.putCharSequence(FINGER_PATTERN_KEY, mFingerPattern.getText());
        outState.putCharSequence(TOLERANCE_KEY, mTolerance.getText());
        outState.putCharSequence(STOCK_WIDTH_KEY, mStockWidth.getText());
        outState.putCharSequence(SMOOTHNESS_KEY, mSmoothness.getText());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothSerialUtil = BluetoothSerialUtil.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(
                R.layout.boxjoint_calculation_fragment, container, false);
        mMoveCalculator.setKerf(100).setTolerance(2).setStockWidth(3000);

        mMinWidth = root.findViewById(R.id.minWidth);
        mMaxWidth = root.findViewById(R.id.maxWidth);
        mNumOfPairs = root.findViewById(R.id.numOfPairs);
        mRandomBtn = root.findViewById(R.id.random_btn);

        mGapFirstOutput = root.findViewById(R.id.gap_first_output);
        mFingerFirstOutput = root.findViewById(R.id.finger_first_output);
        mKerf = root.findViewById(R.id.kerf);
        mTolerance = root.findViewById(R.id.tolerance);
        mSmoothness =  root.findViewById(R.id.smoothness);
        mStockWidth = root.findViewById(R.id.stock_width);
        mFingerPattern = root.findViewById(R.id.finger_pattern);
        mMessage = root.findViewById(R.id.message);
        Button sendGapBtn = root.findViewById(R.id.send_gap_first);
        Button sendFingerBtn = root.findViewById(R.id.send_finger_first);

        mFingerPattern.addTextChangedListener(new SimpleTextWather() {
            @Override
            public void afterTextChanged(Editable s) {
                tryCalculateMoves();
            }
        });

        if (savedInstanceState != null) {
            mKerf.setText(savedInstanceState.getCharSequence(KERF_KEY));
            mFingerPattern.setText(savedInstanceState.getCharSequence(FINGER_PATTERN_KEY));
            mTolerance.setText(savedInstanceState.getCharSequence(TOLERANCE_KEY));
            mStockWidth.setText(savedInstanceState.getCharSequence(STOCK_WIDTH_KEY));
            mSmoothness.setText(savedInstanceState.getCharSequence(SMOOTHNESS_KEY));
        } else {
            mKerf.setText(String.valueOf(mMoveCalculator.getKerf()));
            mTolerance.setText(String.valueOf(mMoveCalculator.getTolerance()));
            mSmoothness.setText(String.valueOf(mMoveCalculator.getSmoothness()));
            mStockWidth.setText(String.valueOf(mMoveCalculator.getStockWidth()));
        }

        mRandomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(mMinWidth.getText())
                        && !TextUtils.isEmpty(mMaxWidth.getText())
                        && !TextUtils.isEmpty(mNumOfPairs.getText())) {
                    try {
                        int minWidth = Integer.parseInt(mMinWidth.getText().toString());
                        int maxWidth = Integer.parseInt(mMaxWidth.getText().toString());
                        int numOfpairs = Integer.parseInt(mNumOfPairs.getText().toString());
                        List<Integer> pattern =
                                RandomPatternGenerator.generate(minWidth, maxWidth, numOfpairs);
                        mFingerPattern.setText(TextUtils.join(",", pattern));
                    } catch (NumberFormatException ex) {
                        Log.w(TAG, "input wrong", ex);
                    }
                }
            }
        });

        sendGapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gapFirstMoves == null || gapFirstMoves.isEmpty()) {
                    Log.w(TAG, "no gapFirstMoves to send");
                    mMessage.setText("No gapFirstMoves to send");
                } else if (mBluetoothSerialUtil == null) {
                    Log.w(TAG, "sender not set");
                    mMessage.setText("sender not set");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int num : gapFirstMoves) {
                        sb.append(num);
                        sb.append(",");
                    }
                    mMessage.setText(mBluetoothSerialUtil.sendData(sb.toString())
                            ? "" + gapFirstMoves.size() + " Data send."
                            : "Data not send");
                }
            }
        });

        sendFingerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fingerFirstMoves == null || fingerFirstMoves.isEmpty()) {
                    Log.w(TAG, "no fingerFirstMoves to send");
                    mMessage.setText("No fingerFirstMoves to send");
                } else if (mBluetoothSerialUtil == null) {
                    Log.w(TAG, "sender not set");
                    mMessage.setText("sender not set");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int num : fingerFirstMoves) {
                        sb.append(num);
                        sb.append(",");
                    }
                    mMessage.setText(mBluetoothSerialUtil.sendData(sb.toString())
                            ? "" + fingerFirstMoves.size() + " Data send."
                            : "Data not send");
                }
            }
        });

        mKerf.addTextChangedListener(new SimpleTextWather() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mMoveCalculator.setKerf(Integer.parseInt(s.toString()));
                    Log.d(TAG, "setting tolerance");
                    tryCalculateMoves();
                } catch (NumberFormatException e) {
                    mMoveCalculator.setKerf(null);
                    mGapFirstOutput.setText("kerf invalid");
                }
            }
        });

        mTolerance.addTextChangedListener(new SimpleTextWather() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mMoveCalculator.setTolerance(Integer.parseInt(s.toString()));
                    Log.d(TAG, "setting tolerance");
                    tryCalculateMoves();
                } catch (NumberFormatException e) {
                    mMoveCalculator.setTolerance(null);
                    mGapFirstOutput.setText("tolerance invalid");
                }
            }
        });

        mStockWidth.addTextChangedListener(new SimpleTextWather() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mMoveCalculator.setStockWidth(Integer.parseInt(s.toString()));
                    Log.d(TAG, "setting stock width");
                    tryCalculateMoves();
                } catch (NumberFormatException e) {
                    mMoveCalculator.setStockWidth(null);
                    mGapFirstOutput.setText("stock width invalid");
                }
            }
        });


        mSmoothness.addTextChangedListener(new SimpleTextWather() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double smoothness = Double.parseDouble(s.toString());
                    if (smoothness <= 0 || smoothness >= 1) {
                        mGapFirstOutput.setText("smoothness should be (0 - 1)");
                        mMoveCalculator.setSmoothness(null);
                    } else {
                        Log.d(TAG, "setting smoothness: " + smoothness);
                        mMoveCalculator.setSmoothness(smoothness);
                        tryCalculateMoves();
                    }
                } catch (NumberFormatException e) {
                    mGapFirstOutput.setText("smoothness invalid");
                }
            }
        });
        return root;
    }

    private void tryCalculateMoves() {
        List<String> patternList = Arrays.asList(mFingerPattern.getText().toString().split(","));
        gapFirstMoves = mMoveCalculator.calculate(toNumberList(patternList), false);
        mGapFirstOutput.setText(gapFirstMoves != null
                ? TextUtils.join(",", gapFirstMoves)
                : "invalid pattern");
        fingerFirstMoves = mMoveCalculator.calculate(toNumberList(patternList), true);
        mFingerFirstOutput.setText(fingerFirstMoves != null
                ? TextUtils.join(",", fingerFirstMoves)
                : "invalid pattern");
    }

    /**
     * Number list alternate between gap width and finger width, start with gap width.
     * It needs to be in even numbers.
     */
    @Nullable
    private List<Integer> toNumberList(List<String> strings) {
        List<Integer> numbers = new ArrayList<>();
        int number;
        for (String string : strings) {
            try {
                number = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return null;
            }
            numbers.add(number);
        }
        return numbers;
    }

    private abstract class SimpleTextWather implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public abstract void afterTextChanged(Editable s);
    }
}
