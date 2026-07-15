package com.jjikmeok.app.domain.personalization.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CosineSimilarityCalculatorTest {

    @Test
    void scoreCalculatesCosineSimilarityForIntVectors() {
        assertThat(CosineSimilarityCalculator.score(
                new int[]{1, 1, 0, 0},
                new int[]{1, 1, 0, 0}
        )).isEqualTo(100);
        assertThat(CosineSimilarityCalculator.score(
                new int[]{1, 1, 0, 0},
                new int[]{1, 0, 1, 0}
        )).isEqualTo(50);
        assertThat(CosineSimilarityCalculator.score(
                new int[]{1, 0},
                new int[]{0, 1}
        )).isZero();
    }

    @Test
    void scoreRejectsInvalidIntVectors() {
        assertThat(CosineSimilarityCalculator.score(new int[]{0, 0}, new int[]{1, 0})).isNull();
        assertThat(CosineSimilarityCalculator.score(new int[0], new int[0])).isNull();
        assertThat(CosineSimilarityCalculator.score(new int[]{1}, new int[]{1, 0})).isNull();
    }

    @Test
    void scoreCalculatesAndClampsCosineSimilarity() {
        assertThat(CosineSimilarityCalculator.score(vector(1, 0), vector(1, 0))).isEqualTo(100);
        assertThat(CosineSimilarityCalculator.score(vector(1, 0), vector(0, 1))).isZero();
        assertThat(CosineSimilarityCalculator.score(vector(1, 0), vector(-1, 0))).isZero();
        assertThat(CosineSimilarityCalculator.score(vector(1, 1), vector(1, 0))).isEqualTo(71);
    }

    @Test
    void scoreRejectsUnsafeVectors() {
        assertThat(CosineSimilarityCalculator.score(vector(0, 0), vector(1, 0))).isNull();
        assertThat(CosineSimilarityCalculator.score(new float[13], vector(1, 0))).isNull();
        assertThat(CosineSimilarityCalculator.score(null, vector(1, 0))).isNull();

        float[] nan = vector(1, 0);
        nan[2] = Float.NaN;
        assertThat(CosineSimilarityCalculator.score(nan, vector(1, 0))).isNull();

        float[] infinity = vector(1, 0);
        infinity[2] = Float.POSITIVE_INFINITY;
        assertThat(CosineSimilarityCalculator.score(infinity, vector(1, 0))).isNull();
    }

    private float[] vector(float first, float second) {
        float[] values = new float[14];
        values[0] = first;
        values[1] = second;
        return values;
    }
}
