package com.jjikmeok.app.domain.activity.privateactivity.service;

import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.DiscoverySourceChannel;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class DiscoveryUrlQualityService {

    private static final List<String> ADS = List.of("공동구매", "쇼핑몰", "쿠폰", "할인", "체험단");
    private static final List<String> NEWS = List.of("뉴스 기사", "블로그 기사", "보도자료");
    private static final List<String> COMMUNITY = List.of("자유게시판", "후기", "익명게시판", "커뮤니티");
    private static final List<String> SEARCH = List.of("검색 결과", "태그 목록", "카테고리 목록");
    private static final List<String> ENDED = List.of("모집 마감", "신청 종료", "종료된 행사", "지난 행사");
    private static final List<String> LOGIN = List.of("로그인", "회원가입", "sign in", "login required", "unauthorized", "private");
    private static final List<String> SIGNALS = List.of("모집중", "참가 신청", "신청하기", "지원하기", "멤버 모집", "신규 회원 모집", "상시 모집");
    private static final List<String> ACTIVITY = List.of("클래스", "원데이 클래스", "워크숍", "세미나", "강연", "특강", "교육 프로그램", "체험 프로그램", "스터디", "동아리", "독서모임", "소모임", "커뮤니티", "봉사활동");
    private static final List<String> OPERATING = List.of("정기 운영", "매주 진행", "수시 모집", "상시 모집");
    private static final List<String> TARGET = List.of("대학생", "청년", "직장인", "취준생", "사회초년생");
    private static final List<String> UNSUPPORTED_HOSTS = List.of("facebook.com", "x.com", "twitter.com", "onoffmix.com", "event-us.kr", "frip.co.kr", "munto.kr");
    private static final List<String> URL_ONLY_HOSTS = List.of("instagram.com", "band.us", "cafe.naver.com");
    private static final List<String> METADATA_HOSTS = List.of("blog.naver.com", "brunch.co.kr", "tistory.com", "notion.site");
    private static final List<String> FULL_CONTENT_HINTS = List.of("go.kr", "or.kr", "ac.kr", "re.kr", "청년센터", "문화재단", "구청", "시청");

    public Assessment evaluate(SearchResultDto searchResult) {
        String url = searchResult == null ? null : searchResult.url();
        String title = lower(searchResult == null ? null : searchResult.title());
        String snippet = lower(searchResult == null ? null : searchResult.snippet());
        String host = host(url);
        String text = join(title, snippet, lower(url));

        if (containsAny(host, UNSUPPORTED_HOSTS)) {
            return new Assessment(ExtractionMode.URL_ONLY, 0, true, host, DiscoverySourceChannel.WEBSITE);
        }

        if (containsAny(text, ADS) || containsAny(text, NEWS) || containsAny(text, COMMUNITY) || containsAny(text, SEARCH) || containsAny(text, ENDED)) {
            return new Assessment(ExtractionMode.URL_ONLY, 0, true, host, classifySourceChannel(host));
        }

        ExtractionMode mode = resolveMode(host, text);
        double score = switch (mode) {
            case FULL_CONTENT -> 20;
            case METADATA_ONLY -> 10;
            case URL_ONLY -> 0;
        };

        if (containsAny(text, SIGNALS)) score += 6;
        if (containsAny(text, ACTIVITY)) score += 6;
        if (containsAny(text, OPERATING)) score += 4;
        if (containsAny(text, TARGET)) score += 4;
        if (containsAny(text, LOGIN)) {
            mode = ExtractionMode.URL_ONLY;
            score -= 20;
        }

        return new Assessment(mode, score, false, host, classifySourceChannel(host));
    }

    private ExtractionMode resolveMode(String host, String text) {
        if (containsAny(text, LOGIN)) {
            return ExtractionMode.URL_ONLY;
        }
        if (containsAny(host, URL_ONLY_HOSTS)) {
            return ExtractionMode.URL_ONLY;
        }
        if (containsAny(host, METADATA_HOSTS)) {
            return ExtractionMode.METADATA_ONLY;
        }
        if (containsAny(host, FULL_CONTENT_HINTS) || containsAny(text, FULL_CONTENT_HINTS)) {
            return ExtractionMode.FULL_CONTENT;
        }
        return ExtractionMode.METADATA_ONLY;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(lower(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String host(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            return lower(uri.getHost());
        } catch (Exception e) {
            return "";
        }
    }

    private DiscoverySourceChannel classifySourceChannel(String host) {
        if (host == null || host.isBlank()) {
            return DiscoverySourceChannel.WEBSITE;
        }
        if (host.contains("instagram.com")) return DiscoverySourceChannel.INSTAGRAM;
        if (host.contains("blog.naver.com")) return DiscoverySourceChannel.NAVER_BLOG;
        if (host.contains("cafe.naver.com")) return DiscoverySourceChannel.NAVER_CAFE;
        if (host.contains("band.us")) return DiscoverySourceChannel.BAND;
        if (host.contains("brunch.co.kr")) return DiscoverySourceChannel.BRUNCH;
        if (host.contains("tistory.com")) return DiscoverySourceChannel.TISTORY;
        if (host.contains("notion.site") || host.contains("notion.so")) return DiscoverySourceChannel.NOTION;
        return DiscoverySourceChannel.WEBSITE;
    }

    public record Assessment(
            ExtractionMode extractionMode,
            double confidenceScore,
            boolean excluded,
            String platform,
            DiscoverySourceChannel sourceChannel
    ) {
    }
}
