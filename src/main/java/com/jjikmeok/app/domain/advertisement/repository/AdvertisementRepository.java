package com.jjikmeok.app.domain.advertisement.repository;

import com.jjikmeok.app.domain.advertisement.entity.Advertisement;
import com.jjikmeok.app.domain.advertisement.enums.AdvertisementPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @Query("""
            select advertisement
            from Advertisement advertisement
            where advertisement.isActive = true
              and (:position is null or advertisement.position = :position)
              and (advertisement.startAt is null or advertisement.startAt <= :now)
              and (advertisement.endAt is null or advertisement.endAt >= :now)
            order by advertisement.position asc, advertisement.sortOrder asc, advertisement.id asc
            """)
    List<Advertisement> findVisibleAdvertisements(
            @Param("position") AdvertisementPosition position,
            @Param("now") LocalDateTime now
    );
}
