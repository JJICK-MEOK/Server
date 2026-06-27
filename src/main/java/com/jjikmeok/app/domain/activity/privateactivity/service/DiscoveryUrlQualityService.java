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

    private static final List<String> ADS = List.of("怨듬룞援щℓ", "?쇳븨紐?", "荑좏룿", "?좎씤", "泥댄뿕??");
    private static final List<String> NEWS = List.of("?댁뒪 湲곗궗", "釉붾줈洹?湲곗궗", "蹂대룄?먮즺");
    private static final List<String> COMMUNITY = List.of("?먯쑀寃뚯떆??", "?꾧린", "?듬챸寃뚯떆??", "而ㅻ??덊떚");
    private static final List<String> SEARCH = List.of("寃??寃곌낵", "?쒓렇 紐⑸줉", "移댄뀒怨좊━ 紐⑸줉");
    private static final List<String> ENDED = List.of("紐⑥쭛 留덇컧", "?좎껌 醫낅즺", "醫낅즺???됱궗", "吏???됱궗");
    private static final List<String> LOGIN = List.of("濡쒓렇??", "?뚯썝媛??", "sign in", "login required", "unauthorized", "private");
    private static final List<String> SIGNALS = List.of("紐⑥쭛以?", "李멸? ?좎껌", "?좎껌?섍린", "吏?먰븯湲?", "硫ㅻ쾭 紐⑥쭛", "?좉퇋 ?뚯썝 紐⑥쭛", "?곸떆 紐⑥쭛");
    private static final List<String> ACTIVITY = List.of("?대옒??", "?먮뜲???대옒??", "?뚰겕??", "?몃???", "媛뺤뿰", "?밴컯", "援먯쑁 ?꾨줈洹몃옩", "泥댄뿕 ?꾨줈洹몃옩", "?ㅽ꽣??", "?숈븘由?", "?낆꽌紐⑥엫", "?뚮え??", "而ㅻ??덊떚", "遊됱궗?쒕룞");
    private static final List<String> OPERATING = List.of("?뺢린 ?댁쁺", "留ㅼ＜ 吏꾪뻾", "?섏떆 紐⑥쭛", "?곸떆 紐⑥쭛");
    private static final List<String> TARGET = List.of("??숈깮", "泥?뀈", "吏곸옣??", "痍⑥???", "?ы쉶珥덈뀈??");
    private static final List<String> UNSUPPORTED_HOSTS = List.of("facebook.com", "x.com", "twitter.com", "onoffmix.com", "event-us.kr", "frip.co.kr", "munto.kr");
    private static final List<String> URL_ONLY_HOSTS = List.of("instagram.com", "band.us", "cafe.naver.com");
    private static final List<String> METADATA_HOSTS = List.of("blog.naver.com", "brunch.co.kr", "tistory.com", "notion.site");
    private static final List<String> FULL_CONTENT_HOST_SUFFIXES = List.of("go.kr", "or.kr", "ac.kr", "re.kr");
    private static final List<String> FULL_CONTENT_TEXT_HINTS = List.of("泥?뀈?쇳꽣", "臾명솕?щ떒", "援ъ껌", "?쒖껌");

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
