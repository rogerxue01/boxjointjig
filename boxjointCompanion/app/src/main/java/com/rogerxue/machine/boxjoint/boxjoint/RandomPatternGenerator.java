package com.rogerxue.machine.boxjoint.boxjoint;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomPatternGenerator {

    private static final Random random = new Random();

    public static List<Integer> generate(int minWidth, int maxWidth, int pairCount) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < pairCount; ++i) {
            result.add(random.nextInt((maxWidth - minWidth) + 1) + minWidth);
            result.add(random.nextInt((maxWidth - minWidth) + 1) + minWidth);
        }
        return result;
    }
}
