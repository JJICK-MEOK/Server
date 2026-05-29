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
                  {"contentid":"t1","title":"여행 탐방","addr1":"서울","firstimage":"thumb","eventstartdate":"20260501","eventenddate":"20260502"}
                ]}}}}
                """;
        List<NormalizedActivity> tour = normalizer.normalize(SourceType.TOUR_API, "https://tour", "JSON", json);

        assertThat(tour).hasSize(1);
        assertThat(tour.getFirst().externalId()).isEqualTo("t1");
        assertThat(tour.getFirst().title()).isEqualTo("여행 탐방");
        assertThat(tour.getFirst().category()).isEqualTo(ActivityCategory.TRAVEL);
        assertThat(tour.getFirst().approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        String xml = """
                <response><body><items><item>
                  <mt20id>k1</mt20id><prfnm>국악 콘서트</prfnm><genrenm>국악</genrenm><poster>poster</poster>
                </item></items></body></response>
                """;
        List<NormalizedActivity> kopis = normalizer.normalize(SourceType.KOPIS, "https://kopis", "XML", xml);

        assertThat(kopis).hasSize(1);
        assertThat(kopis.getFirst().externalId()).isEqualTo("k1");
        assertThat(kopis.getFirst().thumbnailUrl()).isEqualTo("poster");
        assertThat(kopis.getFirst().category()).isEqualTo(ActivityCategory.MUSIC);
    }

    @Test
    void normalize_mapsUppercaseAndPeriodFields() {
        String payload = """
                {"data":[{"LOCAL_ID":"e1","TITLE":"사진 전시","DESCRIPTION":"전시 설명","IMAGE_OBJECT":"img",
                "URL":"https://exhibition","EVENT_SITE":"갤러리","PERIOD":"2026.05.01 ~ 2026.05.03","CHARGE":"5,000"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.externalId()).isEqualTo("e1");
        assertThat(activity.title()).isEqualTo("사진 전시");
        assertThat(activity.sourceUrl()).isEqualTo("https://exhibition");
        assertThat(activity.address()).isEqualTo("갤러리");
        assertThat(activity.price()).isEqualTo(5000);
        assertThat(activity.category()).isEqualTo(ActivityCategory.PHOTO_VIDEO);
        assertThat(activity.startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(activity.endAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    void normalize_unescapesTitleAndUsesFirstPriceAmount() {
        String payload = """
                {"data":[{"LOCAL_ID":"e2","TITLE":"폴란드 포스터전 &lt;침묵, 그 고요한 외침&gt;",
                "URL":"https://www.acc.go.kr/main/exhibition.do?PID=0202&action=Read&bnkey=EM_0000009455",
                "CHARGE":"성인 10,000원 / 청소년·어린이 7,000원 / 48개월 미만 유아 무료"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.title()).isEqualTo("폴란드 포스터전 <침묵, 그 고요한 외침>");
        assertThat(activity.price()).isEqualTo(10000);
    }

    @Test
    void normalize_repairsMojibakeAndFiltersInvalidPlaceMetadata() {
        String payload = """
                {"data":[{"LOCAL_ID":"e3","TITLE":"ììëìê´ 5ì ììë´ì¬ì ëª¨ì§ ìë´",
                "PLACE":"서울","ORG_NAME":"의 규정에 따릅니다. 각 시설의 규정 및 허가조건을 반드시 준수하여야 합니다."}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.EXHIBITION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.title()).isEqualTo("아양도서관 5월 자원봉사자 모집 안내");
        assertThat(activity.address()).isNull();
        assertThat(activity.organizer()).isNull();
    }

    @Test
    void normalize_parsesKopisDbsDb() {
        String xml = """
                <dbs><db>
                  <mt20id>PF1</mt20id><prfnm>뮤지컬 공연</prfnm><prfpdfrom>2026.05.24</prfpdfrom>
                  <prfpdto>2026.06.24</prfpdto><fcltynm>극장</fcltynm><poster>poster</poster>
                  <genrenm>뮤지컬</genrenm><prfstate>공연중</prfstate>
                </db><db>
                  <mt20id>PF2</mt20id><prfnm>연극 공연</prfnm><prfpdfrom>20260525</prfpdfrom>
                  <prfpdto>20260625</prfpdto><fcltynm>소극장</fcltynm><poster>poster2</poster>
                  <genrenm>연극</genrenm><prfstate>공연완료</prfstate>
                </db></dbs>
                """;

        List<NormalizedActivity> activities = normalizer.normalize(SourceType.KOPIS, "https://kopis", "XML", xml);
        NormalizedActivity activity = activities.getFirst();

        assertThat(activities).hasSize(2);
        assertThat(activity.externalId()).isEqualTo("PF1");
        assertThat(activity.title()).isEqualTo("뮤지컬 공연");
        assertThat(activity.address()).isEqualTo("극장");
        assertThat(activity.thumbnailUrl()).isEqualTo("poster");
        assertThat(activity.active()).isTrue();
        assertThat(activities.get(1).externalId()).isEqualTo("PF2");
        assertThat(activities.get(1).startAt().toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(activities.get(1).active()).isFalse();
    }

    @Test
    void normalize_parsesYouthContentResponse() {
        String payload = """
                {"resultCode":200,"result":{"pagging":{"totCount":271,"pageNum":1,"pageSize":10},"youthPolicyList":[
                  {"bbsSn":"48","pstSn":"10580","pstSeSn":"10003","pstSeNm":"직업훈련",
                   "pstTtl":"AI활용 업무효율 극강 MICE기획자 양성과정 참여 안내",
                   "pstWholCn":"<p><a href=\\"https://sesac.seoul.kr/course\\">✅ 신청안내&nbsp;바로가기</a></p>",
                   "pstUrlAddr":null,"atchFile":"data:image/jpeg;base64,abcdef"}
                ]}}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.YOUTH_CONTENT,
                "https://www.youthcenter.go.kr/go/ythip/getContent?pageNum=1", "JSON", payload).getFirst();

        assertThat(activity.externalId()).isEqualTo("48:10580");
        assertThat(activity.title()).isEqualTo("AI활용 업무효율 극강 MICE기획자 양성과정 참여 안내");
        assertThat(activity.description()).isEqualTo("✅ 신청안내 바로가기");
        assertThat(activity.sourceUrl()).isEqualTo("https://sesac.seoul.kr/course");
        assertThat(activity.thumbnailUrl()).startsWith("data:image/jpeg;base64,");
        assertThat(activity.category()).isEqualTo(ActivityCategory.CAREER);
        assertThat(activity.activityType()).isEqualTo(ActivityType.PROGRAM);
    }

    @Test
    void normalize_filtersYouthNoticeNewsAndTips() {
        String payload = """
                {"result":{"youthPolicyList":[
                  {"bbsSn":"46","pstSn":"1","pstSeNm":"청년꿀팁","pstTtl":"[2025_온청 꿀팁] 지원사업 안내","pstWholCn":"내용"},
                  {"bbsSn":"54","pstSn":"2","pstSeNm":"공지","pstTtl":"시스템 공지","pstWholCn":"내용"},
                  {"bbsSn":"48","pstSn":"3","pstSeNm":"직업훈련","pstTtl":"실무 프로젝트 모집","pstWholCn":"<a href=\\"https://example.com\\">신청</a>"}
                ]}}
                """;

        List<NormalizedActivity> activities = normalizer.normalize(SourceType.YOUTH_CONTENT, "https://api", "JSON", payload);

        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().externalId()).isEqualTo("48:3");
    }

    @Test
    void normalize_removesStatusFromTargetAndMarksClosedInactive() {
        String payload = """
                {"data":[{"SVCID":"s1","SVCNM":"요가 클래스","SVCSTATNM":"예약마감","RCPTBGNDT":"20260501","RCPTENDDT":"20260502","USE_TRGT":"성인"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.SEOUL_RESERVATION, "https://api", "JSON", payload).getFirst();

        assertThat(activity.target()).isEqualTo("성인");
        assertThat(activity.active()).isFalse();
    }

    @Test
    void normalize_ignoresNumericDescription() {
        String payload = """
                {"data":[{"id":"n1","title":"직무 교육","description":"12345"}]}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.YOUTH_CONTENT, "https://api", "JSON", payload).getFirst();

        assertThat(activity.description()).isEqualTo("상세 설명은 원문에서 확인하세요.");
    }

    @Test
    void normalize_parsesSeoulCultureRows() {
        String payload = """
                {"culturalEventInfo":{"row":[
                  {"TITLE":"마티네콘서트","CODENAME":"클래식","ORG_LINK":"https://example.com/event",
                   "MAIN_IMG":"https://example.com/img.jpg","PLACE":"영등포아트홀","ORG_NAME":"영등포문화재단",
                   "INQUIRY":"02-2629-2250","USE_TRGT":"초등학생 이상","USE_FEE":"전석 15,000원",
                   "STRTDATE":"2026-10-15 00:00:00.0","END_DATE":"2026-10-15 00:00:00.0"}
                ]}}
                """;

        NormalizedActivity activity = normalizer.normalize(SourceType.SEOUL_CULTURE, "https://api", "JSON", payload).getFirst();

        assertThat(activity.title()).isEqualTo("마티네콘서트");
        assertThat(activity.sourceUrl()).isEqualTo("https://example.com/event");
        assertThat(activity.thumbnailUrl()).isEqualTo("https://example.com/img.jpg");
        assertThat(activity.organizer()).isEqualTo("영등포문화재단");
        assertThat(activity.contactInfo()).isEqualTo("02-2629-2250");
        assertThat(activity.target()).isEqualTo("초등학생 이상");
    }
}
