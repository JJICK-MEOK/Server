package com.jjikmeok.app.domain.personalization.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FloatArrayJsonConverterTest {

    private final FloatArrayJsonConverter converter = new FloatArrayJsonConverter();

    @Test
    void converterRoundTripsFourteenDimensions() {
        float[] vector = new float[14];
        vector[0] = 0.25f;
        vector[13] = 1.0f;

        String json = converter.convertToDatabaseColumn(vector);

        assertThat(converter.convertToEntityAttribute(json)).containsExactly(vector);
    }

    @Test
    void converterRejectsNullWrongDimensionAndNonFiniteValues() {
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(new float[13]))
                .isInstanceOf(IllegalArgumentException.class);

        float[] invalid = new float[14];
        invalid[0] = Float.NaN;
        assertThatThrownBy(() -> converter.convertToDatabaseColumn(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void converterReportsInvalidStoredJson() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deserialize");
        assertThatThrownBy(() -> converter.convertToEntityAttribute("[1,2]"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
