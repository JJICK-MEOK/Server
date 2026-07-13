package com.jjikmeok.app.domain.personalization.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FloatArrayJsonConverter implements AttributeConverter<float[], String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        float[] validated = PreferenceVectorValidator.validateAndCopy(attribute);
        try {
            return OBJECT_MAPPER.writeValueAsString(validated);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize preference vector to JSON", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            throw new IllegalArgumentException("Stored preference vector JSON must not be null");
        }

        try {
            float[] parsed = OBJECT_MAPPER.readValue(dbData, float[].class);
            return PreferenceVectorValidator.validateAndCopy(parsed);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize preference vector JSON", e);
        }
    }
}
