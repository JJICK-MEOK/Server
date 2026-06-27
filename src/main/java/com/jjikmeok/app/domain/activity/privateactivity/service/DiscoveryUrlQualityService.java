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

    private static final List<String> ADS = List.of("광고", "스폰서", "후원", "프로모션", "배너");
    private static final List<String> NEWS = List.of("뉴스", "기사", "보도", "속보");
    private static final List<String> COMMUNITY = List.of("커뮤니티", "게시판", "카페", "모임");
    private static final List<String> SEARCH = List.of("검색결과", "검색어", "검색", "찾으시는");
    private static final List<String> ENDED = List.of("마감", "종료", "신청 종료", "모집 종료");
    private static final List<String> LOGIN = List.of("로그인", "회원 전용", "sign in", "login required", "unauthorized", "private");
    private static final List<String> SIGNALS = List.of("모집", "신청", "접수", "참가", "예약");
    private static final List<String> ACTIVITY = List.of("활동", "프로그램", "강연", "체험", "행사", "클래스", "투어", "전시", "봉사");
    private static final List<String> OPERATING = List.of("운영", "진행", "주최", "주관");
    private static final List<String> TARGET = List.of("대상", "누구나", "청소년", "성인", "대학생", "어린이", "가족");
    private static final List<String> UNSUPPORTED_HOSTS = List.of("facebook.com", "x.com", "twitter.com", "onoffmix.com", "event-us.kr", "frip.co.kr", "munto.kr");
    private static final List<String> URL_ONLY_HOSTS = List.of("instagram.com", "band.us", "cafe.naver.com");
    private static final List<String> METADATA_HOSTS = List.of("blog.naver.com", "brunch.co.kr", "tistory.com", "notion.site");
    private static final List<String> FULL_CONTENT_HOST_SUFFIXES = List.of("go.kr", "or.kr", "ac.kr", "re.kr");
    private static final List<String> FULL_CONTENT_TEXT_HINTS = List.of("공공기관", "지자체", "교육청", "복지관");

    public Assessment evaluate(SearchResultDto searchResult) {
        String url = searchResult == null ? null : searchResult.url();
        String title = lower(searchResult == null ? null : searchResult.title());
        String snippet = lower(searchResult == null ? null : searchResult.snippet());
        String host = host(url);
        String text = join(title, snippet, lower(url));

        if (matchesAnyHost(host, UNSUPPORTED_HOSTS)) {
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
        if (matchesAnyHost(host, URL_ONLY_HOSTS)) {
            return ExtractionMode.URL_ONLY;
        }
        if (matchesAnyHost(host, METADATA_HOSTS)) {
            return ExtractionMode.METADATA_ONLY;
        }
        if (matchesAnyHost(host, FULL_CONTENT_HOST_SUFFIXES) || containsAny(text, FULL_CONTENT_TEXT_HINTS)) {
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

    private boolean matchesAnyHost(String host, List<String> domains) {
        if (host == null || host.isBlank()) {
            return false;
        }
        for (String domain : domains) {
            if (matchesHost(host, domain)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesHost(String host, String domain) {
        if (host == null || host.isBlank() || domain == null || domain.isBlank()) {
            return false;
        }
        String normalizedHost = lower(host);
        String normalizedDomain = lower(domain);
        return normalizedHost.equals(normalizedDomain) || normalizedHost.endsWith("." + normalizedDomain);
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
        if (matchesHost(host, "instagram.com")) return DiscoverySourceChannel.INSTAGRAM;
        if (matchesHost(host, "blog.naver.com")) return DiscoverySourceChannel.NAVER_BLOG;
        if (matchesHost(host, "cafe.naver.com")) return DiscoverySourceChannel.NAVER_CAFE;
        if (matchesHost(host, "band.us")) return DiscoverySourceChannel.BAND;
        if (matchesHost(host, "brunch.co.kr")) return DiscoverySourceChannel.BRUNCH;
        if (matchesHost(host, "tistory.com")) return DiscoverySourceChannel.TISTORY;
        if (matchesHost(host, "notion.site") || matchesHost(host, "notion.so")) return DiscoverySourceChannel.NOTION;
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
