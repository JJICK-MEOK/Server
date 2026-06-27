package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.tag.entity.Tag;
import com.jjikmeok.app.domain.tag.entity.TagType;
import com.jjikmeok.app.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityTagAutoAttachService {

    private final ActivityTagSuggestionService suggestionService;
    private final TagRepository tagRepository;

    public void refresh(Activity activity) {
        if (activity == null) {
            return;
        }

        List<Tag> tags = suggestionService.suggest(activity).stream()
                .map(this::resolve)
                .filter(tag -> tag != null)
                .toList();

        activity.replaceTags(tags);
    }

    private Tag resolve(PreferenceTag preferenceTag) {
        return tagRepository.findByNameAndType(preferenceTag.getLabel(), TagType.PREFERENCE_TAG)
                .orElseGet(() -> tagRepository.save(Tag.create(preferenceTag.getLabel(), TagType.PREFERENCE_TAG)));
    }
}
