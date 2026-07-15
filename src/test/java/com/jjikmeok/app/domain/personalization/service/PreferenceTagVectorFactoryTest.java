package com.jjikmeok.app.domain.personalization.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenceTagVectorFactoryTest {

    @Test
    void create_usesDatabaseIdOrderAsVectorDimensionOrder() {
        int[] vector = PreferenceTagVectorFactory.create(
                List.of(10L, 20L, 30L, 40L),
                List.of(30L, 10L)
        );

        assertThat(vector).containsExactly(1, 0, 1, 0);
    }

    @Test
    void create_ignoresIdsOutsidePreferenceTagCatalog() {
        int[] vector = PreferenceTagVectorFactory.create(
                List.of(1L, 2L),
                List.of(1L, 999L)
        );

        assertThat(vector).containsExactly(1, 0);
    }
}
