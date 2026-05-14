package com.jjikmeok.app.domain.activity.repository;

import com.jjikmeok.app.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Activity a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findActiveActivitiesWithRegion();

    @Query("""
            SELECT a
            FROM Activity a
            JOIN FETCH a.region
            WHERE a.isActive = true
              AND a.region.id = :regionId
            ORDER BY a.createdAt DESC
            """)
    List<Activity> findActiveActivitiesByRegionIdWithRegion(@Param("regionId") Long regionId);

    @Query("SELECT a FROM Activity a JOIN FETCH a.region WHERE a.id = :id")
    Optional<Activity> findByIdWithRegion(@Param("id") Long id);

    boolean existsByRegionId(Long regionId);
}
