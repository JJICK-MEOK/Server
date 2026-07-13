package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.entity.PreferenceVectorConstants;

public final class CosineSimilarityCalculator {

    private CosineSimilarityCalculator() {
    }

    public static Integer score(float[] first, float[] second) {
        if (!isValid(first) || !isValid(second) || first.length != second.length) {
            return null;
        }

        double dot = 0.0;
        double firstNormSquared = 0.0;
        double secondNormSquared = 0.0;

        for (int i = 0; i < first.length; i++) {
            dot += (double) first[i] * second[i];
            firstNormSquared += (double) first[i] * first[i];
            secondNormSquared += (double) second[i] * second[i];
        }

        if (firstNormSquared == 0.0 || secondNormSquared == 0.0) {
            return null;
        }

        double similarity = dot / (Math.sqrt(firstNormSquared) * Math.sqrt(secondNormSquared));
        if (!Double.isFinite(similarity)) {
            return null;
        }

        double clamped = Math.max(0.0, Math.min(1.0, similarity));
        return (int) Math.round(clamped * 100.0);
    }

    private static boolean isValid(float[] vector) {
        if (vector == null || vector.length != PreferenceVectorConstants.DIMENSION) {
            return false;
        }

        for (float value : vector) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
