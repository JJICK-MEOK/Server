package com.jjikmeok.app.domain.personalization.entity;

public final class PreferenceVectorValidator {

    private PreferenceVectorValidator() {
    }

    public static float[] validateAndCopy(float[] embedding) {
        if (embedding == null) {
            throw new IllegalArgumentException(
                    "벡터는 null일 수 없습니다."
            );
        }

        if (embedding.length != PreferenceVectorConstants.DIMENSION) {
            throw new IllegalArgumentException(
                    "벡터 차원은 "
                            + PreferenceVectorConstants.DIMENSION
                            + "이어야 합니다. 입력된 차원: "
                            + embedding.length
            );
        }

        for (float value : embedding) {
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new IllegalArgumentException(
                        "벡터에는 NaN 또는 Infinity를 저장할 수 없습니다."
                );
            }
        }

        return embedding.clone();
    }
}
