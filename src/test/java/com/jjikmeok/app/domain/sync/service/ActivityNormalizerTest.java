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
                {"response":{"body":{"items":{"item":[
                  {"contentid":"t1","title":"walk","addr1":"seoul","firstimage":"thumb","eventstartdate":"20260501","eventenddate":"20260502"}
                ]}}}}
                """;
        List<NormalizedActivity> tour = normalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", json);

        assertThat(tour).hasSize(1);
        assertThat(tour.getFirst().externalId()).isEqualTo("t1");
        assertThat(tour.getFirst().title()).isNotBlank();
        assertThat(tour.getFirst().category()).isNotNull();
        assertThat(tour.getFirst().approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

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
    }

    @Test
    void normalize_mapsUppercaseAndPeriodFields() {
        String payload = """
                {"data":[{"LOCAL_ID":"e1","TITLE":"photo","DESCRIPTION":"desc","IMAGE_OBJECT":"img",
                "URL":"https://exhibition","EVENT_SITE":"gallery","PERIOD":"2026.05.01 ~ 2026.05.03","CHARGE":"5,000"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.externalId()).isEqualTo("e1");
        assertThat(activity.title()).isNotBlank();
        assertThat(activity.sourceUrl()).isEqualTo("https://exhibition");
        assertThat(activity.category()).isNotNull();
        assertThat(activity.startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(activity.endAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 3));
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

    @Test
    void normalize_parsesKopisDbsDb() {
        String xml = """
                <dbs><db>
                  <mt20id>PF1</mt20id><prfnm>music</prfnm><prfpdfrom>2026.05.24</prfpdfrom>
                  <prfpdto>2026.06.24</prfpdto><fcltynm>hall</fcltynm><poster>poster</poster>
                  <genrenm>music</genrenm><prfstate>공연중</prfstate>
                </db><db>
                  <mt20id>PF2</mt20id><prfnm>theater</prfnm><prfpdfrom>20260525</prfpdfrom>
                  <prfpdto>20260625</prfpdto><fcltynm>art</fcltynm><poster>poster2</poster>
                  <genrenm>theater</genrenm><prfstate>공연완료</prfstate>
                </db></dbs>
                """;

        List<NormalizedActivity> activities = normalizer.normalize(SourceType.KOPIS, "https://kopis", "XML", xml);
        assertThat(activities).hasSize(2);
        assertThat(activities.getFirst().externalId()).isEqualTo("PF1");
        assertThat(activities.get(1).externalId()).isEqualTo("PF2");
        assertThat(activities.get(1).startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 25));
    }

    @Test
    void normalize_parsesYouthContentResponse() {
        String payload = """
                {"resultCode":200,"result":{"youthPolicyList":[
                  {"bbsSn":"48","pstSn":"10580","pstSeNm":"job","pstTtl":"AI program",
                   "pstWholCn":"<p><a href=\\"https://sesac.seoul.kr/course\\">apply</a></p>","atchFile":"data:image/jpeg;base64,abcdef"}
                ]}}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.YOUTH_CONTENT, "https://api", "JSON", payload).getFirst();
        assertThat(activity.externalId()).isEqualTo("48:10580");
        assertThat(activity.title()).isNotBlank();
        assertThat(activity.description()).isNotBlank();
        assertThat(activity.sourceUrl()).isNotBlank();
        assertThat(activity.thumbnailUrl()).startsWith("data:image/jpeg;base64,");
        assertThat(activity.category()).isNotNull();
        assertThat(activity.activityType()).isNotNull();
    }

    @Test
    void normalize_filtersYouthNoticeNewsAndTips() {
        String payload = """
                {"result":{"youthPolicyList":[
                  {"bbsSn":"46","pstSn":"1","pstSeNm":"notice","pstTtl":"notice title","pstWholCn":"content"},
                  {"bbsSn":"54","pstSn":"2","pstSeNm":"news","pstTtl":"news title","pstWholCn":"content"},
                  {"bbsSn":"48","pstSn":"3","pstSeNm":"job","pstTtl":"program recruit","pstWholCn":"<a href=\\"https://example.com\\">apply</a>"}
                ]}}
                """;

        List<NormalizedActivity> activities = normalizer.normalize(SourceType.YOUTH_CONTENT, "https://api", "JSON", payload);
        assertThat(activities).isNotEmpty();
        assertThat(activities.getFirst().externalId()).isNotBlank();
    }
}
