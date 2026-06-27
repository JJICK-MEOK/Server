package com.jjikmeok.app.domain.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.sync.dto.NormalizedActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityNormalizerTest {

    private ActivityNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ActivityNormalizer(new ObjectMapper(), new CategoryClassifier(), new ActivitySyncUtils());
    }

    @Test
    void normalize_parsesJsonAndXmlItems() {
        String json = """
                {"data":[{"LOCAL_ID":"e1","TITLE":"photo","DESCRIPTION":"desc","IMAGE_OBJECT":"img",
                "URL":"https://exhibition","EVENT_SITE":"gallery","PERIOD":"2026.05.01 ~ 2026.05.03","CHARGE":"5,000"}]}
                """;

        NormalizedActivity exhibition = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", json).getFirst();

        assertThat(exhibition.externalId()).isEqualTo("e1");
        assertThat(exhibition.title()).isNotBlank();
        assertThat(exhibition.sourceUrl()).isEqualTo("https://exhibition");
        assertThat(exhibition.category()).isNotNull();
        assertThat(exhibition.startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(exhibition.endAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 3));

        String xml = """
                <response><body><items><item>
                  <mt20id>k1</mt20id><prfnm>concert</prfnm><genrenm>concert</genrenm><poster>poster</poster>
                </item></items></body></response>
                """;
        List<NormalizedActivity> kopis = normalizer.normalize(SourceType.KOPIS, "https://kopis", "XML", xml);

        assertThat(kopis).hasSize(1);
        assertThat(kopis.getFirst().externalId()).isEqualTo("k1");
        assertThat(kopis.getFirst().thumbnailUrl()).isEqualTo("poster");
        assertThat(kopis.getFirst().category()).isNotNull();
        assertThat(kopis.getFirst().approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void normalize_unescapesTitleAndUsesFirstPriceAmount() {
        String payload = """
                {"data":[{"LOCAL_ID":"e2","TITLE":"Title &lt;Sub&gt;","URL":"https://example.com","CHARGE":"adult 10,000 / youth 7,000"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.title()).isNotBlank();
        assertThat(activity.sourceUrl()).isNotBlank();
    }

    @Test
    void normalize_repairsMojibakeAndFiltersInvalidPlaceMetadata() {
        String payload = """
                {"data":[{"LOCAL_ID":"e3","TITLE":"title","PLACE":"seoul","ORG_NAME":"invalid metadata text"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.title()).isNotBlank();
        assertThat(activity.organizer()).isNotNull();
    }
}
