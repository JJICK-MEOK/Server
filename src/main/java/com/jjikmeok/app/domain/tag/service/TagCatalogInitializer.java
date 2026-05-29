package com.jjikmeok.app.domain.tag.service;

import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagCatalog;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TagCatalogInitializer implements ApplicationRunner {

    private final TagRepository tagRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        TagCatalog.entries().forEach(entry -> {
            if (!tagRepository.existsByNameAndType(entry.name(), entry.type())) {
                tagRepository.save(Tag.create(entry.name(), entry.type()));
            }
        });
    }
}
