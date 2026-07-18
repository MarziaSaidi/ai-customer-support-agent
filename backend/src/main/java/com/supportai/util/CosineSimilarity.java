package com.supportai.util;

public final class CosineSimilarity {

    private CosineSimilarity() {}

    public static double calculate(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            return 0.0;
        }

        int length = Math.min(left.length, right.length);
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
