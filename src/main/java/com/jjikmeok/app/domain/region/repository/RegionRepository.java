package com.jjikmeok.app.domain.region.repository;

import com.jjikmeok.app.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByParentIdIsNull();
    List<Region> findByParentId(Long parentId);
    List<Region> findAllByIdIn(Collection<Long> ids);

    boolean existsByParentId(Long parentId);
    boolean existsByParentIsNullAndName(String name);
    boolean existsByParentIsNullAndNameAndIdNot(String name, Long id);
    boolean existsByParentIdAndName(Long parentId, String name);
    boolean existsByParentIdAndNameAndIdNot(Long parentId, String name, Long id);
}
