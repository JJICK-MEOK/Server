package com.jjikmeok.app.domain.tag.repository;

import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagGroupType;
import com.jjikmeok.app.domain.tag.entity.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByOrderByTypeAscNameAsc();

    List<Tag> findAllByTypeOrderByNameAsc(TagType type);

    List<Tag> findAllByTagGroupTypeOrderByTypeAscNameAsc(TagGroupType tagGroupType);

    List<Tag> findAllByTypeAndTagGroupTypeOrderByNameAsc(TagType type, TagGroupType tagGroupType);

    List<Tag> findAllByIdIn(Collection<Long> ids);

    List<Tag> findAllByIdInAndType(Collection<Long> ids, TagType type);

    boolean existsByNameAndType(String name, TagType type);

    boolean existsByNameAndTypeAndIdNot(String name, TagType type, Long id);
}
