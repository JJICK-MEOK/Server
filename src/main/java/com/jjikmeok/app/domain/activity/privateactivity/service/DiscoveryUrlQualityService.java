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

    private static final List<String> ADS = List.of("광고", "스폰서", "프로모션", "배너");
    private static final List<String> NEWS = List.of("뉴스", "기사", "보도", "언론");
    private static final List<String> COMMUNITY = List.of("커뮤니티", "게시판", "카페", "모임");
    private static final List<String> SEARCH = List.of("검색결과", "검색어", "검색", "찾으시는");
    private static final List<String> ENDED = List.of("마감", "종료", "접수 종료", "모집 종료");
    private static final List<String> LOGIN = List.of("로그인", "회원 전용", "sign in", "login required", "unauthorized", "private");
    private static final List<String> PROMOTIONAL = List.of("광고", "홍보", "체험", "후기", "이벤트", "마케팅", "상품 안내");
    private static final List<String> TRUST = List.of("공식", "주최", "주관", "운영", "센터", "재단", "기관", "협회", "대학");
    private static final List<String> ACTIVITY = List.of("모집", "신청", "원데이", "프로그램", "동아리", "행사", "강연", "전시", "워크숍", "클래스", "교육");
    private static final List<String> OPERATING = List.of("운영", "진행", "주최", "주관");
    private static final List<String> TARGET = List.of("대상", "청소년", "성인", "초등", "중등", "고등", "대학생", "직장인");
    private static final List<String> INSTAGRAM_OFFICIAL = List.of("공식", "official", "verified", "인증", "account", "accounts", "프로필", "계정");
    private static final List<String> INSTAGRAM_PROGRAM = List.of("모집", "신청", "프로그램", "동아리", "행사", "강연", "원데이", "클래스", "워크숍", "교육");

    private static final List<String> TRUSTED_SOCIAL_HOSTS = List.of(
            "band.us",
            "cafe.naver.com",
            "blog.naver.com",
            "brunch.co.kr",
            "tistory.com",
            "notion.site",
            "notion.so"
    );
    private static final List<String> UNSUPPORTED_HOSTS = List.of(
            "facebook.com",
            "x.com",
            "twitter.com",
            "onoffmix.com",
            "event-us.kr",
            "munto.kr"
    );
    private static final List<String> FULL_CONTENT_HOST_SUFFIXES = List.of("go.kr", "or.kr", "ac.kr", "re.kr");
    private static final List<String> FULL_CONTENT_TEXT_HINTS = List.of("공공기관", "지자체", "교육청", "문화재단");

    public Assessment evaluate(SearchResultDto searchResult) {
        String url = searchResult == null ? null : searchResult.url();
        String title = lower(searchResult == null ? null : searchResult.title());
        String snippet = lower(searchResult == null ? null : searchResult.snippet());
        String host = host(url);
        String text = join(title, snippet, lower(url));

        if (matchesAnyHost(host, UNSUPPORTED_HOSTS)) {
            return excluded(host, DiscoverySourceChannel.WEBSITE);
        }

        if (containsAny(text, ADS) || containsAny(text, NEWS) || containsAny(text, COMMUNITY) || containsAny(text, SEARCH) || containsAny(text, ENDED)) {
            return excluded(host, classifySourceChannel(host));
        }

        boolean trustedSocial = matchesAnyHost(host, TRUSTED_SOCIAL_HOSTS);
        boolean official = matchesAnyHost(host, FULL_CONTENT_HOST_SUFFIXES) || containsAny(text, FULL_CONTENT_TEXT_HINTS);
        boolean hasActivitySignal = containsAny(text, ACTIVITY);
        boolean hasTrustSignal = containsAny(text, TRUST);
        boolean hasOperatingSignal = containsAny(text, OPERATING);
        boolean hasTargetSignal = containsAny(text, TARGET);
        boolean promotional = containsAny(text, PROMOTIONAL);

        if (containsAny(text, LOGIN)) {
            return new Assessment(ExtractionMode.URL_ONLY, 0, true, host, classifySourceChannel(host));
        }

        if (matchesHost(host, "instagram.com")) {
            boolean officialInstagram = containsAny(text, INSTAGRAM_OFFICIAL);
            boolean programLike = containsAny(text, INSTAGRAM_PROGRAM) || hasActivitySignal;
            boolean trustLike = hasTrustSignal || hasOperatingSignal || hasTargetSignal;
            boolean instagramPost = isInstagramPostPath(url);
            if (!instagramPost || !officialInstagram || !programLike || !trustLike) {
                return excluded(host, DiscoverySourceChannel.INSTAGRAM);
            }
            if (promotional && !hasTrustSignal) {
                return excluded(host, DiscoverySourceChannel.INSTAGRAM);
            }
            double score = 42 + signalBonus(hasActivitySignal, hasTrustSignal, hasOperatingSignal, hasTargetSignal);
            return new Assessment(ExtractionMode.METADATA_ONLY, score, false, host, DiscoverySourceChannel.INSTAGRAM);
        }

        if (official) {
            double score = 24 + signalBonus(hasActivitySignal, hasTrustSignal, hasOperatingSignal, hasTargetSignal);
            if (promotional && !hasTrustSignal) {
                return excluded(host, classifySourceChannel(host));
            }
            return new Assessment(ExtractionMode.FULL_CONTENT, score, false, host, classifySourceChannel(host));
        }

        if (trustedSocial) {
            double score = 32 + signalBonus(hasActivitySignal, hasTrustSignal, hasOperatingSignal, hasTargetSignal);
            if (promotional && !hasTrustSignal) {
                return excluded(host, classifySourceChannel(host));
            }
            return new Assessment(ExtractionMode.METADATA_ONLY, score, false, host, classifySourceChannel(host));
        }

        if (!hasActivitySignal || promotional) {
            return excluded(host, DiscoverySourceChannel.WEBSITE);
        }

        if (!(hasTrustSignal || hasOperatingSignal || hasTargetSignal)) {
            return excluded(host, DiscoverySourceChannel.WEBSITE);
        }

        double score = 16 + signalBonus(hasActivitySignal, hasTrustSignal, hasOperatingSignal, hasTargetSignal);
        return new Assessment(ExtractionMode.METADATA_ONLY, score, false, host, DiscoverySourceChannel.WEBSITE);
    }

    private Assessment excluded(String host, DiscoverySourceChannel sourceChannel) {
        return new Assessment(ExtractionMode.URL_ONLY, 0, true, host, sourceChannel);
    }

    private double signalBonus(boolean hasActivitySignal, boolean hasTrustSignal, boolean hasOperatingSignal, boolean hasTargetSignal) {
        double score = 0;
        if (hasActivitySignal) score += 8;
        if (hasTrustSignal) score += 6;
        if (hasOperatingSignal) score += 4;
        if (hasTargetSignal) score += 4;
        return score;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(lower(keyword))) {
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

    private boolean isInstagramPostPath(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String path = lower(uri.getPath());
            return path.startsWith("/p/")
                    || path.startsWith("/reel/")
                    || path.startsWith("/tv/")
                    || path.startsWith("/share/");
        } catch (Exception e) {
            return false;
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
