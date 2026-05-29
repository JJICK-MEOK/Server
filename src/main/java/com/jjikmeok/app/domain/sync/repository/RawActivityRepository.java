package com.jjikmeok.app.domain.sync.repository;

import com.jjikmeok.app.domain.sync.entity.RawActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawActivityRepository extends JpaRepository<RawActivity, Long> {
}
