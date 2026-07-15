package com.jjikmeok.app.domain.personalization.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PreferenceTagVectorFactory {

    private PreferenceTagVectorFactory() {
    }

    static int[] create(
            List<Long> preferenceTagIdsOrderedByDatabaseId,
            Collection<Long> selectedTagIds
    ) {
        int[] vector = new int[preferenceTagIdsOrderedByDatabaseId.size()];
        if (selectedTagIds == null || selectedTagIds.isEmpty()) {
            return vector;
        }

        Set<Long> selectedTagIdSet = new HashSet<>(selectedTagIds);
        for (int index = 0; index < preferenceTagIdsOrderedByDatabaseId.size(); index++) {
            if (selectedTagIdSet.contains(preferenceTagIdsOrderedByDatabaseId.get(index))) {
                vector[index] = 1;
            }
        }
        return vector;
    }
}
