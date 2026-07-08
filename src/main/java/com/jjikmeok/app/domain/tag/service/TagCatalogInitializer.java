package com.jjikmeok.app.domain.tag.service;

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
        // Tag data is seeded through SQL; no runtime initialization is required here.
    }
}
