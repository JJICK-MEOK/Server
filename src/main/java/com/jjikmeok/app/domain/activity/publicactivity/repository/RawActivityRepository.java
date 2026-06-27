package com.jjikmeok.app.domain.activity.publicactivity.repository;

import com.jjikmeok.app.domain.activity.publicactivity.entity.RawActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawActivityRepository extends JpaRepository<RawActivity, Long> {
}
