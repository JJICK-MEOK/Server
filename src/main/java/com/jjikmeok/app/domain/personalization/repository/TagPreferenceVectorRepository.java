package com.jjikmeok.app.domain.personalization.repository;

import com.jjikmeok.app.domain.personalization.entity.TagPreferenceVector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagPreferenceVectorRepository extends JpaRepository<TagPreferenceVector, Long> {

    Optional<TagPreferenceVector> findByTagId(Long tagId);
}
