package com.jjikmeok.app.domain.tag.repository;

import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Tag repository.
 * <p>
 * 태그 조회와 온보딩 검증에 필요한 접근 메서드를 제공한다.
 *
 * @author Codex
 * @version 1.0
 * @since 2026-05-16
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByIdIn(Collection<Long> ids);

    List<Tag> findAllByIdInAndType(Collection<Long> ids, TagType type);
}
