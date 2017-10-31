package com.rogerxue.machine.boxjoint.boxjoint;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * All units in 1/1000 of an inch A.K.A 1 thou
 */
public class MoveCalculator {
    private static final String TAG = "MoveCalculator";
    // from 0 to 1, 0 is the finest, 1 is the coarsest
    private Double smoothness = 0.5;
    private Integer kerf = 100;
    private Integer tolerance = 2;
    private Integer stockWidth;

    public Double getSmoothness() {
        return smoothness;
    }

    public Integer getKerf() {
        return kerf;
    }

    public Integer getTolerance() {
        return tolerance;
    }

    public Integer getStockWidth() {
        return stockWidth;
    }

    public MoveCalculator setKerf(@Nullable Integer kerf) {
        this.kerf = kerf;
        return this;
    }

    public MoveCalculator setTolerance(@Nullable Integer tolerance) {
        this.tolerance = tolerance;
        return this;
    }

    public MoveCalculator setSmoothness(@Nullable Double smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public MoveCalculator setStockWidth(@Nullable Integer stockWidth) {
        this.stockWidth = stockWidth;
        return this;
    }

    /**
     * calculates the moves
     */
    @Nullable
    public List<Integer> calculate(@Nullable List<Integer> pattern, boolean fingerFirst) {
        if (pattern == null) {
            Log.d(TAG, "pattern is null");
            return null;
        }
        if (kerf == null || tolerance == null || smoothness == null || stockWidth == null) {
            Log.w(TAG, "not setup yet: " + kerf + tolerance + smoothness + stockWidth);
            return null;
        }
        // pattern has to be even number
        if (pattern.size() % 2 != 0) {
            Log.w(TAG, "Pattern is not even number.");
            return null;
        }
        List<Integer> moves = new ArrayList<>();
        boolean first = true;
        for (int totalMoves = 0; totalMoves < stockWidth; /*increment done in loop*/) {
            int travel = addMovesForOneIteration(pattern, moves, first, fingerFirst);
            if (travel < 0) {
                return null;
            } else {
                totalMoves += travel;
            }
            first = false;
        }
        return moves;
    }

    /**
     * @return the total width of this iteration, -1 if any error.
     */
    private int addMovesForOneIteration(
            List<Integer> pattern, 
            List<Integer> moves,
            boolean first,
            boolean fingerFirst) {
        int totalMove = 0;
        int i = 0;
        if (fingerFirst) {
            if (first) {
                // Right side of blade line up with right side of stock,
                // so first cut is special.
                // if finger is too thin, we can't cut the gap + tolerance for it
                int finger = pattern.get(i++);
                if (finger < kerf + tolerance) {
                    Log.w(TAG, "finger too thin: " + finger);
                    return -1;
                }
                moves.add(finger - tolerance);

                int gap = cutGap(pattern.get(i++), moves);
                if (gap < 0) {
                    return -1;
                }
                totalMove += gap;
            }
            for (/* already initialized */; i < pattern.size(); i += 2) {
                // for the finger
                int finger = cutFinger(pattern.get(i), moves);
                if (finger < 0) {
                    return -1;
                }
                totalMove += finger;
                
                // for the gap
                int gap = cutGap(pattern.get(i + 1), moves);
                if (gap < 0) {
                    return -1;
                }
                totalMove += gap;
            }
        } else {
            for (/* already initialized */; i < pattern.size(); i += 2) {
                // for the gap
                int gap = cutGap(pattern.get(i), moves);
                if (gap < 0) {
                    return -1;
                }
                totalMove += gap;
    
                // for the finger
                int finger = cutFinger(pattern.get(i + 1), moves);
                if (finger < 0) {
                    return -1;
                }
                totalMove += finger;
            }
        }
        return totalMove;
    }
    
    private int cutGap(int gap, List<Integer> moves) {
        if (gap < kerf + tolerance) {
            Log.w(TAG, "gap too thin: " + gap);
            return -1;
        }
        int cutWidth = kerf;
        while (true) {
            int possibleMove = (int) (kerf * smoothness);
            if (cutWidth + possibleMove < gap + tolerance) {
                moves.add(possibleMove);
                cutWidth += possibleMove;
            } else {
                // the normal increment will exceed the gap.
                // that means this is the final cut, break the loop;
                moves.add(gap + tolerance - cutWidth);
                break;
            }
        }
        return gap;
    }
    
    private int cutFinger(int finger, List<Integer> moves) {
        // if finger is too thin, we can't cut the gap + tolerance for it
        if (finger < kerf + tolerance) {
            Log.w(TAG, "finger too thin: " + finger);
            return -1;
        }
        // finger width is thinner by a tolerance
        moves.add(kerf + finger - tolerance);
        return finger;
    }
}
